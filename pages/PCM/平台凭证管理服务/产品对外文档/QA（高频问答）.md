# QA（高频问答）

**PCM 的定义与核心目标**
PCM（Platform Credential Management，[[PCM/平台凭证管理服务/index|平台凭证管理服务]]）是 `baseServiceAll` 中的基础服务。其核心目标是**接管平台底表 AK，实现凭证的动态轮换与安全管控**，从而提升平台的整体安全性。

**核心名词解释**
- **底表 AK**：通过全局变量方式声明、云平台初始化时自动创建的 AK。
- **initAK**：原始底表 AK，即 PCM 改造前应用直接使用的凭证。
- **IAMID**：产品申请派生时的身份标识。格式为 `${CLUSTERNAME}:<serverrole名称>`，PaaS 格式为 `{{ .Values.productName }}:{{ .Release.Name }}`（当前未强校验格式）。
- **secretARN**：凭证目标资源标识，格式为 `apsara:pcm:akid:<accessKeyId>:dst_endpoint:<GatewayCode>:sk:<accessKeySecret>`。
- **GatewayCode**：服务的认证网关 code，用于区分 AK 私用网关和标准 AK 认证网关（当前版本仅标准 AK 认证网关支持使用底表 AK）。

## 认证场景与网关

**标准 AK 认证与 AK 私用场景的区别**
- **标准 AK 认证**：AK 生命周期在 UMM 中保管，标准网关通过对接 UMM 进行 AK 签名校验（如 POP、OpenAPI、OSS）。当前访问标准 AK 认证服务的云产品均已适配完成。
- **AK 私用场景**：服务不接或无法接 UMM，直接把 AK 参数记录到本地配置文件或数据库中，请求过来时用本地配置校验。当前尚未强制要求适配，已适配的产品通过 PCM 服务兑换出原始底表 AK。

## 派生 AK 队列机制

**队列运作机制**
底表在生成派生 AK 时，每个派生 AK 会关联一个派生 AK 队列。队列默认维持 **7 把有效派生 AK**，每把派生 AK 有效期为 **24 小时**。因此，一把派生 AK 从创建到默认过期需要 7 天。队列会持续轮转（定期创建新 AK、禁用老 AK）。

**队列划分级别**
派生 AK 队列有两种划分级别：
- **initAK 级别（默认且推荐）**：一个底表 AK 对应一个派生 AK 队列，全局共享。
- **ClusterName 级别（不推荐）**：按集群划分，同一集群内一个底表 AK 对应一个派生 AK 队列。
> **不推荐 ClusterName 级别的原因**：在 UMM AK 管理中，每个账户对应的 AK 数量有上限。按 ClusterName 级别划分时，每个集群都会为同一个底表 AK 创建独立的派生 AK 队列，多集群叠加极易把账户的 AK 上限打满，导致无法创建新的派生 AK。

**队列保护机制**
为保护正在使用中的凭证，队列在以下三种情况下会暂停轮转：
1. **产品最新派生 AK 保护**：当要禁用队列里最早的 AK 时，若该 AK 是某个产品获取的“最新”派生 AK（即该产品拿到后未再获取新 AK），队列将停止轮转，直到其他产品获取了更新的派生 AK。
2. **平台 AK 访问日志不可行保护**：当访问日志不可行时，PCM 无法确认即将禁用的派生 AK 是否仍有产品在调用，将在第一把队列即将禁用时停止轮转。
3. **平台 AK 访问日志保护（日志可信时）**：在准备禁用某把派生 AK 前，系统会检查平台 AK 访问日志。若日志显示该 AK 当前仍在被使用，则停止轮转。

## 管控模式与兼容策略

**管控模式**
PCM 支持以下四种管控模式：

| 模式 | 含义 | 行为 | 适用场景 | 版本 |
| --- | --- | --- | --- | --- |
| **None（默认）** | 不受 PCM 管理 | AK 正常使用，PCM 不介入 | 尚未改造的存量凭证 | / |
| **CompatibilityMode** | 部分完成改造 | 提供轮换能力，但不对旧 AK 禁用 | 改造中的过渡态 | v3182-2510 |
| **StrictMode** | 使用方改造完成 | 新部署严格托管；热升级/扩等场景自动降级为兼容模式 | 存量改造完成后的目标终态 | v3182-2515以后 |
| **initStrictMode** | 新建凭证即完成改造 | 任何场景都开启严格处理 | 新增收口凭证 | v320 |

