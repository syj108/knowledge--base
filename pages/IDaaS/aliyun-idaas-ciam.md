# 阿里云IDaaS CIAM

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   [[ciam/aliyun-idaas-ciam|阿里云IDaaS CIAM]]是面向消费者（C端）、会员等**外部用户**的客户身份与访问管理（Customer Identity & Access Management）解决方案，聚焦于统一身份认证、跨平台无缝体验、高并发可用性及本土化服务能力，区别于面向员工/内部人员的EIAM（Employee IAM）。
        
    *   作为IDaaS产品体系的重要分支，CIAM自2023年起独立演进，逐步强化私有化部署能力、中国合规适配（如手机号一键登录、银联/微信/支付宝OAuth2.0深度集成、等保三级支持）、多租户隔离与品牌定制化能力；2024年上线专属云版本，支持资源独享与专家级客制化交付。
        
    *   能力覆盖核心产品：`IDaaS CIAM控制台`、`CIAM Auth Service（认证中心）`、`CIAM User Directory（客户用户目录）`、`CIAM Self-Service Portal（自助门户）`、`CIAM API Gateway（开放API网关）`，以及私有化部署所需的`CIAM On-Premise Orchestrator`组件。

*   对外介绍架构图
    
    *   **部署架构**：支持三类形态——  
        ▪ 公共云标准版（多租户共享底座，SaaS模式）  
        ▪ 公共云专属版（VPC内独占资源池，含独立K8s集群、DB实例、Redis集群）  
        ▪ 私有化部署版（交付至客户IDC或信创环境，支持ARM/x86混合架构、国产OS/数据库适配）  
        所有形态均采用容器化部署（Alibaba Cloud ACK），核心组件以Pod形式运行于物理节点或ECS实例之上。
        
    *   **数据流向图**：终端用户 → 前端应用（Web/App/小程序）→ CIAM API Gateway → Auth Service（鉴权/会话管理）↔ User Directory（用户主数据CRUD）↔ Self-Service Portal（密码重置、MFA绑定等）→ 同步至下游业务系统（通过SCIM/API/消息队列）。
        
    *   **上下游依赖关系**：  
        ▪ 依赖：Tianji（资源纳管与健康巡检）、OpsApi（运维指令下发）、ARMS（全链路监控埋点）、SLS（日志采集）、ACR（镜像仓库）  
        ▪ 被依赖：电商中台、会员中心、金融风控系统、教育SaaS平台等业务系统通过CIAM SDK/API接入身份能力  
        ▪ 无强依赖：VPC（仅网络连通性要求，不依赖其安全组策略逻辑）、ECS（仅作为运行载体，不耦合实例生命周期）、SLB（可选，CIAM自身提供Ingress Controller）
        
    *   参考：  
        ![IDaaS CIAM 架构示意图](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/54Lq35oXpkvg1l7E/img/1213e0b1-d245-4023-8f12-1a8fa1c09600.png)

*   各核心组件能力详细说明
    
    *   `CIAM Auth Service`：承担OAuth2.0/OpenID Connect协议实现、JWT签发与校验、会话状态管理（Redis-backed）、风险登录识别（设备指纹+行为分析）、MFA（短信/邮件/TOTP/生物识别）策略引擎。支持QPS ≥ 50,000（专属版）。
        
    *   `CIAM User Directory`：基于分库分表的客户主数据存储，支持千万级用户实时读写；字段模型兼容GDPR/《个人信息保护法》，内置敏感字段加密（SM4/AES-256）、脱敏策略配置、数据生命周期管理（如“注册后180天未激活自动归档”）。
        
    *   `CIAM Self-Service Portal`：白标化前端门户，支持多语言（含简体中文优先）、主题皮肤定制、流程编排（如“忘记密码→短信验证→新密码设置→安全问题绑定”可拖拽配置）。
        
    *   `CIAM API Gateway`：统一对接入口，提供RESTful API（含OpenAPI 3.0规范）、SDK（Java/Python/iOS/Android）、Webhook事件回调（用户注册成功、登录异常等），具备限流、熔断、审计日志全量记录能力。
        
    *   `CIAM On-Premise Orchestrator`（私有化专有）：Ansible+Helm驱动的自动化部署与升级引擎，支持离线环境安装、证书自动轮转、国产中间件（达梦/人大金仓/OceanBase）适配、等保三级基线加固。

