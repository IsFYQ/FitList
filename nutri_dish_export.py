# -*- coding: utf-8 -*-
"""
抓取 nutridata 菜品库 (id=2) 全部 22180 条食物数据。
- 列表接口 dish/selectFoodList 仅含 食物名称 + 能量(calorie)
- 详情接口 dish/selectFoodById 含 蛋白质/脂肪/碳水化合物(nutritionMap)
复刻前端 AES-128-ECB + RSA(nutridata-random) 加密，自动游客登录拿 token。
特性：分页拉全列表 -> 逐条详情；并发限速、异常重试、token 刷新、断点续跑、进度日志。
"""
import re, json, base64, random, string, time, os, csv, threading, sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from Crypto.Cipher import AES, PKCS1_v1_5
from Crypto.PublicKey import RSA
from Crypto.Util.Padding import pad, unpad
import urllib.request, urllib.error
from playwright.sync_api import sync_playwright

BASE = "https://www.nutridata.cn"
COUNT_URL  = BASE + "/api/nutri-service/dish/selectFoodCount"
LIST_URL   = BASE + "/api/nutri-service/dish/selectFoodList"
DETAIL_URL = BASE + "/api/nutri-service/dish/selectFoodById"
PAGE_URL   = BASE + "/database/list?id=2&date=1786414600983"
DB_ID = 2
OUT_DIR = "D:/project/cursor/diet_records/references-manual"
OUT_FILE = os.path.join(OUT_DIR, "nutridata_dishes_id2.csv")
LIST_META = "D:/project/cursor/diet_records/dish_list_meta.json"
DETAIL_CK = "D:/project/cursor/diet_records/dish_details_ck.json"

PUB = open("D:/project/cursor/diet_records/nutri_pubkey.txt", encoding="utf-8").read().strip()
PUB_PEM = "-----BEGIN PUBLIC KEY-----\n"+PUB+"\n-----END PUBLIC KEY-----"

UAS = [
 "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
 "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36",
 "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
 "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0 Safari/537.36",
]
WORKERS = 8
MIN_INTERVAL = 0.05  # 全局最小请求间隔(秒)，限流反爬

_lock = threading.Lock()
_token = [None]
_last_req = [0.0]

def rsa_enc(s): return base64.b64encode(PKCS1_v1_5.new(RSA.import_key(PUB_PEM)).encrypt(s.encode())).decode()
def aes_enc(s, k): return base64.b64encode(AES.new(k.encode(), AES.MODE_ECB).encrypt(pad(s.encode(), 16))).decode()
def aes_dec(b, k): return unpad(AES.new(k.encode(), AES.MODE_ECB).decrypt(base64.b64decode(b)), 16).decode()

def refresh_token():
    with sync_playwright() as p:
        browser = p.chromium.launch(channel="chrome", headless=True, args=["--no-sandbox","--disable-dev-shm-usage"])
        page = browser.new_page()
        page.goto(PAGE_URL, wait_until="load", timeout=60000)
        page.wait_for_timeout(4000)
        token = page.evaluate("""() => {
          function dig(o,d){ if(d>6) return null;
            if(o && typeof o==='object'){ for(const k of Object.keys(o)){
              if(k==='token' && typeof o[k]==='string' && o[k].length>20) return o[k];
              const r=dig(o[k],d+1); if(r) return r; } }
            return null; }
          const app=document.getElementById('app'); const v=app && (app.__vue__||app.__vue_app__);
          if(v){ const t=dig(v,0); if(t) return t; }
          for(const wk of Object.keys(window)){ try{ const t=dig(window[wk],0); if(t) return t; }catch(e){} }
          return null;
        }""")
        browser.close()
        return token

def get_token():
    if _token[0] is None:
        _token[0] = refresh_token()
    return _token[0]

def call(path, data, retries=5):
    """带限流、重试的加密请求。返回解析后的 dict（解密失败返回 {'__error__':...}）。"""
    global _last_req
    for attempt in range(retries):
        with _lock:
            now = time.time()
            wait = MIN_INTERVAL - (now - _last_req[0])
            if wait > 0:
                time.sleep(wait)
            _last_req[0] = time.time()
            aes_key = ''.join(random.choices(string.ascii_letters+string.digits, k=16))
            body = aes_enc(json.dumps(data), aes_key)
            ua = random.choice(UAS)
            req = urllib.request.Request(path, data=body.encode(), headers={
                "User-Agent": ua,
                "Content-Type": "application/json;charset=UTF-8",
                "Accept": "application/json, text/plain, */*",
                "Referer": PAGE_URL,
                "nutridata-random": rsa_enc(aes_key),
                "nutridata-token": get_token()})
        try:
            raw = urllib.request.urlopen(req, timeout=30).read().decode("utf-8", "ignore")
        except urllib.error.HTTPError as e:
            err = e.read().decode("utf-8", "ignore")[:200]
            # 登录过期 -> 刷新 token 后重试
            if e.code in (401, 403) or "过期" in err or "登录" in err:
                with _lock:
                    _token[0] = refresh_token()
            time.sleep(1.5 * (attempt+1))
            continue
        except Exception as e:
            time.sleep(1.0 * (attempt+1))
            continue
        try:
            return json.loads(raw)
        except Exception:
            try:
                return json.loads(aes_dec(raw, aes_key))
            except Exception:
                time.sleep(1.0 * (attempt+1))
                continue
    return {"__error__": "max retries exceeded"}

