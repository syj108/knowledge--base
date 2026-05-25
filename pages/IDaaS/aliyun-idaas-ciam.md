# IDaaS CIAM

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   [[ciam/aliyun-idaas-ciam|阿里云IDaaS CIAM]]是面向消费者（C端）、会员等**外部用户**的客户身份与访问管理（Customer Identity & Access Management）解决方案，聚焦于统一身份认证、跨平台无缝体验、高并发可用性及本土化服务能力，区别于面向员工/内部人员的EIAM（Employee IAM）。其核心价值涵盖**统一身份整合、用户体验优化、安全风控与合规治理**四大维度，支撑 App、小程序、H5、Web 等多端一致、无摩擦的身份体验。
        
    *   作为IDaaS产品体系的重要分支，CIAM自2023年起独立演进，逐步强化私有化部署能力、中国合规适配（如手机号一键登录、银联/微信/支付宝OAuth2.0深度集成、等保三级支持）、多租户隔离与品牌定制化能力；2024年上线专属云版本，支持资源独享与专家级客制化交付。公有云标准版则体现“云原生、持续交付、合规内建”特性：自动获取最新漏洞补丁、即时启用新功能、持续满足《个人信息保护法》《数据安全法》等最新安全合规要求。
        
    *   当前版本已全面支持 OneID 身份融合架构、全链路无感认证配置、多租户隔离的弹性伸缩能力、等保三级/PCI DSS/ISO 27001 等多维合规基线，并持续增强本土化（如微信/支付宝/银联快捷登录、手机号一键登录、网证核验）与全球化（GDPR/CCPA 兼容策略引擎、多语言自适应门户）双轨能力。
        
    *   能力覆盖核心产品：`IDaaS CIAM控制台`、`CIAM Auth Service（认证中心）`、`CIAM User Directory（客户用户目录）`、`CIAM Self-Service Portal（自助门户）`、`CIAM API Gateway（开放API网关）`，以及私有化部署所需的`CIAM On-Premise Orchestrator`组件；同时延伸至CIAM专属能力层：**消费者门户（Consumer Portal）**、**营销身份桥接器**、**隐私合规工作台（Privacy & Compliance Console）**，并与阿里云生态深度集成模块（如 [[IDaaS/OneID融合方案|OneID融合方案]]、[[IDaaS/合规能力矩阵|合规能力矩阵]]）形成完整能力矩阵。

*   对外介绍架构图
    
    *   **部署架构**：支持三类形态——  
        ▪ 公共云标准版（多租户共享底座，SaaS模式），具备弹性伸缩、自动升级、最新合规等云原生优势；  
        ▪ 公共云专属版（VPC内独占资源池，含独立K8s集群、DB实例、Redis集群）；  
        ▪ 私有化部署版（交付至客户IDC或信创环境，支持ARM/x86混合架构、国产OS/数据库适配）；  
        所有形态均采用容器化部署（Alibaba Cloud ACK），核心组件以Pod形式运行于物理节点或ECS实例之上。中心端采用多可用区高可用部署，端侧通过轻量 SDK / JS SDK / OpenAPI / SAML/OIDC 标准协议对接 Web/App/小程序/IoT 等全触点。
        
    *   **数据流向图**：终端用户 → 前端应用（Web/App/小程序）→ CIAM API Gateway → Auth Service（鉴权/会话管理）↔ User Directory（用户主数据CRUD）↔ Self-Service Portal（密码重置、MFA绑定、实名认证、条款签署等）→ 同步至下游业务系统（通过SCIM/API/消息队列）；审计日志与合规事件同步至审计中心与阿里云 SLS/ActionTrail。
        
    *   **上下游依赖关系**：  
        ▪ 依赖：[[云产品/天基（Tianji）|Tianji]]（资源纳管与健康巡检）、[[云产品/OpsApi|OpsApi]]（运维指令下发）、[[云产品/ARMS|ARMS]]（全链路监控埋点）、[[云产品/SLS|SLS]]（日志采集）、[[云产品/云盾WAF|WAF]] 与 [[云产品/态势感知|态势感知]]（攻击防护联动）、ACR（镜像仓库）；  
        ▪ 被依赖：电商中台、会员中心、金融风控系统、教育SaaS平台等业务系统通过CIAM SDK/API接入身份能力；  
        ▪ 无强依赖：VPC（仅网络连通性要求，不依赖其安全组策略逻辑）、ECS（仅作为运行载体，不耦合实例生命周期）、SLB（可选，CIAM自身提供Ingress Controller）。
        
    *   参考：  
        ![IDaaS CIAM 架构示意图](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/54Lq35oXpkvg1l7E/img/1213e0b1-d245-4023-8f12-1a8fa1c09600.png)  
        ![IDaaS CIAM 对外架构图](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/8oLl952KrmxMNlap/img/idaas-ciam-arch-external-v2.png)

