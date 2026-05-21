# IDaaS EIAM

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   IDaaS EIAM（Enterprise Identity Access Management）是阿里云提供的云原生身份及权限管理服务（Identity-as-a-Service），面向企业用户提供组织架构管理、账户全生命周期管控、单点登录（SSO）和跨账号体系桥接等一站式能力。
        
    *   当前为成熟商用版本，已深度集成阿里云VPC、RAM、CloudSSO、Resource Directory等核心云产品；暂无公开的多版本演进说明，以“云原生、开箱即用、免运维”为统一交付形态。
        
    *   能力覆盖产品层（IDaaS控制台、EIAM SSO门户）、组件层（身份认证中心、权限策略引擎、组织同步服务、应用接入网关）。

*   对外介绍架构图
    
    *   中心端部署于阿里云公共云Region内，采用多可用区高可用容器化部署（ACK集群），不涉及客户侧端侧部署；所有服务均通过HTTPS/API网关对外暴露能力。
        
    *   数据流向：用户终端 → 应用接入网关（处理SAML/OIDC协议） → 身份认证中心（校验凭证、发起MFA） → 组织同步服务（对接LDAP/AD/钉钉/企微等源） → 权限策略引擎（执行RBAC/ABAC策略决策） → 返回令牌至应用。
        
    *   与上下游系统依赖关系：
        *   **上游**：依赖阿里云账号体系（主账号/资源目录RD）、Tianji（用于部分底层资源调度监控）、OpsApi（接收运维事件上报）；
        *   **下游**：向云产品（如云效、DataWorks、ASCM）提供标准OIDC/SAML身份源；与RAM协同实现跨账号权限委派。
        
    *   参考：
        
    *   ![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/4maOgXbWdMDRmlWN/img/82c50c63-0138-4de4-bc53-3e37e4c22ef9.png)

*   各核心组件能力详细说明
    
    *   **身份认证中心（Authn Core）**：支持用户名密码、手机号、邮箱、OIDC/SAML联合身份、MFA（TOTP/短信/钉钉扫码）等多种认证方式；提供会话管理、风险登录识别（基础版）。
        
    *   **权限策略引擎（Authz Engine）**：基于RBAC模型实现角色-权限绑定，支持自定义策略语法（类RAM Policy）；可对接阿里云RAM进行细粒度云资源授权。
        
    *   **组织同步服务（Org Sync Service）**：支持双向/单向同步：从AD/LDAP/钉钉/企微/自建HR系统拉取组织架构与用户；支持手动维护组织树与用户属性。
        
    *   **应用接入网关（App Gateway）**：提供标准化SAML 2.0、OIDC 1.0协议适配能力；内置常见SaaS应用模板（如Jira、Confluence、泛微OA）；支持自定义应用接入配置。
        
    *   **SSO门户（Access Portal）**：面向终端用户的统一应用访问入口，支持个性化首页、快捷应用卡片、待办提醒（对接钉钉/邮件）。

*   与阿里云其他产品的关系
    
    *   与 **RAM**：EIAM不替代RAM，而是作为RAM的“身份源扩展”。可通过EIAM用户/角色映射到RAM角色，实现对云资源的集中授权；EIAM本身不直接管理云API权限，需经RAM中转。
        
    *   与 **VPC/SLB/ECS**：无直接依赖。若客户将自建应用部署在VPC内并接入EIAM SSO，则需确保该应用可访问公网或通过PrivateLink访问IDaaS服务端点；SLB/ECS仅为应用载体，不影响EIAM核心逻辑。
        
    *   与 **CloudSSO**：二者定位互补。CloudSSO聚焦“阿里云账号体系内多账号单点登录”，EIAM聚焦“企业自有身份体系对接云上多应用（含非阿里云SaaS）”。可共存，EIAM可作为CloudSSO的上游身份源。
        
    *   产品异常可能造成的影响：
        *   **会影响**：所有接入EIAM的Web应用SSO登录失败、组织架构变更无法同步至下游系统、新用户自助注册流程中断。
        *   **不会影响**：已登录用户的会话持续性（受令牌有效期保护）、RAM内已有授权策略的执行、云平台控制台（ASCM）本地账号登录、ECS/VPC等IaaS资源运行状态。

### QA（高频问答）：

