# PCM应急处置

注意事项

:::
应急操作优先建议控制台白屏操作，当白屏无法访问时，采用在容器中执行脚本（调用服务借口），当容器无法访问时，直接在数据库中执行SQL

**控制台白屏 > 调用接口（容器脚本） > 数据库执行SQL**
:::

##  启用某个已经禁用的initAK

适用场景：确认因为某把AK被禁用而影响业务

### 白屏操作

     通过PCM控制台的 initAK 管理功能查询特定AK，并在操作中启用该AK

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/71c6499e-968c-4b82-aa25-e3093bc7d9b7.png)

### 调用接口（容器中执行脚本）

当白屏不可用时，采用此方案

[《工具》](https://alidocs.dingtalk.com/i/nodes/7NkDwLng8Za7QYkeHxdzN0A7JKMEvZBY?utm_scene=team_space&iframeQuery=anchorId%3Duu_mocpgly2iwborsrkk7e)

### 数据库中操作

当白屏、容器均不可用时，采用此方案

1.  AK状态存储在UMMAK 数据库中，进入 UMMAK 数据库
    
    service：baseService-umm-ak
    
    db实例：ummak
    
    数据库：ummak
    
2.  执行sql
    

```http
update accesskey_table set enabled_flag=1 where access_id = {akid};
```

##  启用全量底表AK

适用场景：环境内存在被底表AK禁用而影响业务，涉及多把底表AK或无法确认某把底表AK，可采用启用全量底表AK

**注意：暂不支持通过白屏解禁全量AK**

### 调用接口（容器汇中执行脚本）

[《工具》](https://alidocs.dingtalk.com/i/nodes/7NkDwLng8Za7QYkeHxdzN0A7JKMEvZBY?utm_scene=team_space&iframeQuery=anchorId%3Duu_mocpgly2iwborsrkk7e)

### 数据库

当容器不可访问时，采用此方案

1.  先获取全量底表AK，pcm托管的底表AK存储在 clm\_db 实例的 pcm 数据库中
    
    service：certificate-lifecycle-manager-server
    
    db实例：clm\_db
    
    数据库：pcm\_db
    
    1.  **进入clm\_db实例数据库后切换到pcm\_db**
        
        :::
        use pcm\_db;
        :::
        
    2.  **检索已经禁用的initAK**
        
    
    :::
    select access\_key\_id from init\_ak\_info where umm\_ak\_status = 0;
    :::
    
2.  禁用全量底表AK
    
    service：baseService-umm-ak
    
    db实例：ummak
    
    数据库：ummak
    
    *   执行sql：执行sql前，将access\_id字段参数改成步骤一种检索到的底表ak信息
        

:::
update accesskey\_table set enabled\_flag=1 where access\_id in ('qNNm2yFXF70Zy6Hx','qNNm2yFXF70Zy6Hx2','qNNm2yFXF70Zy6Hx3');
:::

##  启用派生AK

使用场景：确认某把派生AK被禁用 影响业务

### 白屏操作

白屏支持查询派生AK，查询后可通过启用操作恢复

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/610ebe22-d389-44fe-a31b-cce2b4c221c2.png)

### 数据库操作

:::
注意事项

每个派生队列中通过白屏仅可以查询最近14把派生AK，如果超过14把AK后，会在ummak侧执行删除操作，但pcm数据库会保留派生AK记录。

当通白屏未查询到该AK，有可能是14天前派生的ak，可通过pcm数据库进行查询
:::

查询派生AK

*   service：certificate-lifecycle-manager-server
    
*   db实例：clm\_db
    
*   数据库：pcm\_db
    

1.  **进入clm\_db实例数据库后切换到pcm\_db**
    
    ```http
    use pcm_db;
    ```
    
2.  在UMMAK中启用
    
    如果存在，直接更新启用状态
    
    ```http
    update accesskey_table set enabled_flag=1,hidden_flag=0,deleted_flag=0 where access_id='qNNm2yFXF70Zy6Hx';
    ```
    
    如果已经删除，创建ak：
    
    说明：
    
    access\_id：akid
    
    access\_key：sk
    
    user\_id：账号
    
    ```http
    INSERT INTO `ummak`.`accesskey_table` (`access_id`, `access_key`, `user_id`) VALUES ('000cFXr3DBPZHxML11', 'XE5sP5dF6asjJsCkxL4QYifS7rRU11', '999999999');
    ```
    
    ## 容量告警场景
    
    :::
    家里测试环境出现过，现场暂未出现
    :::
    
    [《容量问题数据处理》](https://alidocs.dingtalk.com/i/nodes/QG53mjyd800agdlKHbek2aXQ86zbX04v)
    
    UMMAK侧每个uid下最大1000把有效AK，当达到1000把以后会出现派生失败的情况
    
    ### 查询
    
    *   检查特定uid下(1000000047)的ak数量
        
    
    ```http
    MySQL [ummak]> SELECT user_id, COUNT(access_id) AS access_count FROM accesskey_table where user_id = '1000000047' GROUP BY user_id;
    +------------+--------------+
    | user_id    | access_count |
    +------------+--------------+
    | 1000000047 |           12 |
    +------------+--------------+
    ```
    *   查询是否有uid下的ak超过1000
        

```http
SELECT user_id, COUNT(access_id) AS access_count FROM accesskey_table GROUP BY user_id HAVING access_count >= 1000;
```

### 清理

分析出环境内已经无用的ak，在ummak中置成删除状态

```http
update accesskey_table set enabled_flag = 0, deleted_flag = 1 , modified_time = UNIX_TIMESTAMP() where access_id in (xxxxx);
```