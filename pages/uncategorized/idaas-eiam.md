# IDaaS EIAM

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   [[identity/idaas-eiam|IDaaS EIAM]]（Enterprise Identity Access Management）是阿里云IDaaS体系下的核心云原生身份及权限管理服务，面向企业用户提供**组织架构统一纳管、账户全生命周期管控、单点登录（SSO）集成、细粒度应用级权限控制**的一站式能力。
        
    *   当前为成熟商用版本（v1.x），已全面支持多云/混合云部署形态，持续增强与阿里云原生产品（如RAM、CloudSSO、Resource Directory、Tianji）的深度协同；后续演进聚焦AI驱动的权限智能推荐、动态策略引擎与零信任访问网关融合。
        
    *   能力覆盖产品主体（EIAM控制台、EIAM SSO网关、EIAM Identity Engine）及关键组件（组织同步服务、SCIM适配器、SAML/OIDC协议网关、权限策略引擎）。

*   对外介绍架构图
    
    *   中心端采用多可用区高可用容器化部署（ACK集群），核心组件以StatefulSet+Service形式运行；端侧通过轻量Agent或标准协议（SAML/OIDC/LDAP）对接企业本地AD/LDAP、自建应用、SaaS应用及阿里云云产品。
        
    *   数据流向：用户请求 → SSO网关（鉴权路由）→ Identity Engine（身份认证/属性解析）→ 组织服务/策略引擎（权限决策）→ 应用后端（携带授权上下文）；同步流：AD/LDAP/HR系统 ↔ SCIM Adapter ↔ 组织同步服务 ↔ EIAM主库。
        
    *   与上下游系统依赖关系：
        *   **Tianji**：作为底层资源调度与元数据底座，提供集群纳管、配置分发、健康巡检能力；
        *   **OpsApi**：对接运维事件中心，上报审计日志、告警与变更事件；
        *   **CloudSSO / RAM**：双向打通——EIAM可作为企业主身份源向RAM同步角色绑定关系，也可消费RAM角色策略实现跨账号精细授权；
        *   **Resource Directory**：支持基于RD组织单元（OU）自动映射EIAM组织架构，实现“组织即权限边界”。
        
    *   参考：[[IDaaS/EIAM/架构图|EIAM对外架构示意图]]

*   各核心组件能力详细说明
    
    *   **SSO网关**：支持SAML 2.0 / OIDC / CAS协议，提供应用接入模板、自定义断言映射、会话管理、MFA联动能力。
        
    *   **Identity Engine**：统一身份认证中心，支持密码、短信、邮箱、WebAuthn、第三方OAuth等多种认证方式，内置风险识别模块（异常登录检测）。
        
    *   **组织同步服务**：通过SCIM v2.0标准协议对接HR系统、AD/LDAP、钉钉/企微等，支持增量/全量同步、冲突自动处理、字段映射配置。
        
    *   **权限策略引擎**：基于ABAC模型，支持按用户属性、应用属性、环境属性（时间/IP/设备）动态生成访问策略；提供可视化策略编辑器与RBAC兼容模式。
        
    *   **应用门户（MyApps）**：面向终端用户的聚合访问入口，支持个性化应用推荐、快捷单点跳转、自助密码重置、MFA绑定等自服务能力。

*   与阿里云其他产品的关系
    
    *   与 **VPC**：SSO网关需部署于指定VPC内，支持PrivateLink私网接入；不直接操作VPC资源，但依赖VPC网络连通性保障应用通信。
        
    *   与 **ECS**：无直接依赖；若客户将自建应用部署于ECS，需确保ECS安全组放行SSO回调地址；EIAM不管理ECS实例生命周期。
        
    *   与 **SLB**：SSO网关默认由ALB（应用型负载均衡）提供七层流量分发；客户可自定义SLB接入，但需满足HTTPS/HTTP/Redirect协议兼容性要求。
        
    *   产品异常可能造成的影响：
        *   ✅ **会造成**：所有接入SSO的应用无法完成单点登录；新用户入职/离职流程中断；组织架构变更不同步；权限策略更新延迟生效。
        *   ❌ **不会造成**：已登录用户的会话持续有效（受会话TTL保护）；不影响ECS/RDS等IaaS资源运行；不导致RAM主账号权限变更或AK泄露；不中断Tianji集群基础运维能力。

### QA（高频问答）：

