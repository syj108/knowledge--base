# IDaaS CIAM

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   **产品是做什么的**：IDaaS CIAM 是阿里云面向顾客身份管理（Customer Identity and Access Management, CIAM）推出的云原生身份即服务（IDaaS）解决方案，聚焦**统一身份整合、用户体验优化、安全风控与合规治理**四大核心能力，服务于企业面向公众/会员的数字化身份体系构建，支撑 App、小程序、H5、Web、IoT 等多端一致、无摩擦的身份体验。  
        阿里云 IDaaS CIAM（Consumer Identity and Access Management）是面向消费者场景的身份与访问管理解决方案，聚焦于提升终端用户旅程体验、统一身份治理、加速业务接入、保障安全合规，支撑企业数字化转型中的“人”要素底座。

    *   **各版本新增功能的简要说明**：当前公有云版本已全面支持 MAU 弹性计费、账号去重与僵尸清理、分步注册、动态 MFA 与行为风险建模、条款统一管理及《个人信息保护法》《数据安全法》合规适配；同时已全面支持 OneID 身份融合架构、全链路无感认证配置、多租户隔离的弹性伸缩能力、等保三级/PCI DSS/ISO 27001 等多维合规基线，并持续增强本土化（如微信/支付宝/银联快捷登录、手机号一键登录、网证核验）与全球化（GDPR/CCPA 兼容策略引擎、多语言自适应门户）双轨能力。后续将增强跨云身份联邦、隐私计算辅助的身份图谱构建、AIGC 驱动的自助式身份策略编排等能力（详见 [[IDaaS/路线图|IDaaS CIAM 路线图]]）。

    *   **能力涉及到产品、组件**：  
        - 核心产品：IDaaS CIAM（含统一认证中心、用户自服务平台、风控引擎、条款管理中心、账号治理工具、消费者门户、OneID 融合引擎、策略引擎、隐私合规工作台、应用网关）  
        - 关键组件：  
          - **AuthCore**（认证网关）：承载所有登录/注册/登出/令牌颁发流程  
          - **UserHub**（用户主数据中心）：唯一可信身份源，支撑多ID绑定与懒加载同步  
          - **RiskEngine**（AI驱动风控服务）：实时评估风险等级，输出标准化风险信号  
          - **ConsentManager**（同意与条款服务）：支持多版本条款发布与GDPR/PIPL合规审计  
          - **SyncBridge**（多源身份同步适配器）：提供预置连接器，支持增量/全量同步  
          - **消费者门户（Consumer Portal）**：品牌可定制的注册/登录/资料管理/隐私设置界面，内置防爬、人机识别、行为风控插件  
          - **OneID 身份融合引擎**：基于实体解析与图谱关联技术，实现跨渠道、跨系统的身份归一，输出唯一消费者ID（CID）  
          - **策略引擎（Policy Engine）**：声明式策略语言（支持 Rego），支持动态风险评分、实时合规拦截、分级授权（ABAC+RBAC混合）  
          - **隐私合规工作台**：内置 GDPR/PIPL/CCPA 合规模板，支持自动化数据主体权利响应（DSAR）、数据地图绘制、第三方共享审计、Cookie 同意管理（CMP）集成  
          - **应用网关（App Gateway）**：轻量级反向代理，提供 OIDC/SAML 协议转换、JWT 签名校验、会话加密存储、细粒度 API 权限控制  

*   对外介绍架构图
    
    *   中心端与端侧的部署架构图、容器组在物理机上的部署关系  
        → 公有云全托管部署：所有组件以 K8s 容器化形态部署于阿里云 ACK 集群，按租户隔离 + 多可用区高可用设计；端侧通过标准 OAuth2.0/OIDC 协议、轻量 SDK / JS Bridge / OpenAPI 接入，支持 Web / iOS / Android / H5 / 小程序 / IoT 等全触点。  
        → 中心端采用多可用区高可用部署，核心服务以容器化（ACK）方式部署于阿里云公共云环境；端侧通过轻量 SDK / JS Bridge / OpenAPI 接入 Web / App / 小程序 / IoT 等全触点。

    *   各组件之间的数据流向图  
        → 用户请求经 AuthCore / 应用网关鉴权 → 触发 UserHub / identity_master 查证身份状态 → RiskEngine / 策略引擎实时评估风险等级与合规策略 → 决策是否放行/挑战 MFA/拦截 → ConsentManager / 隐私合规工作台同步记录条款同意快照与 DSAR 任务 → 日志与事件异步写入 SLS 与 DataHub 供审计与分析。  
        → 数据流向：终端用户请求 → 应用网关（路由+会话管理）→ 认证中心（多因子/生物识别/风险感知）→ 身份目录（统一用户主数据）→ 策略引擎（实时风控+合规检查）→ 审计中心（全链路操作留痕）；同步向营销系统、CRM、CDP 等下游推送脱敏身份事件（通过 [[IDaaS/事件总线对接规范|事件总线]]）。

    *   与上下游系统（Tianji、OpsApi 等）的依赖关系  
        - 依赖：  
          - [[Tianji/资源纳管规范|Tianji]]（资源纳管与计量计费对接，支撑 MAU 自动统计）  
          - ARMS（全链路监控埋点）  
          - SLS（日志采集与审计）  
          - OSS（静态资源托管，如登录页、协议 PDF）  
          - [[OpsApi/账号生命周期接口|OpsApi]]（对接企业 HR/SCM 系统完成入职/离职自动开户/销户）  
          - [[VPC/私网DNS配置|VPC]]、[[SLB/HTTPS监听配置|SLB]]（协同保障公网接入安全与流量分发）  
          - [[ECS/日志采集配置|ECS]]（日志服务 SLS 集中分析）  
        - 被依赖：企业业务中台、CRM、CDP、营销平台等通过 OpenAPI 或 Webhook 订阅用户身份事件（如注册成功、实名完成、条款更新、DSAR 请求）。

    *   参考：  
        *   ![IDaaS CIAM 架构概览](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/8oLl952KrmxMNlap/img/ciam-arch-overview-v2.png)  
        *   ![IDaaS CIAM 架构概览](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/8oLl952KrmxMNlap/img/idas-ciam-arch-overview-v2.png)  
        *   （注：实际链接需由 IDaaS 产品团队维护，此处为占位示意）

