# 健康打卡 App — 开发提示词工具箱

> 配套 PRD: `prd-health-checkin-official-v2.0-2026-07-29.md`（v2.0 正式版）
> 配套路线图: `roadmap-health-checkin-mvp-2026-07-29.md`
> 用途: 将标准化开发提示词直接复制给 AI 模型（Claude、GPT、Gemini 等）启动 vibe coding
> 使用方式: 选择对应阶段的提示词，替换标记为 `{...}` 的占位符后使用

---

# 一、整体项目上下文提示词

> 用途: 在新对话中让模型快速理解整个项目。建议作为**每个阶段的第一条消息**发送。
> 适用模型: 所有支持长上下文的模型（Claude、GPT-4+、Gemini 等）

## 1.1 完整版（推荐用于首次对话）

```
你是一位 Android 全栈开发专家，精通 Kotlin + Jetpack Compose + Supabase + Room 技术栈。我将向你提供一份完整的产品需求文档，你需要基于它开发一个健康管理 App。

## 项目概况

- **项目名**: 健康打卡（Health Check-in）
- **产品定位**: Android 健康管理应用，核心聚焦「极低摩擦的饮食记录」与「采购→库存→消耗全链路闭环」
- **版本**: v0.1 MVP（P0），14 条需求，40 人天，预计 9 周
- **开发方式**: 单人全栈 vibe coding（AI 辅助）
- **商业模式**: 完全免费，无广告，无订阅

## 技术栈（必须严格遵守）

| 层 | 技术选型 | 约束 |
|---|---|---|
| 客户端 | Android 原生 / Kotlin / Jetpack Compose / Material 3 | minSdk 26、targetSdk 35 |
| 本地存储 | Room（SQLite） | **本地为唯一真源** |
| 依赖注入 | Hilt | — |
| 网络 | Retrofit + OkHttp + kotlinx.serialization | — |
| 图表 | Vico（Compose 图表库） | 不自行实现 Canvas 绘制 |
| 认证 | Supabase Auth（邮箱+密码） | — |
| 云端存储 | Supabase PostgreSQL | 仅标准 PostgreSQL 特性；禁用专有 SQL 扩展 |
| 服务端代理 | Supabase Edge Function（仅 2 个，无状态纯转发） | 仅用于 FatSecret API 代理和账号注销 |
| OCR（P2） | Google ML Kit Text Recognition v2（中文模型，端侧） | 不上传图片 |

## 架构决策（全局生效，不可违反）

### 存储与同步（决策 D-05）
- **本地优先**: Room 为唯一真源。所有 UI 通过 Room Flow 订阅数据，不直接消费网络响应。
- **写入路径**: 写操作 → 校验 → Room 事务提交 → 立即刷新 UI（乐观更新）→ 入备份队列。**网络失败绝不影响写入成功**。
- **后台备份**: 进入后台/每日首次启动/手动触发时单向上传到 Supabase。这不是双向同步！
- **主键**: 所有业务表主键为客户端生成的 UUID v7，禁止使用数据库自增主键。
- **幂等**: 云端写入统一使用 `INSERT ... ON CONFLICT (id) DO UPDATE`。
- **软删除**: 所有业务表使用 `deleted_at` 软删除。
- **离线可用**: 除食物远程搜索、云端备份/恢复、邮箱验证外，全部功能离线可用。

### 认证
- 邮箱注册即登录，邮箱验证异步非阻断（决策 D-01）
- Token 存储于 `EncryptedSharedPreferences`（AES-256-GCM），严禁写入 Room 或日志
- 用户密码仅内存中传输给 Supabase Auth，不本地持久化

### 第三方 API
- FatSecret 密钥必须经 Supabase Edge Function 代理，严禁打包进 APK（决策 D-03）
- 所有网络请求仅允许 HTTPS（TLS 1.2+）

## 全局规则（影响所有需求的实现）

### 时间与日期
- 时区基准 = 设备本地时区。自然日 = 本地时间 [00:00, 23:59:59.999]
- 所有时间戳以 UTC 存储（Long epoch millis），聚合查询使用 local_date 字段
- 用户可编辑的日期字段禁止选择未来日期
- 历史补录日期下限 = max(注册日期, 今日-365天)

### 单位与换算
- 营养基准统一为每 100g（固态）或 100ml（液态）存储
- 饮食记录 unit ∈ {G, ML, SERVING}，SERVING 必须绑定 serving_grams
- 库存 unit ∈ {G, KG, ML, L, PIECE}，PIECE 必须绑定 piece_grams
- 体重 kg（1位小数）、身高 cm（1位小数）、身体维度 cm（1位小数）

### 数值精度
- 存储: 热量/宏量 Double（2位小数）
- 展示: 热量整数、宏量 1位小数、百分比整数
- 取整: 一律 HALF_UP
- 聚合: 先按 2 位小数精度累加，再对结果取整展示（禁止逐条取整后累加）

### 输入校验
- 失焦时 + 提交时双校验，提交按钮在有校验错误时置灰
- 所有文本输入在校验与保存前执行 trim()，连续空白折叠为单个空格
- 食物名称 1-50 字符，邮箱 ≤ 254 字符

### 网络与重试
- 连接超时 5s、读取超时 10s
- 仅对幂等请求（GET、带幂等键的 UPSERT）自动重试，退避 1s→3s→9s，最多 3 次
- 离线判定以 `NetworkCapabilities.NET_CAPABILITY_VALIDATED` 为准

### 文案规范
- 全部用户可见文案定义于 strings.xml，禁止硬编码
- 空态文案用行动引导句式，禁止用「暂无数据」(如「记录你的第一餐吧」)
- 错误文案 = 「发生了什么 + 可以怎么做」，不暴露技术细节
- 健康提示用中性表述（「今日摄入已超出预算 320 大卡」而非「你吃太多了」）
- 数字与单位之间不加空格（如「1,680大卡」）

## 需求清单（P0，14 条）

| 编号 | 需求 | 人天 | 依赖 |
|---|---|---|---|
| REQ-001 | 邮箱注册、登录与会话管理 | 3.0 | — |
| REQ-002 | 账号安全与生命周期 | 2.0 | REQ-001 |
| REQ-003 | 身体档案与目标设定 | 4.0 | REQ-001 |
| REQ-004 | 仪表盘首页 | 5.0 | REQ-003, REQ-011 |
| REQ-005 | 饮食记录（新增/编辑/删除/补录） | 6.5 | REQ-006, REQ-011 |
| REQ-006 | 食物搜索（三层数据源） | 4.0 | REQ-011 |
| REQ-007 | 最近与常吃食物 | 1.0 | REQ-005 |
| REQ-008 | 自建食物管理 | 2.0 | REQ-006 |
| REQ-009 | 体重记录与曲线 | 3.0 | REQ-003 |
| REQ-010 | 健康提示与预警 | 1.0 | REQ-004, REQ-005 |
| REQ-011 | 本地优先存储与云端备份 | 4.0 | REQ-001 |
| REQ-012 | 数据导出 | 1.5 | REQ-011 |
| REQ-013 | 埋点采集 | 2.0 | REQ-011 |
| REQ-014 | 设置中心与关于 | 1.0 | REQ-001 |

## 关键算法（已完整定义，可直接实现）

### BMR（Mifflin-St Jeor）
```
MALE:   BMR = 10 × weight_kg + 6.25 × height_cm − 5 × age_years + 5
FEMALE: BMR = 10 × weight_kg + 6.25 × height_cm − 5 × age_years − 161
BMR_final = round(BMR)  // HALF_UP，< 800 钳制为 800
```

### 每日热量预算
```
TDEE = round(BMR × PAL)
// PAL: SEDENTARY=1.200, LIGHT=1.375, MODERATE=1.550, ACTIVE=1.725, ATHLETE=1.900

Δw = target_weight_kg − current_weight_kg
goal_type = LOSE (Δw < −0.5) | GAIN (Δw > 0.5) | MAINTAIN

