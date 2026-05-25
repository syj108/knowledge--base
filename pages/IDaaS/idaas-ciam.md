# IDaaS CIAM

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   阿里云 [[IDaaS/aliyun-idaas-ciam|IDaaS CIAM]] 是面向顾客身份管理（Customer Identity and Access Management, CIAM）的云原生身份即服务（IDaaS）解决方案，核心使命是**帮助企业构建统一、安全、可扩展、体验无摩擦的顾客身份体系**，支撑跨 App/小程序/H5/网站等多触点的一致品牌体验。
        
    *   当前为公有云原生交付形态，持续迭代强化三大能力支柱：**统一身份整合能力（去重/僵尸清理/多源同步）、用户自助服务深度（分步注册/MFA/社交登录/账号全生命周期自管理）、高级安全与合规能力（动态风控/行为建模/条款统一治理）**；后续将深化与阿里云安全中台（如Threat Detection、RiskEngine）、数据中台（如DataWorks用户画像集成）及生态平台（如钉钉开放平台、支付宝小程序）的融合。
        
    *   能力涉及核心产品模块：**统一身份中心（UIC）、自助服务门户（Self-Service Portal）、风险控制引擎（Risk Engine）、条款与同意管理中心（Consent Hub）、开放API网关（CIAM API Gateway）**

*   对外介绍架构图
    
    *   中心端部署于阿里云公共云Region内，采用多可用区高可用容器化部署（ACK集群），核心组件以微服务形态运行于独立命名空间；端侧通过标准OAuth 2.0 / OIDC协议、RESTful API或SDK嵌入客户业务应用（Web/App/小程序），支持轻量级JS SDK、Android/iOS SDK、服务端SDK（Java/Python/Go）。
        
    *   数据流向：终端用户 → 客户前端应用 → CIAM API网关 → 身份认证服务（登录/注册）/ 风控引擎（实时策略决策）/ 用户中心（CRUD）→ 同步至客户业务系统（通过Webhook/API回调）或阿里云生态（如钉钉组织同步）。
        
    *   与上下游系统依赖关系：
        - **Tianji（阿里云资源调度平台）**：用于底层资源弹性伸缩与容量水位监控；
        - **OpsApi（阿里云运维开放平台）**：对接告警、巡检、变更事件，实现SRE协同；
        - **VPC/ECS/SLB**：作为基础设施依赖，CIAM服务实例部署在客户指定VPC内（私网访问模式）或通过SLB暴露公网Endpoint（需客户配置WAF）；
        - **云盾系列（如WAF、DDoS防护）**：CIAM默认集成云盾防护策略，客户可按需增强；
        - **不强依赖**：无需对接ACM（配置中心）、ARMS（应用监控）——CIAM内置可观测性能力。
        
    *   参考：[[IDaaS CIAM 架构概览图|IDaaS-CIAM-Architecture-Overview]]

*   各核心组件能力详细说明
    
    *   **统一身份中心（UIC）**：提供唯一用户标识（UID）、多源身份绑定（手机号/邮箱/微信/OpenID）、账号去重建议引擎、僵尸账号识别与清理工作流、懒加载式跨系统账号同步（支持SCIM v2.0协议对接HR/CRM等老旧系统）。
        
    *   **自助服务门户（Self-Service Portal）**：白标可定制化Web门户，支持分步注册（Step-by-Step Registration）、社交登录（微信/支付宝/Apple ID等）、MFA（TOTP/短信/语音/生物识别）、密码策略自定义、账号注销与数据导出（GDPR/PIPL合规）、操作日志可视化。
        
    *   **风险控制引擎（Risk Engine）**：基于阿里云多年风控经验沉淀的实时决策引擎，支持IP/设备指纹/地理位置/行为序列/登录时段等千人千面建模，输出风险分（0–100）、风险等级（Low/Medium/High/Critical）及处置建议（放行/二次认证/人工审核/拒绝），支持策略热更新与A/B测试。
        
    *   **条款与同意管理中心（Consent Hub）**：集中管理隐私政策、用户协议、营销授权等条款版本，支持条款动态签署、撤回、历史追溯与审计，自动同步至各业务系统，满足《个人信息保护法》《数据安全法》《App违法违规收集使用个人信息行为认定方法》等监管要求。
        
    *   **CIAM API网关**：提供标准化RESTful接口（OpenAPI 3.0规范），覆盖认证（/auth/login）、用户管理（/users/{uid}）、风控查询（/risk/assess）、条款操作（/consent）等全场景，支持JWT/OAuth2.0鉴权、QPS限流、调用链追踪。