*   与阿里云其他产品的关系
    
    *   与 VPC：仅需基础网络连通（ICMP + TCP 443/80），不依赖VPC高级功能（如流日志、网络ACL策略联动）；CIAM异常**不会导致VPC网络中断或路由变更**。
        
    *   与 ECS：运行载体关系，CIAM Pod故障仅影响本实例上服务，**不会触发ECS实例重启、释放或系统盘损坏**；ECS底层故障由云平台自动恢复，CIAM具备Pod级自愈能力（Liveness Probe）。
        
    *   与 SLB：非必需依赖；若客户使用SLB做流量入口，CIAM仅将其视为四层/七层转发器，**SLB配置错误（如健康检查路径误配）可能导致502，但CIAM自身服务无损**；CIAM可直连ECS IP或通过ALB Ingress暴露服务。
        
    *   边界声明：  
        ▪ **CIAM不负责业务权限决策**（如“用户能否查看订单详情”），该逻辑由业务系统基于CIAM提供的身份上下文（sub/roles/scopes）自行判断；  
        ▪ **CIAM不存储业务数据**（如订单、课程、资产），仅保存身份属性与认证凭证；  
        ▪ **CIAM不替代WAF/防火墙**，但提供防暴力破解、验证码、人机识别等应用层防护能力。

### QA（高频问答）：

*   Q：CIAM是否支持微信/支付宝/银联等国内主流身份源？  
    A：是。已预集成微信开放平台、支付宝开放平台、银联云闪付OAuth2.0协议，并支持企业自有身份源（LDAP/SAML 2.0）对接。

*   Q：私有化版本是否支持信创环境（麒麟OS+达梦DB+鲲鹏CPU）？  
    A：是。2024年Q2起全面支持信创全栈适配，已通过工信部信创实验室认证。

*   Q：用户量超千万时，性能如何保障？  
    A：专属版/私有化版支持水平扩展：User Directory可分片至32个MySQL Shard；Auth Service支持Stateless横向扩容（最高200+ Pod）；全链路压测QPS ≥ 120,000（P99 < 300ms）。

*   Q：是否满足等保2.0三级要求？  
    A：是。公共云专属版与私有化版均通过等保三级测评，提供审计日志留存≥180天、操作留痕、双因子认证、传输加密（TLS1.2+）、存储加密（SM4）等能力。

*   Q：能否与企业现有HR系统（如北森、Moka）打通同步员工账号？  
    A：不推荐。CIAM定位为**C端用户**管理，员工账号应由EIAM（如阿里云Resource Directory）或HRIS系统管理；如需少量员工兼用CIAM门户，可通过SCIM API单向同步只读基础信息（姓名/邮箱），禁止同步薪资、职级等敏感字段。

### 组件接口人、研发负责人等角色信息：

*   `CIAM Auth Service`：研发负责人 张伟（zhangwei@alibaba-inc.com），L2可操作：重启Pod、调整HPA阈值、查看Redis连接池指标；L1仅限查看Prometheus监控大盘。
    
*   `CIAM User Directory`：研发负责人 李婷（liting@alibaba-inc.com），L2可操作：执行DB Schema热更新（ALTER TABLE ADD COLUMN）、触发分片数据再平衡；L1不可直接访问数据库，须提SQL工单。
    
*   `CIAM Self-Service Portal`：研发负责人 王磊（wanglei@alibaba-inc.com），L2可操作：上传新主题包、发布前端灰度版本、配置流程节点跳转规则；L1仅限查看CDN缓存命中率与JS错误率。
    
*   `CIAM API Gateway`：研发负责人 陈明（chenming@alibaba-inc.com），L2可操作：新增API路由、配置限流策略（QPS/用户级）、导出审计日志；L1仅限查看API调用量趋势图。
    
