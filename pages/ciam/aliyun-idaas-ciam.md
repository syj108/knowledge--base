# 阿里云IDaaS CIAM

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   阿里云IDaaS CIAM（Customer Identity & Access Management）是面向消费者（C端）用户的客户身份与访问管理解决方案，聚焦于外部用户（如会员、买家、学员、患者等）的统一身份治理、安全认证、自助服务与跨平台体验一致性，区别于面向员工/内部人员的EIAM（Employee IAM）。
        
    *   作为IDaaS（Identity-as-a-Service）体系下的垂直子产品，CIAM自2023年起独立演进，强化对高并发注册登录、本土化合规（如《个人信息保护法》PIPL）、多终端一致体验、私有化交付等C端核心诉求的支持；2024年上线专属云版本，支持资源独享与深度客制化；2025年增强账号生命周期全链路审计与第三方身份源（微信/支付宝/银联）融合能力。
        
    *   能力覆盖核心产品IDaaS平台，关键组件包括：认证网关（Auth Gateway）、用户目录服务（User Directory）、自助门户（Self-Service Portal）、策略引擎（Policy Engine）、审计日志中心（Audit Log Center）及私有化部署套件（CIAM On-Prem Kit）。

*   对外介绍架构图
    
    *   **部署架构**：支持三种形态——公共云共享版（多租户SaaS）、公共云专属版（单租户VPC内独占资源）、私有化部署版（K8s集群或物理机离线部署）。所有形态均采用微服务架构，核心组件以容器化方式部署，支持水平扩缩容。
        
    *   **数据流向**：终端应用（Web/App/小程序）→ 认证网关（JWT/OAuth2.0/OpenID Connect协议接入）→ 用户目录（读写用户主数据）↔ 策略引擎（实时鉴权决策）→ 审计日志中心（全链路操作留痕）；自助门户通过API与用户目录、策略引擎双向交互。
        
    *   **上下游依赖**：
        - 依赖阿里云基础服务：VPC（网络隔离）、SLB（负载均衡）、OSS（静态资源托管）、SLS（日志采集）、ARMS（应用监控）；
        - 对接上游系统：Tianji（资源纳管与计量）、OpsApi（运维指令下发）；
        - 对接下游系统：客户业务系统（通过标准RESTful API或SDK集成）、短信/邮件服务商（验证码通道）、微信/支付宝开放平台（联合登录）。
        
    *   参考：  
        ![IDaaS CIAM 架构示意图](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/54Lq35oXpkvg1l7E/img/1213e0b1-d245-4023-8f12-1a8fa1c09600.png)

*   各核心组件能力详细说明
    
    *   **认证网关（Auth Gateway）**：提供标准化OAuth2.0 / OIDC / SAML协议接入能力，支持无感续签、设备指纹绑定、风险登录识别（IP/UA/行为模型），QPS峰值≥50,000。
        
    *   **用户目录服务（User Directory）**：基于分布式图数据库构建，支持亿级用户存储、毫秒级查询；内置标准属性模型（姓名、手机号、身份证、微信OpenID等）及扩展Schema机制；支持SCIM协议同步。
        
    *   **自助门户（Self-Service Portal）**：白标可定制H5页面，提供注册/登录/密码重置/多因子绑定/隐私设置/账号注销等全流程自助能力，符合GDPR/PIPL注销要求。
        
    *   **策略引擎（Policy Engine）**：声明式策略语言（ALP）驱动，支持基于角色、属性（ABAC）、上下文（时间/IP/设备）的动态权限判定，策略热更新不重启。
        
    *   **审计日志中心（Audit Log Center）**：全量记录用户操作、管理员操作、系统事件，保留周期≥180天，支持SLS对接与EOCC联动告警。
        
    *   **CIAM On-Prem Kit**：私有化交付包，含K8s Helm Chart、离线镜像仓库、国产化适配清单（麒麟OS/统信UOS/海光/鲲鹏）、等保三级加固配置模板。