*   与阿里云其他产品的关系
    
    *   与 VPC、ECS、SLB 等Top30产品的交互方式：
        - **VPC**：CIAM服务支持VPC内网部署模式，客户业务系统通过PrivateLink或内网SLB直连，避免公网暴露；VPC路由表、安全组策略由客户自主配置，CIAM不修改客户网络拓扑。
        - **ECS**：CIAM为SaaS服务，**不占用客户ECS资源**；客户若需自建前置代理或定制化网关，可部署于自有ECS，但该ECS不属于CIAM服务范围。
        - **SLB**：客户可选用阿里云SLB作为CIAM公网入口负载均衡器，CIAM提供健康检查探针（/healthz），SLB仅做四层/七层转发，**不参与身份逻辑处理**。
        - 影响：CIAM异常**不会导致客户VPC网络中断、ECS宕机或SLB不可用**；仅影响身份相关服务（登录/注册/风控等）的可用性。客户业务系统若未做降级（如本地缓存Token），可能触发“无法登录”类客诉，但**不影响已有会话的业务功能持续运行**（Session有效性由客户业务系统自身维护）。
        
    *   边界清晰声明：
        - ✅ CIAM负责：用户身份凭证颁发（ID Token/Access Token）、认证决策、风险评估、条款生命周期管理、基础用户属性存储（非业务属性）。
        - ❌ CIAM不负责：客户业务逻辑（如订单/支付）、客户数据库读写（除用户主数据同步外）、客户端UI渲染（门户仅提供白标框架）、第三方短信/邮件通道运维（对接阿里云短信服务SMS或客户自选通道，通道稳定性由对应产品保障）。

### QA（高频问答）：

*   Q：MAU计费是否包含测试账号、机器人流量？  
    A：否。MAU仅统计**真实终端用户**在自然月内完成至少一次成功身份认证（login/register/social-login）的去重人数；测试账号（如test@xxx.com）、自动化脚本、爬虫流量均不计入，需客户通过`is_test_user`等字段在调用API时主动标注。

*   Q：能否将现有用户库一键迁入CIAM并自动去重？  
    A：支持分阶段迁移。CIAM提供「迁移评估工具」（CLI + Web Console）扫描源库，输出重复账号匹配度报告（基于手机号/邮箱/设备ID等维度）；**合并操作需客户人工确认**，确保合规与数据主权，不支持全自动合并。

*   Q：风控引擎是否支持客户自定义规则？  
    A：支持。除预置规则外，客户可通过「风控策略中心」低代码配置规则（如“同一IP 1小时内注册>5次 → 拒绝”），支持引用客户自有风控标签（通过API注入），策略生效延迟<30秒。

*   Q：PIPL/GDPR合规审计报告如何获取？  
    A：客户登录IDaaS控制台 → 「合规中心」→ 下载《年度SOC2 Type II & PIPL合规自评估报告》，含数据存储地域（仅中国内地节点）、加密算法（AES-256+RSA-2048）、审计日志留存周期（180天）等关键项。

### 组件接口人、研发负责人等角色信息：