raw_delta = 7700 × |Δw| / (target_weeks × 7)
daily_delta = LOSE → −min(raw_delta, 1000) | GAIN → +min(raw_delta, 500) | MAINTAIN → 0
budget = round(max(TDEE + daily_delta, safety_floor))
// safety_floor: MALE=1500, FEMALE=1200
```

### 餐次自动推断（24小时无空隙）
```
[04:00, 10:30) → BREAKFAST
[10:30, 14:30) → LUNCH
[14:30, 17:00) → SNACK
[17:00, 21:30) → DINNER
[21:30, 04:00) → SNACK
```

### 食物搜索匹配度评分
```
score = 0.45 × nameMatch + 0.25 × sourceWeight + 0.20 × personalRecency + 0.10 × dataCompleteness
// 排序: score 降序 → last_used_at 降序 → name 升序
```

### 热量颜色判定
```
remaining = budget − consumed
ratio = remaining / budget (budget > 0)
绿色: ratio > 0.25 | 黄色: 0 < ratio ≤ 0.25 | 橙色: −0.25 < ratio ≤ 0 | 红色: ratio ≤ −0.25
```

## 15 张数据表

核心表: profiles(用户档案), goals(目标设定), daily_budgets(每日预算快照), weight_records(体重), meal_entries(饮食记录), foods(食物库), public_foods(预建中式食物), food_search_cache(搜索缓存), inventory_items(库存), inventory_logs(库存流水), body_measurements(身体维度), milestones(里程碑), analytics_events(埋点), backup_state(备份状态), streak_records(连续天数)

所有含 user_id 的表必须启用 Supabase Row Level Security（4 条策略: select_own/insert_own/update_own/delete_own），public_foods 例外（仅 SELECT）。

## Non-goals（明确不做）
社交功能、AI 教练、复杂运动跟踪（GPS/心率/手环）、多人协作、膳食计划、条形码扫描、水摄入记录、断食计时器、订阅/付费墙、国际化、深色模式（暂不做）、Apple Health/Google Fit、实时双向同步、数据导入

---

现在我需要你理解以上项目概览。接下来我会提供具体阶段的需求详情，你要严格按 PRD 规格实现。

有任何疑问请先提出，不要自行假设。
```

## 1.2 精简版（用于后续阶段，省 token）

```
继续开发「健康打卡」App。技术栈: Android + Kotlin + Jetpack Compose + Material 3 + Room(本地唯一真源) + Supabase(PostgreSQL，仅备份) + Hilt + Retrofit + Vico 图表库。

架构原则: 本地优先、UUID v7 主键、软删除、EncryptedSharedPreferences 存 token、HALF_UP 精度。

全局规则: 设备本地时区、营养基准 per 100g(ml)、体重 kg 1位小数、热量展示整数、输入 trim 后校验、失焦+提交双校验、空态用行动引导不用「暂无数据」、错误文案不暴露技术细节。

详细规格见完整 PRD，我会给你本次要开发的需求的具体内容。
```

---

# 二、分阶段开发提示词

## Phase 0: 项目初始化与基础架构（Week 0，第 0 周）

### 提示词 0-A: 搭建 Android 项目脚手架

```
## 任务: 搭建健康打卡 Android 项目脚手架

### 1. 创建项目
- 使用 Android Studio 创建项目，包名: `com.example.healthcheckin`
- Kotlin DSL (build.gradle.kts)
- minSdk 26、targetSdk 35、compileSdk 35
- 不使用 New Build System（BuildConfig 用于存放 Supabase URL 和 anon key）

### 2. 依赖配置（使用 Version Catalog libs.versions.toml）
必须包含且版本锁定:
- Jetpack Compose BOM (最新稳定版)
- Material 3
- Room (runtime + ktx + compiler)
- Hilt (android + compiler + navigation-compose)
- Retrofit + OkHttp + kotlinx.serialization
- Navigation Compose
- DataStore Preferences
- Vico (compose 图表库)
- Supabase Kotlin SDK (或直接用 Retrofit 调 PostgREST — 推荐后者，避免 SDK 锁定)
- kotlinx-datetime

### 3. 项目基础结构（按 Clean Architecture 分层）

```
:app/
├── di/          # Hilt 模块（Room、网络、Repository）
├── data/
│   ├── local/   # Room DAO、Entity、Database、Converters、Migrations
│   ├── remote/  # Supabase API 接口（Retrofit）、Edge Function 接口
│   └── repository/  # Repository 实现
├── domain/
│   ├── model/   # 领域模型（与 Entity 分离的数据类）
│   ├── algorithm/  # 核心算法（BMR、TDEE、宏量分配、餐次推断等）
│   └── repository/  # Repository 接口
├── ui/
│   ├── navigation/   # NavHost、Screen sealed class
│   ├── theme/        # Material 3 Theme、Color、Type
│   ├── screens/      # 按页面分包（auth/dashboard/meal/etc）
│   └── components/   # 共享 UI 组件
├── util/          # 工具类（日期、单位换算、数值精度、校验器）
├── di/            # AppModule、DatabaseModule、NetworkModule
└── HealthCheckInApp.kt  # Application 类，@HiltAndroidApp
```

### 4. 核心工具类（必须实现，后续所有需求依赖）

#### 4.1 日期工具 `DateTimeUtil.kt`
```kotlin
// 获取今日本地日期 local_date (YYYY-MM-DD)
// 获取当前 UTC epoch millis
// 日期转 epoch millis 范围（当天 00:00-23:59.999）
// 年龄计算（从 YYYY-MM 出生年月+今日）
// 判断是否为未来日期
// 历史补录下限（max(注册日期, 今日-365)）
```

#### 4.2 单位换算工具 `UnitConverter.kt`
```kotlin
// KG ↔ G、L ↔ ML 换算
// 营养值换算: 每100基准 × basis_amount ÷ 100
// PIECE → grams（需 piece_grams）
// SERVING → grams（需 serving_grams）
// 外部 API 结果换算为 per-100
```

#### 4.3 数值精度工具 `PrecisionUtil.kt`
```kotlin
// roundToDisplay(热量: Double, 0位小数, HALF_UP)
// roundToDisplay(宏量: Double, 1位小数, HALF_UP)
// 千分位格式化: 1680 → "1,680"
// 百分比: 0.253 → "25%"
// 聚合: 先 2 位小数累加再取整展示
```

#### 4.4 校验器 `Validators.kt`
```kotlin
// 邮箱格式校验 (RFC 5322 简化)
// 密码强度: ≥8 字符 + 至少 1 字母 1 数字（客户端宽松校验，Supabase 为准）
// 文本长度: food_name 1-50、email ≤ 254、描述 ≤ 200
// 数值范围: weight 20-500 kg, height 50-300 cm
// trim + 连续空白折叠 = 单个空格
```

### 5. 网络安全配置
- `res/xml/network_security_config.xml`: 禁止明文流量
- `AndroidManifest.xml`: `usesCleartextTraffic="false"`
- INTERNET + ACCESS_NETWORK_STATE 权限

### 6. Material 3 主题
- 使用默认配色方案（不要自定义颜色）
- 颜色 Token: 绿色系 #4CAF50（充足）、黄色 #FF9800（临近）、红色 #F44336（超标）
- 排版使用 Material 3 默认，不做自定义
- 中文环境，使用系统默认字体

### 7. Application 类
```kotlin
@HiltAndroidApp
class HealthCheckInApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 后续在此初始化埋点、WorkManager 等
    }
}
```

请实现以上所有内容，确保项目可以编译通过。
```

### 提示词 0-B: 设计 Room 数据库与 Supabase 表结构

