# 平台凭证管理服务（PCM）介绍 

## PCM 是什么

PCM（Platform Credential Management）是 `baseServiceAll` 中的基础服务，核心目标是**接管平台底表 AK，实现凭证的动态轮换与安全管控**，提升安全性。

## 基本概念介绍

| 概念 | 说明 |
| --- | --- |
| **底表 AK** | 通过全局变量方式声明、云平台初始化时自动创建的 AK |
| **IAMID** | 产品申请派生时身份标识：格式为 `${CLUSTERNAME}:<serverrole名称>`，PaaS 格式为 `{{ .Values.productName }}:{{ .Release.Name }}`<br>当前未强校验格式 |
| **secretARN** | 凭证目标资源标识，格式为 `apsara:pcm:akid:<accessKeyId>:dst_endpoint:<GatewayCode>:sk:<accessKeySecret>` |
| **GatewayCode** | 服务的认证网关 code，用于区分 AK 私用网关和标准 AK 认证网关<br>当前版本仅标准 AK 认证网关支持使用底吧AK |
| **initAK** | 原始底表 AK，PCM 改造前应用直接使用的凭证 |

---

## PCM 核心能力

### 凭证生命周期管理

PCM 接管底层分配的凭证，为对应凭证创建**主动过期的凭证队列**，并定期清洗禁用老化的派生凭证。

###  派生 AK 队列机制

#### 队列基本概念

底表在生成派生 AK 时每个派生AK会关联一个派生AK队列，队列默认维持 7 把有效 派生AK，每把 派生AK 有效期 24 小时。因此，一把派生 AK 从创建到默认过期需要 7 天。

#### 队列级别

派生 AK 队列有两种划分级别：

| 级别 | 划分方式 | 说明 | 推荐程度 |
| --- | --- | --- | --- |
| initAK 级别（默认） | 一个底表 AK 对应一个派生 AK 队列，全局共享 | 默认配置，也是推荐的选择 | ✅ 推荐 |
| ClusterName 级别 | 按集群划分，同一集群内一个底表 AK 对应一个派生 AK 队列 | 多集群会为同一个底表 AK 创建多个队列，叠加后可能把 UMM 账户的 AK 上限打满 | ⚠️ 有风险，不推荐 |

> _为什么不推荐 ClusterName 级别？ UMM AK 管理中，每个账户对应的 AK 数量有上限。按 ClusterName 级别，每个集群都会为同一个底表 AK 创建独立的派生 AK 队列，多集群叠加可能把账户的 AK 上限打满，导致无法创建新的派生 AK。因此默认和推荐的配置都是 initAK 级别。_

#### 队列轮转保护机制

派生 AK 队列会持续轮转（定期创建新 AK、禁用老 AK），但在以下两种情况下会暂停轮转，以保护正在使用中的凭证：

*   保护一：产品最新派生 AK 保护
    

当要禁用队列里最早的那把 AK 时，系统会检查这把 AK 是否是某个产品获取的最新派生 AK。如果产品 A 拿到这把 AK 后就没再获取过新 AK，那这把就是产品 A 的"最新"，队列就会停止轮转，保持当前状态。直到后续其他产品都获取了更新的派生 AK，队列才会继续轮转。这样保证不会因为轮转把某个产品正在用的 AK 给禁掉。

*   保护二：平台 AK 访问日志不可行（当前状态）
    

当不可行时，PCM无法确认即将禁用的派生AK是否仍产品在调用，将在第一把队列即将禁用时停止轮转。

*   保护三：平台 AK 访问日志可信时：平台 AK 访问日志保护
    

平台 AK 访问日用于检查底表AK和派生AK是否在网关中有调用记录

在准备禁用某把派生 AK 前，系统会检查平台 AK 访问日志，确认这把 AK 当前是否还在被使用。如果日志显示还有产品在用这把 AK，也会停止轮转。

###  三种管控模式

| 模式 | 含义 | 行为 | 适用场景 | 版本 |
| --- | --- | --- | --- | --- |
| **None（默认）** | 不受 PCM 管理 | AK 正常使用，PCM 不介入 | 尚未改造的存量凭证 | / |
| **CompatibilityMode（兼容模式）** | 部分完成改造 | 提供轮换能力，但不对旧 AK 禁用 | 改造中的过渡态 | v3182-2510 |
| **StrictMode（严格模式）** | 使用方改造完成 | 新部署严格托管；热升级/扩等场景自动降级为兼容模式 | 存量改造完成后的目标终态 | v3182-2515以后 |
| **initStrictMode（初始严格模式）** | 新建凭证即完成改造 | 任何场景都开启严格处理 | 新增收口凭证 | v320 |