*   与阿里云其他产品的关系
    
    *   与 VPC：CIAM专属版/私有化版必须部署在客户指定VPC内，依赖VPC网络策略实现南北向访问控制与东西向服务发现；VPC路由异常将导致终端无法访问认证网关。
        
    *   与 ECS：所有组件默认以ECS实例为运行载体（云上）或K8s Node（私有化）；ECS宕机将触发Pod自动漂移，但若未配置多可用区，则存在单点风险。
        
    *   与 SLB：认证网关前端强依赖SLB做七层负载；SLB监听配置错误或健康检查失败将直接导致“502 Bad Gateway”类用户侧故障。
        
    *   影响边界：
        - ✅ 会造成：终端用户无法注册/登录/修改密码；自助门户页面空白或报错；审计日志中断；策略变更不生效。
        - ❌ 不会造成：不影响ECS上运行的客户业务系统自身逻辑；不干扰RDS中业务库数据；不触发Tianji对非CIAM资源的计量偏差；不导致OSS中客户静态资源不可访问。

## QA（高频问答）：

*   Q：CIAM是否支持微信/支付宝一键登录？  
    A：支持。通过预置的社交身份源（Social Identity Provider）模块，开箱即用接入微信开放平台、支付宝开放平台，支持UnionID/OpenID映射与属性同步，需客户自行完成三方平台资质认证。

*   Q：私有化版本能否满足等保三级要求？  
    A：可以。CIAM On-Prem Kit提供完整等保三级合规基线包，含操作系统加固、数据库审计、日志留存、双因素登录、传输加密（TLS1.2+）、敏感信息脱敏等能力，并附第三方测评机构（如赛宝）适配报告。

*   Q：用户量超千万后性能是否下降？  
    A：不会。用户目录采用分片+读写分离架构，实测单集群支撑5000万用户、峰值10万QPS无延迟劣化；超大规模场景建议启用多活部署模式（需专属版或私有化高级许可）。

*   Q：能否与企业现有AD/LDAP打通？  
    A：可以。通过「企业身份源（Enterprise IdP）」模块，支持LDAPv3/SAML2.0协议对接，实现员工账号复用为C端会员（B2E2C模式），支持属性映射与自动同步。

*   Q：注销账号后，数据是否真正删除？  
    A：是。遵循PIPL“删除权”要求，执行注销操作后，用户主数据、设备绑定、会话Token、审计日志（脱敏后）均被逻辑清除；物理删除在72小时内完成，全程可审计，支持导出《注销执行凭证》。

## 组件接口人、研发负责人等角色信息：

| 组件 | 研发负责人 | L1可操作范围 | L2可操作范围 | 必须升级产研场景 |
|------|------------|----------------|----------------|---------------------|
| 认证网关 | 张伟（IDaaS-CIAM后端组） | 查看Pod状态、重启容器、切换灰度流量比例 | 修改OIDC Issuer配置、调整风控规则阈值、回滚网关版本 | 协议兼容性变更（如新增SAML SP元数据解析）、核心算法升级（如设备指纹模型迭代） |
| 用户目录 | 李婷（IDaaS-DataPlane组） | 查询用户基础属性、触发单用户同步、清理测试账号 | 扩容分片节点、调整索引策略、执行全量重建 | Schema结构变更、跨集群数据迁移、图数据库底层引擎升级 |
| 自助门户 | 王磊（IDaaS-FE组） | 更新前端静态资源（JS/CSS）、切换白标Logo/文案 | 配置多语言包、启用/禁用某项自助功能开关（如MFA绑定） | 前端框架升级（React 18→19）、核心流程重构（如注册页字段逻辑重写） |
| 策略引擎 | 陈默（IDaaS-AuthZ组） | 启停策略服务、导入导出ALP策略包、查看策略命中日志 | 编辑策略条件表达式、配置上下文变量来源（如从SLS注入） | 策略语言语法扩展、与新风控系统（如阿里云RiskCenter）深度集成 |
| CIAM On-Prem Kit | 刘洋（IDaaS-Deployment组） | 执行Helm upgrade、校验离线包完整性、生成部署报告 | 修改K8s资源配置（CPU/Mem）、适配国产OS内核参数 | 国产芯片指令集适配（如申威SW64）、等保三级测评问题闭环 |