*   **统一身份中心（UIC）**：研发负责人 `@chenyi`（IDaaS-Core），L1运维权限：服务启停、配置热更（`configmap`）、基础指标查看（QPS/错误率）；L2需升级：数据库Schema变更、批量账号操作（>1000条）。
*   **自助服务门户（Portal）**：研发负责人 `@liujing`（IDaaS-Portal），L1权限：白标资源（Logo/CSS/文案）更新、静态页面发布；L2需升级：登录流程逻辑修改、MFA策略调整。
*   **风险控制引擎（Risk Engine）**：研发负责人 `@wangkai`（IDaaS-Risk），L1权限：策略启停、风险分阈值微调；L2需升级：模型特征工程变更、新设备指纹算法接入。
*   **条款与同意管理中心（Consent Hub）**：研发负责人 `@zhaoyan`（IDaaS-Consent），L1权限：条款版本发布/下线、签署记录导出；L2需升级：跨系统条款同步协议开发、审计日志深度分析。
*   **CIAM API网关**：研发负责人 `@sunlei`（IDaaS-Gateway），L1权限：API配额调整、黑白名单配置；L2需升级：协议转换（如OIDC→SAML）、自定义鉴权插件部署。

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `CIAM_UIC_UserMerge_Failure_Rate_High` | P1 | Prometheus指标 `uic_merge_failure_rate{job="uic"} > 0.05` 持续5min | UIC | 用户合并操作失败率超5% | 表明身份去重服务异常，可能导致客户无法完成历史账号整合 | [[KB-IDaaS-UIC-Merge-Fail|UIC合并失败应急手册]] | ✅ EOCC-2024-089 |
| `CIAM_RiskEngine_Response_Latency_GT_2s` | P1 | ArgoCD监控 `risk_engine_p95_latency_ms > 2000` | Risk Engine | 风控评估P95延迟超2秒 | 用户登录/注册流程卡顿，体验受损，需立即扩容或熔断非核心策略 | [[KB-IDaaS-Risk-Latency|Risk引擎高延迟处置]] | ✅ EOCC-2024-112 |
| `CIAM_Portal_LoginPage_5xx_Rate_High` | P2 | SLB访问日志 `http_code_5xx_rate > 0.01` | Portal | 门户登录页HTTP 5xx错误率超1% | 门户前端或后端服务异常，影响新用户注册/老用户登录 | [[KB-IDaaS-Portal-5xx|Portal 5xx故障排查]] | ✅ KB-2024-045 |
| `CIAM_Gateway_API_QPS_Exceed_Limit` | P2 | API网关监控 `gateway_qps_limit_exceeded_count > 0` | Gateway | 单API接口QPS持续超客户购买配额 | 客户业务突发流量或未合理配置限流，触发API拒绝响应 | [[KB-IDaaS-Gateway-QPS|网关QPS超限自助处理]] | ✅ KB-2024-077 |
| `CIAM_ConsentHub_Sync_Failure` | P3 | 日志关键字 `ConsentSyncFailed` + `error_code=SYNC_TIMEOUT` | Consent Hub | 条款同步至下游系统超时（>30s） | 条款状态未及时同步，不影响用户当前操作，但存在合规审计风险 | [[KB-IDaaS-Consent-Sync|条款同步失败处理]] | ✅ KB-2024-131 |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    - **PostgreSQL（UIC主库）**：`users`（用户主表，含uid/status/created_at）、`identities`（身份凭证表，含phone/email/wechat_openid）、`merges`（合并记录表，含source_uid/target_uid/merged_at）  
    - **Redis（Risk Engine缓存）**：`risk:device:{fingerprint}`（设备风险画像）、`risk:ip:{ip}`（IP风险分）  
    - **MongoDB（Consent Hub）**：`consent_records`（用户条款签署记录）、`policy_versions`（条款版本快照）

*   关键日志路径，组件，内容，轮转策略  
    - `/var/log/idass/uic/app.log`（UIC服务日志，JSON格式，含trace_id/user_id，按日轮转，保留7天）  
    - `/var/log/idass/risk/engine.log`（Risk Engine决策日志，含risk_score/risk_reason，按小时轮转，保留30天）  
    - `/var/log/idass/portal/nginx-access.log`（Portal Nginx访问日志，含user_agent/referer，按日轮转，保留15天）  
    - *注：所有日志已接入SLS，客户可通过SLS控制台查询，无需登录宿主机*