*   各核心组件能力详细说明
    
    | 组件 | 能力说明 | 关键特性 |
    |---|---|---|
    | **AuthCore** | 统一认证网关，承载所有登录/注册/登出/令牌颁发流程 | 支持 OIDC/OAuth2.0/SAML2.0；内置社交登录（微信/支付宝/Apple ID）；可插拔认证因子（短信/邮件/生物识别/MFA）；支持自定义登录页白标 |
    | **UserHub** | 用户主数据中心，唯一可信身份源 | 支持多ID绑定（手机号+微信OpenID+邮箱）、账号去重建议与人工确认工作流、僵尸账号识别（365天未活跃+无资产关联）、懒加载式跨系统账号同步 |
    | **RiskEngine** | 实时风控引擎，基于用户行为建模的风险决策服务 | 千人千面风险模型（IP/设备/时段/操作序列）；支持策略编排（规则+AI评分）；异常操作主动触发二次认证或锁定；输出标准化风险信号（`risk_score`, `risk_reason`）供调用方决策 |
    | **ConsentManager** | 条款与同意统一管理中心 | 支持多版本条款发布、用户级同意快照存储、跨业务系统条款同步（通过 EventBridge）、GDPR/PIPL 合规审计报告一键生成 |
    | **SyncBridge** | 异构身份源同步适配器 | 提供预置连接器（LDAP/AD/MySQL/Oracle/主流CRM），支持增量/全量同步；冲突解决策略可配置（Last-Write-Win / Manual-Review） |
    | **消费者门户（Consumer Portal）** | 提供品牌可定制的注册/登录/资料管理/隐私设置界面 | 支持多语言、多主题、埋点分析，内置防爬、人机识别、行为风控插件 |
    | **OneID 身份融合引擎** | 基于实体解析（Entity Resolution）与图谱关联技术，支持跨渠道（Web/App/小程序/线下POS）、跨系统（会员/订单/营销/客服）的身份归一 | 输出唯一消费者ID（CID），支持血缘追溯与冲突消解；支持模糊匹配（设备指纹/WiFi MAC/SIM卡/行为轨迹）与人工审核队列 |
    | **策略引擎（Policy Engine）** | 声明式策略语言（支持 Rego），支持动态风险评分（设备指纹+IP信誉+行为序列）、实时合规拦截（如未成年人保护模式、地域访问限制）、分级授权（ABAC+RBAC混合） | 支持「年龄属性+应用标签」组合策略，结合实名认证结果（对接公安部 eID 或网证通）实时生效 |
    | **隐私合规工作台** | 内置 GDPR/PIPL/CCPA 合规模板 | 支持自动化数据主体权利响应（DSAR）、数据地图绘制、第三方共享审计、Cookie 同意管理（CMP）集成；满足等保三级、JR/T 0171-2020、GM/T 0028，支持国密 SM2/SM4 加解密，审计日志留存≥180天 |
    | **应用网关（App Gateway）** | 轻量级反向代理，提供 OIDC/SAML 协议转换、JWT 签名校验、会话加密存储、细粒度 API 权限控制 | 兼容遗留系统零改造接入；所有 HTTPS 流量强制经由 SLB 进行 TLS 卸载与 HTTP/2 支持；健康检查路径 `/healthz` 由其提供 |