```
## 任务: 实现健康打卡 App 的 Room 数据库与数据模型

### 数据库名: `health_checkin.db`

### 核心 Entity 定义（全部使用 UUID v7 主键，`deleted_at` 软删除）

请实现以下所有 Entity 和对应的 DAO。每个 Entity 都需要包含 `id: String`（UUID v7），`created_at: Long`（UTC epoch ms），`updated_at: Long`，`deleted_at: Long?`，以及 `sync_state: String`（PENDING/SYNCING/SYNCED/FAILED）。

#### 1. ProfileEntity — 用户档案
```kotlin
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,           // = user_id
    val email: String,
    val sex: String,                      // "MALE" | "FEMALE"
    val birth_year_month: String,         // "YYYY-MM"
    val height_cm: Double,
    val created_at: Long,
    val updated_at: Long,
    val deleted_at: Long?,
    val sync_state: String
)
```

#### 2. GoalEntity — 目标设定
```kotlin
// 字段: id, user_id, goal_type(LOSE/GAIN/MAINTAIN), current_weight_kg, target_weight_kg,
//       target_weeks, pal_level, age_at_set, height_at_set, sex_at_set,
//       bmr_calculated, bmr_clamped, tdee_calculated,
//       daily_delta(可为负), budget_calculated, budget_clamped, budget_clamped_reason,
//       actual_daily_delta, estimated_weeks, is_active(Boolean),
//       created_at, updated_at, deleted_at, sync_state
```

#### 3. DailyBudgetEntity — 每日预算快照
```kotlin
// 字段: id, user_id, local_date, goal_id,
//       bmr, pal, tdee, daily_delta, budget,
//       protein_target_g, carb_target_g, fat_target_g,
//       macro_budget_adjusted(Boolean),
//       created_at, updated_at, deleted_at, sync_state
```

#### 4. WeightRecordEntity — 体重记录
```kotlin
// 字段: id, user_id, weight_kg(Double, 1位小数), local_date, tz_offset_minutes,
//       recorded_at(UTC epoch ms), note(String?),
//       created_at, updated_at, deleted_at, sync_state
// 约束: UNIQUE(user_id, local_date) WHERE deleted_at IS NULL
```

#### 5. MealEntryEntity — 饮食记录（核心表）
```kotlin
// 字段: id, user_id, food_id, food_snapshot_id,
//       quantity(数量), unit(G/ML/SERVING), serving_grams(可为null),
//       consumed_at(消费时间, UTC epoch ms), local_date, tz_offset_minutes,
//       meal_slot(BREAKFAST/LUNCH/DINNER/SNACK),
//       from_inventory(Boolean), inventory_item_id(String?),
//       snap_calories(Double), snap_protein_g(Double), snap_carbs_g(Double),
//       snap_fat_g(Double), snap_fiber_g(Double?), snap_name(String),
//       created_at, updated_at, deleted_at, sync_state
// 说明: snap_* 为食物营养快照，保证历史记录不受食物数据源变化影响
```

#### 6. FoodEntity — 食物库
```kotlin
// 字段: id, user_id(String?), food_name, brand(String?), category(String?),
//       basis_unit(G/ML), serving_grams(Double?),
//       calories_per_100, protein_per_100, carbs_per_100, fat_per_100,
//       fiber_per_100(Double?),
//       source("SELF" | "FATSECRET" | "OPENFOODFACTS" | "PUBLIC"),
//       source_id(String?), data_incomplete(Boolean),
//       last_used_at(Long?), last_quantity(Double?), last_unit(String?),
//       last_meal_slot(String?),
//       created_at, updated_at, deleted_at, sync_state
// 约束: user_id IS NULL 表示公共食物，不为 NULL 表示该用户的自建食物
```

#### 7. PublicFoodEntity — 预建中式食物
```kotlin
// 结构同 FoodEntity，user_id 恒为 NULL
// 表名: public_foods（不参与同步，仅 service_role 维护）
```

#### 8. FoodSearchCacheEntity — 搜索缓存
```kotlin
// 字段: id, query_normalized, response_json, source,
//       created_at(TTL = 24h 自动清理), sync_state(不参与同步)
```

#### 9. AnalyticsEventEntity — 埋点事件
```kotlin
// 字段: id, event_name, event_at(UTC epoch ms), local_date, tz_offset_minutes,
//       session_id, app_version, os_version, device_model, user_id,
//       params_json(事件专属字段 JSON), sync_state
```

#### 10. BackupStateEntity — 备份状态
```kotlin
// 字段: id, table_name, row_id, last_backup_at(Long?),
//       created_at, updated_at
// 用于追踪每行数据的上次备份时间
```

### DAO 要求

每个 Entity 创建对应的 DAO，至少包含:
- `@Insert(onConflict = OnConflictStrategy.REPLACE)` — 插入或替换
- `@Query("SELECT * FROM {table} WHERE id = :id AND deleted_at IS NULL")` — 按 ID 查询
- `@Query("SELECT * FROM {table} WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC")` — 按用户查询
- `@Query("DELETE FROM {table} WHERE id = :id")` — 物理删除（仅数据清理用）
- Flow 版本: 查询返回 `Flow<List<...>>`

**Special DAOs**:
- `MealEntryDao`: 额外需要 `getByLocalDate(userId, localDate)`, `getRecentFoods(userId, limit=20)`, `getFrequentFoods(userId, days=30, minCount=3)`
- `WeightRecordDao`: 额外需要 `getByDateRange(userId, startDate, endDate)`, `upsertByDate(userId, localDate, weightKg)`
- `DailyBudgetDao`: 额外需要 `getByDate(userId, localDate)`
- `FoodDao`: 额外需要 `searchByName(userId, query, limit=20)`, `getRecentAndFrequent(userId, limit=20)`
- `AnalyticsEventDao`: 额外需要 `getUnsynced(limit=200)`

### Database 类

```kotlin
@Database(
    entities = [...全部 Entity],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HealthDatabase : RoomDatabase() {
    // 声明所有抽象 DAO
}
```

### Converters
```kotlin
class Converters {
    @TypeConverter fun fromTimestamp(value: Long?): Date?
    @TypeConverter fun dateToTimestamp(date: Date?): Long?
    // UUID ↔ String
    // List<String> ↔ JSON String (如果需要)
}
```

### supabase/init.sql — Supabase 表结构

为每张表生成对应的 PostgreSQL DDL，包括:
- 字段定义（类型与 Room 对应: TEXT for String, DOUBLE PRECISION/NUMERIC(8,2), TIMESTAMPTZ for epoch ms, BOOLEAN）
- PRIMARY KEY (id)
- 索引: CREATE INDEX idx_{table}_user_id ON {table}(user_id) WHERE deleted_at IS NULL
- Row Level Security 4 条策略（见通用规则）
- public_foods 的只读策略

请实现以上所有内容。Room Entity 的数据类用 `data class`，DAO 用 `interface` 注解 `@Dao`，Database 用 `abstract class` 注解 `@Database`。
```

---

## Phase 1: 认证与用户系统（Week 1，对应 REQ-001 + REQ-002）

### 提示词 1: 登录注册 + 账号安全

```
## 任务: 实现健康打卡 App 的认证系统

### 技术背景
- Supabase Auth（邮箱+密码），Supabase URL 和 anon key 存放在 BuildConfig
- Room 本地唯一真源，Supabase 仅做备份
- Token 存储: EncryptedSharedPreferences (AES-256-GCM)，key 名 `supabase_session`
- 依赖 Hilt 注入

### 需要实现

#### A. SupabaseAuthService（封装 Supabase Auth API）

使用 Retrofit 直接调用 Supabase Auth REST API（不用 SDK）:

```
1. POST /auth/v1/signup      → 注册
   Body: { email, password }
   Header: apikey={SUPABASE_ANON_KEY}
   Response: { access_token, refresh_token, expires_in, user: { id, email, email_confirmed_at } }
   异常: 400 "User already registered" → 提示「该邮箱已注册，请直接登录」，并提供「前往登录」按钮

2. POST /auth/v1/token?grant_type=password  → 登录
   Body: { email, password }
   异常: 400 "Invalid login credentials" → 提示「邮箱或密码错误」

3. POST /auth/v1/token?grant_type=refresh_token  → 刷新 token
   Body: { refresh_token }
   异常: 401 → 清除本地 session，跳转登录页

4. GET /auth/v1/user  → 获取当前用户信息
   Header: Authorization: Bearer {access_token}
   用于: 获取 email_confirmed_at，判断是否需要提醒验证

5. POST /auth/v1/recover  → 发送密码重置邮件
   Body: { email }
   无论邮箱是否存在，均返回成功（防枚举）

6. POST /auth/v1/resend  → 重发验证邮件
   Header: Authorization: Bearer {access_token}
```

#### B. SessionManager（管理认证状态）

```kotlin
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 存储: access_token, refresh_token, expires_at, user_id, email
    // 使用 EncryptedSharedPreferences
    fun saveSession(...)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun isLoggedIn(): Flow<Boolean>  // 通过 DataStore 的 flow 暴露
    fun clearSession()
}
```

#### C. AuthRepository

```kotlin
interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun refreshToken(): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun resendVerificationEmail(): Result<Unit>
    suspend fun changePassword(newPassword: String): Result<Unit>  // 调 PUT /auth/v1/user
    suspend fun deleteAccount(): Result<Unit>  // 调 Edge Function /functions/v1/account-delete
    fun observeAuthState(): Flow<AuthState>
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String, val email: String, val emailVerified: Boolean) : AuthState()
}
```

#### D. UI 页面

##### SC-01 启动页 (SplashScreen)
- 检查 SessionManager 是否有有效 session
- 有效 → 刷新 token → 成功则进入仪表盘，失败则进入登录页
- 无效 → 进入登录页
- 展示 App 图标 + 名称，不展示加载动画（冷启动目标 ≤ 3 秒）

##### SC-02 登录页 (LoginScreen)
- 邮箱输入框（自动获焦，键盘类型 email）
- 密码输入框（支持显示/隐藏切换）
- 「登录」按钮（校验通过方可点击）
- 「注册新账号」链接（跳转 SC-03）
- 「忘记密码？」链接（跳转 SC-04）
- 错误处理:
  * 网络错误 → 「网络不太顺畅，请检查网络后重试」
  * 邮箱或密码错误 → 「邮箱或密码错误」
  * 邮箱未注册 → 「该邮箱尚未注册」并提供「去注册」按钮

##### SC-03 注册页 (RegisterScreen)
- 邮箱、密码、确认密码
- 密码规则: ≥8 字符 + 至少 1 字母 1 数字
- 「注册」按钮
- 成功后自动登录（不等待邮箱验证），直接跳转目标设定 SC-05
- 异常:
  * 邮箱已注册 → 「该邮箱已注册」+ 「去登录」链接
  * 密码不匹配 → 「两次输入的密码不一致」

##### SC-04 忘记密码页 (ForgotPasswordScreen)
- 邮箱输入 + 「发送重置邮件」按钮
- 发送后不论邮箱是否存在，展示「如果该邮箱已注册，我们已发送重置链接，请查收邮件」
- 「返回登录」链接

##### SC-14 设置中心 (SettingsScreen)
- 见 REQ-014，此处先实现账号相关:
  * 显示当前邮箱（未验证时橙色标记 + 「重新发送验证邮件」按钮）
  * 「修改密码」→ 弹窗输入新密码 + 确认 → 调 changePassword()
  * 「退出登录」→ 确认弹窗 → clearSession() → 跳转登录页
  * 「注销账号」（红色文字）→ 二次确认弹窗（警告数据不可恢复）→ 调 deleteAccount()

#### E. 网络拦截器 AuthInterceptor

```kotlin
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Chain): Response {
        // 自动附加 Authorization: Bearer {access_token} 到所有请求
        // 遇到 401 → 尝试 refresh token → 重试原请求 1 次
        // refresh 失败 → 清除 session → 抛出 UnauthorizedException
    }
}
```

### 验收标准
- [ ] 注册 → 自动登录 → 进入目标设定页
- [ ] 登录 → 进入仪表盘（或目标设定页，如果首次）
- [ ] Token 过期后自动刷新
- [ ] 冷启动自动恢复登录态
- [ ] 忘记密码邮件成功发送
- [ ] 修改密码成功（新旧密码不能相同）
- [ ] 退出登录清除所有本地认证数据
- [ ] 注销账号二次确认 + Edge Function 调用
- [ ] 所有 Token 相关日志脱敏（Release 构建下不输出 token）

### 工程约束
- 所有用户可见字符串在 `strings.xml` 中定义，禁止硬编码
- 错误提示遵循格式：「发生了什么 + 可以怎么做」
- 不暴露 Supabase 返回的原始错误消息给用户
```

