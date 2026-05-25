# IDaaS CIAM

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   阿里云IDaaS CIAM是面向消费者（C端）、会员、访客等**外部用户**的客户身份与访问管理（Customer Identity & Access Management, CIAM）解决方案，聚焦**安全、高并发、高可用、强体验、统一身份整合、用户自助服务、高级安全风控与合规可控**，区别于面向员工/合作伙伴的EIAM（Employee IAM）体系。
        
    *   作为IDaaS产品矩阵的关键分支，CIAM自202X年正式发布公测版，逐步完善多租户隔离、全链路可观测、国产密码支持、微信/支付宝/手机号一键登录等本土化能力；2024年起提供私有化部署版本及公共云专属版，支撑金融、政务、大型零售等行业关键业务上线。当前为公有云原生架构版本，持续迭代中；已具备MAU弹性计费、自动热补丁升级、千人千面风控建模、条款统一治理等差异化能力，后续将深化与阿里云VPC/SLB/ECS/ARMS/SLS等产品的深度协同（如基于SLB的流量染色风控联动、VPC内网免密调用鉴权等），并扩展GDPR/CCPA等国际合规适配能力。
        
    *   能力覆盖核心身份服务组件（认证中心、账号中心、会话中心、风险引擎）、管理控制台（Admin Console）、开放API网关、配套SDK/JS插件（如Login Widget），以及IDaaS核心产品层（统一认证中心、风险控制引擎、用户自服务平台、条款管理中心、身份图谱服务、账号同步网关、动态MFA服务、行为分析模型服务）和集成层（OpenAPI、SDK、通用登录页、SSO联邦网关）。

*   对外介绍架构图
    
    *   中心端采用微服务架构，核心组件以容器化方式部署于阿里云ACK集群（支持专有云/混合云/公共云Region如cn-hangzhou），支持水平弹性伸缩；端侧通过标准OAuth 2.1 / OIDC协议与客户App、H5、小程序、Web门户等前端无缝集成。
        
    *   **部署架构**：中心端全托管于阿里云公共云Region（如cn-hangzhou），采用多可用区高可用部署；端侧通过HTTPS/API/SDK轻量接入，支持Web/iOS/Android/H5/小程序等全终端形态；容器组（如`auth-core`、`risk-engine`、`selfservice-ui`）按微服务粒度部署于ACK集群，共享VPC网络与SLB入口。
        
    *   数据流向：终端用户 → 前端SDK/Widget → API网关 → 认证服务（登录/注册/SSO）→ 账号服务（CRUD/Profile/Consent）→ 会话与令牌服务（JWT/OAuth Token）→ 风险决策服务（实时风控）→ 同步至客户自有数据库/CDP（可选）。  
        或：用户请求 → SLB → API网关 → 认证中心（含注册/登录/密码管理）→ 账号同步网关（对接客户旧系统）/风险引擎（实时评分）/条款中心（合规校验）→ 返回令牌/用户属性 → 业务应用消费。
        
    *   与上下游系统依赖关系：
        *   依赖：阿里云RAM（用于CIAM控制台自身权限管控）、云监控（CloudMonitor）与ARMS（告警与链路追踪）、KMS（密钥管理，用于敏感字段加密）、Tianji（资源纳管与容量调度）、OpsApi（运维指令下发与状态回传）；
        *   被依赖：客户业务系统（通过OpenAPI或SDK调用）、统一消息服务（SMS/Email推送）、第三方身份源（微信开放平台、支付宝开放平台、运营商网关等）；
        *   可选对接：DataWorks（用户画像数据回流）、MaxCompute（风控模型训练）；
        *   无强依赖：Tianji（非基础设施层纳管）、OpsApi（非IDaaS运维通道，CIAM自有运维通道）—— 此条已根据新文档更新为**强依赖**，故以新文档为准：**依赖Tianji与OpsApi**；
        *   已知问题沉淀：[[IDaaS CIAM-已知问题与规避方案|Known Issues]]（含OAuth 2.1 Refresh Token reuse漏洞缓解方案、微信UnionID跨应用映射延迟说明等）。
        
    *   参考：  
        ![IDaaS CIAM 架构示意图](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/54Lq35oXpkvg1l7E/img/1213e0b1-d245-4023-8f12-1a8fa1c09600.png)  
        [[IDaaS CIAM 架构图|IDaaS-CIAM-Architecture-Diagram]]