*   与阿里云其他产品的关系
    
    *   与 VPC、ECS、SLB 等 Top30 产品的交互方式，有什么影响。  
        - **VPC**：IDaaS CIAM 默认部署于客户指定 VPC 内（PrivateLink 接入模式），或通过公网 Endpoint 提供服务；不直接占用客户 VPC 资源，但建议客户将业务应用与 IDaaS 置于同地域 VPC 以降低延迟。CIAM 所有公网入口必须通过 VPC 内 SLB + WAF 统一接入，禁止直暴露 EIP；内网调用依赖 VPC 对等连接或云企业网（CEN）打通至客户私有业务系统。  
        - **SLB**：AuthCore / 应用网关前端由阿里云 ALB 统一负载，客户无需自行部署 SLB；若客户启用私网接入，需配置 ALB PrivateLink Endpoint。SLB 健康检查路径 `/healthz` 由应用网关提供，失败将触发自动摘流。  
        - **ECS**：IDaaS 为全托管 SaaS 服务，**不依赖客户 ECS 资源**；客户 ECS 上运行的应用仅需通过 HTTPS 调用 IDaaS OpenAPI 或嵌入 JS SDK，无反向依赖。审计日志、运营日志默认投递至 ECS 挂载的 SLS Logstore；关键组件（如策略引擎）支持 ECS 自建部署模式（需客户自行维护 OS 安全补丁）。  
        - **影响边界**：  
            ✅ **会造成的影响**：AuthCore / 应用网关公网 Endpoint 不可用将导致所有依赖 IDaaS 认证的前端无法登录；ConsentManager / 隐私合规工作台故障将阻断新用户注册（因条款签署为必经环节）；OneID 融合中断将导致用户画像更新延迟、CID 生成失败或重复；策略引擎异常将导致风控与合规拦截失效。  
            ❌ **不会造成的影响**：IDaaS 故障**不影响**客户已有 ECS 实例运行、数据库读写、SLB 转发、VPC 网络连通性；**不接管**客户存量用户数据库，UserHub / identity_master 仅作为新身份主库，旧系统仍可独立运行（需客户自行做双写或迁移）；不影响已有会话的业务功能访问（会话状态本地缓存 15min）；不影响非消费者类身份（如员工 SSO、开发者 API Key）的认证流程（属 IDaaS B2E/B2D 子域，物理隔离）。

### QA（高频问答）：

*   Q：IDaaS CIAM 是否支持私有化部署？  
    A：当前仅提供公有云全托管服务（含金融云），暂不支持纯私有化交付；但支持 VPC 私网接入、专属密钥加密、BYOK 密钥管理等满足高等级安全要求的混合云模式。

*   Q：MAU 如何统计？是否包含测试账号、机器人流量？  
    A：MAU = 当月任意一天完成至少 1 次成功认证（login/sso）的**唯一自然人用户数**；系统自动过滤测试手机号（如 13900139000 类号段）、已标记为“测试用户”的账号、无真实设备指纹的爬虫请求；统计口径与 Tianji 计费系统完全一致。

*   Q：能否保留原有用户密码策略，如必须含大小写字母+数字+特殊字符且 90 天强制更换？  
    A：可以。UserHub 支持租户级密码策略配置（长度、复杂度、过期周期、历史密码禁止复用等），兼容传统 IAM 强管控要求；同时支持渐进式策略——对新注册用户启用强策略，对存量用户平滑过渡。

*   Q：风控引擎的模型是否可调参？能否对接客户自有的风控系统？  
    A：RiskEngine 提供两级能力：① SaaS 层开箱即用 AI 模型（不可调参，持续迭代）；② 支持通过 Webhook 将原始风险事件（含设备/IP/行为日志）实时推送至客户风控系统，由客户自主决策并回调 IDaaS 执行拦截/放行（需开通高级风控 License）。

*   Q：条款管理是否支持多语言、多区域差异化条款？  
    A：支持。ConsentManager 允许按「国家/地区+语言+业务线」维度发布独立条款版本（如：中国大陆简体中文版《隐私政策》、欧盟英文版《GDPR Notice》），用户首次访问时自动匹配地理与语言上下文，并记录精确到版本号的同意凭证。隐私合规工作台亦支持多语言自适应门户与 GDPR/CCPA 兼容策略引擎。

*   Q：CIAM 是否支持与企业自有会员系统双向同步？  
    A：支持。通过 [[IDaaS/会员系统对接方案|标准对接方案]] 提供 RESTful API + Webhook 双通道，支持增量/全量同步，字段映射与冲突策略可配置。

*   Q：OneID 如何处理同一用户在不同渠道使用不同手机号注册的情况？  
    A：基于设备指纹、WiFi MAC、SIM 卡信息、行为轨迹等多维信号进行模糊匹配；支持人工审核队列与业务规则干预（如“同一身份证号下仅允许 1 个活跃 OneID”）。

*   Q：是否满足金融行业强监管要求（如等保2.0三级、JR/T 0171-2020）？  
    A：满足。已通过等保三级测评（报告编号：ALI-SEC-2024-XXXX），密码模块符合 GM/T 0028，支持国密 SM2/SM4 加解密，审计日志留存≥180天。

*   Q：能否限制某类用户（如未成年人）仅能访问特定应用？  
    A：可以。在策略引擎中配置「年龄属性+应用标签」组合策略，结合实名认证结果（对接公安部 eID 或网证通）实时生效。

### 组件接口人、研发负责人等角色信息：