---

## Phase 2: 目标设定与仪表盘（对应 REQ-003 + REQ-004）

### 提示词 2-A: 目标设定

```
## 任务: 实现身体档案与目标设定（REQ-003）

### 依赖
- REQ-001（登录态已完成）

### 核心算法（已在 util/algorithm 包中实现）

#### 1. BMR 计算器 `BmrCalculator.kt`

```kotlin
object BmrCalculator {
    fun calculate(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): BmrResult
}

data class BmrResult(
    val bmr: Int,               // round HALF_UP，钳制后 ≥ 800
    val rawBmr: Double,         // 钳制前的原始值
    val clamped: Boolean        // 是否触发钳制
)

enum class Sex { MALE, FEMALE }
```

Mifflin-St Jeor 公式:
- MALE: 10×W + 6.25×H − 5×A + 5
- FEMALE: 10×W + 6.25×H − 5×A − 161
- 结果 < 800 → 钳制为 800，标记 clamped=true

#### 2. 热量预算计算器 `BudgetCalculator.kt`

```kotlin
object BudgetCalculator {
    fun calculate(
        sex: Sex, currentWeightKg: Double, targetWeightKg: Double,
        targetWeeks: Int, palLevel: PalLevel
    ): BudgetResult
}

// 完整实现 §5.3 的 5 步计算，包括钳制 + 反算 est_weeks
```

#### 3. 宏量分配计算器 `MacroCalculator.kt`

```kotlin
object MacroCalculator {
    fun calculate(
        goalType: GoalType, budget: Int, currentWeightKg: Double
    ): MacroResult
}

// 完整实现 §5.4，包括蛋白质下限钳制 + 脂肪下限 + 碳水兜底 + 回缩算法
```

### UI: SC-05 目标设定向导（五步）

**引导页**: 欢迎文案「开始你的健康之旅」，1 个「开始」按钮

**Step 1 — 性别**:
- 两个大卡片「男」「女」，选中态高亮
- 底部「下一步」按钮

**Step 2 — 身体数据**:
- 身高输入（cm，1 位小数，范围 50-300）
- 当前体重输入（kg，1 位小数，范围 20-500）
- 出生年月选择（YearMonthPicker，年月两级滚轮，不选到日）
  * 自动计算年龄 = 满岁
  * 弹窗内完成选择，不在向导页展开

**Step 3 — 目标设定**:
- 目标体重输入（kg，1 位小数，范围 20-500）
- 目标周数选择（滑块或 Stepper，4-52 周，默认 12）
- 实时预览: 判断 goal_type（LOSE/GAIN/MAINTAIN）并展示文字
  * Δw < −0.5 → 「减重 XX.X kg」
  * Δw > 0.5 → 「增重 XX.X kg」
  * |Δw| ≤ 0.5 → 「维持当前体重」

**Step 4 — 活动水平**:
- 5 个选项卡片，展示标题+副标题:
  * 久坐 (1.200) / 轻度活动 (1.375) / 中度活动 (1.550) / 高度活动 (1.725) / 运动员 (1.900)
- 默认选中「久坐」

**Step 5 — 计算结果确认**:
展示计算结果卡片:
- BMR: {bmr} 大卡/日（clamped 时橙色标注）
- TDEE: {tdee} 大卡/日
- 每日预算: {budget} 大卡（clamped 时橙色提示条显示原因和 est_weeks）
- 宏量目标: 蛋白 {p}g / 碳水 {c}g / 脂肪 {f}g
- 「确认并开始」按钮 → 写入 Room（profiles + goals + daily_budgets[今日]）→ 跳转仪表盘

**导航规则**:
- 可「上一步」回退修改（StateFlow 保存中间值）
- 不允许在 Step 5 之前退出（如果无已存在的 goal，启动后强制进入向导）

### 后端逻辑
- 确认后:
  1. INSERT `profiles` (user_id, email, sex, birth_year_month, height_cm)
  2. INSERT `goals` (全部计算值，is_active = true，旧的 goal 置 is_active = false)
  3. INSERT `daily_budgets` (今日 local_date 的预算 + 宏量目标)
  4. 同步 profile.goal_id → 当前活跃 goal 的 id

### 年龄更新
- 用户生日过后首次使用 App，在仪表盘提示「新的一岁，要不要更新身体数据？」（不强制）
- 检测方式: goals.age_at_set < 当前年龄（按出生年月计算）→ 触发
- 提示入口在 SC-14 设置中心，展示为「年龄已变化，建议更新目标」

### 验收标准
- [ ] 5 步向导完整可用，可前进后退
- [ ] BMR 计算与示例校验表（§5.3 的 5 行示例）逐行一致
- [ ] 钳制场景正确展示提示条
- [ ] 写操作失败（如 Room 异常）时回滚，不写入部分数据
- [ ] 重新进入时不重复强制向导（已有 active goal 则直接进入仪表盘）
```

### 提示词 2-B: 仪表盘首页

