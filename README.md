# 健康打卡（Health Check-in）

Android 健康管理应用 MVP（v0.1），基于 PRD v2.0 开发。

## 技术栈

- **客户端**: Kotlin + Jetpack Compose + Material 3
- **本地存储**: Room（唯一真源）
- **依赖注入**: Hilt
- **网络**: Retrofit + OkHttp + kotlinx.serialization
- **认证/备份**: Supabase Auth + PostgreSQL
- **图表**: Vico

## 项目结构

```
app/src/main/java/com/example/healthcheckin/
├── data/           # Room、Retrofit、Repository 实现
├── domain/         # 算法、Repository 接口
├── di/             # Hilt 模块
├── ui/             # Compose 页面与导航
└── util/           # 日期、精度、校验、UUID v7
```

## 本地开发

### 前置条件

- Android Studio Ladybug 或更新版本
- JDK 17
- Android SDK 35

### 配置 Supabase

在 `gradle.properties` 或命令行中设置：

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

### 配置邮件（注册验证 / 找回密码）

Auth 邮件由 **Supabase** 发送，App 内不包含 SMTP 密钥。

1. 复制 `secrets.properties.example` 为 `secrets.properties`（已在 `.gitignore` 中，勿提交）
2. 填入 Supabase 与 163 SMTP 信息（见下方「获取 Token」）
3. 运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\configure-supabase-auth.ps1
```

脚本会写入 SMTP、允许未验证登录、以及 Deep Link `healthcheckin://reset-password`。

**获取 Supabase Access Token**

1. 打开 [Supabase Account Tokens](https://supabase.com/dashboard/account/tokens)
2. 点击 **Generate new token**，复制 token（只显示一次）

**获取 Project Ref 与 anon key**

1. 打开项目 → **Project Settings** → **General** → **Reference ID**（即 `PROJECT_REF`）
2. **Project Settings** → **API** → **Project URL** 与 **anon public** key

也可在控制台手动配置：**Authentication** → **SMTP Settings**（163：`smtp.163.com`，端口 `465`，用户名为完整邮箱，密码为授权码而非登录密码）。

### 初始化云端数据库

在 Supabase SQL Editor 中执行 [`supabase/init.sql`](supabase/init.sql)。

部署食物搜索 Edge Function（REQ-006）：

```bash
supabase secrets set FATSECRET_CLIENT_ID=your-id FATSECRET_CLIENT_SECRET=your-secret
supabase functions deploy food-search
```

部署账号注销 Edge Function（REQ-002 / REQ-014）：

```bash
supabase functions deploy account-delete
```

未配置 FatSecret 密钥时，Function 仍会返回 Open Food Facts 结果；客户端离线时自动降级为本地 + 24h 缓存。

### 构建

```bash
./gradlew assembleDebug
./gradlew test
```

## 当前进度（Phase 0 + Phase 1 骨架）

| 模块 | 状态 |
|------|------|
| Gradle 脚手架 + Version Catalog | ✅ |
| 核心工具类（DateTime/Unit/Precision/Validators/UUID v7） | ✅ |
| 核心算法（BMR/TDEE/Budget/Macro/MealSlot）+ 单元测试 TC-ALG-01 | ✅ |
| Room 数据库（12 张表 + DAO） | ✅ |
| Supabase DDL + RLS | ✅ |
| 认证骨架（SessionManager + AuthApi + 登录/注册 UI） | ✅ |
| **REQ-003 目标设定向导（SC-05，5步）** | ✅ |
| **REQ-004 仪表盘首页（SC-06）** | ✅ |
| **REQ-005 饮食记录（SC-07/08/10）** | ✅ |
| **REQ-006 食物搜索（本地 + 远程 + 缓存）** | ✅ |
| **REQ-007/008 最近常吃 + 自建食物** | ✅ |
| 体重曲线 REQ-009 | ✅ |
| 云端备份 REQ-011 | ✅ |
| **REQ-012 数据导出（SC-16）** | ✅ |
| **REQ-013 埋点采集** | ✅ |
| **REQ-014 设置中心与关于（SC-14/17/18）** | ✅ |
| v0.1 收尾（密码重置 Deep Link、健康提示补录、预建食物库） | ✅ |

## 架构原则

- **本地优先**: Room 为唯一真源，UI 通过 Flow 订阅
- **UUID v7** 主键 + **软删除**（`deletedAt`）
- Token 存 **EncryptedSharedPreferences**，禁止写入 Room/日志
- 网络失败不影响本地写入成功

## 文档

- PRD: [`deliverables/product-strategy/prd-health-checkin-official-v2.0-2026-07-29.md`](deliverables/product-strategy/prd-health-checkin-official-v2.0-2026-07-29.md)
- 开发提示词: [`deliverables/product-strategy/dev-prompts-health-checkin-v2.0.md`](deliverables/product-strategy/dev-prompts-health-checkin-v2.0.md)
- 路线图: [`deliverables/product-strategy/roadmap-health-checkin-mvp-2026-07-29.md`](deliverables/product-strategy/roadmap-health-checkin-mvp-2026-07-29.md)

## 下一步

v0.1 MVP 功能已全部完成。按路线图进入 **v0.5 (P1)**：

1. **REQ-015** 身体维度记录