*   Q：EIAM是否支持替代企业AD？  
    A：支持桥接（AD作为上游源），也支持独立托管（EIAM作为主身份源）。建议中大型企业采用“EIAM主身份源 + AD只读同步”模式，兼顾安全与兼容。

*   Q：SSO失败时，如何快速判断是EIAM侧还是应用侧问题？  
    A：查看`/api/v1/sso/debug`调试接口返回码（4xx=应用配置错误，5xx=EIAM服务异常），并检查应用侧SAML元数据URL是否可达、证书是否过期。

*   Q：组织架构同步失败，常见原因有哪些？  
    A：SCIM Token失效、上游系统API限流、字段映射配置错误、网络策略阻断443端口、同步任务被手动暂停。

*   Q：权限策略不生效，应优先排查哪些环节？  
    A：① 用户是否归属目标组织单元；② 策略是否绑定至该组织/用户/应用；③ 策略条件是否匹配当前访问上下文（如IP段、时间窗）；④ 是否存在更高优先级策略覆盖。

### 组件接口人、研发负责人等角色信息：

*   **SSO网关**：研发负责人 `eiam-sso@aliyun.com`，L1可重启Pod、切换ALB后端权重；L2可修改协议配置、重载证书；策略变更/核心参数调优需升级产研。
    
*   **Identity Engine**：研发负责人 `eiam-auth@aliyun.com`，L1可查询认证日志、触发人工登出；L2可临时关闭MFA策略、重置风控状态；认证算法/密钥轮转需升级产研。
    
*   **组织同步服务**：研发负责人 `eiam-sync@aliyun.com`，L1可手动触发同步任务、查看同步日志；L2可调整同步频率、修复字段映射；上游API凭证重置需升级产研。
    
