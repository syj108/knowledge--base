# QA（高频问答）

应急操作优先建议控制台白屏操作，当白屏无法访问时，采用在容器中执行脚本（调用服务接口），当容器无法访问时，直接在数据库中执行 SQL。

**优先级：控制台白屏 > 调用接口（容器脚本） > 数据库执行 SQL**

## 常见问题排查总览

```mermaid
graph TD
    Start[运行时问题现象] --> P1[AK调用网关被拦截]
    Start --> P2[客户端产生大量PCM相关WARN日志]
    Start --> P3[Controller磁盘打满/大量日志]
    Start --> P4[Go SDK日志文件持续增长]
    Start --> P5[RPM包安装失败]
    Start --> P6[Java应用线程阻塞]
    Start --> P7[CLI工具报错ResponseParseFailure]

    P1 --> S1[核心排查场景]
    P2 --> S2[客户端WARN日志处理]
    P3 --> S3[Controller磁盘清理]
    P4 --> S4[Go SDK日志处理]
    P5 --> S5[RPM包冲突处理]
    P6 --> S6[Java线程阻塞处理]
    P7 --> S7[CLI工具报错处理]
```

## AK 调用网关被拦截如何排查？

**现象**：产品调用网关时报 AK 被禁用/AK 无效/AK 不存在。这是 PCM 接入后最核心的排查场景。

**排查步骤**：

1. **判断 AK 类型**：从网关日志中取出被拦截的 AK ID，在控制台查询是底表 AK 还是派生 AK。
   - **底表 AK**：直接通过控制台查询。
     ![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oJGq75k4LxgGzlAK/img/2fd7cf34-2df5-4f17-9e5e-693a440fa80f.png)
   - **派生 AK**：控制台仅可查询每个队列最近 14 把派生 AK。
     ![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oJGq75k4LxgGzlAK/img/38f3918a-5682-46ea-bdfd-ef66d94b16e4.png)
     若未查到，可进入 `clm_db` 实例的 `pcm_db` 数据库查询：
     ```sql
     use pcm_db;
     select * from ak_info where access_key_id='****';
     ```
2. **分支一：底表 AK 被拦截**
   - **核心判断**：产品在使用底表 AK，说明 SDK 没有成功获取派生 AK，走了降级逻辑，或使用底表 AK 未适配。排查方向是**为什么 SDK 没拿到派生 AK**。
   - **处理步骤**：
     - **先恢复**：在 PCM 控制台启用该底表 AK 恢复业务。
     - **查 SDK 日志 code**：确认是哪种降级场景（参考下方“Core 错误码如何快速定位？”）。