*   各核心组件能力详细说明
    
    *   **统一认证中心（Auth Core）**：支持用户名密码、手机验证码、邮箱验证码、社交登录（微信/支付宝/Apple ID）、生物识别（WebAuthn）、MFA（TOTP/短信/邮件）等多种认证方式；提供自适应认证策略（基于设备、IP、行为风险动态增强认证强度）；提供标准化OAuth2.0/OIDC协议支持、分步注册流程引擎、多因子认证（SMS/Email/TOTP/生物识别）、密码策略与生命周期管理。
        
    *   **账号中心（AccountCore）**：提供全生命周期账号管理（注册/激活/冻结/注销）、属性扩展（自定义Profile Schema）、用户分群（Tag/Segment）、同意管理（GDPR/PIPL合规Consent Hub）、多因素绑定解绑。
        
    *   **会话与令牌中心（Session & Token Service）**：基于Redis Cluster实现毫秒级会话状态同步；签发符合RFC规范的JWT/OAuth 2.1 Access Token/Refresh Token；支持细粒度Scope控制与Token吊销。
        
    *   **风险控制引擎（Risk Engine）**：内置规则引擎+轻量级AI模型，实时识别异常登录、撞库、批量注册、设备指纹异常等风险行为；基于用户设备指纹、IP地理围栏、登录时段、行为序列建模的实时风险评分服务；支持策略编排（如“高风险IP+新设备=强制MFA”）、风险事件回调（Webhook）、EOCC联动告警；支持客户自定义规则与人工复核工作流；高级客户可通过Risk Engine OpenAPI接入自研模型输出的风险分，CIAM负责执行拦截/MFA等动作。
        
    *   **用户自服务平台（Self-Service Portal）**：白标化可定制门户，支持账号信息查看/修改、安全设置（MFA绑定/解绑）、登录历史审计、隐私数据导出/删除、条款签署状态管理。
        
    *   **条款管理中心（Consent Hub）**：统一存储与版本化管理《用户协议》《隐私政策》等法律文本；支持按用户/场景/地域精准推送、签署留痕、到期自动提醒、审计溯源，满足《个人信息保护法》第13/17条合规要求；所有签署行为均上链（蚂蚁链BaaS），生成唯一哈希值与时间戳，符合《电子签名法》第十三条，可直接用于司法举证。
        
    *   **身份图谱服务（Identity Graph）**：实现跨渠道身份ID（手机号、微信OpenID、设备ID等）的关联、去重与合并；提供“疑似重复账号”人工确认工作台与自动化僵尸账号识别（基于登录频次、活跃度阈值）能力。
        
    *   **账号同步网关（Sync Gateway）**：支持双向懒加载同步（On-Demand Sync）与定时全量同步，兼容LDAP/AD/MySQL/Oracle等异构源系统，解决大型企业身份割据问题；支持单向（AD→CIAM）或双向同步；推荐初期采用“AD主写、CIAM只读”，待身份体系稳定后启用双向。
        
    *   **管理控制台（Admin Console）**：面向企业管理员的可视化运营平台，支持用户查询/导出、登录趋势分析（DAU/MAU/留存率）、自助服务配置（密码策略、注册流程）、审计日志（操作日志+认证日志）下载。
        
    *   **开放API网关（OpenAPI Gateway）**：提供RESTful风格标准API（含OpenAPI 3.0规范文档），覆盖认证、账号、会话、风控、统计等全部能力；支持API调用配额、签名验签、流量控制。

*   与阿里云其他产品的关系
    
    *   与 VPC：CIAM私有化/专属云版本需部署在客户指定VPC内，通过VPC内网与客户业务系统互通；不暴露公网入口时，依赖VPC对等连接或云企业网（CEN）打通跨VPC访问；CIAM所有服务部署于客户指定VPC内网（可选），API调用走内网SLB，避免公网暴露；与ECS同VPC时支持免密Token直调。
        
    *   与 ECS：所有CIAM组件以容器形式运行于客户ECS（或ACK节点）之上；专属云版本默认使用客户提供的ECS资源池，CIAM不直接管理ECS生命周期；CIAM自身运行于ACK托管集群，依赖ECS节点资源；客户业务若部署于ECS，可通过内网域名直连CIAM服务，降低延迟与安全风险。
        
    *   与 SLB：CIAM API网关及控制台前端默认通过ALB（应用型负载均衡）对外提供HTTPS服务；SLB异常仅影响API/Console入口可用性，不影响内部组件间通信（Service Mesh直连）；所有对外API/门户入口均经SLB负载均衡，支持WAF规则注入、CC防护、TLS 1.3加密；CIAM风控引擎可将高危请求特征同步至SLB实现动态限流。
        
    *   产品异常可能造成的影响：
        *   ✅ 会导致客户C端用户无法注册、登录、修改密码、获取Token；
        *   ✅ 会导致Admin Console不可访问或用户数据无法查询；
        *   ▪️ 认证中心不可用 → 所有依赖CIAM登录的App/小程序/网站无法注册/登录（**P0级影响**）；
        *   ▪️ 风控引擎延迟 >5s → 登录二次验证超时，导致用户流失率上升（**P1级影响**）；
        *   ▪️ 条款中心不可写 → 新用户无法完成首次合规签署，但存量用户不受影响（**P2级影响**）；
        *   ❌ 不会影响客户已有ECS实例运行、RDS数据库服务、OSS存储等底层IaaS资源；
        *   ❌ 不会触发RAM角色权限变更、不会修改客户云账号下任何资源ACL策略；
        *   ❌ 不涉及Tianji纳管节点状态，不产生OpsApi调用依赖；
        *   ❌ **不会造成的影响**：CIAM异常**不中断**客户已有业务逻辑（订单/支付/内容服务），仅阻断身份环节；**不触发**ECS/VPC底层资源故障；**不污染**客户自有数据库数据。

