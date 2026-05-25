# DDoS高防

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   DDoS高防是阿里云提供的**代理式DDoS防护服务**，通过全球分布式清洗中心对入向流量进行**牵引→清洗→回源**，抵御L3/L4/L7层大规模分布式拒绝服务攻击，保障业务在强攻击下的可用性与稳定性。
        
    *   演进主线：从中国内地单地域防护 → 全球化多地域覆盖（含非中国内地）→ 融合安全加速能力（安全加速线路2.0）→ AI驱动的智能识别与自适应策略（CC防护、IP信誉、DPI深度检测）→ 弹性计费与资源包体系完善。
        
    *   能力涉及核心产品与组件：DDoS高防实例（含专业版/高级版/保险防护/无限防护/安全加速线路2.0等规格）、全球清洗集群（含BGP多线机房、海外POP点）、AI防护引擎、DNS调度系统、回源隧道网关、弹性带宽/QPS计量模块。

*   对外介绍架构图
    
    *   **部署架构**：中心端为分布在全球的高防清洗中心（中国内地+非中国内地多AZ），端侧为用户源站（ECS/SLB/IDC等）；高防实例以独立容器组或虚拟化网元形式部署于清洗中心物理节点，不与用户业务共宿。
        
    *   **数据流向**：客户端 → DNS解析/CNAME/IP指向 → 高防清洗中心（L3/L4特征匹配 + L7 AI行为建模）→ 清洗后合法流量 → 端口协议转发 → 源站服务器。
        
    *   **上下游依赖**：
        *   依赖 **Tianji**（提供清洗中心资源纳管、健康巡检、故障自动切换）
        *   依赖 **OpsApi**（提供实例生命周期管理、规则配置下发、弹性带宽扩缩容接口）
        *   依赖 **云解析DNS**（实现CNAME引流与智能调度）
        *   与 **SLB/ECS/VPC** 深度协同（回源路径需配置VPC内网/公网路由，SLB可作为源站入口）
        
    *   参考：
        
    *   ![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/pLdn55XmA4gy9no8/img/89f7f08c-eecb-46e1-b6be-0564ae273ad9.svg)

*   各核心组件能力详细说明
    
    *   **清洗引擎集群**：承载L3/L4流量型攻击（SYN/UDP Flood）实时检测与丢弃；支持基于IP信誉库、连接状态、速率阈值的多维判定；L7层集成AI模型，支持URL级CC防护策略、人机识别（JS挑战/滑块）、业务指纹学习。
        
    *   **DNS调度系统**：提供CNAME接入能力，支持基于地理位置、延迟、攻击状态的智能解析调度；与高防控制面联动实现秒级引流切换。
        
    *   **回源网关**：支持TCP/UDP/HTTP/HTTPS协议透明转发；提供源站健康检查、自动故障隔离、TLS卸载（可选）；回源链路支持VPC内网、公网、专线多种模式。
        
    *   **弹性计量模块**：实时采集攻击峰值带宽、业务清洁带宽、QPS等指标，驱动弹性费用结算；支持95峰值计费模式（业务带宽/QPS）与瞬时峰值计费模式（攻击带宽）。
        
    *   **安全加速线路2.0**：在非中国内地高防基础上叠加中国内地骨干网直连链路，提供低延迟访问+应用层防护双重能力，支持禁用95弹性模式以适配稳定业务场景。

*   与阿里云其他产品的关系
    
    *   与 **VPC**：回源必须配置VPC路由（如添加高防清洗中心网段路由），否则流量无法抵达源站；高防实例本身不占用用户VPC资源。
        
    *   与 **ECS/SLB**：典型源站形态；SLB可作为高防回源目标，实现负载均衡与高可用；ECS需开放对应端口并确保安全组允许高防清洗中心IP段访问。
        
    *   与 **SLB**：SLB可作为源站入口，亦可与高防组合构建“高防→SLB→ECS”纵深防护链路；SLB自身不提供DDoS清洗能力，依赖高防前置防护。
        
    *   产品异常可能造成的影响：  
        ▶️ 高防实例不可用 → 所有经该实例引流的业务流量中断（DNS未回切时）；  
        ▶️ 清洗引擎误判 → 合法用户被限速/拦截（如AI模型未收敛导致CC误杀）；  
        ▶️ 回源链路异常 → 源站收不到清洗后流量，表现为“黑屏”或502/504错误。  
        不会造成的影响（边界清晰）：  
        ❌ 不影响用户DNS解析服务本身（DNS由云解析独立提供）；  
        ❌ 不影响源站ECS/SLB的本地服务能力（仅回源路径失效）；  
        ❌ 不影响VPC内网通信（除非显式配置了高防相关路由）；  
        ❌ 不会主动扫描或探测用户源站资产。