### 热升级兼容策略

*   **新部署项目**：根据 `restrict` 取值禁用原始通用能力，应用使用凭证进入定时轮换状态
    
*   **热升级项目**：原始凭证**不禁用**其通用能力，进入定时轮换状态；如需禁用老凭证，通过观测日志在运维控制台灰度进行
    
*   **非 PCM 托管凭证**：一切照旧；若使用了 PCM SDK/CLI 但未被托管，将入参 initAK 返回让应用接着使用
    

---

## 架构与组件调用关系

### 接入后对比示意图

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/a/pJA81LLOXuxmaky7/430aa436587d422d9f57898864077aa50521.png)

### 调用PCM服务（获取派生AK）示意图

```mermaid
graph TB
    subgraph L1["🖥️ 云产品组件层"]
        direction TB
        J[Java 组件]
        G[Go 组件]
        P[Python 组件]
        C[其他组件]
    end

    subgraph L2["📦 接入层"]
        SDK["PCM SDK / CLI / AKless\n凭证获取端 · 公专一体兼容"]
    end

    subgraph L3["🔐 PCM 服务层"]
        direction LR
        Core["PCM Core\n凭证缓存网关"]
        Ctrl["PCM Controller\n策略中心"]
    end

    subgraph L4["🗄️ PCM 依赖服务"]
        direction LR
        UMM["UMM\nAK 生命周期管理"]
        AAS["AAS\n账户管理"]
    end

    J --> SDK
    G --> SDK
    P --> SDK
    C --> SDK

    SDK --> Core
    Core --> Ctrl

    Ctrl --> UMM
    UMM --> AAS

    style L1 fill:#e3f2fd
    style L2 fill:#fff3e0
    style L3 fill:#e8f5e9
    style L4 fill:#f3e5f5
    style Core fill:#4caf50,color:#fff
    style Ctrl fill:#2196f3,color:#fff
    style SDK fill:#ff8800,color:#fff
```

### 调用时序图

```mermaid
sequenceDiagram
    autonumber
    participant App as 云产品组件<br/>(Java/Go/Python/CLI)
    participant SDK as PCM SDK/CLI<br/>(本地凭证管理)
    participant Core as PCM Core<br/>(凭证缓存网关)
    participant Ctrl as PCM Controller<br/>(策略中心)
    participant UMM as UMM<br/>(AK管理)
    participant AAS as AAS<br/>(账户管理)

    App->>SDK: 请求凭证<br/>(传入底表AK/SK + IAMID + SecretARN)

    SDK-->>SDK: 第一层：检查内存缓存<br/>(凭据是否存在且未过期)

    alt 内存缓存命中且未过期
        SDK-->>App: 返回缓存凭证 (code=201)
    else 内存缓存未命中/已过期
        SDK->>Core: 请求凭证<br/>(SecretName + PcmToken JWT签名)
        Core-->>Core: 检查共享内存缓存<br/>(initak_queue/endpoint_queue等)

        alt Core缓存命中
            Core-->>SDK: 返回缓存的派生凭证
            SDK-->>SDK: 更新内存缓存 + 加密持久化到本地文件
            SDK-->>App: 返回派生凭证 (code=202/203)
        else Core缓存未命中/过期
            Core->>Ctrl: 请求派生新凭证<br/>(initAK + IAMID + endpoint)
            Ctrl->>UMM: 创建/获取派生AK
            UMM->>AAS: 关联账户凭证
            AAS-->>UMM: 返回账户数据
            UMM-->>Ctrl: 返回派生AK
            Ctrl-->>Core: 返回派生凭证
            Core-->>Core: 写入共享内存缓存
            Core-->>SDK: 返回新派生凭证
            SDK-->>SDK: 更新内存缓存 + 加密持久化到本地文件
            SDK-->>App: 返回派生凭证 (code=202/203)
        end
    end

    Note over App,SDK: === 以下为异常降级路径 ===

    alt SDK请求Core失败(网络不通/超时/Core异常)
        SDK-->>SDK: 第二层：读取本地加密文件<br/>(AES-256-GCM解密)
        alt 本地文件有有效凭证
            SDK-->>App: 返回本地缓存凭证 (code=301/302)
        else 本地文件也没有
            SDK-->>App: 降级返回底表AK (code=401)
        end
    end

    Note over Core,Ctrl: Core 定时同步 Controller<br/>保持缓存最新
    Note over Ctrl,UMM: Controller 定期轮转派生AK<br/>禁用老化凭证

```

## 各组件内部安全特性