*   Q：EIAM是否支持私有化部署？  
  A：不支持。EIAM为纯SaaS服务，仅提供公有云托管模式。

*   Q：能否用EIAM替代AD做域控？  
  A：不能。EIAM不提供Kerberos/LDAP Server服务，不可替代Windows域控；但可作为AD的上层身份聚合层，实现AD用户单点登录云应用。

*   Q：EIAM与钉钉/企微打通后，离职员工是否自动停权？  
  A：支持自动同步状态（需开启“组织变更自动触发禁用”开关），但权限回收时效依赖同步周期（默认5分钟），关键系统建议叠加人工复核流程。

*   Q：一个用户能同时属于多个组织单元（OU）吗？  
  A：不支持。EIAM采用单归属树形结构，用户仅能归属于一个叶子节点OU；多角色需求应通过“角色分配”而非组织嵌套实现。

### 组件接口人、研发负责人等角色信息：

*   当前未公开内部组织分工。对外统一支持入口为[[阿里云IDaaS帮助中心|IDaaS帮助中心]]与[[工单系统|提交工单]]；重大故障升级路径：L1值班 → L2 SRE → EIAM产研TL（钉钉群：`EIAM-Platform-Owner`）。
    
*   运维操作权限分级：
    *   **L1（一线客服/客户成功）**：可重置用户密码、强制登出会话、查看SSO接入状态、导出基础审计日志。
    *   **L2（阿里云SRE）**：可重启应用网关Pod、调整同步任务并发度、查询认证链路TraceID、触发紧急组织全量同步。
    *   **必须升级产研**：修改核心策略引擎规则语法、定制协议字段映射、新增LDAP Schema解析逻辑、数据库Schema变更。

### 告警/风险/异常汇总表

*   当前官方文档未披露具体告警列表；根据服务SLA与典型故障模式，梳理高优风险如下：

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `EIAM_SSO_Login_Failure_Rate_High` | P1 | Prometheus指标 `eiam_authn_login_failure_rate{job="eiam-authn"} > 0.15` 持续5min | Authn Core | 连续5分钟登录失败率超15% | 认证服务异常或上游身份源（AD/LDAP）不可达 | [[EIAM登录失败率突增应急手册|EIAM-P1-Login-Failure]] | 是 |
| `EIAM_OrgSync_Task_Failed` | P2 | 日志中连续出现 `org.sync.task.failed` ERROR且重试3次 | Org Sync Service | LDAP连接超时、证书过期、DN路径错误 | 组织架构无法同步，新用户/部门不可见 | [[EIAM组织同步失败排查指南|EIAM-P2-OrgSync]] | 是 |
| `EIAM_AppGateway_Unavailable` | P1 | HTTP探测 `/healthz` 返回非200 | App Gateway | Pod CrashLoopBackOff或Ingress路由异常 | 所有SAML/OIDC应用接入中断 | [[EIAM应用网关不可用止血方案|EIAM-P1-Gateway-Downtime]] | 是 |
| `EIAM_Portal_5xx_Rate_High` | P2 | ALB访问日志中 `status:5xx` 占比 >5% | SSO Portal | 前端静态资源CDN回源失败或后端API超时 | 用户无法打开SSO门户首页 | [[EIAM门户5xx问题定位|EIAM-P2-Portal-5xx]] | 否 |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
  EIAM为SaaS服务，**不开放客户直连数据库权限**；所有数据操作须通过OpenAPI或控制台完成。后台使用PolarDB集群，核心逻辑表包括：  
  `eiam_user`（用户主数据）、`eiam_org_unit`（组织单元）、`eiam_app`（接入应用元信息）、`eiam_policy`（权限策略）、`eiam_audit_log`（操作审计日志，保留180天）。

*   关键日志路径，组件，内容，轮转策略  
  客户不可见服务端日志路径；L2 SRE可观测日志流：  
  - `authn-core`：`/var/log/eiam/authn/*.log`，记录认证请求、MFA挑战、令牌签发；按日轮转，保留7天。  
  - `org-sync`：`/var/log/eiam/sync/*.log`，记录每次同步的起止时间、变更条目数、失败详情；按任务ID分文件，保留3天。  
  - 集群级：所有组件日志统一接入SLS，Project=`eiam-prod`，Logstore=`audit-trace`（含TraceID关联）。