### QA（高频问答）：

*   Q：DDoS高防能否防护直接访问源站IP的攻击？  
  A：仅通过**IP直接指向**方式接入时可防护；DNS解析方式下，攻击者绕过CNAME直打源站IP将不受保护，因此务必隐藏源站IP并关闭源站公网暴露。

*   Q：攻击发生时是否需要人工干预才能启动清洗？  
  A：无需。清洗引擎全自动触发，当入向流量超过保底带宽阈值或检测到攻击特征即实时启动，秒级生效。

*   Q：安全加速线路2.0与普通DDoS高防（非中国内地）能否同时使用？  
  A：可以组合购买，分别承担“中国内地用户低延迟访问+防护”和“非中国内地用户原生防护”职责，适用于全球化混合访问场景。

*   Q：弹性防护费用如何避免意外超支？  
  A：可通过控制台设置**弹性带宽上限**（如封顶至保底带宽200%），或购买**高级防护资源包**锁定固定次数/容量，实现成本可控。

*   Q：高级防护（每月2次）用完后是否无法再防护CC攻击？  
  A：否。基础AI防护能力始终在线；高级防护特指调用更深度的专家策略引擎（如定制规则、人工研判介入），基础CC防护仍持续生效。

### 组件接口人、研发负责人等角色信息：

*   清洗引擎集群：研发负责人 `@ddos-engine-core`；L1运维权限：告警确认、实例重启、日志下载；L2权限：引擎参数热更新、策略灰度发布；重大策略变更需升级产研评审。
    
*   DNS调度系统：研发负责人 `@dns-scheduler`；L1权限：CNAME记录状态核查、解析延迟监控；L2权限：引流策略临时切换（如攻击期间强制切至高防）；DNS配置变更需同步云解析团队。
    
*   回源网关：研发负责人 `@backhaul-gateway`；L1权限：回源健康检查状态查看、连接数监控；L2权限：回源IP/端口热更新、TLS卸载开关；回源路由变更需协同网络团队。
    