*   问题排查SOP，通用场景的排查思路和路径  
    1. **现象：用户无法登录** → 查Gateway 5xx日志 → 若为`401 Unauthorized` → 查UIC token签发日志 → 若为`403 Forbidden` → 查Risk Engine风控结果 → 若为`503 Service Unavailable` → 查UIC Pod Ready状态及CPU/Mem水位  
    2. **现象：风控策略未生效** → 查Risk Engine策略中心配置状态 → 查`risk_engine_policy_active`指标 → 若为0 → 检查策略发布时间是否早于当前时间 → 若仍无效 → 查`risk_engine_rule_eval_error_total`指标是否突增  

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    - 升级：全自动灰度升级，客户无感知；重大版本变更提前30天邮件通知，详见 [[AONE-IDaaS-Release-Plan|AONE-2024-IDaaS发布计划]]  
    - 巡检：每日自动执行《IDaaS健康巡检清单》（含DB连接池、Redis内存、证书有效期、SLB健康检查），报告推送至客户OpsApi工单系统  

### 典型问题排查解决方案

```yaml
（针对“用户注册时收不到短信验证码”）
一、问题描述
● 问题现象：用户在CIAM自助门户注册页填写手机号后，点击“获取验证码”无响应，或提示“发送失败”；客户侧短信通道（阿里云SMS）无扣量记录。
● 适用范围：所有云版本（v2.3.0+），公有云部署形态，影响范围为使用短信验证码的注册/登录流程。

二、排查信息收集
● 必须收集的信息：用户手机号（脱敏）、注册时间（精确到秒）、客户Project ID、CIAM实例ID（如 idaas-cn-hangzhou-abc123）
● 检查终态的方法：登录SLS控制台 → 选择日志库 `idaas-uic` → 查询 `__topic__: uic_sms_send AND phone:"138****1234" AND time_range:"2024-05-20 10:00:00~2024-05-20 10:05:00"`
● 排查问题步骤：
  - 步骤1：确认UIC服务是否正常 → 检查 `uic_sms_send_request_total` 指标是否有增量
  - 步骤2：若无增量 → 检查前端JS SDK是否加载失败（浏览器Console报错）
  - 步骤3：若有增量但无成功日志 → 查看 `uic_sms_send_failure_reason` 字段（常见值：`THROTTLE_EXCEEDED`, `MOBILE_INVALID`, `TEMPLATE_NOT_APPROVED`）

三、解决步骤
 场景一：短信发送被限流（THROTTLE_EXCEEDED）
 - 适用条件：`uic_sms_send_failure_reason = "THROTTLE_EXCEEDED"` 且 `uic_sms_send_request_total` 在1分钟内>10次
 - 实施步骤：
   - 登录IDaaS控制台 → 「安全设置」→ 「短信策略」→ 将“单手机号每分钟发送上限”从默认5提升至10
   - （L2权限）执行命令：`kubectl patch configmap uic-config -n idaas --type='json' -p='[{"op": "replace", "path": "/data/sms/throttle_per_min", "value":"10"}]'`
 - 结果验证：重新触发注册，SLS中出现 `sms_status: "success"` 日志

 场景二：短信模板未审批（TEMPLATE_NOT_APPROVED）
 - 适用条件：`uic_sms_send_failure_reason = "TEMPLATE_NOT_APPROVED"` 且客户使用了自定义模板
 - 实施步骤：
   - 登录阿里云短信服务控制台 → 「国内消息」→ 「短信模板」→ 找到对应模板（Code: `IDASS_REGISTER`）→ 点击「提交审核」
   - 审核通常需1-3工作日，期间可临时启用默认模板（无需审批）
 - 结果验证：审核通过后，SLS中 `sms_template_status: "APPROVED"` 出现

四、非本产品排查
● 明确标注：若SLS日志显示 `sms_status: "success"` 但用户仍未收到，属于阿里云短信服务（SMS）通道问题，需提工单至 [[SMS产品组|SMS-Support]] 协助排查运营商通道。

五、快速定位工具
● 脚本位置：`/opt/idass/tools/sms-debug.sh`（容器内）
● 使用方法：`bash /opt/idass/tools/sms-debug.sh -p <project_id> -m 138****1234 -t register`
```

### 紧急场景止血与恢复手册：

*   **P1级故障（如全量用户无法登录）一键止血**：  
    执行 `idaas-emergency-fallback.sh --mode=portal-bypass`（L2权限），自动将Portal登录请求旁路至UIC直连模式（跳过Risk Engine风控），5分钟内恢复基础登录能力；同步触发 `idaas-risk-engine-scale-down.sh` 降配风控引擎至最低规格，释放资源。