*   问题排查SOP，通用场景的排查思路和路径  
  通用路径：  
  1. 确认现象范围（单用户/全量用户/某类应用）→  
  2. 查看控制台「监控大盘」中对应组件健康状态 →  
  3. 在「审计日志」中搜索关键词（如用户邮箱、应用ID、错误码）→  
  4. 若涉及协议交互，使用浏览器开发者工具抓包分析SAML Response/OIDC Token Claims →  
  5. 提交工单时必须附带：`TraceID`、`应用ClientID`、`用户PrincipalName`、复现时间戳。

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
  全量灰度升级，客户无感知；无客户侧升级操作。  
  巡检建议每日执行：  
  - 控制台「系统健康」页检查各组件状态灯；  
  - 「审计日志」筛选 `event_type=SYNC_ORG_SUCCESS` 确认最近1小时同步成功；  
  - 「应用管理」页验证TOP3关键应用的「测试登录」按钮返回成功。  
  相关Aone：[[Aone-EIAM-Release-Plan|EIAM发布计划]]（内部可见）

### 典型问题排查解决方案

```yaml
（针对“用户点击SSO应用跳转后提示‘Invalid SAML Response’”）
一、问题描述
● 问题现象：用户从EIAM门户点击某SAML应用，跳转至目标应用登录页后显示SAML校验失败（常见错误：Signature validation failed / Audience mismatch）
● 适用范围：所有SAML接入应用，云版本无差异，影响单个应用或全部SAML应用
二、排查信息收集
● 必须收集的信息：应用ClientID、用户邮箱、发生时间（精确到秒）、浏览器控制台Network标签下SAMLResponse原始Base64字符串
● 检查终态的方法：登录EIAM控制台 → 应用管理 → 选择该应用 → 查看「SAML配置」页的Issuer URL、Audience URI、签名证书有效期
● 排查问题步骤：
  - 检查SAMLResponse中`<saml:Audience>`值是否与EIAM配置的Audience URI完全一致（含末尾斜杠）
  - 使用在线SAML解码器解析Response，确认`<ds:Signature>`区块存在且未被截断
  - 核对目标应用端配置的IdP证书是否为EIAM当前启用证书（控制台「SAML配置」页「下载证书」）
三、解决步骤
 场景一：Audience URI不匹配
 - 适用条件：解码后Audience值为`https://example.com`，而EIAM配置为`https://example.com/`
 - 实施步骤：控制台编辑应用 → SAML配置 → 修改Audience URI为`https://example.com`（去除斜杠） → 保存
 - 结果验证：重新发起SSO，响应中Audience字段更新，目标应用成功登录
 场景二：证书过期或不匹配
 - 适用条件：目标应用使用了旧版证书，或EIAM证书已更新但未同步
 - 实施步骤：控制台 → 应用管理 → 选择应用 → 「SAML配置」→ 「下载证书」→ 替换目标应用端IdP证书 → 重启应用服务
 - 结果验证：SAMLResponse签名验证通过，登录成功