| 组件 | 研发负责人（阿里云） | L1运维权限（值班SRE） | L2运维权限（高级SRE） | 必须升级产研场景 |
|---|---|---|---|---|
| AuthCore | 张伟（IDaaS认证组） | 查看ALB日志、重启Pod、切换灰度流量比例 | 修改OAuth2.0 Scope白名单、调整RateLimit阈值 | 认证流程逻辑变更、新增社交登录渠道、OIDC Provider配置错误导致大面积500 |
| UserHub | 李婷（IDaaS数据组） | 执行账号冻结/解冻、触发单用户数据同步 | 手动触发全量去重任务、调整僵尸账号判定阈值 | 主键冲突导致写入失败、跨租户数据泄露疑似事件 |
| RiskEngine | 王磊（IDaaS风控组） | 查看风险评分分布、启停某条风控规则 | 调整单条规则权重、导入客户自定义设备指纹库 | 模型误判率突增>5%、AI服务OOM导致拒绝服务 |
| ConsentManager | 陈敏（IDaaS合规组） | 发布新条款版本、查看用户同意记录 | 回滚条款版本、导出合规审计包 | 条款内容渲染异常致用户无法签署、同意快照丢失 |
| 消费者门户 | 张伟（IDaaS-FE） | 修改UI主题、开关埋点、重启Pod | 修改登录流程逻辑、调整风控规则阈值 | 登录流程逻辑变更、UI样式大规模异常 |
| OneID 融合引擎 | 李婷（IDaaS-IdentityGraph） | 查看融合任务状态、重试失败任务 | 调整实体解析算法参数、新增数据源接入 | 融合成功率持续低于95%、CID生成冲突激增 |
| 策略引擎 | 王磊（IDaaS-Policy） | 启停策略、导入导出策略包、查看实时评分日志 | 修改Rego策略语法、新增风险因子接入 | 策略误判率突增、Rego编译失败导致全局拦截失效 |
| 隐私合规工作台 | 陈静（IDaaS-Compliance） | 配置DSAR响应模板、启用/禁用CMP | 修改数据地图扫描逻辑、对接新监管API | DSAR超期未处理、数据地图扫描失败导致合规报告缺失 |
| 应用网关 | 刘洋（IDaaS-Gateway） | 切换TLS证书、调整超时参数、查看访问日志 | 修改协议转换逻辑、新增OIDC Provider配置 | 协议转换异常导致大量401/403、TLS握手失败率突增 |

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `AuthCore_5xx_Rate_High` | P0 | ARMS监控 > 1%持续5min | AuthCore | 5xx错误率超阈值 | 认证网关后端服务异常，影响全部登录流程 | [[IDaaS/应急手册/AuthCore-5xx|AuthCore 5xx 高频告警处置]] | ✅ EOCC-2024-CIAM-001 |
| `UserHub_Sync_Lag_Over_1h` | P1 | DataHub消费延迟 > 3600s | UserHub | 跨系统账号同步延迟超1小时 | 新注册用户信息未及时同步至CRM/CDP，影响营销触达 | [[IDaaS/应急手册/UserHub-SyncLag|UserHub 同步延迟处置]] | ✅ KB-2024-CIAM-SYNC-01 |
| `RiskEngine_Model_Inference_Fail` | P1 | SLS日志含"model load failed" | RiskEngine | AI模型加载失败或推理超时 | 风控策略失效，默认降级为规则引擎，部分高风险请求可能漏检 | [[IDaaS/应急手册/RiskEngine-ModelFail|RiskEngine 模型加载失败]] | ✅ EOCC-2024-CIAM-003 |
| `ConsentManager_Terms_Not_Signed` | P2 | API调用返回`403 missing_consent` | ConsentManager | 新用户注册时条款服务不可用 | 注册流程中断，用户无法完成首次登录 | [[IDaaS/应急手册/ConsentManager-Down|ConsentManager 不可用]] | ✅ KB-2024-CIAM-CONSENT-01 |
| `MAU_Quota_Exceeded` | P2 | Tianji计费API返回quota_limit_reached | 全局（Tianji集成） | 当月MAU用量达购买额度100% | 新用户注册/登录被限流，老用户不受影响 | [[IDaaS/应急手册/MAU-OverQuota|MAU 配额超限处置]] | ✅ KB-2024-CIAM-MAU-01 |
| `CIAM-OneID-FusionRate-Drop` | P1 | Prometheus 指标 `oneid_fusion_success_rate{job="fusion-engine"} < 95` 持续5min | OneID 融合引擎 | 身份融合成功率低于95% | 表明跨渠道身份归一能力严重劣化，可能导致OneID生成失败或重复 | [[IDaaS/OneID融合失败应急手册|OneID融合失败应急手册]] | ✅ EOCC-2024-CIAM-001 |
| `CIAM-AuthLatency-99th-Exceed` | P1 | SLS 查询 `latency_p99 > 2000 AND service="auth-center"` | 认证中心 | 认证请求99分位耗时超2s | 用户登录卡顿，大规模投诉风险 | [[IDaaS/高延迟认证应急手册|高延迟认证应急手册]] | ✅ EOCC-2024-CIAM-002 |
| `CIAM-Portal-5xx-Rate-High` | P2 | SLB 访问日志统计 `status >= 500` 占比 > 1% | 消费者门户 | 门户后端返回大量5xx错误 | 品牌页面不可用，影响用户第一触点 | [[IDaaS/门户5xx应急手册|门户5xx应急手册]] | ✅ KB-ALI-IDAS-PORTAL-5XX |
| `CIAM-PolicyEngine-ConfigError` | P2 | 日志关键词 `policy load failed` + `config checksum mismatch` | 策略引擎 | 策略配置加载失败且校验和不一致 | 新策略未生效，旧策略可能被误回滚 | [[IDaaS/策略加载失败应急手册|策略加载失败应急手册]] | ✅ KB-ALI-IDAS-POLICY-LOAD |
| `CIAM-Compliance-DSAR-Overdue` | P3 | 工作台数据库表 `dsar_task` 中 `status='pending' AND due_time < now()` | 隐私合规工作台 | 数据主体权利请求超期未处理 | 合规风险，可能触发监管问询 | [[IDaaS/DSAR超期处置指南|DSAR超期处置指南]] | ✅ KB-ALI-IDAS-DSAR-OVERDUE |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    - **UserHub PostgreSQL 实例（逻辑库：`ciam_userdb`）**  
      `user_identity`：用户主身份（uid, primary_id_type, primary_id_value, status）  
      `identity_link`：多ID绑定关系（uid, linked_id_type, linked_id_value, verified_at）  
      `consent_record`：用户条款同意记录（uid, terms_version, signed_at, ip, device_fingerprint）  
    - **RiskEngine MySQL 实例（逻辑库：`ciam_riskdb`）**  
      `risk_event_log`：原始风险事件（event_id, uid, ip, device_id, risk_score, risk_reason, created_at）  
      `rule_config`：启用中的风控规则（rule_id, name, condition_json, weight, enabled）  
    - **OneID 融合引擎 PostgreSQL 实例（逻辑库：`idas_ciam`）**  
      `identity_master`：OneID 主表，存储 CID、创建时间、融合来源、主身份标识（手机号/邮箱/身份证）  
      `auth_event_log`：认证事件全量日志，含设备指纹、IP、风险评分、认证方式  
      `policy_rule`：策略规则定义表，含 Rego 代码、生效范围、优先级  
      `dsar_task`：DSAR 请求任务表，含用户标识、请求类型（删除/导出/更正）、截止时间、处理状态  