*   **权限策略引擎**：研发负责人 `eiam-policy@aliyun.com`，L1可导出策略快照、查看评估结果；L2可启用/禁用策略、调整优先级；策略语法校验失败需升级产研。

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `SSO_Gateway_5xx_Rate_High` | P1 | Prometheus指标 `eiam_sso_http_server_requests_total{code=~"5.."}` 5分钟均值 > 5% | SSO网关 | 连续5分钟HTTP 5xx响应占比超阈值 | 网关内部错误或下游服务不可用 | [[IDaaS/EIAM/应急手册#SSO网关5xx突增|SSO网关5xx突增处置]] | ✅ |
| `Identity_Engine_Auth_Fail_Rate_High` | P1 | `eiam_auth_failed_total` 5分钟环比增长 >300% | Identity Engine | 认证失败率异常飙升 | 可能遭遇暴力破解、风控规则误杀或认证源故障 | [[IDaaS/EIAM/应急手册#认证失败率突增|认证失败率突增处置]] | ✅ |
| `Org_Sync_Task_Failed` | P2 | 同步任务状态为 `FAILED` 且持续 >10分钟 | 组织同步服务 | 单次同步任务执行失败 | 上游系统不可达、凭证失效或数据格式异常 | [[IDaaS/EIAM/应急手册#组织同步失败|组织同步失败处置]] | ✅ |
| `Policy_Evaluation_Timeout` | P2 | `eiam_policy_eval_duration_seconds` P99 > 2s | 权限策略引擎 | 策略评估耗时超长 | 策略数量过多、条件嵌套过深或属性解析超时 | [[IDaaS/EIAM/应急手册#策略评估超时|策略评估超时处置]] | ✅ |
| `MyApps_Portal_Unavailable` | P3 | HTTP探针 `/healthz` 返回非200 | 应用门户 | 门户服务不可访问 | 前端静态资源加载失败或后端API熔断 | [[IDaaS/EIAM/应急手册#门户不可用|门户不可用处置]] | ✅ |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    *   主库（MySQL 8.0，高可用集群）：  
        *   `eiam_org_unit`：组织单元树结构（id, parent_id, name, path）  
        *   `eiam_user`：用户主表（id, username, status, created_at）  
        *   `eiam_app`：接入应用元数据（id, app_key, protocol_type, callback_url）  
        *   `eiam_policy`：权限策略定义（id, effect, resource, condition_json）  
        *   `eiam_sync_task`：同步任务记录（task_id, source_type, status, last_exec_time）

*   关键日志路径，组件，内容，轮转策略  
    *   `SSO网关`：`/var/log/eiam/sso/access.log`（Nginx格式访问日志）、`error.log`（错误日志）；Logrotate每日切割，保留7天。  
    *   `Identity Engine`：`/var/log/eiam/auth/auth.log`（认证全流程trace）、`risk.log`（风控事件）；JSON格式，Filebeat采集至SLS；保留30天。  
    *   `组织同步服务`：`/var/log/eiam/sync/sync.log`（同步任务详情、字段映射日志）；按任务ID分割，保留15天。

*   问题排查SOP，通用场景的排查思路和路径  
    *   **SSO失败通用路径**：① 查看浏览器开发者工具Network标签页，确认SAML Response是否返回；② 检查EIAM控制台「应用管理」中该应用的配置状态与证书有效期；③ 登录SSO网关Pod，`curl -v https://<app-callback>` 验证连通性；④ 查询`eiam_sso_http_server_requests_total{app="<app_key>"}`指标定位失败阶段。  
    *   **用户无法登录门户**：① 检查`eiam_user`表中该用户`status=active`；② 查询`eiam_auth_failed_total{username="xxx"}`确认是否被风控锁定；③ 检查Identity Engine Pod内存使用率（>90%可能触发GC停顿）。

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    *   升级流程详见 [[IDaaS/EIAM/运维手册/版本升级]]；  
    *   日常巡检项见 [[IDaaS/EIAM/运维手册/巡检清单]]；  
    *   Aone发布单：`AONE-IDAAS-EIAM-RELEASE-*`

### 典型问题排查解决方案

```yaml
一、问题描述
● 问题现象：用户点击应用图标后跳转至EIAM登录页，输入凭据后页面空白或报错“Invalid SAML Response”
● 适用范围：所有SSO接入应用（SAML协议），云版本≥v1.8.0，影响单个或多个应用
二、排查信息收集
● 必须收集的信息：应用AppKey、用户UID、发生时间（精确到秒）、浏览器User-Agent
● 检查终态的方法：登录SSO网关Pod，执行 `kubectl exec -it <sso-pod> -- tail -n 50 /var/log/eiam/sso/error.log`
● 排查问题步骤：
  - 步骤1：在EIAM控制台「应用管理」→「调试模式」中开启该应用调试，复现问题
  - 步骤2：查看调试日志中`SAMLResponse` Base64解码后`<samlp:Status>`节点值
  - 步骤3：比对日志中`AssertionConsumerServiceURL`与应用实际配置的ACS URL是否完全一致（含末尾/）
  - 步骤4：检查EIAM侧SAML签名证书是否过期（控制台「设置」→「协议配置」）
三、解决步骤
 场景一：ACS URL不匹配
 - 适用条件：日志显示`AssertionConsumerServiceURL mismatch`
 - 实施步骤：进入应用编辑页 → 修改「断言消费者服务URL」为应用实际接收SAML的完整URL（含https://及路径）
 - 结果验证：重新发起SSO，成功跳转至应用首页
 场景二：签名证书过期
 - 适用条件：日志含`Signature validation failed`且证书到期日早于当前时间
 - 实施步骤：控制台「设置」→「协议配置」→「SAML签名证书」→ 点击「更新证书」生成新密钥对
 - 结果验证：下载新元数据XML，上传至应用侧IDP配置，SSO恢复正常
四、非本产品排查
● 若日志显示`Connection refused to https://<app-domain>/acs`：属应用侧Web服务不可用，需通知应用Owner检查其服务状态
五、快速定位工具
● 脚本位置：`/opt/eiam/tools/saml-debug.sh <app_key>`（SSO网关Pod内）
● 使用方法：自动提取最近10条该应用SAML交互日志并解码Response/Request
```

### 紧急场景止血与恢复手册：

*   **SSO全局不可用（P0）**：立即执行 `kubectl scale deploy eiam-sso-gateway --replicas=0 -n idaas` → 等待30秒 → `--replicas=3` 触发滚动重启；同步检查ALB后端健康检查状态。
    
*   **组织架构大规模丢失（P1）**：从备份库（每日全量+binlog）恢复`eiam_org_unit`/`eiam_user`表至最近可用快照；执行`eiam-sync-cli force-full-sync --source ad`强制全量重建。
    
*   **权限策略全部失效（P1）**：临时启用兜底策略（`effect: allow, resource: "*"`, 仅限管理员IP段），同时回滚策略引擎至前一稳定版本镜像。

### 横向研发文档：

*   接入指引：[[IDaaS/EIAM/开发者指南/应用接入]]
    
*   产品对接方案细节：[[IDaaS/EIAM/对接方案/RAM集成]]、[[IDaaS/EIAM/对接方案/CloudSSO互通]]
    
*   产品对接范围：支持SAML 2.0 / OIDC 1.0 / SCIM 2.0 / LDAP v3 标准协议；与阿里云全量Top30产品完成兼容性认证（详见[[IDaaS/EIAM/兼容性矩阵]]）

## 产品对内文档：

### 完整架构图：

*   系统采用分层架构：接入层（ALB + SSO Gateway）→ 服务层（Auth Service, Policy Service, Sync Service）→ 数据层（MySQL主从集群 + Redis缓存集群 + OSS策略快照）→ 集成层（Tianji Operator, OpsApi Adapter, CloudSSO Syncer）。
    
*   深层知识点：  
    *   SSO网关采用Envoy Proxy定制扩展，实现协议转换与策略前置注入；  
    *   权限策略引擎基于Open Policy Agent（OPA）二次开发，策略DSL兼容Rego语法；  
    *   已知问题：SCIM同步在超大规模组织（>50万用户）下存在内存峰值压力，已规划分片同步优化（Roadmap ID: EIAM-2025-Q3-SCIM-SHARDING）。

### 业务逻辑时序图

*   用户使用：  
    *   「首次SSO登录」：浏览器重定向 → SSO网关生成AuthnRequest → Identity Engine认证 → 策略引擎评估 → 生成SAML Assertion → 应用消费 → 创建会话  
    *   「权限变更生效」：管理员在控制台修改策略 → 策略引擎编译为WASM字节码 → 推送至SSO网关内存 → 下一次请求实时生效（毫秒级）  

*   工作流流转：  
    *   「入职流程」：HR系统推送新员工事件 → SCIM Adapter接收 → 组织同步服务创建用户+分配组织 → 触发默认策略绑定 → MyApps门户自动展示应用图标  
    *   「离职流程」：AD标记用户disabled → 同步服务捕获变更 → 自动禁用EIAM用户 + 解绑所有策略 + 清理SSO会话  

### 代码仓库

*   基线仓库：`idaas-eiam-base`（公共依赖、基础框架）
    
*   代码仓库：  
    *   `idaas-eiam-sso-gateway`（SSO网关，Go + Envoy）  
    *   `idaas-eiam-auth-service`（认证服务，Java Spring Boot）  
    *   `idaas-eiam-policy-engine`（策略引擎，Rust + OPA）  
    *   `idaas-eiam-sync-service`（同步服务，Python + Celery）  

*   制品仓库：`aliyun-acr/idaas/eiam-*`（ACR企业版，按组件分命名空间）
    
*   关联依赖仓库：`idaas-common-utils`、`tianji-operator-sdk`、`opsapi-client-java`

### 数据表结构

*   `eiam_user`：  
    ```sql
    CREATE TABLE `eiam_user` (
      `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
      `username` VARCHAR(128) NOT NULL COMMENT '登录用户名',
      `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
      `status` ENUM('active','inactive','locked') DEFAULT 'active',
      `org_unit_id` BIGINT UNSIGNED NOT NULL COMMENT '所属组织单元ID',
      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_username` (`username`),
      KEY `idx_org_unit` (`org_unit_id`)
    ) ENGINE=InnoDB COMMENT='用户主表';
    ```

*   `eiam_policy`：  
    ```sql
    CREATE TABLE `eiam_policy` (
      `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
      `name` VARCHAR(128) NOT NULL COMMENT '策略名称',
      `effect` ENUM('allow','deny') NOT NULL COMMENT '效果',
      `resource` TEXT NOT NULL COMMENT '资源表达式，如 app:console:*',
      `condition_json` JSON COMMENT 'ABAC条件JSON，如 {"ip": ["10.0.0.0/8"]}',
      `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级，数值越小越先匹配',
      `scope_type` ENUM('global','org','user','app') NOT NULL COMMENT '作用域类型',
      `scope_id` BIGINT UNSIGNED COMMENT '作用域ID',
      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      KEY `idx_scope` (`scope_type`, `scope_id`)
    ) ENGINE=InnoDB COMMENT='权限策略表';
    ```