四、非本产品排查
● 明确标注：若解码后SAMLResponse中`<saml:Subject><saml:NameID>`为空，且EIAM用户详情页中该用户“用户名”字段为空，则问题根因为用户属性映射缺失，需检查「用户属性映射」配置（属EIAM配置范畴）；若NameID有值但目标应用拒绝，属目标应用SAML实现缺陷，需联系其技术支持。
五、快速定位工具
● 脚本位置：`aliyun-eiam-saml-decoder`（内部CLI工具，SRE可用）
● 使用方法：`eiam-saml decode --response "PHNhbWx..."`
```

### 紧急场景止血与恢复手册：

*   **场景：全部SSO登录失败（P1）**  
  止血动作（5分钟内）：  
  1. L2执行：`kubectl -n eiam-prod scale deploy/eiam-app-gateway --replicas=0 && sleep 10 && kubectl -n eiam-prod scale deploy/eiam-app-gateway --replicas=3`（强制滚动重启网关）；  
  2. 同步检查`eiam-authn` Pod就绪探针状态；  
  3. 若未恢复，立即切换至备用认证通道：在控制台启用「备用登录页」（静态HTML+基础密码认证，绕过MFA与组织校验）。  
  恢复验证：使用测试账号完成SAML/OIDC双协议登录。

*   **场景：组织架构同步中断超1小时（P2）**  
  止血动作：  
  1. L2执行紧急全量同步命令：`eiam-cli orgsync trigger --full --source ad-server-01`；  
  2. 检查同步日志末尾是否出现`FULL_SYNC_COMPLETED`；  
  3. 若失败，临时启用「手动组织维护」模式，在控制台直接编辑OU结构保障业务连续性。

### 横向研发文档：

*   接入指引  
  全流程文档：[[EIAM应用接入指南|EIAM-App-Onboarding]]，含SAML/OIDC/Webhook三种模式。

*   产品对接方案细节  
  - 与钉钉对接：使用钉钉ISV模式，EIAM作为「第三方企业应用」获取通讯录只读权限；支持增量同步（通过钉钉回调事件）与全量拉取（定时Job）。  
  - 与企微对接：基于企微「通讯录同步API」，EIAM作为可信应用调用`/cgi-bin/user/simplelist`等接口；需提前在企微管理后台配置IP白名单与AgentID。  
  - 与自建LDAP对接：支持LDAP v3，要求开启TLS（LDAPS端口636），支持Bind DN + 密码认证，Base DN需指定为组织根节点。

*   产品对接范围等  
  EIAM明确**不负责**：  
  - 终端设备级认证（如Intune MDM设备信任链）；  
  - 数据库行级权限控制（需应用层或DMS实现）；  
  - 非HTTP协议应用接入（如SSH/RDP，需结合JumpServer等网关二次集成）。

## 产品对内文档：

### 完整架构图：

*   系统采用分层架构：  
  **接入层**（ALB + App Gateway）→ **协议适配层**（SAML/OIDC Adapter）→ **核心服务层**（Authn Core / Authz Engine / Org Sync）→ **数据层**（PolarDB + Redis缓存 + OSS附件存储）→ **集成层**（OpenAPI + EventBridge事件总线）。  
  调用关系严格遵循「前端只调后端API，后端服务间通过gRPC通信，跨域数据同步通过EventBridge解耦」原则。  
  已知问题：Org Sync在超大规模AD（>10万用户）下首次全量同步耗时较长（>2h），已优化为分块拉取+异步索引构建（v3.2.0+）。

### 业务逻辑时序图

*   用户使用  
  `用户访问SSO门户` → `门户调用Authn Core校验Session` → `若过期则重定向至登录页` → `用户输入凭证` → `Authn Core调用MFA服务` → `校验通过后生成OIDC ID Token` → `门户渲染应用卡片列表` → `用户点击应用` → `门户构造SAML AuthnRequest发送至App Gateway` → `Gateway签发SAML Response重定向至目标应用`。

*   工作流流转  
  `管理员在控制台创建应用` → `系统生成ClientID/Secret & SAML元数据` → `触发EventBridge事件` → `App Gateway加载新应用配置` → `同步至Redis缓存` → `下次请求实时生效`。

### 代码仓库

*   基线仓库：`idaas-eiam-platform`（主干分支`main`，语义化版本`v3.x`）
*   代码仓库：`idaas-eiam-authn` / `idaas-eiam-authz` / `idaas-eiam-sync` / `idaas-eiam-portal`
*   制品仓库：ACR企业版实例 `eiam-prod-registry.cn-zhangjiakou.cr.aliyuncs.com`
*   关联依赖仓库：`aliyun-openapi-java-sdk`（OpenAPI生成）、`aliyun-cloud-sso-sdk`（与CloudSSO互通模块）

### 数据表结构

*   `eiam_user`：`id`(PK), `username`, `email`, `mobile`, `status`(ENABLED/DISABLED), `org_unit_id`, `created_at`, `updated_at`
*   `eiam_org_unit`：`id`(PK), `name`, `parent_id`, `path`(e.g. `/root/dept-a/team-b`), `type`(DEPT/TEAM)
*   `eiam_app`：`id`(PK), `name`, `protocol`(SAML/OIDC), `client_id`, `issuer_url`, `audience_uri`, `cert_pem`, `status`
*   `eiam_policy`：`id`(PK), `name`, `effect`(ALLOW/DENY), `resource`, `action`, `condition_json`, `applied_to`（user/role/org）
*   `eiam_audit_log`：`id`(PK), `event_type`, `principal`, `resource_id`, `status`(SUCCESS/FAILED), `ip`, `user_agent`, `created_at`