```
## 任务: 实现仪表盘首页（REQ-004）

### 依赖
- REQ-003（目标设定已有活跃 goal + 今日 daily_budget）
- REQ-011（Room 基础数据层已完成）
- REQ-010（健康提示引擎，可先预留接口）

### 页面: SC-06 仪表盘

采用三段式布局，自上而下:

#### 区块 1: 头部 — 今日热量概览

**大数字**: 今日剩余热量（白色背景下的醒目大字体）
```
remained = daily_budget.budget − 今日已摄入热量总和
(通过 Room Flow: SUM(meal_entries.snap_calories) WHERE local_date = 今日 AND deleted_at IS NULL)
```

**颜色规则**: 
- 剩余 > 25% 预算 → 绿色
- 0 < 剩余 ≤ 25% → 黄色
- −25% < 剩余 ≤ 0 → 橙色
- 剩余 ≤ −25% → 红色

**副标题**: 「已摄入 XXX / 预算 X,XXX 大卡」

**目标体重提示行**: 「目标 {goal_type_chinese} {|Δw|} kg · 预计 {est_weeks} 周 · 已坚持 {streak_days} 天」

#### 区块 2: 宏量营养素进度条（三个并排进度条）

每个进度条: 标签(蛋白质/碳水/脂肪) + 当前值/目标值 g + 进度条(LinearProgressIndicator)
- 蛋白质、碳水: 超过目标变橙色
- 脂肪: 超过目标变红色

颜色使用 `progressIndicatorColor` Token:
- 正常: 主题色
- 接近上限 (>90%): 黄色
- 超出: 红色

计算:
```kotlin
val todayMeals = mealEntryDao.getByLocalDate(userId, todayLocalDate).first()
val totalProtein = todayMeals.sumOf { it.snap_protein_g }
val totalCarbs = todayMeals.sumOf { it.snap_carbs_g }
val totalFat = todayMeals.sumOf { it.snap_fat_g }
```

#### 区块 3: 今日饮食列表

- 按餐次分组展示（早餐 → 午餐 → 加餐 → 晚餐），每组标题: 「早餐 · XXX 大卡」
- 每个饮食条目: 食物名 + 份量（如「300g」）+ 热量
- 默认展示**今日**的饮食，支持左右滑动切换日期（或顶部日期选择器）
  * 今日不可右滑（不展示未来日期）
  * 历史日期左滑上限 = 注册日期或 365 天前
- 每个条目支持:
  * 点击 → 跳转编辑（REQ-005）
  * 左滑 → 删除（红色背景 + 垃圾桶图标），删除后展示 Snackbar「已删除『XXX』· 撤销」（5 秒自动消失）

#### 健康提示 Banner（REQ-010，条件展示）

在头部和宏量进度条之间，当触发健康规则时展示:
- 信息图标 + 提示文字
- 可关闭（× 按钮，关闭后当日不再展示）
- 无命中规则时**完全不占位**

规则暂定:
- W-01: 蛋白质连续 3 天 < 目标的 50% → 提示
- W-02: 热量连续 3 天超出预算 → 提示
(具体规则先预留，可先实现空壳)

#### 空态（首次使用，无任何记录）

展示引导卡片: 「记录你的第一餐吧」 + 插图（可暂用文字）

#### FAB（右下角，直径 56dp）

- 点击 → 跳转 SC-07 饮食记录页（搜索状态）
- 颜色: `primaryContainer`
- 位置: 右下角 16dp margin

#### 下拉刷新

- Pull-to-refresh 触发手动备份（REQ-011），顶部展示刷新指示器
- 实际数据来自 Room Flow，不需要「刷新」

#### TopAppBar

- 左侧: App 名称或 Logo
- 右侧: 设置入口（齿轮图标 → SC-14）

### 后端逻辑
- 通过 Room Flow 订阅 `meal_entries`、`daily_budgets`、`weight_records`、`goals`
- 所有数据变化自动驱动 UI 更新
- 进入仪表盘时触发 `app_session_start` 埋点（REQ-013）

### 页面四态
- **加载态**: 骨架屏（Skeleton），3 个占位块模仿三段式布局
- **空态**: 「记录你的第一餐吧」引导卡片
- **正常态**: 三段式数据展示
- **错误态**: 仅当 Room 数据不可读时展示，带「重试」按钮（几乎不会触发）

### 验收标准
- [ ] 热量大数字颜色随剩余变化正确切换
- [ ] 三个宏量进度条正确展示
- [ ] 饮食列表按餐次分组正确
- [ ] 删除 + 撤销流程正确
- [ ] 空态/加载态/正常态/错误态完整
- [ ] FAB 点击跳转饮食记录页
- [ ] 设置图标可点击进入设置
```

---

## Phase 3: 饮食记录核心（对应 REQ-005 + REQ-006 + REQ-007 + REQ-008）

### 提示词 3-A: 饮食记录 CRUD

```
## 任务: 实现饮食记录功能（REQ-005）

### 依赖
- REQ-006（食物搜索已就绪 → 先做桩，返回假数据）
- REQ-011（Room 基础层）
- REQ-004（仪表盘作为入口 → SC-06 的 FAB 跳转至此）

### 核心页面

#### SC-07 饮食记录页（入口）

**打开方式**: 
- 仪表盘 FAB → 默认当前时间 + 今日日期
- 仪表盘点击已有记录 → 编辑模式
- 历史日期下的 FAB → 默认补录模式（日期为所选历史日期）

**页面结构（自上而下）**:

1. **顶部搜索区**（搜索框自动获焦）
   - TextField + 搜索图标
   - 输入 ≥ 1 字符 → 搜索本地（Room: foods + public_foods + 缓存）
   - 输入 ≥ 2 字符 → 额外搜索远程（Edge Function 代理 FatSecret + Open Food Facts）
   - 防抖 300ms
   - 搜索请求竞态: 新请求取消旧请求
   - 空输入 → 展示「最近与常吃」列表（REQ-007）

2. **搜索结果列表**（占剩余区域）
   - 每行: 食物名 + 品牌/来源标签 + 热量/100基准
   - 来源标签: 「自建」「FatSecret」「Open Food Facts」
   - 点击某条 → 进入 SC-08 数量确认页

3. **零结果态**: 
   - 「没找到『{query}』」
   - 按钮:「创建自建食物『{query}』」→ 跳转 SC-11，自动填入食物名

#### SC-08 数量确认页

- 顶部: 食物名（大字体）
- 营养概况卡片: 每 100g(ml) 的热量 + 蛋白 + 碳水 + 脂肪
- 数量输入:
  * 数字键盘输入 quantity
  * 单位选择: 下拉/分段按钮 [克(g)/毫升(ml)/份]
  * 选中「份」时额外展示 serving_grams 输入
  * 实时计算并展示: 预估摄入热量 = quantity × calories_per_100 / 100
- 日期选择: 默认今日（可改，但不能选未来日期）
- 时间选择: 默认当前时间（根据 §5.5 规则推断餐次，可手动改）
- 餐次选择: 兜底展示推断值（早/午/晚/加餐），可手动改
- 「从库存扣减」勾选框（P1 实现，P0 先隐藏）
- 「确认记录」按钮 → 写入 Room → 返回仪表盘

#### SC-10 饮食编辑页（复用 SC-08 布局 + 预填数据）

- 从仪表盘点击已有记录进入
- 预填: 食物、数量、单位、日期、时间、餐次
- 「保存修改」→ UPDATE Room
- 「删除记录」→ 确认弹窗 → 软删除 → 返回仪表盘
- 编辑不影响食物本身的营养数据（meal_entries 使用 snap_* 快照）

### 后端逻辑（MealRepository）

```kotlin
interface MealRepository {
    suspend fun addMeal(entry: MealEntry): Result<MealEntry>      // INSERT + 写快照 + 更新 foods.last_used_at
    suspend fun updateMeal(entry: MealEntry): Result<MealEntry>   // UPDATE（改 quantity/date/meal_slot 等）
    suspend fun deleteMeal(entryId: String): Result<Unit>          // 软删除（设 deleted_at）
    fun getMealsByDate(userId: String, localDate: String): Flow<List<MealEntry>>
    fun getTodayTotalCalories(userId: String, localDate: String): Flow<Double>
    fun getTodayMacros(userId: String, localDate: String): Flow<MacroSum>
}
```

### 写快照规则（决策 D-09）

插入 meal_entry 时:
```kotlin
val food = foodDao.getById(foodId)
entry.snap_calories = food.calories_per_100 * basisAmount / 100
entry.snap_protein_g = food.protein_per_100 * basisAmount / 100
entry.snap_carbs_g = food.carbs_per_100 * basisAmount / 100
entry.snap_fat_g = food.fat_per_100 * basisAmount / 100
entry.snap_fiber_g = food.fiber_per_100?.let { it * basisAmount / 100 }
entry.snap_name = food.food_name
```

同步更新:
```kotlin
food.last_used_at = now()
food.last_quantity = entry.quantity
food.last_unit = entry.unit
food.last_meal_slot = entry.meal_slot
```

### 补录规则
- 日期选择器上限锁定为今日，下限 = max(注册日期, 今日−365)
- 补录默认时间: 早餐 08:00 / 午餐 12:00 / 晚餐 19:00 / 加餐 15:30
- 补录不触发健康提示延迟重算（下次进入仪表盘自然重算）

### 异常分支
| 异常 | 表现 | 处理 |
|---|---|---|
| 搜索无网络 | 仅搜索本地 | 结果顶部灰色提示「当前离线，仅显示本地食物」 |
| 远程 API 超时 (3s) | 仅展示本地结果 | 提示「在线食物库响应超时」 |
| 房间写入失败 | — | 提示「数据保存失败，请重试」+ 上报 sync_failed |

### 验收标准
- [ ] 搜索 → 选择食物 → 输入数量 → 确认记录，完整流程可用
- [ ] 记录后仪表盘实时更新（Room Flow 自动推送）
- [ ] 编辑已有记录（改数量、改时间、改餐次）
- [ ] 删除记录 + Snackbar 撤销
- [ ] 补录历史日期默认时间正确
- [ ] 餐次自动推断正确（覆盖 24 小时 5 个区间）
- [ ] 离线时搜索仅搜索本地食物
- [ ] 营养快照正确写入
```