## QA（高频问答）：

*   Q：CIAM是否支持与客户现有LDAP/AD做账号同步？  
    A：支持。通过[[IDaaS CIAM-对接LDAP/AD指南|LDAP/AD对接方案]]提供的SCIM 2.0 Connector或定制同步Agent，可实现单向/双向账号同步，支持增量同步与冲突处理策略配置。也支持通过账号同步网关配置单向（AD→CIAM）或双向同步；推荐初期采用“AD主写、CIAM只读”，待身份体系稳定后启用双向，需配合[[IDaaS CIAM AD同步配置指南|IDaaS-CIAM-AD-Sync-Guide]]操作。

*   Q：私有化版本是否支持信创环境（麒麟OS+达梦DB+海光CPU）？  
    A：支持。自v2.3.0起全面适配主流信创栈，已通过工信部信创实验室兼容性认证，详情见[[IDaaS CIAM-信创适配清单|信创兼容矩阵]]。

*   Q：用户登录失败次数超限后，多久自动解封？能否自定义？  
    A：默认15分钟自动解封；管理员可在控制台「安全策略 > 登录保护」中调整锁定时长、解锁方式（自动/人工审核）及通知渠道。

*   Q：CIAM是否满足等保2.0三级和GB/T 35273-2020《个人信息安全规范》要求？  
    A：满足。已通过等保三级测评（报告编号：EP-2024-CIAM-XXXX），所有PII数据默认AES-256加密落盘，日志脱敏，审计日志保留≥180天，详见[[IDaaS CIAM-合规白皮书|合规认证文档]]。

*   Q：一个API调用失败，是否意味着整个CIAM服务不可用？  
    A：否。CIAM采用微服务隔离设计，单个API（如`/api/v1/login`）故障不会导致`/api/v1/profile`或控制台功能中断；各服务具备独立熔断与降级能力。

*   Q：MAU如何定义与统计？是否包含测试账号或机器人流量？  
    A：MAU = 当月至少发起1次成功认证（登录/注册/令牌刷新）的**唯一自然人用户数**；系统自动过滤测试手机号段（如170/171号段）、已标记的Bot User-Agent、非真实设备指纹，确保计费纯净。

*   Q：能否将CIAM与企业现有AD/LDAP系统打通？是否支持双向同步？  
    A：支持。通过账号同步网关可配置单向（AD→CIAM）或双向同步；推荐初期采用“AD主写、CIAM只读”，待身份体系稳定后启用双向，需配合[[IDaaS CIAM AD同步配置指南|IDaaS-CIAM-AD-Sync-Guide]]操作。

*   Q：风控策略是否支持客户自定义？能否对接客户自研AI模型？  
    A：支持策略编排（图形化界面配置条件与动作）；高级客户可通过Risk Engine OpenAPI接入自研模型输出的风险分，CIAM负责执行拦截/MFA等动作。

*   Q：条款签署记录是否满足司法存证要求？  
    A：是。所有签署行为均上链（蚂蚁链BaaS），生成唯一哈希值与时间戳，符合《电子签名法》第十三条，可直接用于司法举证。

## 组件接口人、研发负责人等角色信息：

| 组件 | 研发负责人 | L1可操作项 | L2可操作项 | 必须升级产研场景 |
|------|------------|-------------|-------------|-------------------|
| 统一认证中心（AuthCore） | 张伟（IDaaS-Core）<br>（zhang.san@alibaba-inc.com） | 重启Pod、切换灰度流量、查看认证成功率大盘<br>重启Pod、查看日志、触发健康检查 | 回滚API版本、调整JWT过期时长（≤1h）、临时关闭某社交登录源<br>调整认证策略配置、回滚策略版本 | 修改OAuth2.0授权码模式逻辑、新增协议支持（如SAML2.0）<br>修改认证流程代码、发布新认证方式 |
| 账号中心（AccountCore） | 李婷（IDaaS-Risk）<br>（li.si@alibaba-inc.com） | 执行账号冻结/解冻命令、导出审计日志<br>查看账号变更日志 | 修改Profile Schema、配置同意模板<br>修改Profile Schema、配置同意模板 | 变更账号状态机逻辑、上线新属性类型<br>变更账号状态机逻辑、上线新属性类型 |
| 风险控制引擎（RiskEngine） | 王磊（IDaaS-Consent）<br>（wang.wu@alibaba-inc.com） | 查看实时风险分TOP10、启停单个风控策略<br>查看风险事件列表、标记误报 | 调整策略阈值（±10%）、导入测试设备指纹库<br>启停规则、调整阈值 | 修改行为分析模型特征工程、新增风险维度（如WiFi SSID）<br>训练/上线新模型、修改特征工程逻辑 |
| 条款管理中心（Consent Hub） | 陈明（IDaaS-Graph）<br>（chen.qi@alibaba-inc.com） | 发布新条款版本、设置签署有效期<br>统一由IDaaS PaaS平台组维护，接口人 @chen.qi（chen.qi@alibaba-inc.com）；L1/L2均不可变更前端页面或API路由，仅可重启服务、切换灰度流量 | 回滚至历史版本、导出某用户签署记录CSV<br>所有配置类变更必须升级至产研 | 修改条款签署法律效力校验规则、对接外部公证平台<br>所有配置类变更必须升级至产研 |
| 身份图谱服务（Identity Graph） | — | 触发手动去重任务、查看关联关系图谱 | 设置僵尸账号识别阈值（90天未登录）、导出去重建议列表 | 修改ID关联算法（如增加邮箱模糊匹配权重） |