*   关键日志路径，组件，内容，轮转策略  
    - `/var/log/idass/authcore/access.log`（AuthCore Nginx 访问日志）：记录所有认证请求（method, uri, status, upstream_time）；按日轮转，保留30天；集群：`ciam-prod-shenzhen-az-a`，SR：SR-2024-CIAM-AUTH-001  
    - `/opt/idass/riskengine/logs/risk-inference.log`（RiskEngine 推理日志）：记录每次 AI 模型打分输入/输出；按小时轮转，压缩保留7天；集群：`ciam-prod-hangzhou-az-b`，SR：SR-2024-CIAM-RISK-002  
    - `/var/log/idas/ciam-gateway/access.log`（应用网关）：Nginx 格式访问日志，按日轮转，保留30天  
    - `/opt/idas/policy-engine/logs/policy-execution.log`（策略引擎）：Rego 执行详情日志，JSON 格式，按大小 100MB 轮转，保留7天  
    - `/data/idas/oneid-fusion/logs/fusion-job.log`（OneID 引擎）：融合任务调度日志，含任务ID、输入源、匹配数、冲突数，按日轮转，保留90天  

*   问题排查 SOP，通用场景的排查思路和路径  
    1. **现象：用户反馈“注册页面空白/无限加载”**  
       → 检查 AuthCore 前端 JS SDK 加载（浏览器 F12 Network Tab）→ 若 404，检查 OSS 静态资源 Bucket 权限 → 若加载成功但无响应，检查 ConsentManager / 隐私合规工作台服务健康（`curl -I https://consent.{region}.aliyuncs.com/healthz`）→ 若失败，跳转至 [[IDaaS/应急手册/ConsentManager-Down]]  
    2. **现象：“登录成功但跳转回首页，未进入业务系统”**  
       → 检查 OAuth2.0 redirect_uri 是否在 AuthCore 控制台白名单中注册 → 检查业务端 token 解析逻辑（是否校验 `iss`/`aud` 字段）→ 抓包确认 ID Token 是否含 `scope=openid profile`  
    3. **现象定位**：先确认是全局性（所有用户）还是局部性（某渠道/某应用/某地域）；  
    4. **链路切片**：使用 `trace_id` 在 SLS 中串联「门户→网关→认证中心→目录→策略」全链路；  
    5. **瓶颈识别**：检查各组件 CPU/Mem/网络延迟指标（Prometheus），重点关注 `auth-center` 的 `jwt_verify_duration_seconds` 和 `policy_engine` 的 `rego_eval_duration_seconds`；  
    6. **配置验证**：登录对应组件 Pod，检查 `/etc/idas/config/` 下配置文件 MD5 与 CMDB 是否一致。  

*   版本升级指南（如有）、巡检手册（如有）、相关 aone  
    - 升级：公有云版本全自动滚动升级，客户无感知；重大版本变更提前15天邮件通知，详情见 [[IDaaS/公告/Version-Upgrade-Notice]]；升级指南：[[IDaaS/CIAM版本升级手册|CIAM版本升级手册]]（AONE：ALI-IDAS-CIAM-UPGRADE）  
    - 巡检：每日自动执行（通过 OSS 脚本）：① AuthCore ALB 健康检查通过率 ≥99.99%；② UserHub 主从同步延迟 < 5s；③ RiskEngine 模型加载状态 `ready`；④ ConsentManager 条款版本最新标记有效；⑤ `oneid_fusion_success_rate`、`auth_center_2xx_rate`、`policy_engine_rego_compile_failures_total`、`dsar_task_overdue_count`；报告存于 SLS `ciam-daily-inspection` Logstore。  
    - 巡检脚本：`/opt/idas/bin/ciam-healthcheck.sh`（自动执行并邮件告警）  
    - Aone 链接：[[AONE/IDaaS-CIAM-Release|IDaaS CIAM 发布流水线]]  