def fetch_list_meta():
    """分页拉全列表元数据 {id, name, calorie}。结果缓存到 LIST_META，可断点续跑。"""
    if os.path.exists(LIST_META):
        meta = json.load(open(LIST_META, encoding="utf-8"))
        print("[list] 已有缓存 %d 条，校验计数..." % len(meta))
        cnt = call(COUNT_URL, {"id": DB_ID})
        total = cnt.get("result") if isinstance(cnt, dict) else None
        if total and len(meta) >= total:
            print("[list] 缓存完整 (%d >= %s)，跳过。" % (len(meta), total))
            return meta
        else:
            print("[list] 缓存不完整，重新拉取。")
    meta = []
    page = 1
    PAGE_SIZE = 500
    while True:
        r = call(LIST_URL, {"id": DB_ID, "page": page, "pageSize": PAGE_SIZE})
        if not isinstance(r, dict) or r.get("code") != 200:
            print("[list] ERR page", page, str(r)[:200])
            if page == 1:
                raise RuntimeError("列表首页失败: " + str(r)[:200])
            time.sleep(3); continue
        res = r.get("result", {})
        lst = res.get("list", []) if isinstance(res, dict) else []
        if not lst:
            break
        for row in lst:
            meta.append({"id": row.get("id"), "name": (row.get("name") or "").strip(),
                         "calorie": (row.get("calorie") or "").strip()})
        print("[list] page %d: +%d (累计 %d)" % (page, len(lst), len(meta)))
        if len(lst) < PAGE_SIZE:
            break
        page += 1
        time.sleep(0.1)
    json.dump(meta, open(LIST_META, "w", encoding="utf-8"), ensure_ascii=False)
    print("[list] 共 %d 条，已缓存。" % len(meta))
    return meta

def extract_macros(detail):
    """从详情的 nutritionMap 提取 蛋白质/脂肪/碳水化合物（及能量兜底）。"""
    res = detail.get("result", {}) if isinstance(detail, dict) else {}
    if not isinstance(res, dict):
        return {}
    nutri = {}
    nm = res.get("nutritionMap")
    if isinstance(nm, dict):
        for cat, items in nm.items():
            if isinstance(items, list):
                for it in items:
                    if isinstance(it, dict) and it.get("name"):
                        nutri[it["name"]] = (it.get("value") or "").strip()
    return {
        "能量": nutri.get("Calorie", ""),
        "蛋白质": nutri.get("Protein", ""),
        "脂肪": nutri.get("Fat", ""),
        "碳水化合物": nutri.get("Carbohydrate", ""),
    }

def fetch_details(meta):
    """并发抓取详情，断点续跑（已完成的 id 跳过），定期写检查点。"""
    ck = {}
    if os.path.exists(DETAIL_CK):
        ck = json.load(open(DETAIL_CK, encoding="utf-8"))
        print("[detail] 检查点已有 %d 条，续跑。" % len(ck))
    todo = [m for m in meta if str(m["id"]) not in ck]
    smoke = int(os.environ.get("SMOKE_LIMIT", "0") or 0)
    if smoke:
        todo = todo[:smoke]
        print("[detail] SMOKE_LIMIT=%d，仅测前 %d 条。" % (smoke, len(todo)))
    print("[detail] 待抓取 %d 条 (总计 %d)。" % (len(todo), len(meta)))

    done_count = [len(ck)]
    ck_lock = threading.Lock()
    last_save = [time.time()]

    def worker(m):
        d = call(DETAIL_URL, {"id": m["id"]})
        mac = extract_macros(d) if isinstance(d, dict) and d.get("code") == 200 else {}
        return m["id"], mac

    with ThreadPoolExecutor(max_workers=WORKERS) as ex:
        futs = {ex.submit(worker, m): m for m in todo}
        for i, fut in enumerate(as_completed(futs), 1):
            fid, mac = fut.result()
            with ck_lock:
                ck[str(fid)] = mac
                done_count[0] = len(ck)
                if i % 250 == 0 or (time.time() - last_save[0]) > 30:
                    json.dump(ck, open(DETAIL_CK, "w", encoding="utf-8"), ensure_ascii=False)
                    last_save[0] = time.time()
                    print("[detail] 进度 %d/%d (已存检查点)" % (done_count[0], len(meta)))
    json.dump(ck, open(DETAIL_CK, "w", encoding="utf-8"), ensure_ascii=False)
    print("[detail] 全部完成，检查点 %d 条。" % len(ck))
    return ck

def build_csv(meta, ck):
    os.makedirs(OUT_DIR, exist_ok=True)
    rows = []
    miss = 0
    for m in meta:
        mac = ck.get(str(m["id"]), {})
        energy = (m.get("calorie") or "").strip() or mac.get("能量", "")
        prot = mac.get("蛋白质", "")
        fat = mac.get("脂肪", "")
        carb = mac.get("碳水化合物", "")
        if not prot and not fat and not carb:
            miss += 1
        rows.append({
            "食物名称": m.get("name", ""),
            "能量": energy,
            "蛋白质": prot,
            "脂肪": fat,
            "碳水化合物": carb,
        })
    with open(OUT_FILE, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["食物名称","能量","蛋白质","脂肪","碳水化合物"])
        w.writeheader()
        w.writerows(rows)
    print("[csv] 写出 %d 行 -> %s" % (len(rows), OUT_FILE))
    print("[csv] 三条宏量营养素全缺的菜品数: %d" % miss)
    return len(rows)

def main():
    t0 = time.time()
    print("== 阶段1: 拉取列表元数据 ==")
    meta = fetch_list_meta()
    print("== 阶段2: 并发抓取详情 ==")
    ck = fetch_details(meta)
    print("== 阶段3: 组装 CSV ==")
    n = build_csv(meta, ck)
    print("总耗时 %.1f 分钟，导出 %d 行。" % ((time.time()-t0)/60, n))

if __name__ == "__main__":
    main()