## 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
|--------|------|-----------|------------|-------------|--------|----------------|----------------|
| AuthGateway_5xx_Rate_High | P0 | Prometheus指标 `rate(nginx_http_requests_total{job="auth-gw",status=~"5.."}[5m]) / rate(nginx_http_requests_total{job="auth-gw"}[5m]) > 0.05` | 认证网关 | 5xx错误率持续5分钟＞5% | 网关层异常（上游服务不可达/证书过期/配置错误），用户登录大面积失败 | [[CIAM/应急手册/网关5xx突增处置|网关5xx突增处置]] | 是（EOCC-2025-CIAM-001） |
| UserDirectory_Latency_Above_500ms | P1 | ARMS链路追踪 `user-directory.readUser` P99 > 500ms | 用户目录 | 单次用户查询P99延迟超500ms持续10分钟 | 目录服务响应迟缓，影响登录速度与自助操作流畅度 | [[CIAM/应急手册/目录延迟升高处置|目录延迟升高处置]] | 是（KB-IDAAS-CIAM-087） |
| SelfService_Portal_404 | P2 | SLS日志 `http_status: 404 AND service: "ssp"` | 自助门户 | 连续10分钟出现>100次404请求 | 前端资源路径错误或CDN缓存失效，导致部分页面无法加载 | [[CIAM/应急手册/门户404处置|门户404处置]] | 是（KB-IDAAS-CIAM-112） |
| AuditLog_Sink_Failure | P2 | Kafka消费组lag > 10000 | 审计日志中心 | 日志投递至SLS/OSS失败且积压超1万条 | 审计能力降级，合规审计可能缺失近期操作记录 | [[CIAM/应急手册/审计日志中断处置|审计日志中断处置]] | 是（EOCC-2025-CIAM-007） |
| PolicyEngine_Rule_Cycle_Detected | P3 | 策略引擎健康检查日志含 `cycle detected in rule dependency` | 策略引擎 | 策略间存在循环引用（A→B→A） | 策略加载失败，部分权限判定返回默认拒绝 | [[CIAM/应急手册/策略循环引用处置|策略循环引用处置]] | 是（KB-IDAAS-CIAM-045） |

---

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    - **PostgreSQL（策略元数据库）**：  
      `policy_rule`：存储ALP策略文本、版本、生效时间；  
      `policy_binding`：绑定策略与应用/用户组的关系；  
      `context_source`：上下文变量定义（如SLS日志源配置）。  
    - **Neo4j（用户目录图库）**：  
      `:User`节点：存储用户基础属性；  
      `:Identity`节点：存储各身份源（微信/手机号/邮箱）凭证；  
      `(:User)-[:HAS_IDENTITY]->(:Identity)`关系：标识用户身份关联。

*   关键日志路径，组件，内容，轮转策略  
    - `/var/log/ciam/auth-gateway/access.log`（认证网关）：Nginx格式访问日志，含请求路径、状态码、耗时；logrotate每日切割，保留7天。  
    - `/opt/idass-ciam/userdir/logs/userdir-app.log`（用户目录）：Spring Boot应用日志，含JDBC执行、缓存命中率；logback配置，按大小滚动（100MB/个），保留30个文件。  
    - `/data/ciam/audit/raw/`（审计日志中心）：原始JSON日志，按小时分区（`year=2025/month=05/day=20/hour=14/`）；通过Flink实时写入SLS，本地保留24小时。