> **说明**：Admin Console & OpenAPI Gateway 统一由IDaaS PaaS平台组维护，接口人 @chen.qi（chen.qi@alibaba-inc.com）；L1/L2均不可变更前端页面或API路由，仅可重启服务、切换灰度流量；所有配置类变更必须升级至产研。

## 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `AuthCore_AuthRate_Drop` | P0 | Prometheus指标 `auth_success_rate{job="authcore"} < 95%` 持续5min | AuthCore | 连续5分钟认证成功率低于95% | 核心认证链路异常，大量用户登录失败 | [[IDaaS CIAM-认证成功率骤降应急手册|认证成功率骤降]] | 是（KB#CIAM-AUTH-001） |
| `AuthCore_5xx_Rate_Above_5pct` | P0 | ARMS监控指标 `auth_core_http_server_requests_total{status=~"5.*"}` / `auth_core_http_server_requests_total` > 0.05 | 统一认证中心 | 连续5分钟认证接口5xx错误率超5% | 核心认证链路严重异常，大量用户登录失败 | [[AuthCore 5xx突增应急手册|IDaaS-AuthCore-5xx-EM]] | 是（KB#CIAM-5XX-001） |
| `AccountCore_DB_Latency_High` | P1 | ARMS链路 `accountcore-db-query-p99 > 1000ms` 持续10min | AccountCore | 账号中心数据库查询P99延迟超1秒 | 账号读写响应变慢，影响注册/资料修改等操作 | [[IDaaS CIAM-DB延迟升高处置|DB延迟升高]] | 是（KB#CIAM-DB-002） |
| `RiskEngine_Response_Latency_Above_3s` | P1 | ARMS指标 `risk_engine_latency_seconds_bucket{le="3"}` < 0.95 | 风控引擎 | 连续10分钟95%请求响应超3秒 | 风控决策延迟，影响登录体验与安全水位 | [[RiskEngine高延迟处置|IDaaS-RiskEngine-Latency-EM]] | 是（KB#CIAM-RISK-LAT-002） |
| `OpenAPI_Gateway_5xx_Rate_High` | P1 | ALB监控 `http_code_backend_5xx > 1%` 持续3min | OpenAPI Gateway | API网关后端返回5xx错误占比超1% | 客户调用CIAM API大规模失败 | [[IDaaS CIAM-API网关5xx突增|API网关5xx突增]] | 是（KB#CIAM-APIGW-004） |
| `RiskEngine_RuleEngine_Full` | P2 | 日志关键词 `rule_engine_queue_full` 出现频次>10次/min | RiskEngine | 风控规则引擎任务队列积压满载 | 新增风险事件无法实时评估，降级为白名单通行 | [[IDaaS CIAM-风控队列积压|风控队列积压]] | 是（KB#CIAM-RISK-003） |
| `Console_Login_Fail_Lockout` | P2 | 控制台审计日志中 `admin_login_failed` 连续10次 | Admin Console | 管理员账号被连续输错密码锁定 | 运维人员无法登录控制台进行日常管理 | [[IDaaS CIAM-管理员账号锁定|管理员锁定]] | 是（KB#CIAM-CONSOLE-005） |
| `ConsentHub_Signature_Failure_Rate_Above_1pct` | P2 | SLS日志 `level: ERROR AND msg: "signature failed"` 计数/总签署请求 > 0.01 | 条款中心 | 单日签署失败率超1% | 法律文本签署环节存在兼容性或签名服务异常 | [[ConsentHub签署失败排查|IDaaS-Consent-Sign-Fail-EM]] | 是（KB#CIAM-CONSENT-SIGN-003） |
| `IdentityGraph_Duplicate_Accounts_Above_10k` | P2 | Prometheus指标 `identity_graph_duplicate_candidates_count` > 10000 | 身份图谱服务 | 待人工确认的疑似重复账号数超1万 | 账号去重任务积压，影响身份治理进度 | [[IdentityGraph去重积压处理|IDaaS-Graph-Dup-Backlog-EM]] | 否 |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    *   `user_profile`（MySQL 8.0）：存储用户基础属性（uid、mobile、email、created_at、last_login_at）；  
    *   `user_identity`（MySQL 8.0）：存储用户认证凭据（uid、identity_type、identity_value、salted_hash）；  
    *   `consent_record`（OTS 5.0）：存储用户授权记录（uid、purpose_code、status、granted_at）；亦用于条款签署记录，含用户ID、条款版本号、签署时间、区块链交易Hash；  
    *   `audit_log`（MySQL 8.0）：存储操作审计日志（operator_id、action_type、target_id、ip、ua、created_at）；  
    *   `idm_user`（MySQL 8.0）：核心用户主表，存储用户ID、注册渠道、创建时间、最后登录时间；  
    *   `idm_identity_link`（MySQL 8.0）：身份关联表，记录同一用户在不同系统中的ID映射（如`wechat_openid → user_id`）；  
    *   `risk_event_log`（OTS 5.0）：风控事件原始日志，含设备指纹、IP、风险分、触发策略ID；  
    *   所有表位于专属云RDS（MySQL 8.0）或客户自建PostgreSQL（私有化场景），字符集统一为`utf8mb4`。