*   **数据误操作恢复**：  
    UIC数据库支持按时间点恢复（PITR），客户可申请恢复至误操作前5分钟快照，RTO<15分钟（需提前开通备份服务）。

### 横向研发文档：

*   接入指引：[[IDaaS-CIAM-QuickStart|CIAM快速接入指南]]（含SDK下载、控制台配置、沙箱环境申请）
*   产品对接方案细节：[[IDaaS-CIAM-Integration-Spec|CIAM对接技术规范V2.3]]
*   产品对接范围：明确CIAM仅提供身份层能力，**不对接客户ERP/CRM/SCM等业务系统**；如需打通，需客户自行开发适配器，或选用阿里云中间件（如DataHub）做数据管道。

## 产品对内文档：

### 完整架构图：

*   系统采用「控制平面+数据平面」分离设计：  
    - 控制平面（Control Plane）：UIC/Risk/Consent等有状态服务，部署于ACK集群，依赖K8s StatefulSet与PV持久化；  
    - 数据平面（Data Plane）：API网关（基于OpenResty）、CDN边缘节点（加速Portal静态资源）、Redis Cluster（风控缓存）、PostgreSQL HA（UIC主库）；  
    - 关键已知问题：UIC在跨Region灾备场景下，用户最终一致性窗口为30秒（因PG逻辑复制延迟），已在Roadmap中规划基于Flink CDC的亚秒级同步方案（预计2024 Q4上线）。

### 业务逻辑时序图

*   用户注册全流程时序（含风控介入点）：  
    `用户提交手机号 → Portal调用Gateway `/auth/register/init` → UIC生成nonce → Gateway返回前端 → 用户输入验证码 → Portal调用 `/auth/register/verify` → Gateway转发至UIC → UIC校验短信 → **同步调用Risk Engine `/risk/assess`** → 返回risk_level → Gateway根据level决定是否放行/二次认证 → UIC创建用户 → 返回Token`

*   工作流流转：  
    `客户提交需求 → IDaaS-PMM评审 → 进入AONE需求池 → 研发排期（UIC/Risk/Portal分队列） → 自动化流水线（Jenkins+SonarQube） → 灰度发布（1%→10%→100%） → SRE值班监控 → 客户反馈闭环`

### 代码仓库

*   基线仓库：`idaas-ciam-base`（GitLab，含公共依赖、CI/CD模板）
*   代码仓库：  
    - `idaas-uic-core`（Java/Spring Boot）  
    - `idaas-risk-engine`（Python/TensorFlow Serving）  
    - `idaas-portal-fe`（Vue3 + TypeScript）  
    - `idaas-gateway`（OpenResty + Lua）  
*   制品仓库：`aliyun-acr.cn-shanghai/idaas/`（镜像仓库，按语义化版本tag）  
*   关联依赖仓库：`aliyun-openapi-java-sdk-idass`（OpenAPI SDK）、`aliyun-idass-js-sdk`（前端SDK）

### 数据表结构

*   `users` 表（PostgreSQL）：  
    `uid VARCHAR(64) PK`, `status VARCHAR(20) NOT NULL DEFAULT 'active'`, `created_at TIMESTAMPTZ NOT NULL`, `last_login_at TIMESTAMPTZ`, `profile JSONB`（存储昵称/头像/性别等，非敏感字段）  
*   `identities` 表：  
    `id SERIAL PK`, `uid VARCHAR(64) NOT NULL`, `type VARCHAR(20) NOT NULL CHECK (type IN ('phone','email','wechat'))`, `value VARCHAR(255) NOT NULL`, `verified BOOLEAN DEFAULT false`, `created_at TIMESTAMPTZ NOT NULL`  
*   `risk_decisions` 表（归档表，按月分区）：  
    `id BIGSERIAL`, `request_id VARCHAR(64)`, `uid VARCHAR(64)`, `risk_score NUMERIC(5,2)`, `risk_level VARCHAR(10)`, `reasons TEXT[]`, `created_at TIMESTAMPTZ`