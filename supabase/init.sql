-- Health Check-in Supabase schema (v0.1 MVP)
-- Run in Supabase SQL Editor. Requires auth.users from Supabase Auth.

-- ============================================================
-- profiles
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    sex TEXT CHECK (sex IN ('MALE', 'FEMALE')),
    birth_year_month TEXT CHECK (birth_year_month ~ '^\d{4}-\d{2}$'),
    height_cm NUMERIC(4,1) CHECK (height_cm BETWEEN 100.0 AND 250.0),
    initial_weight_kg NUMERIC(4,1) CHECK (initial_weight_kg BETWEEN 25.0 AND 300.0),
    onboarding_completed_at TIMESTAMPTZ,
    registered_local_date DATE NOT NULL,
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles(user_id) WHERE deleted_at IS NULL;

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY profiles_select_own ON profiles FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY profiles_insert_own ON profiles FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY profiles_update_own ON profiles FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY profiles_delete_own ON profiles FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- goals
-- ============================================================
CREATE TABLE IF NOT EXISTS goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    current_weight_kg NUMERIC(4,1) NOT NULL CHECK (current_weight_kg BETWEEN 25.0 AND 300.0),
    target_weight_kg NUMERIC(4,1) NOT NULL CHECK (target_weight_kg BETWEEN 25.0 AND 300.0),
    target_weeks INT NOT NULL CHECK (target_weeks BETWEEN 4 AND 52),
    activity_level TEXT NOT NULL CHECK (activity_level IN ('SEDENTARY','LIGHT','MODERATE','ACTIVE','ATHLETE')),
    goal_type TEXT NOT NULL CHECK (goal_type IN ('LOSE','MAINTAIN','GAIN')),
    bmr_kcal INT NOT NULL,
    tdee_kcal INT NOT NULL,
    daily_delta_kcal INT NOT NULL,
    budget_kcal INT NOT NULL CHECK (budget_kcal BETWEEN 1000 AND 6000),
    protein_g NUMERIC(6,1) NOT NULL,
    carb_g NUMERIC(6,1) NOT NULL,
    fat_g NUMERIC(6,1) NOT NULL,
    clamped BOOLEAN NOT NULL DEFAULT false,
    est_weeks INT,
    effective_from DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_goals_active ON goals(user_id) WHERE is_active AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_user_id ON goals(user_id) WHERE deleted_at IS NULL;