### 典型问题排查解决方案

```yaml
（针对“用户注册时收不到短信验证码”）
一、问题描述
● 问题现象：用户在注册页输入手机号，点击“获取验证码”后无短信到达，前端提示“发送失败”或超时。
● 适用范围：所有云版本；Web/H5/小程序端；影响范围为单个手机号或批量手机号（需区分）。

二、排查信息收集
● 必须收集的信息：用户手机号（脱敏）、注册时间（精确到秒）、客户端IP、User-Agent、前端控制台Network截图（含`/api/v1/sms/send`请求响应）。
● 检查终态的方法：登录AuthCore Pod，执行 `kubectl exec -it authcore-xxx -- tail -n 20 /var/log/idass/authcore/sms.log`
● 排查问题步骤：
  | 日志关键词 | 含义 | 对应场景 |
  |---|---|---|
  | `sms_send_failed: invalid_phone` | 手机号格式非法（非11位/非大陆号段） | 用户输入错误或前端未校验 |
  | `sms_send_failed: rate_limit_exceeded` | 该IP/手机号当日发送超限（默认5次/小时） | 需确认是否恶意刷量 |
  | `sms_send_failed: channel_unavailable` | 短信通道（阿里云SMS）临时不可用 | 查看阿里云SMS控制台告警 |
  | `sms_send_success: code=123456` | 短信已发出，但用户未收到 | 运营商通道问题或手机拦截 |

三、解决步骤
 场景一：手机号格式非法
 - 适用条件：日志含`invalid_phone`
 - 实施步骤：前端增加严格手机号正则校验（`^1[3-9]\d{9}$`）；后端AuthCore配置`phone_validation_strict=true`（需重启Pod）
 - 结果验证：输入正确手机号可正常触发发送，日志出现`sms_send_success`

 场景二：短信通道不可用
 - 适用条件：日志含`channel_unavailable`，且阿里云SMS控制台显示“服务异常”
 - 实施步骤：登录阿里云SMS控制台 → 进入【国内消息】→ 【通道管理】→ 切换备用通道（如从“阿里云通道A”切至“通道B”）→ 在AuthCore配置中心更新`sms.channel.id=channel-b`
 - 结果验证：日志出现`sms_send_success`，用户5分钟内收到短信

 场景三：用户手机拦截/运营商问题
 - 适用条件：日志显示`sms_send_success`，但用户未收到
 - 实施步骤：建议用户检查手机短信黑名单、省电模式限制；提供“语音验证码”备用方案（需在AuthCore控制台开启`voice_verify_enabled=true`）
 - 结果验证：用户选择语音验证码后成功接收并填写

四、非本产品排查
● 明确标注：若阿里云SMS控制台显示“发送成功”但用户手机明确未收到（且非拦截），属于运营商侧问题，需引导客户联系对应运营商（移动10086/联通10010/电信10000）申诉，IDaaS不介入。

五、快速定位工具
● 脚本位置：`/opt/idass/tools/sms-debug.sh`（AuthCore Pod内）
● 使用方法：`bash /opt/idass/tools/sms-debug.sh -p 139****1234 -t register`（模拟注册短信发送，输出完整链路日志）
```

```yaml
一、问题描述
● 问题现象：用户反馈小程序登录后跳转至空白页，控制台报错 "Failed to fetch user profile"
● 适用范围：IDaaS CIAM v3.2.0+，小程序接入场景，使用 JS SDK 方式
二、排查信息收集
● 必须收集的信息：小程序AppID、用户OpenID、复现时间点（精确到秒）、前端控制台完整报错截图
● 检查终态的方法：登录 ciam-gateway Pod，执行 `curl -v "http://localhost:8080/api/v1/profile?openid=xxx"`
● 排查问题步骤：
  - 步骤1：检查网关是否收到请求（SLS查 `service=gateway AND path="/api/v1/profile"`）
  - 步骤2：若收到但返回500，检查 auth-center 日志中是否有 `user not found in directory`
  - 步骤3：若 auth-center 返回404，检查 identity_master 表是否存在该 openid 对应记录
  - 步骤4：若不存在，检查 fusion-engine 是否有该 openid 的融合任务失败记录
三、解决步骤
 场景一：融合任务失败（fusion-job.log 含 "timeout"）
 - 适用条件：融合任务超时，且用户为首次登录
 - 实施步骤：
   - 登录 fusion-engine Pod：`kubectl exec -it <pod-name> -n idas-ciam -- bash`
   - 重试任务：`fusion-cli retry --task-id <task_id>`
 - 结果验证：再次调用 `/api/v1/profile` 返回200且含 profile 字段
 场景二：用户被策略引擎拦截（policy-execution.log 含 "deny by age_policy"）
 - 适用条件：用户为未成年人，且策略配置了严格访问限制
 - 实施步骤：
   - 进入策略工作台，临时禁用 `age_policy` 或调整 `allow_under_18_apps` 参数
   - 或引导用户切换至家长监护模式（需前端适配）
 - 结果验证：profile 接口返回200，且 `profile.age_group` 字段为 "minor"
四、非本产品排查
● 若 gateway 日志未收到请求：检查小程序域名是否加入白名单（需在 [[IDaaS/小程序接入指南|小程序接入指南]] 配置）
● 若 auth-center 返回401：检查小程序 secret 是否过期（需在 [[IDaaS/凭证管理规范|凭证管理规范]] 更新）
五、快速定位工具
● 脚本位置：`/opt/idas/bin/ciam-debug-profile.sh <openid>`
● 使用方法：自动串联网关→认证中心→目录查询，输出各环节状态码与关键字段
```