*   各核心组件能力详细说明
    
    *   **统一身份中心 / 身份目录（Identity Directory）**：基于 OneID 模型构建统一消费者身份图谱，支持跨渠道身份归并（Deterministic + Probabilistic Matching）、主身份标识（Primary ID）与关联身份（Linked ID）分层管理；提供账号匹配与合并（人工确认）、僵尸账号识别与清理、跨触点账号懒加载同步能力，解决身份孤岛、数据冗余与画像失真问题。基于分库分表的客户主数据存储，支持千万级用户实时读写；字段模型兼容GDPR/《个人信息保护法》，内置敏感字段加密（SM4/AES-256）、脱敏策略配置、数据生命周期管理（如“注册后180天未激活自动归档”）。
        
    *   **认证中心（AuthN Center）**：承担OAuth2.0/OpenID Connect协议实现、JWT签发与校验、会话状态管理（Redis-backed）、风险登录识别（设备指纹+行为分析）、MFA（短信/邮件/TOTP/生物识别/FIDO2）策略引擎；支持QPS ≥ 50,000（专属版）；支持密码、短信、生物识别、FIDO2、第三方联合登录（OIDC/SAML）、风险自适应认证（设备指纹+IP+行为模型），支持无感续期与静默登录。
        
    *   **消费者门户（Consumer Portal） / Self-Service Portal**：白标化前端门户，支持多语言（含简体中文优先）、主题皮肤定制、流程编排（如“忘记密码→短信验证→新密码设置→安全问题绑定”可拖拽配置）；完整覆盖分步注册、MFA、实名认证、社交登录、账号解绑/删除/日志查看等全自助能力；提供白标可定制的注册/登录/账号管理/隐私设置界面，支持多语言、多主题、行为埋点与A/B测试，内置微信/支付宝/手机号一键登录等本土化能力。
        
    *   **策略引擎（Policy Engine）**：可视化编排认证策略、授权策略、数据最小化策略、地域合规策略（如 GDPR 数据驻留开关），支持运行时动态加载与灰度发布；基于用户历史行为（IP、设备、地理位置、访问时段）构建个性化风险模型，实现“千人千面”策略；支持实时异常检测、主动触发二次认证、风险事件回调通知。
        
    *   **应用网关（App Gateway） / CIAM API Gateway**：统一对接入口，提供RESTful API（含OpenAPI 3.0规范）、SDK（Java/Python/iOS/Android）、Webhook事件回调（用户注册成功、登录异常、条款签署完成等），具备限流、熔断、审计日志全量记录能力；作为业务系统统一接入入口，完成协议转换（OIDC/SAML/LDAP）、属性映射、会话管理、细粒度权限代理，屏蔽下游系统身份逻辑。
        
    *   **隐私合规工作台（Privacy & Compliance Console）**：统一纳管隐私政策、用户协议等法律文本，支持版本发布、用户签署状态追踪、历史同意记录审计，确保 GDPR 及中国《个人信息保护法》等合规落地；提供 DSAR（被遗忘权）自助申请与自动执行、数据影响评估（DPIA）模板、合规检查清单、审计报告生成（等保/PCI/ISO）。
        
    *   **CIAM On-Premise Orchestrator**（私有化专有）：Ansible+Helm驱动的自动化部署与升级引擎，支持离线环境安装、证书自动轮转、国产中间件（达梦/人大金仓/OceanBase）适配、等保三级基线加固。