ALTER TABLE goals ENABLE ROW LEVEL SECURITY;
CREATE POLICY goals_select_own ON goals FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY goals_insert_own ON goals FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY goals_update_own ON goals FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY goals_delete_own ON goals FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- daily_budgets
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    goal_id UUID NOT NULL REFERENCES goals(id),
    budget_kcal INT NOT NULL,
    protein_g NUMERIC(6,1) NOT NULL,
    carb_g NUMERIC(6,1) NOT NULL,
    fat_g NUMERIC(6,1) NOT NULL,
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(user_id, local_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_budgets_user_date ON daily_budgets(user_id, local_date) WHERE deleted_at IS NULL;

ALTER TABLE daily_budgets ENABLE ROW LEVEL SECURITY;
CREATE POLICY daily_budgets_select_own ON daily_budgets FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY daily_budgets_insert_own ON daily_budgets FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY daily_budgets_update_own ON daily_budgets FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY daily_budgets_delete_own ON daily_budgets FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- foods
-- ============================================================
CREATE TABLE IF NOT EXISTS foods (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    source TEXT NOT NULL CHECK (source IN ('CUSTOM','PUBLIC','FATSECRET','OFF')),
    external_id TEXT,
    name TEXT NOT NULL CHECK (char_length(name) BETWEEN 1 AND 50),
    name_normalized TEXT NOT NULL,
    brand TEXT CHECK (char_length(brand) <= 50),
    basis_unit TEXT NOT NULL CHECK (basis_unit IN ('G','ML')),
    kcal_per_100 NUMERIC(8,2) NOT NULL CHECK (kcal_per_100 BETWEEN 0 AND 900),
    protein_per_100 NUMERIC(8,2) CHECK (protein_per_100 BETWEEN 0 AND 100),
    carb_per_100 NUMERIC(8,2) CHECK (carb_per_100 BETWEEN 0 AND 100),
    fat_per_100 NUMERIC(8,2) CHECK (fat_per_100 BETWEEN 0 AND 100),
    serving_name TEXT CHECK (char_length(serving_name) <= 20),
    serving_grams NUMERIC(8,2) CHECK (serving_grams > 0),
    data_incomplete BOOLEAN NOT NULL DEFAULT false,
    nutrition_warning BOOLEAN NOT NULL DEFAULT false,
    ingredient_key TEXT,
    last_used_at TIMESTAMPTZ,
    use_count_30d INT NOT NULL DEFAULT 0,
    last_quantity NUMERIC(8,2),
    last_unit TEXT,
    last_meal_slot TEXT,
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(user_id, source, external_id)
);

CREATE INDEX IF NOT EXISTS idx_foods_user_normalized ON foods(user_id, name_normalized) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_foods_user_last_used ON foods(user_id, last_used_at DESC) WHERE deleted_at IS NULL;

ALTER TABLE foods ENABLE ROW LEVEL SECURITY;
CREATE POLICY foods_select_own ON foods FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY foods_insert_own ON foods FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY foods_update_own ON foods FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY foods_delete_own ON foods FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- public_foods (read-only for authenticated users)
-- ============================================================
CREATE TABLE IF NOT EXISTS public_foods (
    id UUID PRIMARY KEY,
    source TEXT NOT NULL DEFAULT 'PUBLIC',
    external_id TEXT,
    name TEXT NOT NULL,
    name_normalized TEXT NOT NULL,
    brand TEXT,
    basis_unit TEXT NOT NULL CHECK (basis_unit IN ('G','ML')),
    kcal_per_100 NUMERIC(8,2) NOT NULL,
    protein_per_100 NUMERIC(8,2),
    carb_per_100 NUMERIC(8,2),
    fat_per_100 NUMERIC(8,2),
    serving_name TEXT,
    serving_grams NUMERIC(8,2),
    data_incomplete BOOLEAN NOT NULL DEFAULT false,
    nutrition_warning BOOLEAN NOT NULL DEFAULT false,
    ingredient_key TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE public_foods ENABLE ROW LEVEL SECURITY;
CREATE POLICY public_foods_read_all ON public_foods FOR SELECT TO authenticated USING (true);

-- ============================================================
-- meal_entries
-- ============================================================
CREATE TABLE IF NOT EXISTS meal_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    tz_offset_minutes INT NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    meal_slot TEXT NOT NULL CHECK (meal_slot IN ('BREAKFAST','LUNCH','DINNER','SNACK')),
    food_id UUID REFERENCES foods(id) ON DELETE SET NULL,
    quantity NUMERIC(8,2) NOT NULL CHECK (quantity BETWEEN 0.1 AND 5000),
    unit TEXT NOT NULL CHECK (unit IN ('G','ML','SERVING')),
    basis_amount NUMERIC(8,2) NOT NULL,
    snap_food_name TEXT NOT NULL,
    snap_brand TEXT,
    snap_source TEXT NOT NULL,
    snap_basis_unit TEXT NOT NULL,
    snap_kcal_per_100 NUMERIC(8,2) NOT NULL,
    snap_protein_per_100 NUMERIC(8,2),
    snap_carb_per_100 NUMERIC(8,2),
    snap_fat_per_100 NUMERIC(8,2),
    snap_serving_name TEXT,
    snap_serving_grams NUMERIC(8,2),
    kcal NUMERIC(8,2) NOT NULL,
    protein_g NUMERIC(8,2),
    carb_g NUMERIC(8,2),
    fat_g NUMERIC(8,2),
    from_inventory BOOLEAN NOT NULL DEFAULT false,
    inventory_item_id UUID,
    inventory_deducted_amount NUMERIC(8,2),
    entry_source TEXT NOT NULL CHECK (entry_source IN ('SEARCH','RECENT','CUSTOM','RECOMMEND','OCR')),
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_meal_entries_user_date ON meal_entries(user_id, local_date) WHERE deleted_at IS NULL;

ALTER TABLE meal_entries ENABLE ROW LEVEL SECURITY;
CREATE POLICY meal_entries_select_own ON meal_entries FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY meal_entries_insert_own ON meal_entries FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY meal_entries_update_own ON meal_entries FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY meal_entries_delete_own ON meal_entries FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- weight_records
-- ============================================================
CREATE TABLE IF NOT EXISTS weight_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    local_date DATE NOT NULL,
    tz_offset_minutes INT NOT NULL,
    weight_kg NUMERIC(4,1) NOT NULL CHECK (weight_kg BETWEEN 25.0 AND 300.0),
    note TEXT CHECK (char_length(note) <= 100),
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_weight_records_user_date ON weight_records(user_id, local_date) WHERE deleted_at IS NULL;

ALTER TABLE weight_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY weight_records_select_own ON weight_records FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY weight_records_insert_own ON weight_records FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY weight_records_update_own ON weight_records FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY weight_records_delete_own ON weight_records FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- analytics_events
-- ============================================================
CREATE TABLE IF NOT EXISTS analytics_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    event_name TEXT NOT NULL,
    event_at TIMESTAMPTZ NOT NULL,
    local_date DATE NOT NULL,
    tz_offset_minutes INT NOT NULL,
    session_id TEXT NOT NULL,
    app_version TEXT NOT NULL,
    os_version TEXT NOT NULL,
    device_model TEXT NOT NULL,
    params JSONB NOT NULL DEFAULT '{}',
    device_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_analytics_events_user ON analytics_events(user_id, event_name, local_date);

ALTER TABLE analytics_events ENABLE ROW LEVEL SECURITY;
CREATE POLICY analytics_events_select_own ON analytics_events FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY analytics_events_insert_own ON analytics_events FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY analytics_events_update_own ON analytics_events FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY analytics_events_delete_own ON analytics_events FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- api_quota_counter (Edge Function food-search, service role only)
-- ============================================================
CREATE TABLE IF NOT EXISTS api_quota_counter (
    provider TEXT NOT NULL,
    utc_date DATE NOT NULL,
    count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (provider, utc_date)
);

-- Atomically increment daily quota and return remaining count.
CREATE OR REPLACE FUNCTION increment_api_quota(
    p_provider TEXT,
    p_utc_date DATE,
    p_daily_limit INT DEFAULT 5000
) RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_count INT;
BEGIN
    INSERT INTO api_quota_counter (provider, utc_date, count)
    VALUES (p_provider, p_utc_date, 1)
    ON CONFLICT (provider, utc_date)
    DO UPDATE SET count = api_quota_counter.count + 1
    RETURNING count INTO v_count;
    RETURN GREATEST(p_daily_limit - v_count, 0);
END;
$$;

REVOKE ALL ON api_quota_counter FROM PUBLIC;
GRANT SELECT, INSERT, UPDATE ON api_quota_counter TO service_role;
GRANT EXECUTE ON FUNCTION increment_api_quota TO service_role;
