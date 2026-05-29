# QA（高频问答）

应急操作优先建议控制台白屏操作，当白屏无法访问时，采用在容器中执行脚本（调用服务接口），当容器无法访问时，直接在数据库中执行 SQL。

**优先级：控制台白屏 > 调用接口（容器脚本） > 数据库执行 SQL**

## 如何启用某个已经禁用的 initAK？

**适用场景**：确认因为某把 AK 被禁用而影响业务。

### 白屏操作

通过 PCM 控制台的 initAK 管理功能查询特定 AK，并在操作中启用该 AK。

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/71c6499e-968c-4b82-aa25-e3093bc7d9b7.png)

### 调用接口（容器中执行脚本）

当白屏不可用时，采用此方案。

参考：[《工具》](https://alidocs.dingtalk.com/i/nodes/7NkDwLng8Za7QYkeHxdzN0A7JKMEvZBY?utm_scene=team_space&iframeQuery=anchorId%3Duu_mocpgly2iwborsrkk7e)

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

参考：[《工具》](https://alidocs.dingtalk.com/i/nodes/7NkDwLng8Za7QYkeHxdzN0A7JKMEvZBY?utm_scene=team_space&iframeQuery=anchorId%3Duu_mocpgly2iwborsrkk7e)

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
2. 在 UMMAK 中启用全量底表 AK（注：源文档此处标题误写为“禁用”，实为启用操作）：
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