*   `CIAM On-Premise Orchestrator`（私有化）：研发负责人 刘芳（liufang@alibaba-inc.com），L2可操作：执行Ansible Playbook重装、证书续期、国产中间件参数调优；L1不可登录目标服务器，所有操作须经EOCC审批后由L2代执行。

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `CIAM_Auth_JWT_Signature_Verify_Fail_Rate_High` | P0 | Prometheus指标 `auth_jwt_verify_fail_rate{job="ciamauth"} > 0.05` 持续5分钟 | CIAM Auth Service | JWT签名验签失败率超5% | 密钥轮转不一致、时间不同步、恶意Token泛洪攻击 | [[紧急场景止血与恢复手册#jwt密钥不一致应急|JWT密钥不一致应急]] | 是（KB#CIAM-SEC-001） |
| `CIAM_UserDir_DB_Conn_Pool_Exhausted` | P0 | Grafana看板 `userdir_db_conn_used_percent > 95` | CIAM User Directory | 数据库连接池使用率持续>95% | 用户目录读写请求积压，注册/登录超时风险 | [[典型问题排查解决方案#DB连接池耗尽|DB连接池耗尽]] | 是（EOCC#CIAM-DB-002） |
| `CIAM_SSP_Portal_5xx_Rate_High` | P1 | SLS日志统计 `status >= 500 and service=ssportal` QPM > 10 | CIAM Self-Service Portal | 门户5xx错误每分钟超10次 | 前端资源加载失败、后端微服务超时、模板渲染异常 | [[典型问题排查解决方案#SSP门户5xx|SSP门户5xx]] | 是（KB#CIAM-FE-003） |
| `CIAM_APIGW_RateLimit_Exceeded` | P2 | API Gateway监控 `apigw_ratelimit_rejected_total` 突增 | CIAM API Gateway | 单用户/APP Key调用量突破限流阈值 | 客户端Bug导致无限重试，或遭受爬虫攻击 | [[运维指导/运维手册#API限流配置|API限流配置]] | 否（L2可自主调整阈值） |
| `CIAM_OnPrem_Orchestrator_Cert_Expired` | P1 | 自检脚本 `orchestrator-cert-check.sh` 返回非零 | CIAM On-Premise Orchestrator | TLS证书剩余有效期<7天 | 所有HTTPS接口将拒绝连接，影响全部用户访问 | [[紧急场景止血与恢复手册#证书过期一键续签|证书过期一键续签]] | 是（EOCC#CIAM-OP-004） |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    ▪ `user_directory.t_user_base`：用户主表（user_id, mobile, email, status, created_at）  
    ▪ `user_directory.t_user_profile`：扩展属性表（user_id, profile_json, updated_at）  
    ▪ `auth_service.t_session_active`：活跃会话表（session_id, user_id, expire_at, ip, ua）  
    ▪ `auth_service.t_auth_event_log`：认证事件审计表（event_id, user_id, event_type, result, risk_level）

*   关键日志路径，组件，内容，轮转策略  
    ▪ `/var/log/ciam/auth-service/access.log`（CIAM Auth Service）：Nginx access日志，JSON格式，按日切割，保留30天  
    ▪ `/opt/ciam/ssportal/logs/error.log`（CIAM Self-Service Portal）：前端服务错误日志，按大小切割（100MB），保留7天  
    ▪ `/data/ciam/orchestrator/logs/ansible.log`（CIAM On-Premise Orchestrator）：Ansible执行日志，按任务ID归档，永久保留（压缩存储）

*   问题排查SOP，通用场景的排查思路和路径  
    ▪ **现象：用户无法登录** → 查API Gateway 4xx/5xx日志 → 若为401/403，查Auth Service JWT解析日志 → 若为500，查User Directory DB慢查询日志 → 最终定位至具体组件与错误码  
    ▪ **现象：自助门户白屏** → 查SSP Nginx error.log → 若报`Failed to fetch config`，检查`/etc/ciam/ssportal/config.json`是否存在且可读 → 若报`Cannot resolve module`，检查CDN资源URL是否失效  

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    ▪ 升级指南：[[IDaaS CIAM 版本升级SOP|CIAM版本升级SOP]]（含灰度发布checklist、回滚步骤、兼容性矩阵）  
    ▪ 巡检手册：每日自动巡检项见 [[IDaaS CIAM 日常巡检清单|CIAM日常巡检清单]]（含DB连接数、Redis内存、证书有效期、API成功率）  
    ▪ Aone项目：`IDaaS-CIAM-Release`（基线分支：`release/2.5.x`），制品仓库：`acr.aliyuncs.com/idass/ciam-auth:v2.5.3`

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

### 紧急场景止血与恢复手册：

*   **JWT密钥不一致应急**：执行`kubectl exec -it ciamauth-deployment-xxx -- bash -c "cd /etc/ciam/auth-service && cp jwt-key-old.pem jwt-key.pem && kill -HUP 1"`，强制重载密钥（5秒内生效）。
    
*   **证书过期一键续签**：私有化环境运行`/opt/ciam/orchestrator/bin/renew-cert.sh --force`，自动调用cert-manager签发新证书并滚动更新所有Ingress。
    
*   **User Directory DB主从延迟>300s**：立即执行`kubectl exec -it userdir-mysql-master -- mysql -e "STOP SLAVE; START SLAVE;"`，手动触发复制重连；若无效，切换至备用从库（需提前配置VIP）。

### 横向研发文档：

*   接入指引：[[IDaaS CIAM 接入指南|CIAM接入指南]]（含前端SDK初始化、后端Token校验代码示例、Webhook事件订阅）
    
*   产品对接方案细节：[[IDaaS CIAM 与电商中台对接方案|电商中台对接方案]]（SCIM同步字段映射表、订单中心Token透传规范）
    
*   产品对接范围等：明确CIAM仅提供身份层能力，不参与业务逻辑（如优惠券发放、库存扣减），详见 [[IDaaS CIAM 能力边界说明书|CIAM能力边界]]。

## 产品对内文档：

### 完整架构图：

*   系统采用分层架构：  
    ▪ 接入层：ALB/SLB + CIAM API Gateway（Kong）  
    ▪ 服务层：Auth Service（Spring Boot）、User Directory（ShardingSphere-JDBC + MySQL Cluster）、SSP Portal（Vue3 + Nginx）  
    ▪ 数据层：MySQL分片集群（8节点）、Redis Cluster（6节点）、OSS（存储头像/证件照）  
    ▪ 运维层：ARMS（JVM/DB/Redis全指标）、SLS（结构化日志）、Tianji（主机健康度）  
    ▪ 安全层：KMS（密钥管理）、SGX可信执行环境（敏感计算如生物特征比对）  
    ▪ 已知问题：Auth Service在极端高并发下（>10万QPS）偶发JWT签发延迟（已规划2024Q3迁移至eBPF加速模块）。

### 业务逻辑时序图

*   用户使用：  
    `用户打开App → App调用CIAM SDK获取Authorization Code → 重定向至CIAM SSP Portal登录页 → 用户输入手机号+验证码 → Portal调用Auth Service生成Code → App用Code向API Gateway换取ID Token → App解析Token获取user_id → 向业务系统发起带Bearer Token的请求`

*   工作流流转：  
    `注册请求 → API Gateway校验频率 → Auth Service生成临时Session → SSP Portal渲染注册表单 → 用户提交 → Auth Service调用短信服务 → User Directory写入基础信息 → 触发Webhook通知会员中心 → 返回Success`

### 代码仓库

*   基线仓库：`code.alibaba-inc.com/idass/ciam-base`（含公共依赖、配置中心SDK）
    
*   代码仓库：  
    ▪ `code.alibaba-inc.com/idass/ciam-auth`（Auth Service）  
    ▪ `code.alibaba-inc.com/idass/ciam-userdir`（User Directory）  
    ▪ `code.alibaba-inc.com/idass/ciam-ssp`（Self-Service Portal）  
    
*   制品仓库：`acr.aliyuncs.com/idass/ciam-auth:v2.5.3`（Docker镜像）  
    `maven.aliyun.com/repository/public/com/alibaba/idass/ciam-sdk-java/2.5.3`（Maven包）
    
*   关联依赖仓库：  
    ▪ `code.alibaba-inc.com/middleware/shardingsphere-jdbc`（分库分表中间件）  
    ▪ `code.alibaba-inc.com/security/kms-client`（密钥管理SDK）

### 数据表结构

*   `t_user_base`（用户主表）：  
    `id BIGINT PK`, `tenant_id VARCHAR(64) NOT NULL`, `user_id VARCHAR(128) UNIQUE NOT NULL`, `mobile VARCHAR(20)`, `email VARCHAR(128)`, `status TINYINT DEFAULT 1 COMMENT '0-禁用,1-启用,2-待激活'`, `created_at DATETIME`, `updated_at DATETIME`

*   `t_auth_event_log`（认证事件审计表）：  
    `id BIGINT PK`, `event_id VARCHAR(64) UNIQUE`, `tenant_id VARCHAR(64)`, `user_id VARCHAR(128)`, `event_type ENUM('login','logout','register','pwd_reset')`, `result ENUM('success','failed')`, `risk_level TINYINT COMMENT '0-低危,1-中危,2-高危'`, `ip VARCHAR(45)`, `ua TEXT`, `created_at DATETIME`