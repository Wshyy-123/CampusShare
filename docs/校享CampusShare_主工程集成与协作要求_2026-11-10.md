# 主工程集成与协作要求

更新日期：2026-11-10。适用全体开发成员及其Agent。

本次将XIA-1/2、XIA-3/4、XIA-11/14现有分支代码收录到main，分别通过PR #4、#5、#3保留历史。收录代码不表示已经接入主应用或通过整站验收。后续统一按本文件进行工程接入，业务规则仍按《项目背景与业务规则》。

## 一、唯一开发和运行入口

| 内容 | 后续业务代码位置 |
|---|---|
| 主后端 | campus-share-backend/；唯一生产Spring Boot应用 |
| 后端Java根包 | org.campus.campussharebackend；保留现有CampusShareBackendApplication |
| 后端业务 | 在主后端根包下按auth、resource、schedule、reservation、attendance、job等业务分包 |
| 主前端 | frontend/；唯一前端package.json、Vite配置和锁文件 |
| 前端业务 | frontend/src/下的views、api、types、router、components、styles |
| 数据库 | 当前主后端campus-share-backend/docs/datebase/内版本化SQL；保留现有路径，避免多人另建互不兼容脚本目录 |
| 接口 | docs/api/统一契约 |

backend/src、backend/XIA-3-schedule-management、backend/XIA-4-resource-availability、backend/reservation-lifecycle、frontend/XIA-3-XIA-4目前保留为已合入的来源模块。后续默认不在这些独立工程中新增产品功能，也不新增其他按故事编号划分的独立应用。迁移完成并验证后，再在专门清理PR中处理旧文件，不先删来源代码。

此处“修改主工程”指修改以上共享目录，不是要求所有人直接推送main。仍采用功能分支、PR和另一名开发人员检查。

## 二、同学开始开发时怎么做

先保存自己的未提交工作，再获取最新代码。新集成任务从最新main创建功能分支：

```bash
git fetch origin
git switch -c feature/XIA-1-integrate origin/main
```

上例仅展示XIA-1的分支名，按自己真实工作项替换；分支已存在时切换该分支，不重复创建。已有开发分支先合入最新主线：

```bash
git fetch origin
git switch feature/XIA-1-integrate
git merge origin/main
```

正常解决冲突并保留他人修改，不使用强制覆盖。旧分支已经合入后，不继续基于其过时目录做下一轮独立应用；从最新main开新分支更容易核对集成范围。

开始前阅读顺序：README → 本文件 → 项目背景与业务规则 → 对应前端/后端约定。使用Agent时让其先读仓库AGENTS.md。

## 三、现有模块怎样迁入

| 来源任务 | 接入要求 |
|---|---|
| XIA-1/2登录与资源 | 将auth、user、资源管理和必要配置迁到主后端业务包；使用现有Java 21、Boot 3.5.16、MyBatis 3.0.5。将JPA Repository改为统一MyBatis访问，不将3.3.5的独立pom复制覆盖主pom |
| XIA-3/4时段与查询 | 将时段维护、可用计算和查询迁入主后端；保留可用区间算法，数据读取改接共享表/Mapper，移除临时Basic用户及独立SecurityConfig，使用XIA-1的会话和CSRF |
| XIA-3/4前端 | 合并业务页面、路由、类型和请求封装到frontend/src；按现有package.json增补必需依赖并更新锁文件，不能拿独立App.vue、main.ts、Vite配置整份覆盖主工程 |
| XIA-11/14生命周期 | 将业务服务迁入主包，或以库方式显式依赖并导入配置；统一资源锁、预约状态、时间和共享表。创建预约/刷卡入口实际调用共同逻辑，不只把jar放进仓库 |
| XIA-5/6、XIA-7/8及新流程 | 直接在主工程实现预约、本人记录、取消、地点码签到/签退与刷卡离场，复用已有身份/资源/状态，不另起一套用户表或后端 |

迁包时同步import、Mapper扫描和测试包；不要留下多个生产SpringBootApplication、重复同名配置Bean或互相竞争的SecurityFilterChain。保持一个DataSource和一套登录身份，不用开放全部接口代替解决集成问题。

## 四、共享表和接口先统一

- 用户采用app_user.account，资源采用resource的最小预约单位，预约关联resource_id；与已有主库不一致的username、venue_resource、campus_resource/resource_unit等模型通过映射或明确迁移接入，不同时保留数套互不关联的资源ID。
- 按后端约定统一resource_type、状态和原因编码；字段、表、索引和约束变更新增顺序编号SQL，不覆盖已经应用的001/002脚本。
- 不直接执行各独立模块的schema.sql覆盖共享库，也不启用自动建表删除数据来“修好”字段不匹配。
- 同一资源的查询、预约、签到、离场、超时和停用使用共享规则；身份取服务端会话，不靠前端固定用户ID或临时Basic账号。
- 接口先同步docs/api/中的字段、权限、时间和错误结果，前后端使用同一份契约，不各自发明相似地址。

## 五、集成顺序与协作方式

1. 先由开发团队安排共享工程/数据库的牵头维护者，提交最小公共结构、数据字典和会话基础。此处不新增具体成员姓名或人员分配。
2. 登录资源、时段查询、预约到馆、生命周期各负责人基于公共基础持续同步；同一共享实体/配置由牵头者协调变更，避免多人各改一版。
3. 尽早跑通登录→查可用→预约→到馆→离场/结束/未到释放的最小链路，再扩展其他页面与异常场景。
4. 教室/体育15分钟、结束释放、座位30分钟与刷卡离场等继续按最新业务规则处理，不因迁移恢复旧规则。

## 六、提交PR时的检查

- 功能已进入主工程相应目录，或者明确说明本PR是共用契约/纯文档；“独立模块能运行”不能作为主工程接入完成证据。
- 后端在campus-share-backend执行mvnw.cmd verify（其他系统./mvnw verify）；前端在frontend执行npm ci和npm run build，记录真实结果。
- 数据库、登录和手机相关验证写实际环境及结果；未执行就写未执行，不把测试源码、target产物或单模块CI记成全链路通过。
- 检查主应用实际加载新增接口/任务、前端主路由能访问页面、状态与数据在同一个库内联通。
- PR关联真实PingCode编号，说明迁移来源、目标目录、共享接口/SQL变化和验证；由另一名开发人员检查后合并。
- 不再提交target、node_modules、dist等新构建产物；当前已合入的target跟踪清理单独处理，不为隐藏问题删改来源历史。

本文件规定后续开发和接入方式，不声称上述迁移、构建、数据库或手机测试已经完成。