### 紧急场景止血与恢复手册：

*   **场景：AuthCore 全局5xx > 5%，持续10分钟以上**  
    → 止血：立即登录 Aone 流水线，回滚至前一稳定版本（R-20240515.1）；同步在 ALB 控制台将流量 100% 切至灾备集群（`ciam-dr-shanghai`）。  
    → 恢复：待主集群修复后，执行蓝绿发布验证（5%灰度→50%→100%），确认 `AuthCore_5xx_Rate_High` 告警清除。  
    → 工具：一键回滚脚本 `idass-authcore-rollback.sh`（需 L2 权限）

*   **场景：UserHub 主库宕机，RPO>0**  
    → 止血：启用只读从库（`ciam-userdb-ro`）提供查询服务（用户信息展示、登录态校验）；AuthCore 配置 `read_only_mode=true`（降级为无状态鉴权）。  
    → 恢复：DBA 执行从库提升为主库；同步执行 `pg_dump` 恢复缺失增量；UserHub 服务重启加载新主库。  
    → 工具：只读模式开关 `kubectl patch deploy authcore -p '{"spec":{"template":{"spec":{"containers":[{"name":"authcore","env":[{"name":"READ_ONLY_MODE","value":"true"}]}]}}}}'`

*   **OneID 全量融合中断（P0）**：立即执行 `fusion-cli force-resync --all-sources` 触发全量重跑；同步降级策略引擎至「仅基础认证」模式（关闭风控与合规拦截），命令：`kubectl patch cm policy-config -n idas-ciam --type=json -p='[{"op":"replace","path":"/data/mode","value":"basic"}]'`

*   **消费者门户大面积503（P0）**：切换至灾备静态页（`/var/www/portal-maintenance/index.html`），同时扩容 gateway Deployment 至原规格 200%，命令：`kubectl scale deploy ciam-gateway -n idas-ciam --replicas=12`

### 横向研发文档：

*   接入指引  
    → [[IDaaS/接入指南/QuickStart|5分钟快速接入指南]]：含控制台开通、SDK下载、前端集成、后端Token校验全流程。  
    → [[IDaaS/CIAM接入全景图|CIAM接入全景图]]、[[IDaaS/JS-SDK集成文档|JS-SDK集成文档]]

*   产品对接方案细节  
    → [[IDaaS/对接方案/CRM-Integration|CRM系统对接方案]]：描述如何将 UserHub 用户事件（注册/实名/条款更新）通过 Webhook 推送到 Salesforce/纷享销客；含字段映射表、重试机制、签名验签逻辑。  
    → [[IDaaS/与CDP系统对接方案|CDP对接方案]]、[[IDaaS/与营销中台对接方案|营销中台对接方案]]

*   产品对接范围等  
    → IDaaS CIAM **不负责**：用户业务数据存储（如订单、支付信息）、应用级权限控制（RBAC/ABAC）、非身份类审计日志（如操作日志）、短信通道、OCR识别、支付网关。  
    → **明确对接边界**：仅提供 `用户身份标识（sub）`、`基础属性（email/name/phone）`、`认证上下文（amr/acrs）`、`同意快照（consent_claims）` 四类标准化输出。  
    → [[IDaaS/CIAM服务边界说明书|服务边界说明书]]

## 产品对内文档：

### 完整架构图：

*   系统，架构，调用关系，业务流，模块等深层知识点  
    → 全链路时序图（含异步事件）：[[IDaaS/架构/Full-Sequence-Diagram|IDaaS CIAM 全链路时序图]]  
    → 模块依赖矩阵（含版本兼容性）：[[IDaaS/架构/Module-Dependency-Matrix|模块依赖矩阵]]  
    → 已知问题：UserHub 跨 AZ 同步偶发延迟（已纳入 v2.3.0 优化计划，跟踪 AONE-123456）；融合引擎在超大规模（>10亿用户）下图计算内存占用偏高，已规划迁移至 [[Alibaba Graph|Alibaba Graph]] 引擎（Roadmap Q4 2024）  
    → 系统采用「控制面+数据面」分离设计：  
        - 控制面（Control Plane）：含 Portal、Admin Console、Policy Studio、Fusion Orchestrator，部署于独立命名空间，负责配置下发与策略编排；  
        - 数据面（Data Plane）：含 Gateway、Auth Center、Directory、Policy Runtime，部署于高性能节点池，无状态设计，支持水平扩缩；  

