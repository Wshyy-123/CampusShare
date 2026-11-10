# CampusShare

校享 CampusShare——校园公共场地预约与管理平台。

当前分支 `feature/XIA-5-XIA-6` 实现了：

- **XIA-5 创建预约**：资源/时段选择、开放与教学占用检查、冲突检查、`clientRequestId` 幂等、座位/体育到馆期限。
- **XIA-6 我的预约**：本人预约分页列表、状态筛选、空状态、预约详情与越权访问拒绝。

## 目录

```text
backend/            Spring Boot 3.5.16 + Java 21 + MyBatis 3.0.5
frontend/           Vue 3 + TypeScript + Vite
docs/api/           预约与临时资源读取的 OpenAPI 契约
docs/database/      001_schema.sql、002_demo_data.sql
docs/decisions/     ADR-001：预约垂直切片与 XIA-11/14 的衔接
docs/testing/       XIA-5/XIA-6 测试计划与测试用例
docs/verification/  XIA-5/XIA-6 实际验证记录
```

## 快速开始

1. 准备 MySQL 8.0/8.4，在空库执行 `docs/database/001_schema.sql` 与 `docs/database/002_demo_data.sql`（Windows 客户端加 `--default-character-set=utf8mb4`）。
2. 启动后端（默认 8081，因为本机 8080 已被其他进程占用）：

   ```powershell
   cd backend
   $env:JAVA_HOME = 'D:\Program Files\Java\jdk-21.0.9'
   $env:CAMPUSSHARE_DB_URL = 'jdbc:mysql://127.0.0.1:3306/campusshare?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
   $env:CAMPUSSHARE_DB_USERNAME = 'root'
   $env:CAMPUSSHARE_DB_PASSWORD = '你的本地密码'
   .\mvnw.cmd spring-boot:run
   ```

3. 启动前端并访问 `http://localhost:5173`：

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   演示账号：`student01`、`student02`、`teacher01`，密码均为 `Campus@123`。

## 验证与检查

```bash
cd backend && ./mvnw test          # H2；并发行锁测试跳过
cd frontend && npm run build       # vue-tsc + vite build
```

真实 MySQL 8.0.44 与端到端检查结果见 `docs/verification/XIA-5-XIA-6.md`。GitHub Actions（MySQL 8.4 service）配置见 `.github/workflows/ci.yml`。

## 当前边界

- `auth` 是 XIA-1 落地前的最小会话登录，不是 XIA-1 的完整交付。
- `resources` 是 XIA-4 落地前的临时只读依赖；正式接口交付后应替换。
- XIA-7 取消、XIA-8 签到/签退、XIA-11 座位超时任务不在本分支范围。
- 详细约定仍以 `docs/` 下三份团队文档为准。