*   关键日志路径，组件，内容，轮转策略  
    *   `/var/log/idass/ciam/authcore/*.log`（AuthCore容器内）：认证请求流水、Token签发详情；按日轮转，保留7天；  
    *   `/var/log/idass/ciam/accountcore/*.log`（AccountCore容器内）：账号变更、Profile更新日志；按大小轮转（100MB/个），保留14个文件；  
    *   `/var/log/idass/ciam/riskengine/event.log`（RiskEngine容器内）：风险事件原始输入、规则匹配结果；按小时切割，压缩归档至OSS（保留90天）；  
    *   `/var/log/idm/auth-core/access.log`（Auth Core Pod）：Nginx访问日志，JSON格式，含trace_id/user_id/status；每日轮转，保留30天；  
    *   `/var/log/idm/risk-engine/risk-score.log`（Risk Engine Pod）：风控打分明细日志，含输入特征与输出分值；按小时切分，压缩保留7天；  
    *   `/var/log/idm/selfservice/audit.log`（Self-Service Pod）：用户自助操作审计日志（修改密码/MFA绑定等）；实时推送至SLS，保留180天；  
    *   集群范围：所有日志统一采集至SLS（Project: `idass-ciam-prod`，Logstore: `all-components`）。

*   问题排查SOP，通用场景的排查思路和路径  
    *   **现象：用户反馈“登录失败，提示‘系统繁忙’”** → 查OpenAPI Gateway 5xx告警 → 查AuthCore Pod状态与日志 → 查Redis连接池耗尽（`redis_connection_pool_used_ratio > 95%`）→ 临时扩容或清理异常连接。  
    *   **现象：控制台用户列表为空** → 查`accountcore`服务健康检查 → 查`audit_log`表写入延迟 → 查RDS CPU/IO瓶颈 → 切换只读副本查询或优化慢SQL。  
    *   **现象：“微信登录回调失败”** → 查`authcore`日志中`wechat_callback_error` → 核对微信开放平台AppID/Secret是否过期 → 检查客户域名是否在微信白名单 → 验证回调URL签名逻辑。  
    *   **现象：用户反馈“登录页面空白/白屏”** → 检查`selfservice-ui` Pod健康状态 → 查看`/var/log/idm/selfservice/ui-error.log`前端报错 → 验证CDN缓存是否命中旧JS → 清除CDN缓存并发布新版本；  
    *   **现象：微信登录返回“invalid code”** → 在Auth Core日志搜索`wechat_code` → 确认是否`code`被多次使用（微信限制单次有效）→ 检查业务方是否未及时换token → 推送[[微信登录Code复用规避指南|IDaaS-WeChat-Code-Reuse-Guide]]；  
    *   **现象：风控策略未触发** → 在Risk Engine日志搜索`policy_match` → 确认请求Header中是否携带`X-Forwarded-For`（影响IP识别）→ 检查SLB是否透传真实IP → 配置SLB `Proxy Protocol v2`。

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    *   升级指南：[[IDaaS CIAM-版本升级操作手册|CIAM升级指南]]（含滚动升级步骤、回滚checklist、兼容性矩阵）；[[IDaaS CIAM灰度升级SOP|IDaaS-CIAM-Upgrade-SOP]]（Aone工单编号：AONE-IDAAS-CIAM-UPGRADE-2024）；  
    *   巡检手册：[[IDaaS CIAM-日常巡检Checklist|CIAM每日巡检]]（含12项必检指标，自动化脚本位置：`/opt/idass/ciam/bin/health-check.sh`）；每日自动执行`idaas-ciamp-check.sh`（含DB连接池、Redis健康、SLB后端权重、证书有效期）；  
    *   Aone发布单：[AONE#IDASS-CIAM-RELEASE](https://aone.alibaba-inc.com/IDASS-CIAM-RELEASE)（需SSO登录）；Aone项目：`IDAAS-CIAM-PROD`（基线分支：`release/2.3.x`）。

### 典型问题排查解决方案

```yaml
（针对“用户注册成功但无法登录”）
一、问题描述
● 问题现象：用户完成手机号+验证码注册流程，收到“注册成功”提示，但立即使用同一手机号密码登录失败，返回“账号不存在或密码错误”
● 适用范围：所有云版本（含专属云）、Web/H5/小程序接入场景；影响范围为新注册用户首次登录
二、排查信息收集
● 必须收集的信息：用户手机号、注册时间（精确到秒）、客户端User-Agent、注册请求trace_id（来自前端SDK日志或SLS）
● 检查终态的方法：登录AuthCore Pod，执行 `curl -s "http://localhost:8080/api/v1/users?mobile=138****1234"`；登录AccountCore Pod，查 `SELECT * FROM user_profile WHERE mobile='138****1234';`
● 排查问题步骤：
  - 步骤1：确认`user_profile`表中存在该手机号记录 → 若不存在，跳转至「场景一」
  - 步骤2：检查`user_identity`表中对应uid的`identity_type='password'`记录是否存在且`status='active'` → 若不存在或status≠active，跳转至「场景二」
  - 步骤3：比对注册时设置的密码hash与`user_identity.credential_hash`字段是否一致（需用相同salt重算）→ 若不一致，跳转至「场景三」
三、解决步骤
 场景一：注册未写入账号中心
 - 适用条件：`user_profile`无记录，AuthCore日志出现`accountcore_client_timeout`
 - 实施步骤：
   - 登录AccountCore Pod：`kubectl exec -it <accountcore-pod> -- bash`
   - 检查网络连通性：`telnet accountcore-svc 8080`
   - 查看AccountCore日志末尾：`tail -20 /var/log/idass/ciam/accountcore/error.log`
   - 临时修复：手动插入基础记录（仅限紧急止血）：`INSERT INTO user_profile (uid, mobile, created_at) VALUES (UUID(), '138****1234', NOW());`
 - 结果验证：再次调用`/api/v1/users?mobile=...`返回用户信息
 场景二：密码凭证未创建或失效
 - 适用条件：`user_profile`存在，`user_identity`无password记录或status='inactive'
 - 实施步骤：
   - 执行初始化凭证命令（AccountCore容器内）：`/opt/idass/ciam/bin/init-password-credential.sh --uid <found_uid> --mobile 138****1234`
 - 结果验证：`SELECT * FROM user_identity WHERE uid='<found_uid>' AND identity_type='password';` 返回active记录
 场景三：密码加密逻辑不一致
 - 适用条件：hash比对失败，确认注册时使用了旧版PBKDF2参数（迭代次数=10000），当前版本要求=60000
 - 实施步骤：
   - 修改全局密码策略（控制台 > 安全策略 > 密码强度）：启用“兼容旧密码迁移”，保存后触发后台批量重哈希
 - 结果验证：用户下次登录时自动完成密码升级，后续登录正常
四、非本产品排查
● 明确标注：若`user_profile`中mobile字段为NULL或格式非法（如带空格），属于客户前端传参校验缺失，需客户侧修复注册表单逻辑
五、快速定位工具
● 脚本位置：`/opt/idass/ciam/bin/troubleshoot-register-login.sh`
● 使用方法：`bash /opt/idass/ciam/bin/troubleshoot-register-login.sh --mobile 138****1234 --since "2024-05-20T09:00:00Z"`
```

```yaml
（针对“用户注册成功但收不到短信验证码”）
一、问题描述
● 问题现象：用户在H5页面完成手机号注册，点击“获取验证码”按钮无响应，或提示“发送失败”；后台日志显示短信网关调用返回`{"code":500,"msg":"internal error"}`
● 适用范围：所有云版本；仅影响短信通道，Email/微信模板消息正常
二、排查信息收集
● 必须收集的信息：用户手机号、注册时间（精确到秒）、trace_id（从access.log提取）
● 检查终态的方法：登录`sms-gateway` Pod，执行`kubectl exec -it sms-gw-xxx -- tail -n 100 /var/log/sms/gateway.log`
● 排查问题步骤：
  - 步骤1：在SLS中搜索`trace_id:${trace_id} AND "sms.send"`，确认是否进入短信网关
  - 步骤2：若未进入，检查Auth Core是否因风控拦截（搜索`risk_blocked`）；若已进入，看网关日志中`provider_error_code`
  - 步骤3：`provider_error_code=1001` → 运营商通道拥塞 → 自动降级至备用通道（需确认备用通道配置）
  - 步骤4：`provider_error_code=2003` → 手机号在运营商黑名单 → 联系短信供应商解禁
三、解决步骤
 场景一：通道拥塞（provider_error_code=1001）
 - 适用条件：SLS中`sms.send`日志存在，且`provider_error_code=1001`
 - 实施步骤：
   - 登录OpsApi控制台 → 进入“短信通道管理” → 选择当前主通道 → 点击“切换备用通道”
   - 执行命令：`curl -X POST https://opsapi.aliyuncs.com/v1/sms/switch?channel=backup -H "Authorization: Bearer ${TOKEN}"`
 - 结果验证：新注册用户可正常收到验证码，SLS中`provider_error_code`变为`0`
 场景二：号码黑名单（provider_error_code=2003）
 - 适用条件：SLS中`provider_error_code=2003`
 - 实施步骤：
   - 复制手机号至短信供应商后台黑名单查询页
   - 提交解禁工单（模板见[[短信黑名单解禁SLA|IDaaS-SMS-Blacklist-SLA]]）
 - 结果验证：工单关闭后2小时内恢复发送
四、非本产品排查
● 若SLS中无`sms.send`日志，且Auth Core日志出现`risk_blocked:true`，属于风控引擎拦截，转交[[IDaaS-RiskEngine-Blocked-Case]]处理
五、快速定位工具
● 脚本位置：`/opt/idaas/tools/sms-trace.sh ${trace_id}`
● 使用方法：自动聚合Auth Core/SMS Gateway/SLS三方日志，输出关键路径与错误码
```

### 紧急场景止血与恢复手册：

*   **全站登录不可用（P0）**：立即执行`/opt/idass/ciam/bin/emergency-fallback-enable.sh`启用静态密码兜底模式（绕过风控与部分认证插件），5分钟内恢复基础登录能力；同步启动根因分析。
    
*   **P0级故障（认证全链路中断）**：执行`idaas-emergency-failover.sh --mode=standby`，1分钟内切换至同城容灾集群；同步通知客户启用本地缓存登录（JWT过期时间延长至24h）；  
    *脚本位置：`/opt/idaas/emergency/failover/idaas-emergency-failover.sh`*
    
*   **Token大规模失效（P1）**：运行`/opt/idass/ciam/bin/token-renew-batch.sh --hours 24`对近24小时签发的所有Access Token强制刷新，避免用户集中掉线。
    
*   **风控误杀导致90%以上登录被拦截（P1）**：执行`kubectl patch deploy riskengine -p '{"spec":{"replicas":0}}'`临时关闭风控服务，待策略回滚后再扩缩容恢复。
    
*   **条款签署服务不可用**：启用离线签署兜底模式：`curl -X POST https://consent.api.aliyuncs.com/v1/offline-enable -d '{"duration_hours":24}'`，允许用户跳过在线签署，24小时内补签。

### 横向研发文档：

*   接入指引：[[IDaaS CIAM-快速接入指南|CIAM接入五步法]]（含控制台开通、SDK引入、测试账号配置、联调Checklist、上线备案）；[[IDaaS CIAM标准接入流程|IDaaS-CIAM-QuickStart]]（含SDK下载、API调试沙箱、Postman集合）
    
*   产品对接方案细节：[[IDaaS CIAM-与客户系统对接方案|系统对接规范]]（明确API幂等性、重试机制、Webhook事件格式、数据加密要求）；[[IDaaS与业务中台对接规范|IDaaS-Biz-Platform-Integration]]（明确Token传递方式、用户属性映射字段、错误码对齐表）
    
*   产品对接范围：仅限C端用户身份域（注册/登录/资料/同意/风控），**不包含**员工组织架构同步、RBAC权限分配、工单审批流等EIAM能力；如需融合，需联合[[IDaaS EIAM|IDaaS EIAM产品页]]方案设计；支持与阿里云EDAS、SAE、MSE、API网关、函数计算（FC）原生集成；第三方平台需通过OpenAPI或通用登录页嵌入。

## 产品对内文档：

### 完整架构图：

*   系统采用分层架构：接入层（ALB + WAF）→ 网关层（OpenAPI Gateway + Login Widget CDN）→ 服务层（AuthCore/AccountCore/SessionCore/RiskEngine）→ 数据层（RDS/Redis/OSS）→ 运维层（ARMS/SLS/KMS/RAM）；  
*   系统采用“控制平面+数据平面”分离设计：  
    *   控制平面（Control Plane）：CIAM控制台、策略编排引擎、审计中心，部署于独立安全VPC；  
    *   数据平面（Data Plane）：认证中心、风控引擎、图谱服务，部署于客户业务VPC，通过PrivateLink与控制平面通信；  
*   模块间通过gRPC（内部）与REST（对外）双协议通信；所有服务注册至Nacos，配置中心统一管理；  
*   已知问题沉淀：[[IDaaS CIAM-已知问题与规避方案|Known Issues]]（含OAuth 2.1 Refresh Token reuse漏洞缓解方案、微信UnionID跨应用映射延迟说明等）；  
*   已知问题：跨VPC PrivateLink偶发延迟抖动（已提交阿里云网络团队修复，工单ID：NET-PL-2024-0882）。

### 业务逻辑时序图

*   用户使用：  
    `用户打开App → SDK加载Login Widget → 展示登录页 → 用户选择手机号登录 → 输入号码+验证码 → SDK调用/auth/v1/login → AuthCore校验 → 调AccountCore查账号 → 生成JWT → 返回Token → App携带Token访问业务API`  
    或：  
    `H5页面发起登录 → 跳转CIAM通用登录页 → 用户输入手机号+验证码 → Auth Core校验 → Risk Engine实时评分 → 决策是否放行/MFA → 返回ID Token → H5解析Token获取用户信息`  
*   工作流流转：  
    `注册请求 → AuthCore预校验 → AccountCore创建Profile → 发送激活邮件/SMS → 用户点击链接/输入验证码 → AccountCore激活账号 → 触发Consent初始化事件 → RiskEngine打标 → 写入审计日志`  
    或：  
    `管理员在控制台创建新条款 → 条款中心生成版本号并上链 → 推送至自服务平台 → 新用户注册时强制弹窗签署 → 签署记录写入consent_record表 → 同步至DataWorks供法务审计`

### 代码仓库

*   基线仓库：`code.alibaba-inc.com/idass/ciam-base`（含公共依赖、配置框架、基础DTO）；`git@code.aliyun.com:idm/idm-base.git`（主干分支：`main`）  
*   代码仓库：
    *   `code.alibaba-inc.com/idass/authcore`（认证中心主干）；`git@code.aliyun.com:idm/auth-core.git`
    *   `code.alibaba-inc.com/idass/accountcore`（账号中心主干）；`git@code.aliyun.com:idm/account-core.git`
    *   `code.alibaba-inc.com/idass/riskengine`（风控引擎主干）；`git@code.aliyun.com:idm/risk-engine.git`
    *   `code.alibaba-inc.com/idass/selfservice-ui`（自服务平台前端）；`git@code.aliyun.com:idm/selfservice-ui.git`
*   制品仓库：`acr.aliyuncs.com/idass/ciam`（Docker镜像，含`authcore:v2.5.3`等标签）；`registry.cn-hangzhou.aliyuncs.com/idm/`（镜像命名：`auth-core:v2.3.1-release`）  
*   关联依赖仓库：`code.alibaba-inc.com/idass/idp-common`（OIDC协议实现）、`code.alibaba-inc.com/idass/crypto-sdk`（国密SM2/SM4封装）；`git@code.aliyun.com:aliyun-open/aliyun-openapi-java-sdk.git`（OpenAPI SDK）

### 数据表结构

*   `user_profile`（MySQL 8.0）：`uid VARCHAR(64) PK`, `mobile VARCHAR(20)`, `email VARCHAR(100)`, `nickname VARCHAR(50)`, `gender TINYINT`, `birthday DATE`, `created_at DATETIME`, `updated_at DATETIME`, `status ENUM('active','frozen','deleted')`
    
*   `user_identity`（MySQL 8.0）：`id BIGINT PK`, `uid VARCHAR(64)`, `identity_type ENUM('password','sms','wechat','alipay')`, `identity_value VARCHAR(255)`, `credential_hash TEXT`, `salt VARCHAR(32)`, `status ENUM('active','inactive','revoked')`, `created_at DATETIME`
    
*   `consent_record`（OTS 5.0）：`id STRING PK`, `uid STRING`, `purpose_code STRING`, `version STRING`, `status STRING`, `granted_at INTEGER`, `withdrawn_at INTEGER`
    
*   `audit_log`（MySQL 8.0）：`id BIGINT PK`, `operator_type ENUM('user','admin','system')`, `operator_id VARCHAR(64)`, `action_type VARCHAR(50)`（如`user_register`）, `target_type VARCHAR(30)`, `target_id VARCHAR(64)`, `ip VARCHAR(45)`, `ua TEXT`, `result ENUM('success','failed')`, `created_at DATETIME`
    
*   `idm_user`（MySQL 8.0）：`id` BIGINT PK, `user_id` VARCHAR(64) NOT NULL COMMENT '全局唯一用户ID', `channel` VARCHAR(32) COMMENT '注册渠道（wechat/h5/app）', `created_at` DATETIME, `last_login_at` DATETIME, `status` TINYINT COMMENT '0-正常 1-锁定 2-注销'
    
*   `risk_event_log`（OTS 5.0）：`event_id` STRING PK, `user_id` STRING, `device_fingerprint` STRING, `ip` STRING, `risk_score` DOUBLE, `policy_id` STRING, `occurred_at` INTEGER (Unix timestamp)