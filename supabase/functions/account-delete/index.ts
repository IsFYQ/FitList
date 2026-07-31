// Supabase Edge Function: account-delete (REQ-002 / REQ-014)
// Deploy: supabase functions deploy account-delete
// Requires: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY (auto-injected)

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const USER_TABLES = [
  "analytics_events",
  "meal_entries",
  "weight_records",
  "daily_budgets",
  "foods",
  "goals",
  "profiles",
] as const;

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  if (req.method !== "POST") {
    return json({ error_code: "E4000", message: "Method not allowed" }, 405);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return json({ error_code: "E2001", message: "Unauthorized" }, 401);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) {
    return json({ error_code: "E2001", message: "Unauthorized" }, 401);
  }

  const userId = userData.user.id;
  const admin = createClient(supabaseUrl, serviceKey);

  for (const table of USER_TABLES) {
    const { error } = await admin.from(table).delete().eq("user_id", userId);
    if (error && error.code !== "PGRST116") {
      return json({ error_code: "E2010", message: "Delete failed" }, 500);
    }
  }

  const { error: authDeleteError } = await admin.auth.admin.deleteUser(userId);
  if (authDeleteError && !authDeleteError.message.includes("not found")) {
    return json({ error_code: "E2010", message: "Auth delete failed" }, 500);
  }

  return json({ deleted: true });
});

function json(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    },
  });
}