### PCM SDK / CLI — 凭证获取端

**职责**：为云产品应用提供接入能力，直接与 PCM 服务交互获取新凭证，支持多种容错策略。

**安全特性**：

| 特性 | 说明 |
| --- | --- |
| **多级缓存** | 在本地内存、磁盘均有缓存 |
| **容错降级** | PCM 初始化服务异常或报错时，将入参作为凭证返回；如果有缓存，将返回最近一次从服务端获取的凭证 |

###  PCM Core — 缓存中间网关

**职责**：SDK 与 Controller 之间的访问中间网关，缓存 Controller 最新凭证数据，为 SDK 提供 API 获取最新凭证，缓解 Controller 访问压力，提高 SDK 访问响应速度。

**安全特性**：

| 特性 | 说明 |
| --- | --- |
| **本地缓存 + 定时同步** | 本地缓存 & 定时同步 PCM Controller 的最新凭证信息，减少直接访问 Controller 的频率 |
| **缓存隔离** | 缓存数据仅服务于已认证的 SDK 请求，不对外暴露 |
| **降级保护** | Core 宕机后，末期过期老凭证行为暂停，SDK 返回上次获得的老凭证（未在窗口期末尾），依然可以使用 |
| **压力缓解** | 作为中间层，避免所有 SDK 请求直接打到 Controller，防止策略大脑被击穿 |

### PCM Controller — 策略中心

**职责**：PCM 凭证管控核心，执行凭证生命周期管理，提供 PKM 白屏管控、日志查询关联、状态管理能力，支持热升级后以运维变更方式将老凭证进行禁用。

**安全特性**：

| 特性 | 说明 |
| --- | --- |
| **凭证队列管理** | 为每个被托管凭证创建主动过期的凭证队列，定期清洗禁用老化派生凭证 |
| **模式管控** | 根据 `controlByPcm` 配置执行不同模式（CompatibilityMode / StrictMode / initStrictMode） |
| **松→紧变更不自动生效** | 模式从松到紧变更时不自动生效，需 ASO 页面提示人工处理，防止误操作 |
| **灰度禁用** | 支持热升级后以运维变更方式逐步禁用老凭证，而非一刀切 |
| **白屏管控（PKM）** | 提供可视化的凭证管理界面，降低运维门槛 |
| **日志查询关联** | 提供日志查询能力，关联 AK 使用记录，判断是否可以安全禁用 |
| **状态管理** | 管理每个凭证的当前状态（轮换中/已禁用/正常等） |

### UMM — AK 生命周期管理

**职责**：PCM 依赖服务，负责 AK 的存储与生命周期管理，接收 Controller 指令执行凭证轮换和禁用操作。

### AAS — 账户管理服务

**职责**：PCM 依赖服务，负责平台账户统一管理，与 UMM 联动形成账户-凭证关联关系。

---

## 关键安全设计

### 标准 AK 认证 vs AK 私用

| 类型 | 说明 |
| --- | --- |
| **标准 AK 认证** | AK 生命周期在 UMM 中保管，标准网关通过对接 UMM 进行 AK 签名校验（如 POP、OpenAPI、OSS），当前访问标准AK认证服务的云产品均已适配完成； |
| **AK 私用场景** | 服务不接或无法接 UMM，直接把 AK 参数记录到本地配置文件/数据库中，请求过来时用本地配置校验；当前访问AK私用服务的云产品尚未强制要求适配，以适配的产品通过PCM服务将兑换出原始底表AK； |

### 高可用 / 容错逻辑

| 场景 | SDK 行为 | 业务影响 |
| --- | --- | --- |
| 新部署时 PCM Core 还未 ready | 将入参作为返回 | 无影响（Core 未禁用老 AK） |
| 运行时 PCM Core 挂了 | 返回上次获取的老凭证（未在窗口期末尾） | 无影响 |
| 产品独立升级，PCM 未 ready | 将入参作为返回 | 无影响 |
| PCM 和应用都挂了需重拉（SDK 缓存未丢失） | 返回上次获取的老凭证 | 无影响 |
| PCM 和应用都挂了需重拉（SDK 缓存丢失） | **需先恢复 PCM 或使用老凭证应急脚本** | **业务中断** |

## 源码

PCM-core：[https://code.alibaba-inc.com/aliyunas\_sectech/pcm-core](https://code.alibaba-inc.com/aliyunas_sectech/pcm-core)

PCM-controller：[https://code.alibaba-inc.com/aliyunas\_sectech/pcm-controller](https://code.alibaba-inc.com/aliyunas_sectech/pcm-controller)