### 业务逻辑时序图

*   用户使用  
    → [[IDaaS/时序图/User-Journey|用户全旅程时序图]]：覆盖注册（分步）、登录（多因子挑战）、账号管理（自助解绑/删除）、条款签署等12个核心路径。  
    → [[IDaaS/CIAM用户旅程时序图|用户旅程时序图]]（含注册、登录、资料完善、注销全路径）

*   工作流流转  
    → [[IDaaS/时序图/Workflow-Engine|风控工作流引擎时序图]]：描述 RiskEngine 如何接收 AuthCore 事件 → 调用特征提取服务 → 加载用户画像 → 执行规则引擎+AI模型 → 输出决策信号 → 触发 AuthCore 动作（放行/挑战/拦截）。  
    → [[IDaaS/OneID融合工作流|OneID融合工作流]]（含数据源接入、信号提取、图谱构建、CID生成、冲突仲裁）

### 代码仓库

*   基线仓库  
    → `git@code.alibaba-inc.com:idass/ciam-base.git`（基线框架，含 Spring Cloud Alibaba 标准封装）  
    → `git@code.alibaba-inc.com:idassdk/ciam-base.git`（含核心框架与协议适配器）

*   代码仓库  
    → `git@code.alibaba-inc.com:idass/authcore.git`  
    → `git@code.alibaba-inc.com:idass/userhub.git`  
    → `git@code.alibaba-inc.com:idass/riskengine.git`  
    → `git@code.alibaba-inc.com:idass/consent-manager.git`  
    → `git@code.alibaba-inc.com:idassdk/ciam-portal.git`（消费者门户）  
    → `git@code.alibaba-inc.com:idassdk/ciam-policy-engine.git`（策略引擎）  
    → `git@code.alibaba-inc.com:idassdk/ciam-fusion-engine.git`（OneID融合引擎）

*   制品仓库  
    → 阿里云内部 Nexus：`https://nexus.alibaba-inc.com/repository/idass-releases/`  
    → `registry.cn-hangzhou.aliyuncs.com/idassdk/ciam-*`（Docker镜像）

*   关联依赖仓库等  
    → `git@code.alibaba-inc.com:middleware/alicloud-sms-sdk.git`（短信SDK）  
    → `git@code.alibaba-inc.com:security/aliyun-antifraud-sdk.git`（风控特征SDK）  
    → `git@code.alibaba-inc.com:security/aliyun-auth-sdk.git`（认证SDK）  
    → `git@code.alibaba-inc.com:compliance/gdpr-engine.git`（GDPR策略库）

### 数据表结构

*   `user_identity`（UserHub）  
    ```sql
    CREATE TABLE user_identity (
      uid VARCHAR(64) PRIMARY KEY,           -- 全局唯一用户ID（UUID）
      primary_id_type VARCHAR(20),           -- 主身份类型（'phone'/'email'/'wechat_openid'）
      primary_id_value VARCHAR(128),         -- 主身份值（脱敏存储，如139****1234）
      status VARCHAR(20) DEFAULT 'active',   -- 状态（'active'/'frozen'/'deleted'）
      created_at TIMESTAMP DEFAULT NOW(),
      updated_at TIMESTAMP DEFAULT NOW()
    );
    ```

*   `consent_record`（ConsentManager）  
    ```sql
    CREATE TABLE consent_record (
      id BIGSERIAL PRIMARY KEY,
      uid VARCHAR(64) NOT NULL,              -- 关联user_identity.uid
      terms_type VARCHAR(50) NOT NULL,       -- 条款类型（'privacy_policy'/'user_agreement'）
      terms_version VARCHAR(20) NOT NULL,    -- 版本号（'v2.1.0'）
      signed_at TIMESTAMP NOT NULL,          -- 签署时间
      ip INET,                               -- 签署IP（用于风控）
      device_fingerprint VARCHAR(128),       -- 设备指纹（Hash值）
      signature TEXT                         -- 数字签名（RSA-SHA256）
    );
    ```

*   `identity_master`（核心身份主表）：
    | 字段名 | 类型 | 描述 | 索引 |
    |--------|------|------|------|
    | cid | VARCHAR(64) PK | OneID唯一标识 | 主键 |
    | primary_identity | JSON | 主身份信息（type: phone/email/idcard, value） | 无 |
    | sources | JSON | 来源系统列表（[{system:"miniapp",id:"wx123"},{system:"web",id:"u456"}]） | 无 |
    | created_at | DATETIME | 创建时间 | idx_created_at |
    | updated_at | DATETIME | 最后更新时间 | idx_updated_at |

*   `auth_event_log`（认证事件日志）：
    | 字段名 | 类型 | 描述 | 索引 |
    |--------|------|------|------|
    | trace_id | VARCHAR(64) | 全链路追踪ID | 主键 |
    | cid | VARCHAR(64) | 关联OneID | idx_cid |
    | app_id | VARCHAR(32) | 应用标识 | idx_app_id |
    | auth_method | VARCHAR(32) | 认证方式（pwd/sms/wechat/face） | 无 |
    | risk_score | INT | 风险评分（0-100） | idx_risk_score |
    | status | TINYINT | 状态（1成功/0失败） | idx_status |