*   问题排查SOP，通用场景的排查思路和路径  
    1. **用户反馈“登录失败”** → 查SLS中`auth-gateway` access日志筛选`status:500` → 若大量500，查`auth-gateway` error日志定位上游异常（如UserDirectory超时）→ 检查UserDirectory Pod状态与ARMS延迟指标 → 如延迟高，查Neo4j连接池与慢查询。  
    2. **自助门户白屏** → 查浏览器控制台JS错误 → 查SLS中`ssp`服务日志确认404资源路径 → 登录Portal Pod验证`/usr/share/nginx/html/`下文件完整性 → 检查CDN缓存配置或Helm values.yaml中`frontend.assetPath`。  
    3. **策略不生效** → 查`policy-engine`日志确认策略加载成功 → 在ARMS中追踪一次登录请求，观察`policy_decision` span是否返回`allow:true` → 如返回`deny`，检查`policy_rule`表中对应策略条件是否匹配当前上下文。

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    - 升级指南：[[CIAM/运维/版本升级指南|CIAM版本升级指南]]（含灰度发布checklist、回滚步骤、兼容性矩阵）  
    - 巡检手册：[[CIAM/运维/日常巡检SOP|CIAM日常巡检SOP]]（每日执行：网关QPS/错误率、目录P99延迟、审计日志投递lag、策略引擎健康探针）  
    - Aone项目：`IDAAS-CIAM-PROD`（生产环境）、`IDAAS-CIAM-ONPREM`（私有化交付）

### 典型问题排查解决方案

```yaml
一、问题描述
● 问题现象：用户点击微信登录按钮后跳转至空白页，控制台报错“Failed to fetch openid”
● 适用范围：所有接入微信开放平台的CIAM云上版本（v2.3.0+），专属版/私有化版同理
二、排查信息收集
● 必须收集的信息：用户微信OpenID（前端调试获取）、CIAM实例ID（如idaas-ciam-prod-shanghai）、微信AppID、时间戳（精确到秒）
● 检查终态的方法：登录auth-gateway Pod，执行 `curl -v "http://localhost:8080/wechat/callback?code=xxx&state=yyy"`
● 排查问题步骤：
  - 步骤1：确认微信开放平台配置的redirect_uri与CIAM控制台中填写的完全一致（含末尾/、协议、端口）
  - 步骤2：检查auth-gateway日志中是否有`wechat.callback.error`关键字，常见为`invalid code`（code已使用/过期）或`invalid appid`（AppID与密钥不匹配）
  - 步骤3：在Postman中模拟调用微信token接口，验证`appid`/`secret`/`code`三元组有效性
三、解决步骤
 场景一：redirect_uri不一致
 - 适用条件：日志显示`redirect_uri_mismatch`
 - 实施步骤：进入CIAM控制台 → 【社交登录】→ 【微信】→ 修改“回调地址”为微信平台备案的完整URL → 保存并等待3分钟生效
 - 结果验证：重新触发微信登录，成功跳转至首页
 场景二：AppID/Secret错误
 - 适用条件：调用微信token接口返回`{"errcode":40013,"errmsg":"invalid appid"}`
 - 实施步骤：登录微信开放平台 → 【开发管理】→ 【基本配置】→ 复制正确的AppID与AppSecret → CIAM控制台【社交登录】→ 【微信】→ 更新密钥 → 保存
 - 结果验证：重新触发登录，获取有效OpenID并完成绑定
四、非本产品排查
● 明确标注：若微信开放平台侧未授权该公众号/小程序，或未开通“网页授权”接口权限，需客户联系微信平台处理，IDaaS侧无干预能力
五、快速定位工具
● 脚本位置：`/opt/idass-ciam/tools/wechat-debug.sh`（位于auth-gateway Pod内）
● 使用方法：`bash /opt/idass-ciam/tools/wechat-debug.sh -c <code> -a <appid> -s <secret>`，自动输出完整调用链与错误定位
```

### 紧急场景止血与恢复手册：

*   **全站登录不可用（P0）**：立即执行 `kubectl scale deploy auth-gateway --replicas=0 -n idaas-ciam && sleep 30 && kubectl scale deploy auth-gateway --replicas=3 -n idaas-ciam` 强制滚动重启网关，规避内存泄漏或连接池耗尽；同步检查SLB健康检查路径是否被误改。
    