**热升级场景下的兼容策略**
- **新部署项目**：根据 `restrict` 取值禁用原始通用能力，应用使用凭证进入定时轮换状态。
- **热升级项目**：原始凭证**不禁用**其通用能力，进入定时轮换状态；如需禁用老凭证，需通过观测日志在运维控制台灰度进行。
- **非 PCM 托管凭证**：一切照旧；若使用了 PCM SDK/CLI 但未被托管，将入参 initAK 返回让应用继续使用。

## 高可用与容错降级

**SDK 降级处理与业务影响**
PCM 设计了完善的高可用与容错逻辑，具体降级行为及业务影响如下：

| 场景 | SDK 行为 | 业务影响 |
| --- | --- | --- |
| 新部署时 PCM Core 还未 ready | 将入参作为返回 | 无影响（Core 未禁用老 AK） |
| 运行时 PCM Core 挂了 | 返回上次获取的老凭证（未在窗口期末尾） | 无影响 |
| 产品独立升级，PCM 未 ready | 将入参作为返回 | 无影响 |
| PCM 和应用都挂了需重拉（SDK 缓存未丢失） | 返回上次获取的老凭证 | 无影响 |
| PCM 和应用都挂了需重拉（SDK 缓存丢失） | **需先恢复 PCM 或使用老凭证应急脚本** | **业务中断** |

**核心组件关键特性**
- **PCM SDK / CLI（凭证获取端）**：支持**多级缓存**（本地内存、磁盘均有缓存）；具备**容错降级**能力，初始化异常时返回入参凭证，或返回最近一次获取的缓存凭证。
- **PCM Core（缓存中间网关）**：支持**本地缓存与定时同步**，减少直接访问 Controller 的频率；具备**缓存隔离**与**降级保护**，Core 宕机时 SDK 仍可返回上次获取的老凭证；作为中间层有效**缓解 Controller 压力**。
- **PCM Controller（策略中心）**：负责**凭证队列管理**与**模式管控**；支持**松→紧变更不自动生效**（需人工处理防误操作）；支持**灰度禁用**老凭证；提供**白屏管控（PKM）**、**日志查询关联**及凭证**状态管理**能力。

## 运维与辅助工具

**网关日志查询工具**
- **SLS 访问凭证配置**：配置中未自动适配 PCM 轮转，需要直接填写 PCM 轮转后的 AK。请通过 PCM 控制台手动获取派生 AK，并填入配置文件的 `access_key_id` 和 `access_key_secret` 中。
- **运行模式**：
  - `scan`：全量扫描网关中底表 AK 的调用记录。扫描记录默认自动存储在相对路径的 `output/scan_result_{时间戳}.csv` 文件中（输出路径和格式可在配置文件的 `output` 节点自定义）。
  - `query`：关键字查询模式，支持通过网关代码和关键字（如事件 ID）查询日志详细信息。

**底表 AK 黑屏操作工具（容器脚本）**
该工具通常用于在容器内执行脚本调用服务接口，是应急处置的重要手段。
- **环境变量配置**：运行前请确保已在系统环境中正确配置 `pcm_ctrl_domain`（PCM Controller 域名）和 `pcm_rs`（生成请求签名）这两个环境变量，否则会提示“错误: 缺少环境变量”。
- **支持的操作指令**：
  - `enable`：启用指定的 AK（必须配合 `--ak` 参数使用）。
  - `disable`：禁用指定的 AK（必须配合 `--ak` 参数使用）。
  - `enable-all`：获取并启用全部底表 AK。
  - `disable-all`：获取并禁用全部底表 AK。
  - `query`：通过账号 ID 查询对应的 AK（必须配合 `--account-id` 参数使用）。

## 应急处置与故障排查