### 提示词 3-B: 食物搜索

```
## 任务: 实现食物搜索功能（REQ-006）

### 依赖
- REQ-011（Room 基础层 + Supabase 表已就绪）
- Edge Function 已部署（/functions/v1/food-search）

### 数据源架构

```
用户输入 query
    ├── L ≥ 1: 本地搜索（Room: foods + public_foods + food_search_cache）
    └── L ≥ 2: 并行远程搜索
         ├── FatSecret API（经 Edge Function 代理）
         └── Open Food Facts API（经 Edge Function 代理）
              ↓
         合并去重 → 缓存 24h → 返回排序结果
```

### FoodSearchService

```kotlin
interface FoodSearchService {
    suspend fun search(query: String, userId: String): SearchResult
}

data class SearchResult(
    val items: List<FoodSearchItem>,
    val sources: List<SearchSource>,  // 实际响应的来源
    val fromCache: Boolean,
    val quotaExhausted: Boolean
)

data class FoodSearchItem(
    val id: String,
    val name: String,
    val brand: String?,
    val caloriesPer100: Double,
    val proteinPer100: Double?,
    val carbsPer100: Double?,
    val fatPer100: Double?,
    val basisUnit: String,      // "G" or "ML"
    val servingGrams: Double?,
    val source: FoodSource,     // SELF, FATSECRET, OPENFOODFACTS, PUBLIC
    val dataIncomplete: Boolean,
    val score: Double,          // 匹配度得分
    val lastUsedAt: Long?
)

enum class FoodSource { SELF, FATSECRET, OPENFOODFACTS, PUBLIC }
```

### 搜索实现细节

#### 本地搜索
```sql
-- 自建食物 + public_foods
SELECT * FROM foods
WHERE user_id = :userId OR user_id IS NULL
  AND deleted_at IS NULL
  AND food_name LIKE '%' || :normalized || '%'
ORDER BY ... -- 由 Kotlin 侧计算 score 后排序
```

#### 远程搜索 — Supabase Edge Function

`/functions/v1/food-search`:
- 输入: `{ query: "番茄" }`
- 调用 FatSecret API: `foods.search` 方法
- 调用 Open Food Facts API: `https://world.openfoodfacts.org/cgi/search.pl?search_terms=...`
- 输出: `{ results: [...] }`（统一格式后的列表）
- 限流: 维护当日计数器（UTC 日切），剩余 < 200 时仅在本地未命中时调用

#### 合并与去重
```
去重键: normalize(name) + '|' + normalize(brand ?: '')
normalize: 转小写 → 全角转半角 → 去除所有空白与标点
重复时保留 score 最高者，并在 source 上标记多个来源
```

#### 匹配度评分（§5.7）
```
score = 0.45 × nameMatch
      + 0.25 × sourceWeight
      + 0.20 × personalRecency
      + 0.10 × dataCompleteness
```

#### 排序
`score DESC → last_used_at DESC → name ASC (Collator 中文排序)`

#### 缓存
- Key: `normalize(query)`
- TTL: 24 小时
- 表: `food_search_cache`（Room）

#### 零结果
所有来源 0 条 → 展示「找不到『{query}』」+ 按钮「创建自建食物『{query}』」

### 异常处理

| 情况 | 处理 |
|---|---|
| FatSecret 配额耗尽 | 跳过该来源，列表顶部提示 |
| Open Food Facts 超时 | 跳过该来源 |
| 两个远程源均失败 | 仅展示本地结果 + 提示 |
| 离线 | 搜索本地 + 缓存，顶部灰色提示「当前离线」 |

### Edge Function 部署

文件: `supabase/functions/food-search/index.ts`
- TypeScript/Deno 实现
- 无状态，无数据库访问
- ≤ 200 行
- 环境变量: `FATSECRET_CLIENT_ID`, `FATSECRET_CLIENT_SECRET`
- 返回统一 JSON 格式

### FatSecret 署名要求
三处必须标注「数据来源: FatSecret」:
1. 搜索结果行的来源标签
2. 食物详情页（SC-09）的「数据来源」行
3. 设置 → 关于页

### 验收标准
- [ ] 输入 1 字符 → 本地搜索立即响应（≤ 200ms）
- [ ] 输入 2 字符 → 远程搜索并行启动，本地结果先展示
- [ ] 搜索防抖 300ms 正常工作
- [ ] 竞态: 快速输入时旧请求被取消
- [ ] 排序结果符合匹配度公式
- [ ] 零结果态可点击创建自建食物
- [ ] FatSecret 配额耗尽时优雅降级
- [ ] 离线搜索仅用本地数据
- [ ] 三处 FatSecret 署名齐全
```

### 提示词 3-C: 最近与常吃 + 自建食物

```
## 任务: 实现最近与常吃 + 自建食物管理（REQ-007 + REQ-008）

### REQ-007: 最近与常吃食物

**触发条件**: SC-07 搜索框为空时展示（替代空白态）

**数据源**: Room

**「最近吃过」分区**（最多 8 条）:
```sql
SELECT DISTINCT food_id FROM meal_entries
WHERE user_id = :userId AND deleted_at IS NULL
GROUP BY food_id
ORDER BY MAX(created_at) DESC
LIMIT 8
-- 去重后的食物列表，附带 foods 表的完整信息
```

**「常吃」分区**（最近 30 天，≥ 3 次，最多 8 条）:
```sql
SELECT food_id, COUNT(*) as cnt FROM meal_entries
WHERE user_id = :userId AND deleted_at IS NULL
  AND created_at >= :thirtyDaysAgo
GROUP BY food_id
HAVING COUNT(*) >= 3
ORDER BY cnt DESC
LIMIT 8
```

**UI 展示**:
- 分区标题: 「最近吃过」「常吃」+ 更多/收起（可折叠）
- 每个食物条目: 食物名 + 上次记录的份量(如「上次 300g」) + 热量
- **一键记录**: 点击直接进入 SC-08，预填上次的 quantity + unit（份量记忆）
- 来源标签同搜索（自建/FatSecret/OFF）

**份量记忆**: 从 `foods.last_quantity`, `foods.last_unit`, `foods.last_meal_slot` 读取

**独立搜索入口**: 搜索框输入 1 字符后，最近/常吃列表隐藏，展示搜索结果。清空搜索框后恢复。

**验收标准**:
- [ ] 无记录时两个分区均为空（不占位，或展示引导文案）
- [ ] 最近吃过按时间倒序
- [ ] 常吃按频率降序（≥ 3 次才出现）
- [ ] 点击一键记录 → 预填上次份量
- [ ] 与搜索结果切换流畅（消失/出现动画）

### REQ-008: 自建食物管理

#### SC-11 自建食物创建页

**打开方式**: 
- 搜索零结果 → 点击「创建自建食物」→ 自动填入食物名
- 设置中心 → 自建食物管理 → 「新建」

**表单字段**:
- 食物名称（必填, 1-50 字符, trim, 连续空白折叠）
- 分类（可选, 下拉: 主食/肉类/蔬菜/水果/饮品/零食/调味品/其他）
- 基准单位（必填, 切换: 固态「每 100g」/ 液态「每 100ml」）
- 热量（必填, kcal, 数值范围 0-900）
- 蛋白质（必填, g, 范围 0-100）
- 碳水化合物（必填, g, 范围 0-100）
- 脂肪（必填, g, 范围 0-100）
- 膳食纤维（可选, g, 范围 0-100）
- 份量（可选, g/ml「1 份 = XX g(ml)」，没填则不可用 SERVING 单位记录）

**校验规则**:
- 热量必须 > 0
- 蛋白质 + 碳水 + 脂肪 + 纤维中至少 2 项 > 0（全部为 0 拒绝保存）
- 食物名 + 分类 的组合在本用户食物库中唯一（× 允许重复，提示「已存在同名食物「XXX」，是否覆盖？」覆盖 = UPDATE）

**提交**:
- INSERT into `foods`（user_id 填当前用户 id, source = "SELF", data_incomplete = false）
- 成功后返回搜索结果页，并自动选中该食物（直接进入 SC-08 数量确认）

#### SC-12 自建食物列表（设置中心入口）

- 列出用户创建的所有自建食物（user_id = 当前用户, deleted_at IS NULL）
- 每行: 食物名 + 分类 + 热量/100基准
- 点击 → 跳转 SC-11 编辑模式（预填所有字段）
- 左滑 → 删除（确认弹窗: 「删除后该食物的历史饮食记录不受影响，是否确认？」）
- 编辑不影响历史饮食记录（meal_entries 使用 snap_* 快照，不随食物数据变化）

