// Supabase Edge Function: food-search (REQ-006)
// Deploy: supabase functions deploy food-search
// Secrets: FATSECRET_CLIENT_ID, FATSECRET_CLIENT_SECRET

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const FATSECRET_TOKEN_URL = "https://oauth.fatsecret.com/connect/token";
const FATSECRET_API_URL = "https://platform.fatsecret.com/rest/server.api";
const OFF_SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl";
const UPSTREAM_TIMEOUT_MS = 3000;
const DAILY_QUOTA = 5000;
const QUOTA_RESERVE = 200;

type SourceStatus = "OK" | "TIMEOUT" | "FAILED" | "SKIPPED";

interface UnifiedItem {
  source: "FATSECRET" | "OFF";
  external_id: string;
  name: string;
  brand: string | null;
  basis_unit: "G" | "ML";
  kcal_per_100: number;
  protein_per_100: number | null;
  carb_per_100: number | null;
  fat_per_100: number | null;
  serving_name: string | null;
  serving_grams: number | null;
  data_incomplete: boolean;
  barcode: string | null;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  try {
    const url = new URL(req.url);
    const query = (url.searchParams.get("q") ?? "").trim();
    const barcode = url.searchParams.get("barcode");
    const pageSize = Math.min(Number(url.searchParams.get("page_size") ?? "20"), 20);

    if (!query && !barcode) {
      return json({ error_code: "E4001", message: "invalid query" }, 400);
    }
    if (query.length > 50) {
      return json({ error_code: "E4001", message: "invalid query" }, 400);
    }

    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return json({ error_code: "E2011" }, 401);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseAnon = Deno.env.get("SUPABASE_ANON_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseAnon, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userError } = await supabase.auth.getUser();
    if (userError || !userData.user) {
      return json({ error_code: "E2011" }, 401);
    }

    const quotaRemaining = await consumeQuota(supabaseUrl, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY"));
    const skipFatSecret = quotaRemaining <= 0;

    const [fatsecret, off] = await Promise.all([
      skipFatSecret
        ? Promise.resolve({ status: "SKIPPED" as SourceStatus, items: [] as UnifiedItem[] })
        : searchFatSecret(query, pageSize),
      searchOff(query, pageSize),
    ]);

    const items = [...fatsecret.items, ...off.items].slice(0, pageSize);
    const allFailed =
      (fatsecret.status === "FAILED" || fatsecret.status === "TIMEOUT" || fatsecret.status === "SKIPPED") &&
      (off.status === "FAILED" || off.status === "TIMEOUT");

    if (items.length === 0 && allFailed && !skipFatSecret) {
      return json({ error_code: "E4003" }, 502);
    }
    if (items.length === 0 && skipFatSecret && off.status !== "OK") {
      return json({ error_code: "E4002", quota_remaining: 0 }, 429);
    }

    return json({
      query,
      quota_remaining: quotaRemaining,
      sources: {
        fatsecret: { status: fatsecret.status, count: fatsecret.items.length },
        off: { status: off.status, count: off.items.length },
      },
      items,
    });
  } catch {
    return json({ error_code: "E4003" }, 502);
  }
});

async function consumeQuota(supabaseUrl: string, serviceKey: string | undefined): Promise<number> {
  if (!serviceKey) return DAILY_QUOTA;
  const utcDate = new Date().toISOString().slice(0, 10);
  const restUrl = `${supabaseUrl}/rest/v1/rpc/increment_api_quota`;
  try {
    const resp = await fetch(restUrl, {
      method: "POST",
      headers: {
        apikey: serviceKey,
        Authorization: `Bearer ${serviceKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ p_provider: "fatsecret", p_utc_date: utcDate, p_daily_limit: DAILY_QUOTA }),
    });
    if (resp.ok) {
      const remaining = await resp.json();
      if (typeof remaining === "number") return remaining;
    }
  } catch { /* fallback */ }
  return DAILY_QUOTA - 1;
}

async function searchFatSecret(query: string, max: number): Promise<{ status: SourceStatus; items: UnifiedItem[] }> {
  const clientId = Deno.env.get("FATSECRET_CLIENT_ID");
  const clientSecret = Deno.env.get("FATSECRET_CLIENT_SECRET");
  if (!clientId || !clientSecret) {
    return { status: "SKIPPED", items: [] };
  }
  try {
    const token = await withTimeout(getFatSecretToken(clientId, clientSecret), UPSTREAM_TIMEOUT_MS);
    const data = await withTimeout(fatSecretSearch(token, query, max), UPSTREAM_TIMEOUT_MS);
    return { status: "OK", items: data };
  } catch (e) {
    return { status: e instanceof TimeoutError ? "TIMEOUT" : "FAILED", items: [] };
  }
}

async function getFatSecretToken(clientId: string, clientSecret: string): Promise<string> {
  const basic = btoa(`${clientId}:${clientSecret}`);
  const resp = await fetch(FATSECRET_TOKEN_URL, {
    method: "POST",
    headers: {
      Authorization: `Basic ${basic}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: "grant_type=client_credentials&scope=basic",
  });
  if (!resp.ok) throw new Error("token failed");
  const json = await resp.json();
  return json.access_token as string;
}

async function fatSecretSearch(token: string, query: string, max: number): Promise<UnifiedItem[]> {
  const body = new URLSearchParams({
    method: "foods.search",
    search_expression: query,
    format: "json",
    max_results: String(max),
  });
  const resp = await fetch(FATSECRET_API_URL, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!resp.ok) throw new Error("search failed");
  const json = await resp.json();
  const raw = json?.foods?.food;
  const list = raw == null ? [] : Array.isArray(raw) ? raw : [raw];
  return list.map(parseFatSecretFood).filter(Boolean) as UnifiedItem[];
}

function parseFatSecretFood(food: Record<string, unknown>): UnifiedItem | null {
  const name = String(food.food_name ?? "").trim();
  const externalId = String(food.food_id ?? "");
  if (!name || !externalId) return null;
  const desc = String(food.food_description ?? "");
  const parsed = parsePer100FromDescription(desc);
  if (!parsed || parsed.kcal > 900) return null;
  return {
    source: "FATSECRET",
    external_id: externalId,
    name,
    brand: extractBrand(desc),
    basis_unit: "G",
    kcal_per_100: parsed.kcal,
    protein_per_100: parsed.protein,
    carb_per_100: parsed.carb,
    fat_per_100: parsed.fat,
    serving_name: null,
    serving_grams: parsed.servingGrams,
    data_incomplete: parsed.kcal <= 0,
    barcode: null,
  };
}

function parsePer100FromDescription(desc: string): {
  kcal: number; protein: number | null; carb: number | null; fat: number | null; servingGrams: number | null;
} | null {
  const per100 = /per\s*100\s*g/i.test(desc);
  const kcalMatch = desc.match(/calories:\s*([\d.]+)\s*kcal/i);
  const kcal = kcalMatch ? Number(kcalMatch[1]) : 0;
  const fat = num(desc, /fat:\s*([\d.]+)\s*g/i);
  const carb = num(desc, /carbs:\s*([\d.]+)\s*g/i);
  const protein = num(desc, /protein:\s*([\d.]+)\s*g/i);
  if (!per100 && kcal <= 0) return null;
  return { kcal, protein, carb, fat, servingGrams: per100 ? 100 : null };
}

async function searchOff(query: string, max: number): Promise<{ status: SourceStatus; items: UnifiedItem[] }> {
  try {
    const params = new URLSearchParams({
      search_terms: query,
      search_simple: "1",
      action: "process",
      json: "1",
      page_size: String(max),
    });
    const data = await withTimeout(
      fetch(`${OFF_SEARCH_URL}?${params}`).then((r) => r.json()),
      UPSTREAM_TIMEOUT_MS,
    );
    const products = data?.products ?? [];
    const items = products.map(parseOffProduct).filter(Boolean) as UnifiedItem[];
    return { status: "OK", items };
  } catch (e) {
    return { status: e instanceof TimeoutError ? "TIMEOUT" : "FAILED", items: [] };
  }
}

function parseOffProduct(p: Record<string, unknown>): UnifiedItem | null {
  const name = String(p.product_name ?? p.product_name_en ?? "").trim();
  if (!name) return null;
  const n = p.nutriments as Record<string, unknown> | undefined;
  const kcal = Number(n?.["energy-kcal_100g"] ?? n?.["energy-kcal"] ?? 0);
  if (kcal > 900) return null;
  const basisUnit = String(p.quantity ?? "").toLowerCase().includes("ml") ? "ML" : "G";
  const servingGrams = Number(p.serving_quantity ?? 0) || null;
  return {
    source: "OFF",
    external_id: String(p.code ?? p._id ?? name),
    name,
    brand: p.brands ? String(p.brands).split(",")[0].trim() : null,
    basis_unit: basisUnit as "G" | "ML",
    kcal_per_100: kcal,
    protein_per_100: numObj(n, "proteins_100g"),
    carb_per_100: numObj(n, "carbohydrates_100g"),
    fat_per_100: numObj(n, "fat_100g"),
    serving_name: p.serving_size ? String(p.serving_size) : null,
    serving_grams: servingGrams,
    data_incomplete: kcal <= 0,
    barcode: p.code ? String(p.code) : null,
  };
}

class TimeoutError extends Error {}

function withTimeout<T>(promise: Promise<T>, ms: number): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new TimeoutError()), ms);
    promise.then((v) => { clearTimeout(timer); resolve(v); }).catch((e) => { clearTimeout(timer); reject(e); });
  });
}

function num(text: string, re: RegExp): number | null {
  const m = text.match(re);
  return m ? Number(m[1]) : null;
}

function numObj(obj: Record<string, unknown> | undefined, key: string): number | null {
  if (!obj || obj[key] == null) return null;
  return Number(obj[key]);
}

function extractBrand(desc: string): string | null {
  const m = desc.match(/^[^:]+:\s*([^,]+)/);
  return m ? m[1].trim() : null;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Access-Control-Allow-Origin": "*" },
  });
}