*   与阿里云其他产品的关系
    
    *   与 VPC：仅需基础网络连通（ICMP + TCP 443/80），不依赖VPC高级功能（如流日志、网络ACL策略联动）；CIAM异常**不会导致VPC网络中断或路由变更**。IDaaS 控制平面默认部署于客户 VPC 内网，数据面支持 PrivateLink 接入，避免公网暴露；与 VPC 流量策略协同实现南北向访问控制。
        
    *   与 ECS：运行载体关系，CIAM Pod故障仅影响本实例上服务，**不会触发ECS实例重启、释放或系统盘损坏**；ECS底层故障由云平台自动恢复，CIAM具备Pod级自愈能力（Liveness Probe）。IDaaS 容器化部署依赖 ECS 实例资源池，通过弹性伸缩组（ESS）按流量峰值自动扩缩容；不直接操作客户 ECS，但可通过 [[IDaaS/运维手册#ECS资源监控|运维手册]] 提供 ECS 资源水位联动告警。
        
    *   与 SLB：非必需依赖；若客户使用SLB做流量入口，CIAM仅将其视为四层/七层转发器，**SLB配置错误（如健康检查路径误配）可能导致502，但CIAM自身服务无损**；CIAM可直连ECS IP或通过ALB Ingress暴露服务。IDaaS 前端负载由阿里云 SLB 承载，支持 TLS 卸载与 WAF 集成；SLB 异常仅影响接入可用性，不影响身份目录与策略引擎内部状态一致性。
        
    *   边界声明：  
        ▪ **CIAM不负责业务权限决策**（如“用户能否查看订单详情”），该逻辑由业务系统基于CIAM提供的身份上下文（sub/roles/scopes）自行判断；  
        ▪ **CIAM不存储业务数据**（如订单、课程、资产），仅保存身份属性与认证凭证；  
        ▪ **CIAM不替代WAF/防火墙**，但提供防暴力破解、验证码、人机识别等应用层防护能力；  
        ▪ **CIAM异常不影响客户自有业务逻辑执行**（如订单创建、支付处理、内容展示等），仅阻断身份认证与授权环节；**不直接访问或修改客户业务数据库**；**不接管客户应用服务器（ECS）或网络配置（VPC/SLB）**，属独立身份服务层。  
        *不会造成的影响（边界清晰）：*  
        - 不导致客户业务数据库泄露或篡改；  
        - 不接管或修改客户业务系统的用户表结构；  
        - 不替代客户原有 RBAC/ABAC 权限模型，仅提供身份上下文与策略代理能力。

### QA（高频问答）：

*   Q：CIAM是否支持微信/支付宝/银联等国内主流身份源？  
    A：是。已预集成微信开放平台、支付宝开放平台、银联云闪付OAuth2.0协议，并支持企业自有身份源（LDAP/SAML 2.0）对接。

*   Q：私有化版本是否支持信创环境（麒麟OS+达梦DB+鲲鹏CPU）？  
    A：是。2024年Q2起全面支持信创全栈适配，已通过工信部信创实验室认证。

*   Q：用户量超千万时，性能如何保障？  
    A：专属版/私有化版支持水平扩展：User Directory可分片至32个MySQL Shard；Auth Service支持Stateless横向扩容（最高200+ Pod）；全链路压测QPS ≥ 120,000（P99 < 300ms）。公有云标准版依托阿里云底层弹性资源池，具备从千级到亿级用户的线性伸缩能力，经零售、旅游、医疗等行业真实高峰场景验证。

*   Q：是否满足等保2.0三级要求？  
    A：是。公共云专属版与私有化版均通过等保三级测评，提供审计日志留存≥180天、操作留痕、双因子认证、传输加密（TLS1.2+）、存储加密（SM4）等能力。

*   Q：能否与企业现有HR系统（如北森、Moka）打通同步员工账号？  
    A：不推荐。CIAM定位为**C端用户**管理，员工账号应由EIAM（如阿里云Resource Directory）或HRIS系统管理；如需少量员工兼用CIAM门户，可通过SCIM API单向同步只读基础信息（姓名/邮箱），禁止同步薪资、职级等敏感字段。

*   Q：CIAM 收费模式是什么？是否包含僵尸账号？  
    A：按月活用户数（MAU）计费，仅对当月实际活跃用户收费；陈旧/僵尸账号不计入 MAU，不产生费用。

*   Q：能否替代企业现有 IAM（员工身份）系统？  
    A：不能。IDaaS CIAM 专为**顾客（C端）身份**设计，与面向员工/合作伙伴的 B2E/B2B IAM 场景存在本质差异（如权限粒度、合规重点、体验诉求）。两者可并存，IDaaS CIAM 不覆盖内部员工管理。

*   Q：是否支持私有化部署？  
    A：支持。除公有云标准版与专属版外，提供完整的私有化部署版本，满足金融、政务、央企等对数据主权与基础设施自主可控的严苛要求。

*   Q：是否满足国内数据合规要求？  
    A：是。内置条款管理中心与用户同意管理能力，深度适配《个人信息保护法》《数据安全法》要求；所有用户数据默认存储于中国内地地域，符合本地化存储规定；支持敏感字段加密、脱敏、生命周期管理等全链路数据治理能力。

*   Q：IDaaS CIAM 是否支持将已有会员系统账号与小程序用户自动合并？  
    A：支持。通过 [[IDaaS/OneID融合方案|OneID融合方案]] 中的「跨源身份归并」能力，配置匹配规则（如手机号+身份证号哈希）后，可在后台触发批量归并或实时注册时自动对齐，生成统一 Primary ID。

*   Q：海外用户访问中国部署的 IDaaS 门户，是否符合 GDPR？  
    A：符合。IDaaS 支持数据驻留策略（Data Residency Policy），可为欧盟租户启用独立数据分区（EU Zone），所有用户数据不出欧盟；同时提供标准 DPA 模板、DSAR 自助通道、数据处理日志审计，满足 GDPR 第28条要求。

*   Q：能否限制某类用户（如未成年人）仅能使用特定认证方式？  
    A：可以。在 [[IDaaS/策略引擎|策略引擎]] 中配置「条件策略」，基于用户属性（age_group）、设备类型（mobile_web）、地理位置（country_code）组合判断，动态启用/禁用认证因子（如禁止密码登录，强制人脸识别）。

*   Q：IDaaS 是否提供等保三级测评支持材料？  
    A：提供。客户可从 [[IDaaS/合规能力矩阵|合规能力矩阵]] 页面下载《IDaaS 等保三级合规实施指南》《系统定级报告》《安全管理制度汇编》及阿里云侧等保测评通过证书（编号可查）。

### 组件接口人、研发负责人等角色信息：

| 组件 | 研发负责人 | L1 运维权限（值班可操作） | L2 运维权限（需审批） | 升级产研条件 |
|------|-------------|---------------------------|--------------------------|----------------|
| 消费者门户 / CIAM Self-Service Portal | 王磊（wanglei@alibaba-inc.com） | 门户主题切换、文案热更、A/B 测试开关 | 修改登录流程节点、新增第三方登录插件 | 出现白屏/JS 报错且 CDN 缓存刷新无效 |
| 认证中心 / CIAM Auth Service | 李婷（liting@alibaba-inc.com） | 认证策略启停、风险模型阈值微调、临时关闭某因子 | 修改核心认证协议逻辑、升级 FIDO2 服务端库 | 连续 5 分钟认证成功率 < 95% 且非网络原因 |
| 身份目录 / CIAM User Directory | 张伟（zhangwei@alibaba-inc.com） | 查询/导出用户快照、触发单用户归并、清理测试数据 | 执行全量身份归并、调整主身份生成规则 | 目录写入延迟 > 30s 或出现重复 Primary ID |
| 策略引擎 | 陈敏（IDaaS-Policy） | 策略启停、变量值热更新、灰度发布比例调整 | 修改策略 DSL 语法、上线新策略类型 | 策略执行超时率 > 10% 或返回空上下文 |
| 应用网关 / CIAM API Gateway | 陈明（chenming@alibaba-inc.com） | 应用接入状态切换、属性映射调试、JWT 签名密钥轮转 | 修改协议转换逻辑、新增 OIDC Scope 映射 | 网关 5xx 错误率 > 5% 或平均延迟 > 800ms |
| CIAM On-Premise Orchestrator（私有化） | 刘芳（liufang@alibaba-inc.com） | 执行Ansible Playbook重装、证书续期、国产中间件参数调优 | — | — |

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
|--------|------|-----------|------------|-------------|--------|----------------|----------------|
| `CIAM_Auth_JWT_Signature_Verify_Fail_Rate_High` | P0 | Prometheus指标 `auth_jwt_verify_fail_rate{job="ciamauth"} > 0.05` 持续5分钟 | CIAM Auth Service | JWT签名验签失败率超5% | 密钥轮转不一致、时间不同步、恶意Token泛洪攻击 | [[紧急场景止血与恢复手册#jwt密钥不一致应急|JWT密钥不一致应急]] | 是（KB#CIAM-SEC-001） |
| `CIAM_UserDir_DB_Conn_Pool_Exhausted` | P0 | Grafana看板 `userdir_db_conn_used_percent > 95` | CIAM User Directory | 数据库连接池使用率持续>95% | 用户目录读写请求积压，注册/登录超时风险 | [[典型问题排查解决方案#DB连接池耗尽|DB连接池耗尽]] | 是（EOCC#CIAM-DB-002） |
| `CIAM_SSP_Portal_5xx_Rate_High` | P1 | SLS日志统计 `status >= 500 and service=ssportal` QPM > 10 | CIAM Self-Service Portal | 门户5xx错误每分钟超10次 | 前端资源加载失败、后端微服务超时、模板渲染异常 | [[典型问题排查解决方案#SSP门户5xx|SSP门户5xx]] | 是（KB#CIAM-FE-003） |
| `CIAM_APIGW_RateLimit_Exceeded` | P2 | API Gateway监控 `apigw_ratelimit_rejected_total` 突增 | CIAM API Gateway | 单用户/APP Key调用量突破限流阈值 | 客户端Bug导致无限重试，或遭受爬虫攻击 | [[运维指导/运维手册#API限流配置|API限流配置]] | 否（L2可自主调整阈值） |
| `CIAM_OnPrem_Orchestrator_Cert_Expired` | P1 | 自检脚本 `orchestrator-cert-check.sh` 返回非零 | CIAM On-Premise Orchestrator | TLS证书剩余有效期<7天 | 所有HTTPS接口将拒绝连接，影响全部用户访问 | [[紧急场景止血与恢复手册#证书过期一键续签|证书过期一键续签]] | 是（EOCC#CIAM-OP-004） |
| `AuthN_Center_Availability_Low` | P0 | ARMS 监控指标 `authn_health_check_success_rate < 95%` 持续 2min | 认证中心 | 健康检查失败率超标 | 认证服务整体不可用风险，影响新登录与会话续期 | [[IDaaS/应急手册#P0-认证中心不可用|P0-认证中心不可用]] | 是（KB#IDAAS-AUTHN-001） |
| `IdRepo_Write_Latency_High` | P1 | ARMS 指标 `idrepo_write_p99_latency > 3000ms` | 身份目录 | 写入延迟 P99 超 3s | 新注册/资料更新响应缓慢，可能引发前端超时 | [[IDaaS/应急手册#P1-目录写入延迟高|P1-目录写入延迟高]] | 是（KB#IDAAS-IDREP-002） |
| `Policy_Engine_Execution_Timeout` | P1 | SLS 日志匹配 `ERROR.*policy_execution_timeout` | 策略引擎 | 单次策略执行耗时超 2s | 授权决策失败，可能导致业务系统拒绝合法请求 | [[IDaaS/应急手册#P1-策略执行超时|P1-策略执行超时]] | 是（KB#IDAAS-POLICY-003） |
| `Consumer_Portal_JS_Error_Rate_High` | P2 | 前端监控 RUM 指标 `js_error_rate > 1%` | 消费者门户 | JS 错误率超阈值 | 门户部分功能异常（如注册表单提交失败），影响用户体验 | [[IDaaS/应急手册#P2-门户JS错误率高|P2-门户JS错误率高]] | 是（KB#IDAAS-PORTAL-004） |
| `Gateway_JWT_Signature_Verify_Fail` | P2 | SLS 日志匹配 `WARN.*jwt_signature_verify_fail` | 应用网关 | JWT 签名校验失败率突增 | 下游业务系统可能因 token 无效拒绝用户，需排查密钥同步或时间偏移 | [[IDaaS/应急手册#P2-网关JWT校验失败|P2-网关JWT校验失败]] | 是（KB#IDAAS-GW-005） |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    ▪ `user_directory.t_user_base`：用户主表（user_id, mobile, email, status, created_at）  
    ▪ `user_directory.t_user_profile`：扩展属性表（user_id, profile_json, updated_at）  
    ▪ `auth_service.t_session_active`：活跃会话表（session_id, user_id, expire_at, ip, ua）  
    ▪ `auth_service.t_auth_event_log`：认证事件审计表（event_id, user_id, event_type, result, risk_level）  
    ▪ `id_repo.users`：主身份信息（primary_id, status, created_at, last_login_at）  
    ▪ `id_repo.identities`：关联身份凭证（identity_type: phone/wechat/openid, credential_hash, linked_to_primary_id）  
    ▪ `policy_engine.policies`：策略定义 JSON（含 conditions, actions, priority）  
    ▪ `audit_center.events`：全量审计事件（event_type: login/register/consent, user_id, ip, ua, timestamp）

*   关键日志路径，组件，内容，轮转策略  
    ▪ `/var/log/ciam/auth-service/access.log`（CIAM Auth Service）：Nginx access日志，JSON格式，按日切割，保留30天  
    ▪ `/opt/ciam/ssportal/logs/error.log`（CIAM Self-Service Portal）：前端服务错误日志，按大小切割（100MB），保留7天  
    ▪ `/data/ciam/orchestrator/logs/ansible.log`（CIAM On-Premise Orchestrator）：Ansible执行日志，按任务ID归档，永久保留（压缩存储）  
    ▪ `/var/log/idaas/authn-center/*.log`（认证中心）：含认证请求、风险判定、因子调用详情；logrotate 每日切割，保留 30 天  
    ▪ `/var/log/idaas/id-repo/*.log`（身份目录）：含 CRUD 操作、归并日志、索引更新；ELK 实时采集，冷数据归档 OSS  
    ▪ `/var/log/idaas/portal/frontend-*`（消费者门户）：RUM 埋点与 JS 错误；SLS 采集，保留 90 天  

*   问题排查SOP，通用场景的排查思路和路径  
    ▪ **现象：用户无法登录** → 查API Gateway 4xx/5xx日志 → 若为401/403，查Auth Service JWT解析日志 → 若为500，查User Directory DB慢查询日志 → 最终定位至具体组件与错误码  
    ▪ **现象：自助门户白屏** → 查SSP Nginx error.log → 若报`Failed to fetch config`，检查`/etc/ciam/ssportal/config.json`是否存在且可读 → 若报`Cannot resolve module`，检查CDN资源URL是否失效  
    ▪ **现象：用户注册成功但无法登录** → 查 `id_repo.users` 状态是否 active → 查 `id_repo.identities` 是否存在有效凭证 → 查 `authn-center` 登录日志是否匹配凭证类型 → 查网关是否拦截未授权字段  
    ▪ **现象：策略未生效** → 查 `policy_engine.policies` 是否启用 → 查 `events` 表是否有对应 event_type 触发 → 查策略 condition 是否匹配用户属性（用 [[IDaaS/策略调试工具|策略调试工具]] 模拟验证）  

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    ▪ 升级指南：[[IDaaS CIAM 版本升级SOP|CIAM版本升级SOP]]（含灰度发布checklist、回滚步骤、兼容性矩阵）  
    ▪ 巡检手册：每日自动巡检项见 [[IDaaS CIAM 日常巡检清单|CIAM日常巡检清单]]（含DB连接数、Redis内存、证书有效期、API成功率）  
    ▪ Aone项目：`IDaaS-CIAM-Release`（基线分支：`release/2.5.x`），制品仓库：`acr.aliyuncs.com/idass/ciam-auth:v2.5.3`  
    ▪ 升级指南见 [[IDaaS/版本升级手册|版本升级手册]]（含灰度发布checklist、回滚步骤）  
    ▪ 每日巡检项见 [[IDaaS/巡检手册|巡检手册]]（含健康检查、延迟水位、错误率、合规策略开关状态）  
    ▪ Aone 链接：[[AONE/IDaaS-CIAM-Release|IDaaS-CIAM 发布流水线]]

### 典型问题排查解决方案

```yaml
（针对“用户注册成功但收不到短信验证码”）
一、问题描述
● 问题现象：用户在SSP门户完成手机号注册，页面提示“发送成功”，但手机未收到短信；后台无报错提示。
● 适用范围：所有部署形态（公共云/专属版/私有化），v2.4.0+
二、排查信息收集
● 必须收集的信息：用户手机号（脱敏）、注册时间（精确到秒）、CIAM租户ID（tenant_id）、SSP前端trace_id
● 检查终态的方法：登录CIAM API Gateway容器，执行`curl -X GET "http://localhost:8080/api/v1/sms/log?phone=138****1234&start=2024-05-20T08:00:00Z"`；登录短信供应商控制台核对发送记录
● 排查问题步骤：
  - 步骤1：查API Gateway日志，过滤`sms/send`关键词，确认请求是否到达网关（关键字段：`status=200`, `sms_result=success`）
  - 步骤2：若网关返回200但无短信，查Auth Service日志，搜索`SMS_PROVIDER_CALL_FAILED`，确认是否调用第三方短信API失败
  - 步骤3：若Auth Service日志显示调用成功，检查`/etc/ciam/auth-service/sms-config.yaml`中`provider: aliyun`是否配置正确，`access_key_secret`是否被base64误解码
  - 排查结果映射表：
    | 排查结果 | 对应场景 |
    |---|---|
    | 网关无`sms/send`日志 | 前端JS未触发API调用（检查浏览器console报错） |
    | 网关返回429 | 短信供应商频控触发（查`x-ratelimit-remaining`响应头） |
    | Auth Service日志报`Invalid access key` | `sms-config.yaml`中AKSK硬编码错误或权限不足 |
三、解决步骤
 场景一：短信供应商频控触发
 - 适用条件：网关日志显示`status=429`, `x-ratelimit-remaining=0`
 - 实施步骤：登录短信供应商控制台 → 进入“频控管理” → 将CIAM租户IP加入白名单或提升QPS配额 → 执行`kubectl rollout restart deploy/ciam-auth-service`
 - 结果验证：重新注册，`curl`查询短信日志返回`"status":"DELIVERED"`
 场景二：sms-config.yaml配置错误
 - 适用条件：Auth Service日志出现`com.aliyun.tea.TeaException: InvalidAccessKeyId.NotFound`
 - 实施步骤：进入`/etc/ciam/auth-service/` → 备份原文件`cp sms-config.yaml sms-config.yaml.bak` → 使用`base64 -d`解码`access_key_secret`字段值 → 替换为明文 → `kubectl exec -it ciamauth-pod -- /bin/sh -c "kill -HUP 1"`
 - 结果验证：日志不再报AK错误，短信正常接收
四、非本产品排查
● 若短信供应商控制台显示“发送成功”但运营商侧拦截，需联系运营商申诉，不属于CIAM责任范围。
五、快速定位工具
● 脚本位置：`/opt/ciam/tools/sms-debug.sh`（需传入手机号与时间范围）
● 使用方法：`bash /opt/ciam/tools/sms-debug.sh -p 138****1234 -s "2024-05-20T08:00:00Z"`
```

```yaml
一、问题描述
● 问题现象：用户使用微信扫码登录后，业务系统收不到 unionid 字段，导致无法关联老会员
● 适用范围：IDaaS v3.2.0+，接入方式为 OIDC，scope 包含 'unionid'
二、排查信息收集
● 必须收集的信息：应用 client_id、用户 openid、微信 access_token（调试模式下可获取）、IDaaS 租户 ID
● 检查终态的方法：登录 authn-center 容器，执行 curl -X GET "http://localhost:8080/api/v1/debug/identity?openid=xxx" 查看原始身份源数据
● 排查问题步骤：
  - 步骤1：确认微信开放平台已开通 unionid 权限（需同主体公众号/小程序）
  - 步骤2：检查 IDaaS 应用配置中「OIDC Claims Mapping」是否将 `unionid` 映射到 `user.unionid`
  - 步骤3：查看 authn-center 日志中 `wechat_auth_callback` 是否返回 unionid（grep 'unionid'）
  - 步骤4：检查网关 JWT payload 是否含 `unionid` 字段（用 https://jwt.io 解析）
三、解决步骤
 场景一：微信未返回 unionid
 - 适用条件：步骤3 日志中无 unionid 字段
 - 实施步骤：联系客户确认微信开放平台资质，或切换为「公众号授权登录」模式（天然支持 unionid）
 - 结果验证：重新扫码，日志中出现 unionid，JWT 中包含该字段
 场景二：Claims Mapping 未配置
 - 适用条件：步骤3 有 unionid，但步骤4 JWT 中缺失
 - 实施步骤：进入 [[IDaaS/应用管理#编辑OIDC映射|应用管理 → OIDC Claims Mapping]]，添加映射 rule: source="unionid", target="unionid", type="string"
 - 结果验证：JWT payload 新增 "unionid": "xxx"
四、非本产品排查
● 若微信 access_token 本身不包含 unionid：属微信侧资质问题，需客户自行联系微信开放平台支持
五、快速定位工具
● 脚本位置：/opt/idaas/tools/debug-wechat.sh（传入 openid 自动拉取全量身份上下文）
● 使用方法：bash /opt/idaas/tools/debug-wechat.sh --openid oABC123...
```

### 紧急场景止血与恢复手册：

*   **JWT密钥不一致应急**：执行`kubectl exec -it ciamauth-deployment-xxx -- bash -c "cd /etc/ciam/auth-service && cp jwt-key-old.pem jwt-key.pem && kill -HUP 1"`，强制重载密钥（5秒内生效）。
    
*   **证书过期一键续签**：私有化环境运行`/opt/ciam/orchestrator/bin/renew-cert.sh --force`，自动调用cert-manager签发新证书并滚动更新所有Ingress。
    
*   **User Directory DB主从延迟>300s**：立即执行`kubectl exec -it userdir-mysql-master -- mysql -e "STOP SLAVE; START SLAVE;"`，手动触发复制重连；若无效，切换至备用从库（需提前配置VIP）。
    
*   **认证风暴止血**：当突发流量导致认证中心 CPU > 90% 且延迟飙升时，执行 `idaas-authn-emergency-throttle.sh --rate 500` 限流至 500 QPS，同时启用缓存策略（`--cache-ttl 300`），5 分钟后自动恢复。
    
*   **身份数据污染回滚**：若误操作导致大量用户 primary_id 错误，立即执行 `idaas-idrepo-rollback.sh --batch-id xxx --restore-to 20240520T100000Z` 回滚至指定快照（需提前开启自动快照）。
    
*   **合规策略误关闭应急**：通过 OpsApi 调用 `POST /api/v1/compliance/enable?policy=gdpr_data_residency` 一键重开关键合规开关。

### 横向研发文档：

*   接入指引：[[IDaaS CIAM 接入指南|CIAM接入指南]]（含前端SDK初始化、后端Token校验代码示例、Webhook事件订阅）；亦支持通用登录页嵌入，文档详见 [[IDaaS/CIAM/接入文档|IDaaS CIAM 接入文档]]；完整覆盖 Web/App/小程序/后台服务四类接入模式。
    
*   产品对接方案细节：[[IDaaS CIAM 与电商中台对接方案|电商中台对接方案]]（SCIM同步字段映射表、订单中心Token透传规范）；[[IDaaS/与CRM系统对接方案|与CRM系统对接方案]]、[[IDaaS/与营销平台对接方案|与营销平台对接方案]]；适用于新 App/小程序/H5 上线、用户体验优化、快速市场验证等典型场景；对接范围覆盖注册、登录、账号管理、MFA、实名认证、社交登录、条款签署等全身份生命周期能力。
    
*   产品对接范围等：明确CIAM仅提供身份层能力，不参与业务逻辑（如优惠券发放、库存扣减）；客户需自行完成应用端调用开发与后端 Token 校验逻辑；详见 [[IDaaS CIAM 能力边界说明书|CIAM能力边界]]；IDaaS 不接管 CRM 中的交易数据、不修改营销平台的用户标签逻辑，仅提供标准化身份 ID 与合规授权令牌。

## 产品对内文档：

### 完整架构图：

*   系统采用分层架构：  
    ▪ 接入层：ALB/SLB + CIAM API Gateway（Kong）  
    ▪ 服务层：Auth Service（Spring Boot）、User Directory（ShardingSphere-JDBC + MySQL Cluster）、SSP Portal（Vue3 + Nginx）  
    ▪ 数据层：MySQL分片集群（8节点）、Redis Cluster（6节点）、OSS（存储头像/证件照）  
    ▪ 运维层：ARMS（JVM/DB/Redis全指标）、SLS（结构化日志）、Tianji（主机健康度）  
    ▪ 安全层：KMS（密钥管理）、SGX可信执行环境（敏感计算如生物特征比对）  
    ▪ 已知问题：Auth Service在极端高并发下（>10万QPS）偶发JWT签发延迟（已规划2024Q3迁移至eBPF加速模块）。
    
*   系统采用「控制面-数据面-接入面」三层解耦设计：  
    - 控制面：策略引擎、门户管理后台、合规工作台（Java Spring Cloud）  
    - 数据面：身份目录（基于 Elasticsearch + MySQL 双写）、审计中心（Kafka + Flink + HBase）  
    - 接入面：认证中心（Go 微服务）、应用网关（Envoy Proxy + WASM 插件）  
    *已知问题：Elasticsearch 在亿级 identity 数据下聚合查询性能下降，已通过 [[IDaaS/性能优化#ES冷热分离|ES冷热分离]] 方案缓解*

### 业务逻辑时序图

*   用户使用：  
    `用户打开App → App调用CIAM SDK获取Authorization Code → 重定向至CIAM SSP Portal登录页 → 用户输入手机号+验证码 → Portal调用Auth Service生成Code → App用Code向API Gateway换取ID Token → App解析Token获取user_id → 向业务系统发起带Bearer Token的请求`

*   工作流流转：  
    `注册请求 → API Gateway校验频率 → Auth Service生成临时Session → SSP Portal渲染注册表单 → 用户提交 → Auth Service调用短信服务 → User Directory写入基础信息 → 触发Webhook通知会员中心 → 返回Success`

*   用户注册全流程时序图见 [[IDaaS/注册时序图|注册时序图]]（含手机号验证、实名核验、营销 consent 收集、OneID 生成）
    
*   跨渠道登录归并时序图见 [[IDaaS/登录归并时序图|登录归并时序图]]（含设备指纹采集、概率匹配、人工审核介入点）

### 代码仓库

*   基线仓库：`code.alibaba-inc.com/idass/ciam-base`（含公共依赖、配置中心SDK）  
    `idaas-ciam-base`（公共组件、基础 SDK）
    
*   代码仓库：  
    ▪ `code.alibaba-inc.com/idass/ciam-auth`（Auth Service）  
    ▪ `code.alibaba-inc.com/idass/ciam-userdir`（User Directory）  
    ▪ `code.alibaba-inc.com/idass/ciam-ssp`（Self-Service Portal）  
    - `idaas-authn-center`（认证中心）  
    - `idaas-id-repo`（身份目录）  
    - `idaas-portal-fe`（消费者门户前端）  
    - `idaas-policy-engine`（策略引擎）  
    
*   制品仓库：`acr.aliyuncs.com/idass/ciam-auth:v2.5.3`（Docker镜像）  
    `maven.aliyun.com/repository/public/com/alibaba/idass/ciam-sdk-java/2.5.3`（Maven包）  
    `aliyun-idaas-maven`（Maven）、`aliyun-idaas-docker`（Docker Hub 镜像）
    
*   关联依赖仓库：  
    ▪ `code.alibaba-inc.com/middleware/shardingsphere-jdbc`（分库分表中间件）  
    ▪ `code.alibaba-inc.com/security/kms-client`（密钥管理SDK）  
    `aliyun-idaas-sdk-java`、`aliyun-idaas-sdk-js`、`aliyun-idaas-openapi-spec`

### 数据表结构

*   `t_user_base`（用户主表）：  
    `id BIGINT PK`, `tenant_id VARCHAR(64) NOT NULL`, `user_id VARCHAR(128) UNIQUE NOT NULL`, `mobile VARCHAR(20)`, `email VARCHAR(128)`, `status TINYINT DEFAULT 1 COMMENT '0-禁用,1-启用,2-待激活'`, `created_at DATETIME`, `updated_at DATETIME`

*   `t_auth_event_log`（认证事件审计表）：  
    `id BIGINT PK`, `event_id VARCHAR(64) UNIQUE`, `tenant_id VARCHAR(64)`, `user_id VARCHAR(128)`, `event_type ENUM('login','logout','register','pwd_reset')`, `result ENUM('success','failed')`, `risk_level TINYINT COMMENT '0-低危,1-中危,2-高危'`, `ip VARCHAR(45)`, `ua TEXT`, `created_at DATETIME`

*   `id_repo.users`：  
    `id` BIGINT PK, `primary_id` VARCHAR(64) UNIQUE NOT NULL, `status` ENUM('active','locked','deleted'), `created_at` DATETIME, `last_login_at` DATETIME, `updated_at` DATETIME  

*   `id_repo.identities`：  
    `id` BIGINT PK, `primary_id` VARCHAR(64) FK, `identity_type` ENUM('phone','wechat','alipay','email'), `credential_hash` VARCHAR(255), `linked_at` DATETIME, `is_primary` BOOLEAN  

*   `policy_engine.policies`：  
    `id` VARCHAR(64) PK, `name` VARCHAR(128), `definition` JSON, `enabled` BOOLEAN, `priority` INT, `updated_at` DATETIME  

*   `audit_center.events`：  
    `id` VARCHAR(64) PK, `event_type` VARCHAR(64), `tenant_id` VARCHAR(64), `user_id` VARCHAR(64), `ip` VARCHAR(45), `ua` TEXT, `timestamp` DATETIME, `details` JSON