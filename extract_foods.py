import re, json, base64, random, string, csv, os
from Crypto.Cipher import AES, PKCS1_v1_5
from Crypto.PublicKey import RSA
from Crypto.Util.Padding import pad, unpad
import urllib.request, urllib.error
from playwright.sync_api import sync_playwright

BASE = "https://www.nutridata.cn"
LIST_URL = BASE + "/api/nutri-service/ingredient/selectIngredientList"
COUNT_URL = BASE + "/api/nutri-service/ingredient/selectIngredientCount"
PAGE_URL = BASE + "/database/list?id=1&date=1786418138107"
OUT_DIR = "D:/project/cursor/diet_records/references-manual"
OUT_FILE = os.path.join(OUT_DIR, "nutridata_foods.csv")
PAGE_SIZE = 1000

# --- crypto (AES-128-ECB PKCS7, key sent RSA-encrypted in nutridata-random) ---
PUB_B64 = re.search(r'w="(MIGf[^"]+)"', open('D:/project/cursor/diet_records/app.js',encoding='utf-8').read()).group(1)
PUB_PEM = "-----BEGIN PUBLIC KEY-----\n"+PUB_B64+"\n-----END PUBLIC KEY-----"
def rsa_enc(s): return base64.b64encode(PKCS1_v1_5.new(RSA.import_key(PUB_PEM)).encrypt(s.encode())).decode()
def aes_enc(s, k): return base64.b64encode(AES.new(k.encode(),AES.MODE_ECB).encrypt(pad(s.encode(),16))).decode()
def aes_dec(b, k): return unpad(AES.new(k.encode(),AES.MODE_ECB).decrypt(base64.b64decode(b)),16).decode()

def get_token():
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
          const app=document.getElementById('app');
          const v=app && (app.__vue__||app.__vue_app__);
          if(v){ const t=dig(v,0); if(t) return t; }
          for(const wk of Object.keys(window)){ try{ const t=dig(window[wk],0); if(t) return t; }catch(e){} }
          return null;
        }""")
        browser.close()
        return token

def api_call(path, data, token):
    aes_key = ''.join(random.choices(string.ascii_letters+string.digits, k=16))
    body = aes_enc(json.dumps(data), aes_key)
    req = urllib.request.Request(path, data=body.encode(), headers={
        "User-Agent":"Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
        "Content-Type":"application/json;charset=UTF-8",
        "Accept":"application/json, text/plain, */*",
        "Referer":PAGE_URL,
        "nutridata-random":rsa_enc(aes_key),
        "nutridata-token":token})
    try:
        raw = urllib.request.urlopen(req, timeout=30).read().decode("utf-8","ignore")
    except urllib.error.HTTPError as e:
        return {"__error__":str(e),"__raw__":e.read().decode()[:200]}
    try:
        return json.loads(raw)
    except Exception:
        return json.loads(aes_dec(raw, aes_key))

def extract_nutrients(row):
    nutri = {}
    for it in row.get("nutrientList", []) or []:
        nutri[it.get("name")] = (it.get("value") or "").strip()
    return {
        "食物名称": (row.get("name") or "").strip(),
        "能量": nutri.get("Calorie", ""),
        "蛋白质": nutri.get("Protein", ""),
        "脂肪": nutri.get("Fat", ""),
        "碳水化合物": nutri.get("Carbohydrate", ""),
    }

def main():
    print("Fetching guest token via browser...")
    token = get_token()
    if not token:
        print("ERROR: could not obtain token")
        return
    print("Token obtained (len=%d)." % len(token))

    cnt = api_call(COUNT_URL, {"id": 1}, token)
    total = cnt.get("result") if isinstance(cnt, dict) else None
    print("Total count reported:", total)

    rows_out = []
    page = 1
    while True:
        r = api_call(LIST_URL, {"id": 1, "page": page, "pageSize": PAGE_SIZE}, token)
        if not isinstance(r, dict) or r.get("code") != 200:
            print("ERR page", page, json.dumps(r, ensure_ascii=False)[:300])
            break
        lst = r.get("result", {}).get("list", [])
        if not lst:
            print("Empty list at page", page, "-> stop.")
            break
        for row in lst:
            rows_out.append(extract_nutrients(row))
        print("Page %d: got %d rows (total collected %d)" % (page, len(lst), len(rows_out)))
        if len(rows_out) >= (total or 10**9):
            break
        if len(lst) < PAGE_SIZE:
            break
        page += 1

    os.makedirs(OUT_DIR, exist_ok=True)
    with open(OUT_FILE, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["食物名称","能量","蛋白质","脂肪","碳水化合物"])
        w.writeheader()
        w.writerows(rows_out)
    print("WROTE", len(rows_out), "rows to", OUT_FILE)
    if total and len(rows_out) != total:
        print("WARNING: collected %d != reported total %d" % (len(rows_out), total))

if __name__ == "__main__":
    main()