**应急操作优先级与原则**
应急操作优先建议控制台白屏操作。当白屏无法访问时，采用在容器中执行脚本（调用服务接口/黑屏工具）；当容器无法访问时，直接在数据库中执行 SQL。
整体优先级为：**控制台白屏 > 调用接口（容器脚本） > 数据库执行 SQL**。

**启用已禁用的 initAK**
*适用场景：确认因为某把 AK 被禁用而影响业务。*
- **白屏操作**：通过 PCM 控制台的 initAK 管理功能查询特定 AK，并在操作中启用该 AK。
- **调用接口**：当白屏不可用时，在容器中执行脚本调用接口（参考 工具 或使用黑屏工具的 `enable` 指令）。
- **数据库操作**：当白屏、容器均不可用时，进入 UMMAK 数据库（service：`baseService-umm-ak`，db实例：`ummak`，数据库：`ummak`），执行以下 SQL：
  ```sql
  update accesskey_table set enabled_flag=1 where access_id = {akid};
  ```

**启用全量底表 AK**
*适用场景：环境内存在被底表 AK 禁用而影响业务，涉及多把底表 AK 或无法确认具体某把底表 AK。注意：暂不支持通过白屏解禁全量 AK。*
- **调用接口**：在容器中执行脚本（参考 工具 或使用黑屏工具的 `enable-all` 指令）。
- **数据库操作**：当容器不可访问时采用。
  1. 先获取全量底表 AK。进入 `clm_db` 实例的 `pcm_db` 数据库（service：`certificate-lifecycle-manager-server`），检索已禁用的 initAK：
     ```sql
     use pcm_db;
     select access_key_id from init_ak_info where umm_ak_status = 0;
     ```
  2. 在 UMMAK 数据库（`ummak`）中启用全量底表 AK，将 `access_id` 替换为上一步检索到的信息：
     ```sql
     update accesskey_table set enabled_flag=1 where access_id in ('ak1','ak2','ak3');
     ```

**启用派生 AK 及注意事项**
*适用场景：确认某把派生 AK 被禁用影响业务。*
- **白屏操作**：白屏支持查询派生 AK，查询后可通过启用操作恢复。
  > **注意事项**：每个派生队列中通过白屏仅可以查询最近 14 把派生 AK。如果超过 14 把，会在 ummak 侧执行删除操作，但 pcm 数据库会保留记录。若白屏未查询到，可能是 14 天前派生的 AK，需通过 pcm 数据库查询。
- **数据库操作**：进入 `clm_db` 实例的 `pcm_db` 数据库查询。然后在 UMMAK 中启用：
  - 如果 AK 存在，直接更新启用状态：
    ```sql
    update accesskey_table set enabled_flag=1,hidden_flag=0,deleted_flag=0 where access_id='qNNm2yFXF70Zy6Hx';
    ```
  - 如果 AK 已经删除，则重新创建 AK（需替换 access_id, access_key, user_id）：
    ```sql
    INSERT INTO `ummak`.`accesskey_table` (`access_id`, `access_key`, `user_id`) VALUES ('000cFXr3DBPZHxML11', 'XE5sP5dF6asjJsCkxL4QYifS7rRU11', '999999999');
    ```

**容量告警与派生失败处理（AK 数量超限）**
*适用场景：UMMAK 侧每个 uid 下最大支持 1000 把有效 AK，当达到 1000 把以后会出现派生失败的情况（更多细节可参考 容量问题数据处理）。*
- **查询特定 uid 下的 AK 数量**：
  ```sql
  SELECT user_id, COUNT(access_id) AS access_count FROM accesskey_table where user_id = '1000000047' GROUP BY user_id;
  ```
- **查询是否有 uid 下的 AK 超过 1000**：
  ```sql
  SELECT user_id, COUNT(access_id) AS access_count FROM accesskey_table GROUP BY user_id HAVING access_count >= 1000;
  ```
- **清理无用 AK**：分析出环境内已经无用的 AK，在 ummak 中置成删除状态：
  ```sql
  update accesskey_table set enabled_flag = 0, deleted_flag = 1 , modified_time = UNIX_TIMESTAMP() where access_id in (xxxxx);
  ```