3. **分支二：派生 AK 被拦截**
   - **核心判断**：产品已使用派生 AK，但该 AK 已被轮转禁用。最可能原因为仅获取一次，未持续轮转。排查方向是**为什么产品没有及时更新到最新的派生 AK**。
   - **处理步骤**：
     - 通常重启服务会刷新 AK 导致可用，然后停止该队列的轮转。
       ![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/oJGq75k4LxgGzlAK/img/d4de6c7b-0ccf-4072-97a2-f3076bedea7c.png)
     - 若无法重启，需手动启用 AK（参考 [《PCM应急处置》](https://alidocs.dingtalk.com/i/nodes/MNDoBb60VLYDGNPytBomBqkPJlemrZQ3?utm_scene=team_space&iframeQuery=anchorId%3Duu_mogmd4kosy5jbbqysjf)）。如果有 SDK 报错，参见“Core 错误码如何快速定位？”。

## 客户端产生大量 PCM 相关 WARN 日志是否有影响？

**现象**：产品日志中大量 `Failed to refresh credential, pcm server is xxx`。

**解答**：
这类 WARN 日志**不影响业务**（SDK 已降级返回原始凭证），主要影响是客户端告警监控被触发。当环境中 PCM 服务（Core）未部署或不可达时，SDK 无法生成缓存，仍会按配置的间隔持续尝试连接，每次失败产生 WARN 级别日志。

## PCM Controller 磁盘打满或产生大量日志如何处理？

**现象**：Controller 日志目录 `/home/admin/pcm_controller/logs/api/logs/` 下出现超大文件，磁盘空间不足。

**处理步骤**：
1. 确认磁盘使用情况：`df -h`
2. 查看日志目录大小：`du -sh /home/admin/pcm_controller/logs/api/logs/`
3. 清理历史日志文件（保留最近日志）。
4. 排查产生大量日志的原因：
   - 是否有大量异常请求持续打到 Controller。
   - 是否有定时任务异常导致循环报错。
5. 确认日志轮转配置是否正常。

## Go SDK 日志文件持续增长如何处理？

**现象**：Go SDK 产生的日志文件不断增大，未按预期轮转。

**原因**：Go SDK 在 2512 之前版本存在日志轮转 Bug。

**解决方案**：
- **彻底解决**：升级 Go SDK 至 2512 及以上版本。
- **临时处理**：`> logfile` 截断日志文件（注意：不要 `rm` 正在写入的文件）。

## Python SDK RPM 包安装失败如何处理？

**现象**：安装 `pcm-python2-sdk-rpm-with-no-six` 报错，关键字包含 `pytz/zoneinfo`、`cpio: File from package already exists as a directory`。

**原因**：系统已有 `/home/tops/lib/python2.7/site-packages/pytz/` 目录，与 RPM 包冲突。

**解决方式**：
```bash
mv /home/tops/lib/python2.7/site-packages/pytz /home/tops/lib/python2.7/site-packages/pytz_bak
sudo yum install pcm-python2-sdk-rpm-with-no-six -y
```

## Java 应用线程阻塞如何处理？

**现象**：线程 dump 中出现阻塞堆栈：
```plaintext
java.lang.Thread.State: BLOCKED (on object monitor)
  at sun.security.provider.NativePRNG$RandomIO.implNextBytes(NativePRNG.java:543)
  at ...PcmSecretCredentialManager.persistCredentials(...)
```

**原因**：SDK 默认使用 `/dev/random` 阻塞模式获取随机数，系统熵值低（< 100）时线程被卡住。

**解决方案**：
- **彻底解决**：升级 SDK 至 `credprovider.plugin >= 1.0.8`。
- **临时规避**：JVM 参数添加 `-Djava.security.egd=file:/dev/./urandom`。

## CLI 工具报错 ResponseParseFailure 如何处理？

**现象**：返回 `{"code": "ResponseParseFailure", "data": "", "message": "xxxxxxx"}`。

**原因**：`pcm_endpoint` 地址不对，该地址响应 200 但格式非预期，CLI 解析失败且未走降级。

**排查与解决**：确认 CLI 的 `pcm_endpoint` 指向正确的 PCM Core 地址，手动 curl 确认返回格式。后续版本已优化解析异常的降级处理。

## Core 错误码如何快速定位？

当排查过程中从 SDK 报错信息中拿到了具体错误码，可按以下说明辅助定位：

### HTTP 400 — 请求参数错误

| 返回 Msg | 报错原因 | 排查方向 |
| --- | --- | --- |
| `SecretName or x_acs_bearer_token is nil` | SecretName 或 token 为空 | SDK 侧 initakid 和 pcm_endpoint 是否正确 |
| `SecretName parse fail, SecretName:xxxx` | SecretName 格式错误 | appName 是否正确以 `:` 分隔 |
| `The access key (AK) is not administered by the PCM service, AK:xxxx` | akid 非底表 AK | initakid 是否填写正确的底表 akid |
| `genJwtKey fail` | 计算 token_key 失败 | Core 内部问题，与 SDK 无关 |
| `Error in AK rotation led to unsuccessful request to the controller...` | 请求 Controller 派生失败 | 1. 派生 AK 容量达上限<br>2. IAMID 非法且关闭了非标开关 |

### HTTP 403 — 认证失败

| 返回 Msg | 报错原因 | 排查方向 |
| --- | --- | --- |
| `reason: signature error` | 签名验证失败 | 见下方 signature error 排查 |
| `reason: "nbf" claim not valid until` | 时钟不同步 | 见下方 nbf 时钟偏差 |
| `token_arn not same with arn...` | ARN 不一致 | SDK 内部问题，基本不出现 |

### signature error 排查

```mermaid
graph TD
    S["Core返回403<br/>reason: signature error"] --> INFO["签名key = sha256(initSK || IKM)<br/>IKM = endpoint去https://→取域名→去掉-regionid"]

    INFO --> Q1{报错范围？}

    Q1 -->|单元region报错<br/>中心region正常| R1["99%是regionid传错<br/>导致两端IKM计算不同"]
    Q1 -->|中心和单元同时报错| R2[initAK/initSK值本身错误]
    Q1 -->|个别产品报错| R3["pcm_endpoint配置错误<br/>（域名拼写/多了路径）"]
    Q1 -->|都确认正确仍403| R4["环境中SK是加密存储的<br/>产品未解密就传给了SDK"]
```

- **nbf 时钟偏差**：SDK 生成 JWT 的 `nbf` 使用客户端 `time.Now()`。版本 3186-2605 / 320-2607 后已增加 5 分钟容错。仍出现则检查 SDK 所在机器 NTP 同步状态。
- **SK 加密未解密导致 403**：部分环境中底表 SK 是加密存储的。产品未解密就传给 SDK → 签名 key 两端不一致 → 必然 403。确认产品侧调用 SDK 前已解密 SK。

### HTTP 502 — 限流触发

大概率限流触发。
**限流排查**：
1. 检查 access.log 中 `limit_req_status` 字段。
2. `tsar -l -i 1 --nginx` 查看 QPS。
3. 调整限流配置：`/services/platform-credential-management/user/pcm_conf/pcm_core.json`。
4. 阈值参考（单核）：x86=200r/s, aarch64=189r/s, sw64=80r/s。

## 接入 PCM 后出现大量报错日志，是否有影响？

**现象**：接入 PCM 后出现大量报错日志。

**解答**：
- 2507 版本 PCM 服务端尚未部署时，部分适配了 PCM 的产品可能访问 PCM 报错，但因降级返回了原始底表 AK，**不影响业务调用**。如果调用非常频繁，可能产生大量错误日志。
- 部分产品升级至 3186-2510 及以上版本，但 baseServiceAll 未升级，可能同样出现以上问题。

## 如何判断底表 AK 是否禁用？

**解答**：可通过运维手册 [《PCM运维手册》](https://alidocs.dingtalk.com/i/nodes/amweZ92PV6DbOdgzUK4on0qD8xEKBD6p?utm_scene=team_space&iframeQuery=anchorId%3Duu_mo8cms9ciyzk8jo83x) 中的方法进行查询。

## 如何判断派生 AK 是否禁用？

**解答**：当前输出版本（3186、320）默认均不禁用派生 AK。

## 时间敏感服务接入 PCM 后延迟加大如何处理？

**现象**：接入 PCM 后可能导致部分时间敏感服务延迟加大，且网络可能出现延迟。

**解答**：
对于时间敏感服务，增加了 1s 超时策略。支持通过 `PCM_TASK_DELAY` 环境变量设置访问 PCM 的最大超时时间（单位：ms）。
- **默认值**：1000ms（即 1s）。
- **适用版本**：1.13-SNAPSHOT (20250908) 及以上。

## 如何启用某个已经禁用的 initAK？

**适用场景**：确认因为某把 AK 被禁用而影响业务。

### 白屏操作

通过 PCM 控制台的 initAK 管理功能查询特定 AK，并在操作中启用该 AK。

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/71c6499e-968c-4b82-aa25-e3093bc7d9b7.png)

### 调用接口（容器中执行脚本）

当白屏不可用时，采用此方案。通过底表 AK 黑屏操作工具调用接口实现。

- **运行位置**：进入 PcmController 容器（Product: baseServiceAll → sn: platform-credential-management → sr：PcmController#），在任意一台容器操作即可。
- **执行命令**：
  ```bash
  # 启用单个 AK
  python3 manage_ak_status.py enable --ak <AK_ID>
  ```

> 工具源码及详细说明参考：[《工具》](https://alidocs.dingtalk.com/i/nodes/7NkDwLng8Za7QYkeHxdzN0A7JKMEvZBY?utm_scene=team_space&iframeQuery=anchorId%3Duu_mocpgly2iwborsrkk7e)

### 数据库操作

当白屏、容器均不可用时，采用此方案。

1. AK 状态存储在 UMMAK 数据库中，进入 UMMAK 数据库：
   - service：baseService-umm-ak
   - db实例：ummak
   - 数据库：ummak
2. 执行 SQL：
```sql
update accesskey_table set enabled_flag=1 where access_id = {akid};
```

## 如何启用全量底表 AK？

**适用场景**：环境内存在被底表 AK 禁用而影响业务，涉及多把底表 AK 或无法确认某把底表 AK，可采用启用全量底表 AK。

**注意**：暂不支持通过白屏解禁全量 AK。

### 调用接口（容器中执行脚本）

当白屏不可用时，采用此方案。通过底表 AK 黑屏操作工具调用接口实现。

- **运行位置**：进入 PcmController 容器（Product: baseServiceAll → sn: platform-credential-management → sr：PcmController#），在任意一台容器操作即可。
- **执行命令**：
  ```bash
  # 启用全部底表 AK
  python3 manage_ak_status.py enable-all
  ```

> 工具源码及详细说明参考：[《工具》](https://alidocs.dingtalk.com/i/nodes/7NkDwLng8Za7QYkeHxdzN0A7JKMEvZBY?utm_scene=team_space&iframeQuery=anchorId%3Duu_mocpgly2iwborsrkk7e)

### 数据库操作

当容器不可访问时，采用此方案。

1. 先获取全量底表 AK，PCM 托管的底表 AK 存储在 clm_db 实例的 pcm 数据库中：
   - service：certificate-lifecycle-manager-server
   - db实例：clm_db
   - 数据库：pcm_db
   - 进入 clm_db 实例数据库后切换到 pcm_db：
     ```sql
     use pcm_db;
     ```
   - 检索已经禁用的 initAK：
     ```sql
     select access_key_id from init_ak_info where umm_ak_status = 0;
     ```
2. 在 UMMAK 中启用全量底表 AK：
   - service：baseService-umm-ak
   - db实例：ummak
   - 数据库：ummak
   - 执行 SQL（执行前，将 `access_id` 字段参数改成步骤一中检索到的底表 AK 信息）：
     ```sql
     update accesskey_table set enabled_flag=1 where access_id in ('qNNm2yFXF70Zy6Hx','qNNm2yFXF70Zy6Hx2','qNNm2yFXF70Zy6Hx3');
     ```

## 如何启用派生 AK？

**适用场景**：确认某把派生 AK 被禁用影响业务。

### 白屏操作

白屏支持查询派生 AK，查询后可通过启用操作恢复。

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/610ebe22-d389-44fe-a31b-cce2b4c221c2.png)

> **注意事项**：
> 每个派生队列中通过白屏仅可以查询最近 14 把派生 AK，如果超过 14 把 AK 后，会在 ummak 侧执行删除操作，但 pcm 数据库会保留派生 AK 记录。当通过白屏未查询到该 AK，有可能是 14 天前派生的 AK，可通过 pcm 数据库进行查询。

### 数据库操作

1. 查询派生 AK：
   - service：certificate-lifecycle-manager-server
   - db实例：clm_db
   - 数据库：pcm_db
   - 进入 clm_db 实例数据库后切换到 pcm_db：
     ```sql
     use pcm_db;
     ```
2. 在 UMMAK 中启用：
   - 如果存在，直接更新启用状态：
     ```sql
     update accesskey_table set enabled_flag=1,hidden_flag=0,deleted_flag=0 where access_id='qNNm2yFXF70Zy6Hx';
     ```
   - 如果已经删除，创建 AK（说明：`access_id` 为 akid，`access_key` 为 sk，`user_id` 为账号）：
     ```sql
     INSERT INTO `ummak`.`accesskey_table` (`access_id`, `access_key`, `user_id`) VALUES ('000cFXr3DBPZHxML11', 'XE5sP5dF6asjJsCkxL4QYifS7rRU11', '999999999');
     ```

## 如何处理容量告警场景？

**适用场景**：UMMAK 侧每个 uid 下最大 1000 把有效 AK，当达到 1000 把以后会出现派生失败的情况（家里测试环境出现过，现场暂未出现）。

参考：[《容量问题数据处理》](https://alidocs.dingtalk.com/i/nodes/QG53mjyd800agdlKHbek2aXQ86zbX04v)

### 查询

1. 检查特定 uid 下（如 1000000047）的 AK 数量：
   ```sql
   SELECT user_id, COUNT(access_id) AS access_count FROM accesskey_table where user_id = '1000000047' GROUP BY user_id;
   ```
2. 查询是否有 uid 下的 AK 超过 1000：
   ```sql
   SELECT user_id, COUNT(access_id) AS access_count FROM accesskey_table GROUP BY user_id HAVING access_count >= 1000;
   ```

### 清理

分析出环境内已经无用的 AK，在 ummak 中置成删除状态：

```sql
update accesskey_table set enabled_flag = 0, deleted_flag = 1 , modified_time = UNIX_TIMESTAMP() where access_id in (xxxxx);
```

## 如何查询网关日志中的 AK 使用情况？

**适用场景**：需要通过网关和事件 ID 查询日志详细信息，或者在网关日志中扫描底表 AK 的使用情况。

### 工具配置

将配置文件与 CLI 工具放在相同目录下。配置示例如下：

```yaml
# 服务端简化配置
sls:
  # 访问凭证（此处未自动适配pcm轮转，直接填 PCM 轮转后的 AK，通过pcm控制台手动获取派生AK）
  credentials:
    sls:   # test1000000004@aliyun.com 对应的派生AK                  
      access_key_id: "RONVzQyJJR2kRoLP" 
      access_key_secret: "hvZ8oi0vWJXjWERK9VVe3j3qm2IYwK" 
    defaultUser:  # aliyuntest 对应的派生ak           
      access_key_id: "beF7AyHhnIjY3eGy"  
      access_key_secret: "2R838QLvk0wjkGxL9mTPMlL1xWFX4q"

  # Endpoint 配置
  inner_endpoint: "data.cn-wulan-env17e-d01.sls.inter.env17e.shuguang.com"        # slsinner
  pub_endpoint: "data.cn-wulan-env17e-d01.sls-pub.inter.env17e.shuguang.com"      # slspub

scan:
  hours_back: 10       # 扫描周期
  page_size: 1000      # 默认 可不修改
  max_workers: 20      # 默认 可不修改 
  auto_create_index: false  # 发现无索引时是否自动创建（true=自动创建，false=跳过）

output:
  path: "./output"
  format: "all"  # 可选: print, json, csv, all
```

### 上传与运行

将工具上传到 OPS1 服务运行（或可以解析 slsinner 的环境）。

### 使用指南

1. **根据事件 ID 查询使用 AK**
   ```bash
   ./main query --gateway <网关代码> --keyword "<事件ID或关键字>"
   ```
   *示例：`./main query --gateway OSS --keyword "tzRzgmefjFjXBC4C"`*

2. **遍历网关中底表 AK 调用记录**
   ```bash
   ./main scan
   ```
   扫描记录将自动存储在相对路径的 `output/scan_result_{时间戳}.csv`（或 json 等配置格式）中。

## PCM 存在哪些潜在风险与已知限制？

### 架构与机制风险
- **Core 限流基于 IP，存在误伤可能**：PCM Core 的限流策略基于客户端 IP。当同一台机器上运行多个产品组件，一个高频产品的请求可能耗尽该 IP 的限流配额，导致同 IP 下其他产品被连带返回 502。
- **链路增加延迟**：对时间敏感业务有影响。
- **半轮转模式首次获取失败导致后续持续异常**：部分产品采用半自动轮转模式（仅在启动时获取一次派生 AK，后续不再主动刷新）。如果该唯一一次获取请求恰好失败（Core 限流、网络抖动、服务未就绪），产品将持续使用底表 AK 或无有效凭据运行，且不会自动恢复。
- **底表禁用后 PCM 可用性和禁用状态联动**：底表 AK 被 PCM 禁用后，产品的凭据供给完全依赖 PCM 链路（Core + Controller）。对于本地有缓存的运行中服务暂时无影响，但重启的服务如果此时 PCM 不可用，将拿不到任何有效凭据（底表已禁、派生获取失败、本地无缓存），业务直接中断。

### 日志与排查限制
- **部分 SDK 未打印关键日志，排查困难**：Java WARN 过多，部分产品屏蔽了报错日志，无请求 PCM 的 requestid 等信息，增加排查难度。
- **SDK 超时日志毫秒数为 null**：未设置 `PCM_TASK_DELAY` 时默认 1s 超时，日志字段显示 null。已知日志格式问题，不影响功能。

### 存量旧版本已知问题
- **CLI 服务端返回异常不降级（ResponseParseFailure）**：2025-12-23 更新修复，旧版本 CLI 直接不可用。
- **Java SDK 线程阻塞（/dev/random 熵值问题）**：`credprovider.plugin >= 1.0.8` 修复，旧版本应用线程卡死。
- **Go SDK 日志文件不轮转**：SDK >= 2512 版本修复，旧版本磁盘打满。