*   弹性计量模块：研发负责人 `@metering-ddos`；L1权限：计费指标查询、账单核对；L2权限：弹性阈值临时调整（仅限应急）；计费逻辑变更必须走财务合规流程。

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `HighRiskAttackDetected` | P0 | 实时流量分析引擎输出 | 清洗引擎集群 | 攻击峰值 ≥ 保底带宽 × 3 且持续30s | 发生高强度L3/L4攻击，弹性带宽已超限，存在业务抖动风险 | [[方案支撑文档/典型问题排查解决方案#HighRiskAttackDetected|HighRiskAttackDetected应急手册]] | 是 |
| `BackhaulConnectionFailed` | P0 | 回源网关心跳探活失败 | 回源网关 | 连续5次ping/端口探测源站超时 | 回源链路中断，所有清洗后流量无法送达源站 | [[方案支撑文档/典型问题排查解决方案#BackhaulConnectionFailed|BackhaulConnectionFailed应急手册]] | 是 |
| `DNSResolutionAbnormal` | P1 | CNAME解析结果偏离高防IP池 | DNS调度系统 | 解析命中率 < 95% 或返回非高防IP超时率 > 5% | DNS引流失效，部分用户流量未进入清洗中心 | [[方案支撑文档/典型问题排查解决方案#DNSResolutionAbnormal|DNSResolutionAbnormal应急手册]] | 是 |
| `EngineModelDrift` | P2 | AI模型预测准确率下降 > 15% | 清洗引擎集群 | 连续2小时CC识别FPR（误杀率）> 8% | AI防护模型偏移，可能出现合法用户被拦截 | [[方案支撑文档/典型问题排查解决方案#EngineModelDrift|EngineModelDrift应急手册]] | 是 |
| `BandwidthUsageExceedThreshold` | P2 | 弹性带宽使用率 ≥ 90% | 弹性计量模块 | 当日累计攻击峰值带宽达保底×1.8 | 接近弹性费用封顶阈值，需关注攻击趋势 | [[方案支撑文档/运维指导/运维手册#弹性带宽监控|弹性带宽监控指南]] | 否 |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    *   `anti_ddos_instance_db.instance_config`：存储实例规格、保底带宽、回源IP/端口、协议类型；  
    *   `anti_ddos_metric_db.attack_summary_daily`：按日聚合攻击类型、峰值、持续时间、清洗量；  
    *   `anti_ddos_dns_db.cname_record`：记录CNAME绑定关系、TTL、生效状态。

*   关键日志路径，组件，内容，轮转策略  
    *   `/var/log/anti_ddos/engine/attack.log`（清洗引擎）：原始攻击包特征、判定结果、处置动作；按日轮转，保留30天；  
    *   `/var/log/anti_ddos/backhaul/probe.log`（回源网关）：源站健康检查结果、延迟、丢包率；按小时轮转，保留7天；  
    *   `/var/log/anti_ddos/dns/scheduler.log`（DNS调度）：CNAME解析请求、调度决策、异常事件；按日轮转，保留15天。

*   问题排查SOP，通用场景的排查思路和路径  
    *   **现象：用户访问502/504** → 查`backhaul.probe.log`确认源站连通性 → 查`instance_config`确认回源配置正确性 → 查清洗引擎是否误判（查`attack.log`中是否有大量`DROP_BY_AI`但无攻击标记）；  
    *   **现象：攻击期间业务延迟升高** → 查`attack_summary_daily`确认攻击峰值与带宽占用 → 查`engine.attack.log`中是否存在大量`CHALLENGE_SENT`（人机验证触发）→ 检查AI模型是否漂移（`EngineModelDrift`告警）；  
    *   **现象：部分用户无法访问** → 查`dns.scheduler.log`解析日志 → 查`cname_record`表确认CNAME状态 → 使用`dig +short`验证DNS解析结果。

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    *   升级由Tianji平台统一灰度推送，无需人工操作；  
    *   巡检手册见 [[方案支撑文档/运维指导/巡检手册|DDoS高防日常巡检手册]]；  
    *   Aone项目：`anti-ddos-pro-platform`（基线）、`anti-ddos-premium-engine`（AI引擎）。

### 典型问题排查解决方案

```yaml
一、问题描述
● 问题现象：用户访问业务返回502 Bad Gateway，控制台显示“回源失败”
● 适用范围：所有DDoS高防实例（中国内地/非中国内地），回源模式为公网或VPC内网
二、排查信息收集
● 必须收集的信息：实例ID、源站IP及端口、回源协议（HTTP/HTTPS）、VPC ID（如适用）
● 检查终态的方法：登录回源网关容器（`kubectl exec -n ddos-pro backhaul-gw-xxx -- sh`），执行`curl -v http://<源站IP>:<端口>/health`
● 排查问题步骤：
  - 步骤1：检查`/var/log/anti_ddos/backhaul/probe.log`中最近10分钟probe结果（关键词`FAIL`/`TIMEOUT`）
  - 步骤2：检查源站安全组是否放行高防清洗中心IP段（中国内地：`100.100.0.0/16`；海外：参见[[产品对外文档/服务介绍#与阿里云其他产品的关系|高防清洗中心IP白名单]]）
  - 步骤3：检查VPC路由表是否存在高防网段路由（如`10.0.0.0/8` via 高防ENI）
  - 排查结果映射：
    | probe.log显示TIMEOUT | 安全组未放行 | VPC路由缺失 | 结论 |
    |---|---|---|---|
    | ✓ | ✓ | ✗ | 源站防火墙拦截 |
    | ✓ | ✗ | ✓ | 安全组策略错误 |
    | ✗ | ✗ | ✓ | 回源网关自身异常（需升级） |
三、解决步骤
 场景一：安全组未放行高防IP
 - 适用条件：probe.log显示TIMEOUT，且安全组规则中无高防IP段
 - 实施步骤：
   - 登录ECS/SLB控制台 → 安全组 → 添加入方向规则
   - 协议：ALL / 端口：ALL / 授权对象：`100.100.0.0/16`（中国内地）或对应海外IP段
 - 结果验证：`curl -v`返回200且probe.log出现`SUCCESS`
 场景二：VPC路由缺失
 - 适用条件：probe.log TIMEOUT，安全组正确，但VPC路由表无高防网段
 - 实施步骤：
   - 登录VPC控制台 → 路由表 → 添加路由条目
   - 目标网段：`10.0.0.0/8`（示例） / 下一跳：高防实例绑定的ENI
 - 结果验证：`curl -v`成功，probe.log状态恢复
四、非本产品排查
● 若`curl -v`在回源网关容器内失败，但源站本地`curl localhost:port`成功 → 排查源站本地防火墙（iptables/firewalld）是否拦截
● 若probe.log显示`SUCCESS`但业务仍502 → 属于源站Web服务异常（如Nginx崩溃），需源站团队介入
五、快速定位工具
● 脚本位置：`/opt/anti_ddos/bin/check-backhaul.sh`
● 使用方法：`./check-backhaul.sh -i <实例ID> -p <端口>`，自动输出连通性、安全组、路由诊断结果
```

### 紧急场景止血与恢复手册：

*   **P0级攻击导致大面积502**：立即执行`/opt/anti_ddos/bin/emergency-failover.sh --mode=dns-bypass`，强制将DNS解析回切至源站IP（需提前配置备用A记录），临时规避高防链路；同步排查回源网关状态。
    
*   **AI模型严重误判（大量合法用户被拦截）**：登录控制台 → 实例详情页 → CC防护策略 → 临时关闭“AI自动学习”并启用“白名单模式”，添加业务域名至信任列表；待模型重训完成后再恢复。
    
*   **清洗中心区域性故障（如某海外POP宕机）**：通过Tianji平台触发全局DNS调度，将该区域流量自动牵引至邻近可用POP；无需人工干预，SLA保障99.95%可用性。

### 横向研发文档：

*   接入指引：[[横向研发文档/接入指引|DDoS高防标准接入流程]]
    
*   产品对接方案细节：[[横向研发文档/对接方案|DDoS高防与云解析/DNS/SLB对接技术规范]]
    
*   产品对接范围：支持HTTP/HTTPS/TCP/UDP四层协议回源；不支持WebSocket长连接保活穿透（需源站自行处理）；不支持FTP/SMTP等非标准协议深度清洗（仅做端口级透传）。

## 产品对内文档：

### 完整架构图：

*   系统采用分层解耦设计：  
    ▶️ **接入层**：DNS调度器、API网关（OpsApi）、控制台前端；  
    ▶️ **控制层**：实例管理服务、策略编排引擎、弹性计量中心；  
    ▶️ **数据层**：配置数据库（MySQL）、时序指标库（TSDB）、日志中心（SLS）；  
    ▶️ **转发层**：清洗引擎（DPDK加速）、回源网关（Envoy定制）、隧道代理；  
    ▶️ **基础设施层**：Tianji纳管的物理服务器/裸金属集群（含GPU节点用于AI训练）。  
    *已知问题*：旧版安全加速线路1.0存在移动线路覆盖盲区，已归档不推荐新购。

### 业务逻辑时序图

*   用户使用：  
    `用户发起HTTP请求` → `Local DNS查询CNAME` → `云解析返回高防IP` → `请求抵达清洗中心` → `AI引擎校验Session/Token/行为` → `合法则转发至源站，非法则挑战/丢弃` → `源站响应返回用户`  
*   工作流流转：  
    `攻击流量注入` → `清洗引擎实时检测` → `触发弹性带宽扩容事件` → `计量模块上报峰值` → `计费系统生成后付费账单` → `用户控制台展示用量报表`

### 代码仓库

*   基线仓库：`git@code.alibaba-inc.com:anti-ddos/anti-ddos-pro-platform.git`（主干分支：`release/2026-q2`）  
*   代码仓库：  
    *   清洗引擎：`git@code.alibaba-inc.com:anti-ddos/ddos-engine-dpdk.git`  
    *   AI防护模块：`git@code.alibaba-inc.com:anti-ddos/ai-mitigation-core.git`  
    *   回源网关：`git@code.alibaba-inc.com:anti-ddos/backhaul-envoy.git`  
*   制品仓库：`ali-registry.cn-zhangjiakou.cr.aliyuncs.com/anti-ddos/`（镜像命名：`engine:v2026.5.0`, `backhaul:v2026.5.0`）  
*   关联依赖仓库：`git@code.alibaba-inc.com:tianji/tianji-resource-manager.git`（资源纳管）、`git@code.alibaba-inc.com:opsapi/opsapi-ddos.git`（API服务）

### 数据表结构

*   `instance_config`（MySQL）：  
    `id`(PK), `instance_id`, `region`, `version_type`(ENUM: 'PRO','PREMIUM','INSURANCE','UNLIMITED'), `clean_bandwidth`, `attack_bandwidth`, `backhaul_ip`, `backhaul_port`, `protocol`, `vpc_id`, `created_at`, `updated_at`  
*   `attack_summary_daily`（TSDB）：  
    `instance_id`, `date`, `attack_type`(TAG), `peak_bps`, `cleaned_bytes`, `drop_ratio`, `cc_challenge_count`  
*   `cname_record`（MySQL）：  
    `id`(PK), `domain`, `cname_value`, `status`(ENUM: 'ACTIVE','INACTIVE'), `ttl`, `updated_at`