# -*- coding: utf-8 -*-
"""
更新 nutridata 菜品库(id=2) CSV：
- 重新抓取每条菜品详情，取 major(配料克重) -> serving_grams(每份克重)
- 取 nutritionMap 营养值(原为单位菜品整份的数值)
- 换算为每100克基准：value / serving_grams * 100
- 输出列：食物名称,能量,蛋白质,脂肪,碳水化合物,serving_grams
特性：分页拉列表 -> 逐条详情；并发限速、异常重试、token 刷新、断点续跑、进度日志。
"""
import os, csv, json, time, threading, sys
sys.path.insert(0, "D:/project/cursor/diet_records")
import nutri_dish_export as N

OUT_DIR = "D:/project/cursor/diet_records/references-manual"
OUT_FILE = os.path.join(OUT_DIR, "nutridata_dishes_id2.csv")
CK = "D:/project/cursor/diet_records/dish_serving_ck.json"
DB_ID = 2

_lock = threading.Lock()

def extract_detail(d):
    """从详情响应提取 营养(原整份数值) + serving_grams(配料克重和)。"""
    if not isinstance(d, dict) or d.get("code") != 200:
        return None
    res = d.get("result", {})
    if not isinstance(res, dict):
        return None
    # 营养
    nutri = {}
    nm = res.get("nutritionMap")
    if isinstance(nm, dict):
        for cat, items in nm.items():
            if isinstance(items, list):
                for it in items:
                    if isinstance(it, dict) and it.get("name"):
                        nutri[it["name"]] = (it.get("value") or "").strip()
    energy = (res.get("calorie") or "").strip() or nutri.get("Calorie", "")
    mac = {
        "能量": energy,
        "蛋白质": nutri.get("Protein", ""),
        "脂肪": nutri.get("Fat", ""),
        "碳水化合物": nutri.get("Carbohydrate", ""),
    }
    # 每份克重 = 配料 note 之和
    major = res.get("major") or []
    serving = 0.0
    if isinstance(major, list):
        for m in major:
            if isinstance(m, dict):
                try:
                    serving += float(m.get("note") or 0)
                except Exception:
                    pass
    return {
        "name": (res.get("name") or "").strip(),
        "energy": mac["能量"], "protein": mac["蛋白质"],
        "fat": mac["脂肪"], "carb": mac["碳水化合物"],
        "serving": round(serving, 1),
    }

def fetch_details(meta):
    ck = {}
    if os.path.exists(CK):
        ck = json.load(open(CK, encoding="utf-8"))
        print("[detail] 已有检查点 %d 条，续跑。" % len(ck))
    todo = [m for m in meta if str(m["id"]) not in ck]
    smoke = int(os.environ.get("SMOKE_LIMIT", "0") or 0)
    if smoke:
        todo = todo[:smoke]
        print("[detail] SMOKE_LIMIT=%d，仅测前 %d 条。" % (smoke, len(todo)))
    print("[detail] 待抓取 %d 条 (总计 %d)。" % (len(todo), len(meta)))
    done = [len(ck)]
    ck_lock = threading.Lock()
    last = [time.time()]
    def worker(m):
        d = N.call(N.DETAIL_URL, {"id": m["id"]})
        ex = extract_detail(d)
        return m["id"], ex
    with N.ThreadPoolExecutor(max_workers=N.WORKERS) as ex:
        futs = {ex.submit(worker, m): m for m in todo}
        cnt = 0
        for fut in N.as_completed(futs):
            fid, ex = fut.result()
            cnt += 1
            with ck_lock:
                if ex is not None:
                    ck[str(fid)] = ex
                else:
                    ck[str(fid)] = {"name": "", "energy": "", "protein": "", "fat": "", "carb": "", "serving": 0.0}
                done[0] = len(ck)
                if cnt % 250 == 0 or (time.time() - last[0]) > 30:
                    json.dump(ck, open(CK, "w", encoding="utf-8"), ensure_ascii=False)
                    last[0] = time.time()
                    print("[detail] 进度 %d/%d" % (done[0], len(meta)))
    json.dump(ck, open(CK, "w", encoding="utf-8"), ensure_ascii=False)
    print("[detail] 完成，检查点 %d 条。" % len(ck))
    return ck

def to_num(s):
    try:
        return float(s)
    except Exception:
        return None

def per100(orig, serving):
    """orig: 原整份数值字符串; serving: 每份克重; 返回每100克字符串(2位小数)或 ''。"""
    if serving is None or serving <= 0:
        return ""
    v = to_num(orig)
    if v is None:
        return ""
    return str(round(v / serving * 100, 2))

def build_csv(meta, ck):
    os.makedirs(OUT_DIR, exist_ok=True)
    rows = []
    blank_serving = 0
    for m in meta:
        c = ck.get(str(m["id"]), {})
        serving = c.get("serving", 0.0)
        if not serving or serving <= 0:
            blank_serving += 1
        name = c.get("name") or m.get("name") or ""
        rows.append({
            "食物名称": name,
            "能量": per100(c.get("energy", ""), serving),
            "蛋白质": per100(c.get("protein", ""), serving),
            "脂肪": per100(c.get("fat", ""), serving),
            "碳水化合物": per100(c.get("carb", ""), serving),
            "serving_grams": ("" if not serving else ("%.1f" % serving)),
        })
    tmp = OUT_FILE + ".writing"
    with open(tmp, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["食物名称","能量","蛋白质","脂肪","碳水化合物","serving_grams"])
        w.writeheader()
        w.writerows(rows)
    os.replace(tmp, OUT_FILE)  # 原子替换，杜绝半成品写入
    print("[csv] 写出 %d 行 -> %s" % (len(rows), OUT_FILE))
    print("[csv] serving_grams 缺失条数: %d" % blank_serving)
    return len(rows)

def main():
    t0 = time.time()
    print("== 阶段1: 拉取列表元数据 ==")
    meta = N.fetch_list_meta()
    print("== 阶段2: 抓取详情(取每份克重+营养) ==")
    ck = fetch_details(meta)
    print("== 阶段3: 换算每100克并写 CSV ==")
    n = build_csv(meta, ck)
    print("总耗时 %.1f 分钟，导出 %d 行。" % ((time.time()-t0)/60, n))

if __name__ == "__main__":
    main()