#### SC-09 食物详情页

- 搜索结果或列表点击食物时进入
- 展示: 食物名 + 品牌 + 分类 + 每 100 基准营养数据
- 数据来源标注（如「来源: FatSecret」→ 必须指明见 REQ-006 B6）
- 「使用此食物记录」按钮 → 跳转 SC-08

### 验收标准
- [ ] 创建自建食物 → 自动进入数量确认页
- [ ] 编辑自建食物后历史饮食记录的营养值不变（快照机制）
- [ ] 删除自建食物后历史记录不受影响（仅 foods.deleted_at 标记，meal_entries 不动）
- [ ] 同名同分类食物提示覆盖
- [ ] 零结果搜索 → 创建 → 自动选中，全流程闭环
```

---

## Phase 4: 体重、提示、备份、导出、埋点、设置（对应 REQ-009 ~ REQ-014）

### 提示词 4-A: 体重记录与曲线

```
## 任务: 实现体重记录与曲线（REQ-009）

### 依赖
- REQ-003（目标设定已有体重数据）

### SC-13 体重页面

#### 顶部: 当前体重卡片
- 最近一次体重记录的大数字（如「68.5 kg」）
- 变化标记: 与 7 天前对比的差值（如「−1.2 kg」绿色 / 「+0.3 kg」红色）
  * 无 7 天前数据 → 与目标体重对比

#### 中部: 体重曲线图（使用 Vico 图表库）

- X 轴 = 日期, Y 轴 = 体重(kg)
- 3 个时间段切换: 「7 天」「30 天」「90 天」（分段按钮）
- 可在任意点长按查看该日体重
- 数据点 ≥ 2 个才开始绘制（与 REQ-015 统一: ≥ 2 条即绘制）
- **不插值**: 数据点之间直线连接（稀疏数据不填充）
- 目标体重虚线（水平参考线，仅 LOSE/GAIN 时显示，MAINTAIN 不显示）

#### 底部: 体重输入区
- 数字输入框（kg, 1 位小数）
- 可选备注（≤ 200 字符）
- 「记录」按钮

#### 历史记录列表
- 曲线下方，时间倒序排列
- 每行: 日期 + 体重 + 变化箭头（与上一次对比）
- 点击 → 编辑（弹窗修改体重值或备注，日期不可改）
- 左滑 → 删除

### 后端逻辑

```kotlin
interface WeightRepository {
    suspend fun recordWeight(userId: String, weightKg: Double, localDate: String, note: String?): Result<WeightRecord>
    // 同日唯一: UNIQUE(user_id, local_date)，重复保存时覆盖
    suspend fun updateWeight(recordId: String, weightKg: Double, note: String?): Result<WeightRecord>
    suspend fun deleteWeight(recordId: String): Result<Unit>
    fun getWeightsByRange(userId: String, startDate: String, endDate: String): Flow<List<WeightRecord>>
    fun getLatestWeight(userId: String): Flow<WeightRecord?>
}
```

### 体重趋势计算

- 7 天变化: latest.weight − weight_7_days_ago（最近 7 天内日期最早的记录）
- 30 天变化: 同理
- 无对应天数数据 → 不展示变化

### 验收标准
- [ ] 体重记录可正常新增
- [ ] 同日重复记录覆盖（弹窗确认）
- [ ] 7/30/90 天曲线切换正确
- [ ] 至少 2 条记录后才绘制曲线
- [ ] LOSE 目标时展示目标体重虚线
- [ ] 编辑/删除体重记录
- [ ] 变化标记显示正确
```

### 提示词 4-B: 健康提示 + 本地存储与备份 + 数据导出 + 埋点 + 设置中心

```
## 任务: 实现 P0 收尾需求（REQ-010 ~ REQ-014）

这些是规模较小的需求，可在一个阶段内完成。

---

### REQ-010: 健康提示与预警

**触发规则**（条件展示在仪表盘）:

| 编号 | 规则 | 判定逻辑 |
|---|---|---|
| W-01 | 蛋白质摄入持续不足 | 最近 3 个自然日的日均蛋白质摄入 < 目标值的 50% |
| W-02 | 热量持续超标 | 最近 3 个自然日的日均摄入 > 预算的 110% |
| W-03 | 体重下降过快 | 最近 7 天内体重下降 > (初始体重 × 1.5%) ÷ 7 × 7（即 1.5%/周）|

**提示文案模板**（定义在 strings.xml）:
- W-01: 「最近 3 天蛋白质摄入不足目标的一半，试试增加蛋奶豆制品」
- W-02: 「最近 3 天热量持续超出预算，可以考虑替换部分主食为蔬菜」
- W-03: 「体重下降速度偏快，建议适当增加热量摄入」

**前端**:
- 在仪表盘头部与宏量进度条之间展示
- 信息图标 + 提示文字 + 关闭按钮（×）
- 关闭后当日不再展示
- 多规则命中时轮播展示（每 5 秒切换）

**后端**:
- RuleEngine 在仪表盘加载时计算，结果缓存至 ViewModel
- 使用 Room Flow 读取最近 3/7 天数据

**验收**:
- [ ] 三条规则各自正确触发
- [ ] 关闭后当日不再次展示
- [ ] 多规则轮播正常

---

### REQ-011: 本地优先存储与云端备份

#### BackupManager

```kotlin
@Singleton
class BackupManager @Inject constructor(
    private val db: HealthDatabase,
    private val supabaseApi: SupabaseApi,
    private val sessionManager: SessionManager,
    private val connectivityManager: ConnectivityManager
) {
    // 触发条件:
    // 1. 应用进入后台 + 距上次备份 > 30 分钟
    // 2. 每日首次冷启动
    // 3. SC-14 设置页手动触发

    suspend fun triggerBackup(): BackupResult
    // 1. 查询 backup_state，找到 last_backup_at 之后有变化的行
    // 2. 按表分批上传（每批 ≤ 200 行），使用 INSERT ... ON CONFLICT (id) DO UPDATE
    // 3. 更新 backup_state
    // 4. 上报埋点 sync_batch_completed 或 sync_failed

    suspend fun restoreFromCloud(userId: String): RestoreResult
    // 1. 全量下载 user_id 的所有行
    // 2. 覆盖本地 Room（替换前提示「本地未备份数据将丢失」+ 展示待备份条数）

    fun observeBackupState(): Flow<BackupState>
}
```

**备份状态机**:
```
每条数据行的 sync_state:
PENDING → SYNCING → SYNCED（成功）
                  → FAILED（失败，退避 5s→15s→60s→300s→900s，最多 5 次）
```

**失败退避**:
```
退避序列: 5s → 15s → 60s → 300s → 900s
最多 5 次，仍失败 → 置 FAILED
在 SC-14 展示「待备份 N 条，点击重试」
上报 sync_failed 埋点
```

**恢复流程**:
1. SC-14 → 点击「从云端恢复」
2. 展示提示: 「云端数据将覆盖本地。本地有 {N} 条未备份数据，覆盖后将丢失。」（N 条时红色警告；N=0 时不警告）
3. 确认 → 全量下载 → 覆盖本地 → 重启 UI（重新从 Room 读取）

**验收**:
- [ ] 后台触发备份
- [ ] 手动触发备份
- [ ] 备份失败退避机制
- [ ] SC-14 展示备份状态
- [ ] 云端恢复流程
- [ ] 离线时备份自动跳过（不报错）
- [ ] 网络恢复后自动重试

---

### REQ-012: 数据导出

**SC-16 数据导出页**（设置中心入口）:

- 选择导出内容（复选框）:
  * 全部（默认选中）
  * 可单独勾选: 饮食记录 / 体重记录 / 身体数据 / 食物库
- 选择格式: JSON / CSV（默认 JSON）
- 开始时间 / 结束时间（可选，默认全部）
- 「导出」按钮 → 生成文件 → 分享 Sheet（`Intent.ACTION_SEND`）
- 同时保存到 `{外部存储}/Documents/HealthCheckIn/export_{timestamp}.{json/csv}`

**JSON 导出格式**（§13.5 定义）:
```json
{
  "exportInfo": {
    "appName": "健康打卡", "appVersion": "x.y.z",
    "exportedAt": "UTC ISO 8601", "userId": "...", "email": "..."
  },
  "profiles": [...], "goals": [...], "dailyBudgets": [...],
  "weightRecords": [...], "mealEntries": [...], "foods": [...],
  "bodyMeasurements": [...], "inventoryItems": [...], "milestones": [...]
}
```

**CSV 格式**: 每张表一个 CSV 文件，打包成 ZIP

**验收**:
- [ ] JSON 导出完整
- [ ] 选择部分数据导出正确
- [ ] 导出文件可送达分享目标
- [ ] 时间范围筛选正确