*   **用户数据误删（P1）**：私有化环境立即停止所有写操作；从最近一次`neo4j-admin dump`备份（每日02:00自动执行）恢复；云上环境联系IDaaS SRE团队发起`Point-in-Time Recovery`申请（RTO<2h）。
    
*   **审计日志中断超24h（P2）**：临时启用本地文件日志兜底（修改`audit-sink` ConfigMap中`mode: file`），待SLS链路修复后，通过`flink-sql`补录积压数据。

### 横向研发文档：

*   接入指引：[[CIAM/开发者/快速接入指南|CIAM快速接入指南]]（含SDK下载、Demo工程、Postman Collection）
    
*   产品对接方案细节：[[CIAM/对接/与电商中台对接方案|电商中台对接方案]]、[[CIAM/对接/与教育SaaS对接方案|教育SaaS对接方案]]
    
*   产品对接范围：明确CIAM仅提供身份层能力（认证/授权/账号管理），不涉及订单、支付、课程等业务逻辑；所有业务属性需由客户系统通过`/api/v1/users/{uid}/attributes` API扩展写入。

---

## 产品对内文档：

### 完整架构图：

*   系统采用“控制平面（Control Plane）+ 数据平面（Data Plane）”分离设计：  
    - 控制平面：含管理控制台（React）、策略编排服务（Go）、配置中心（Nacos）、审计聚合器（Flink）；  
    - 数据平面：含认证网关（Nginx+Lua）、用户目录（Neo4j+Redis缓存）、自助门户（Nginx静态服务）；  
    - 调用关系：控制台通过OpsApi调用配置中心下发策略 → 策略编排服务监听变更并推送到网关/目录 → 网关实时调用目录与策略引擎完成认证决策；  
    - 已知问题：Neo4j在超大规模图遍历场景下存在GC压力，已规划2025 Q3迁移至自研分布式图引擎GraphX。

### 业务逻辑时序图

*   用户使用：  
    `用户打开APP → SDK初始化 → 调用`/authorize`获取code → 重定向至CIAM认证页 → 输入手机号+验证码 → CIAM调用`/login` → 目录查用户 → 策略引擎鉴权 → 返回ID Token → SDK解码并存储 → 后续请求携带Token访问业务API`  

*   工作流流转：  
    `管理员在控制台创建新策略 → 策略服务持久化至PostgreSQL → 发布事件至Kafka → 网关/目录消费者拉取并热加载 → 新策略即时生效（<1s）`

### 代码仓库

*   基线仓库：`idaas-ciam-controlplane`（GitLab: https://gitlab.alibaba-inc.com/idaas/idaas-ciam-controlplane）  
*   代码仓库：`idaas-ciam-dataplane`（含auth-gateway/userdir/ssp）  
*   制品仓库：`idaas-ciam-helm-charts`（Helm Chart发布）、`idaas-ciam-docker-images`（镜像仓库）  
*   关联依赖仓库：`idaas-common-sdk-java`（Java SDK）、`idaas-openapi-spec`（OpenAPI 3.0规范）

### 数据表结构

*   **`policy_rule`（策略规则表）**  
  `id` BIGSERIAL PK  
  `name` VARCHAR(128) NOT NULL（策略名称）  
  `content` TEXT NOT NULL（ALP策略文本）  
  `version` INTEGER DEFAULT 1（版本号，每次更新+1）  
  `status` VARCHAR(20) CHECK IN ('active','inactive','draft')  
  `created_at` TIMESTAMPTZ DEFAULT NOW()  
  `updated_at` TIMESTAMPTZ DEFAULT NOW()

*   **`user_identity`（用户身份凭证表）**  
  `id` UUID PK  
  `user_id` VARCHAR(64) NOT NULL（关联`:User`节点ID）  
  `identity_type` VARCHAR(32) NOT NULL（'phone','wechat','email'）  
  `identity_value` VARCHAR(255) NOT NULL（脱敏存储，如138****1234）  
  `credential_hash` TEXT（凭证密文，如微信access_token加密）  
  `expires_at` TIMESTAMPTZ（过期时间）  
  `created_at` TIMESTAMPTZ DEFAULT NOW()