---

### REQ-013: 埋点采集

#### AnalyticsManager

```kotlin
@Singleton
class AnalyticsManager @Inject constructor(
    private val db: HealthDatabase,
    private val sessionManager: SessionManager
) {
    suspend fun track(event: AnalyticsEvent)
    // 1. 写入 Room analytics_events 表
    // 2. 随 REQ-011 备份通道上传（低优先级，最后上传）
    // 3. SC-14 开关「参与使用数据采集」关闭时停止写入

    fun isEnabled(): Flow<Boolean>
}
```

#### 自动附加字段
每个事件自动附加:
- `event_name`, `event_at`(UTC epoch ms), `local_date`, `tz_offset_minutes`
- `session_id`（应用进程级 UUID，冷启动重新生成）
- `app_version`（BuildConfig.VERSION_NAME）
- `os_version`（Build.VERSION.SDK_INT）
- `device_model`（Build.MODEL）
- `user_id`

#### P0 埋点清单（15 个事件）

| 事件 | 触发时机 | params |
|---|---|---|
| app_session_start | 进入仪表盘 | — |
| app_session_end | 应用退到后台 | duration_ms |
| user_registered | 注册成功 | — |
| user_logged_in | 登录成功 | — |
| goal_set | 目标设定确认 | goal_type, target_weeks, delta_kg |
| meal_logged | 饮食记录成功 | food_source, meal_slot, from_inventory, is_retroactive, duration_ms |
| meal_deleted | 删除饮食记录 | meal_slot |
| meal_edited | 编辑饮食记录 | meal_slot |
| food_search_performed | 搜索完成 | query_length, result_count, sources[], duration_ms |
| food_self_created | 创建自建食物 | category |
| weight_recorded | 体重记录成功 | — |
| dashboard_viewed | 查看仪表盘 | local_date（用于聚合统计） |
| data_exported | 导出数据 | format, tables[], record_count |
| sync_batch_completed | 备份批完成 | table, row_count, duration_ms |
| sync_failed | 备份失败 | error_code, retry_count |

#### 埋点隐私声明
SC-14 设置页:
- 「我们收集匿名的使用数据来改进产品。数据不包含你的食物记录、体重等健康信息。」
- 默认开启，可关闭

#### 验收
- [ ] 所有 15 个事件在对应时机正确触发
- [ ] 开关关闭后不写入新事件
- [ ] 事件 JSON 格式正确
- [ ] 随备份通道上传

---

### REQ-014: 设置中心与关于

#### SC-14 设置中心

四个分组:

**账号**
- 邮箱展示（未验证: 橙色徽标 + 「重新发送验证邮件」按钮）
- 修改密码 → 弹窗
- 退出登录 → 确认弹窗
- 注销账号（红色文字）→ 二次确认

**数据**
- 年龄更新提示（条件展示，REQ-003 B5）
- 自建食物管理 → SC-12
- 云端备份（副标题: 状态，如「已同步」「待备份 3 条」）→ 触发备份
- 从云端恢复（副标题: 上次备份时间）→ 恢复流程
- 数据导出 → SC-16

**偏好**
- 参与使用数据采集（Switch）

**关于（SC-17）**
- App 图标 + 名称 + 版本号
- 数据来源: FatSecret 署名（服务条款要求）、Open Food Facts 署名
- 隐私政策（链接, 暂为占位）
- 数据采集说明（同 REQ-013 A2）
- 诊断信息（折叠，展示: app_version, os_version, device_model, sync_state 统计）
  * 展示所有 62 个错误码的激活状态
  * 仅供调试，不暴露给普通使用流程

#### 验收
- [ ] 所有入口正确跳转
- [ ] 邮箱未验证时展示橙色徽标 + 重发按钮
- [ ] 注销账号二次确认
- [ ] FatSecret 署名完整
- [ ] 诊断信息可查看（隐藏入口: 长按关于页版本号 5 次）
```

---

# 三、通用代码规范提示词

> 适用于任何阶段的代码实现任务

```
## 通用代码规范

在实现以上需求时，请严格遵守以下规范:

### Kotlin 代码风格
- 使用 data class 定义 Entity/Model
- Repository 返回 `Result<T>` 或 `Flow<List<T>>`
- ViewModel 使用 `StateFlow` / `SharedFlow` 暴露状态
- Compose UI 使用 `collectAsStateWithLifecycle()` 收集 Flow
- 所有 suspend 函数在 `viewModelScope` 或 `Dispatchers.IO` 中调用
- 禁止在主线程执行 Room 写入/网络请求（Room 已有 `@Transaction` 保护，但仍应在 IO 调度器）

### Compose UI 规范
- 每个 Screen 是一个 `@Composable` 函数，接收 ViewModel 和 NavController
- 使用 `Scaffold` + `TopAppBar` 作为页面骨架
- 使用 `remember` + `mutableStateOf` 管理本地 UI 状态
- 列表使用 `LazyColumn` / `LazyRow`
- 输入框使用 `OutlinedTextField`（Material 3）
- 按钮使用 `Button` / `FilledTonalButton` / `TextButton`
- 使用 `AnimatedVisibility` 做条件展示动画
- 页面切换使用 Compose Navigation (`NavHost`)
- 错误提示使用 `Snackbar`（通过 `SnackbarHostState`）
- 禁用 `Modifier.fillMaxSize()` 滥用（仅在容器层使用）

### 架构规范
- 严格遵守分层: UI → ViewModel → Repository → DAO/API
- UI 层不直接访问 DAO，必须通过 Repository
- ViewModel 不持有 Context 引用
- 使用 Hilt `@HiltViewModel` + `@Inject constructor`
- 一个 Repository 一个 `@Module` `@Provides` 或使用 `@Binds`

### 测试规范（可选，不强制全覆盖）
- DAO 测试: `@RunWith(AndroidJUnit4::class)` + Room in-memory
- 算法测试: 纯 Kotlin 单元测试（JUnit 4/5）
- BMR 计算器必须包含 5 行示例校验表的逐行测试用例（TC-ALG-01）

### Git 提交规范
- feat: 新功能 (feat: add weight recording screen)
- fix: 修复 (fix: meal entry quantity precision)
- refactor: 重构
- chore: 杂项 (chore: update dependencies)

### 错误处理
- 所有网络请求包装 try-catch，异常转为 Result.failure()
- 所有 Room 操作包装 try-catch
- 用户可见错误经 strings.xml 映射，不直接抛给 UI

### 性能
- 避免在 Compose 函数中直接执行耗时操作
- 搜索结果列表使用 `key` 参数（`LazyColumn { items(list, key = { it.id }) }`）
- 大列表使用 `remember` + `derivedStateOf` 做分页加载触发判定
```

---

# 四、使用指南

## 4.1 推荐的使用顺序

1. **首次对话**: 发送「1.1 完整版项目上下文提示词」+ 附上 PRD 文档链接（或摘要）
2. **Phase 0**: 发送「2.0-A 项目脚手架」→ 确认编译通过 → 发送「2.0-B Room 数据库」
3. **Phase 1**: 发送「2.1 认证系统」
4. **Phase 2**: 发送「2.2-A 目标设定」→ 确认 → 发送「2.2-B 仪表盘」
5. **Phase 3**: 依次发送「2.3-A 饮食记录」→「2.3-B 食物搜索」→「2.3-C 最近常吃+自建食物」
6. **Phase 4**: 发送「2.4 收尾需求」（四条合并）
7. **所有阶段**: 附带「三、通用代码规范提示词」

## 4.2 每次对话的启动模板

```
【继续「健康打卡」App 开发】
当前阶段: {Phase X}
需求编号: {REQ-00X}
技术栈: Android + Kotlin + Compose + Room(本地唯一真源) + Supabase(备份) + Hilt
上一阶段完成情况: {简述已完成的功能}

---

{粘贴对应阶段的提示词}

---

## 通用代码规范

{粘贴「三、通用代码规范提示词」}
```

## 4.3 与模型的交互原则

- **初次对话**: 让模型先输出理解摘要，确认方向正确再写代码
- **每完成一个文件**: 让模型汇报已完成/待完成清单
- **遇到歧义**: 让模型列清单提问，不要假设
- **编译错误**: 复制错误日志给模型，要求分析根因而非修补症状
- **PRD 优先**: 任何实现细节以 PRD 为准，模型猜测 → 要求查 PRD

---

> 本文档与 PRD v2.0 配套使用。PRD 变更时同步更新此文档。
> `${变量}` 标记的占位符在使用前替换为实际值。
> 所有提示词经过设计，可在 Claude/GPT-4/Gemini Pro 等主流模型上正常运行（单条最长约 3000 tokens）。
