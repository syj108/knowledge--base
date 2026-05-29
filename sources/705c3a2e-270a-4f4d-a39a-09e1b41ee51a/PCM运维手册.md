# PCM运维手册

# 基本功能介绍

## PCM 服务位置

所属产品：`baseServiceAll`

部署集群：`StandardCloudCluster-A-xx`

所属service：`platform-credential-management`

核心组件：`PCM Core` `PCM Controller`

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/a12d813d-dfee-4508-aafa-c8c4ae6bc801.png)

## PCM控制台

ASO—> 安全管理 —> 账户安全 —> 平台凭据管理PCM

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/3f1e2933-e9cd-4f84-9344-4984335fc421.png)

## 底表AK管理

1.  可查询底表AK禁用状态
    
2.  启用底表AK
    

未提供白屏底表AK禁用能力，底表AK禁用请详见变更文档

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/1343e9fa-f653-4b8a-9ff5-3e003d77eb0e.png)

## 派生AK管理

### 派生AK状态查询

### 手动创建派生AK

**适用场景：当某个应用需要使用临时ak登录或者使用的initAK被禁用时，可以创建临时ak使用。**

*   步骤一：进入派生AK管理标签页，点击创建临时AK按钮
    

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/dad710b4-9011-4e5c-a529-f4460748de57.png)

*   步骤二：输入申请者、initAKID、有效天数、申请派生AK原因等相关信息创建临时ak
    

注意：

（1）initAKID是托管到pcm的基线或底表ak（要与所使用账号的原始ak对应）

（2）申请者ID即为IAMID是服务的身份标识（常规为集群 +  ： +  sr拼接而成，如：StandardCloudCluster-A-20250906-00bf:PcmController，这里如果系统中提示已经存在了，那么，可以在后面拼接任意一些字符串）

（3）ak类型默认使用临时类型

（4）有效天数范围限制在1~365天

（5）申请者类型分为：ApsaraStackProduct、Other

（6）CloudID、ProductName、ClusterName、ServiceName分别为使用该ak的应用归属的CloudID、产品名称、集群名称、service名称（虽然不是必填，能准确填写请准确填写，以便于更准确的判断该临时ak使用方）

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/66a153fd-c7d3-4df5-99f2-c38acc29d6b3.png)

示例：

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/1f5d718f-296c-4768-b666-a1882a5dd11a.png)

*   步骤三：复制ak、sk保存使用
    

注意：该ak对应的sk明文只会在创建成功后弹窗内展示，关闭弹窗后系统内不再显示（创建成功后请立即复制保存，如果不慎关闭弹窗，则需要重新创建临时ak，系统不对外提供sk明文信息能力）

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/Lk3lbmbxZoXdJOm9/img/3287f4e7-8607-4d2a-9137-08d6db0b5650.png)

示例：

{"accessKeyId":"ZbuIneIC04TElIFW","accessKeySecret":"cnyDzeHzmZWTGcs7ZLbZEHzagQj9jn"}

accessKeyId对应时ak、accessKeySecret对应sk

## AK申请详情

适用场景：用于查看派生AK申请记录

认证状态失败，仅表示IAMID不规范，但不会对申请结果有任何影响

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/cc32c5c0-1e19-4dbb-b486-d37fe586f7ed.png)

轮转状态已停止

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/84ef500a-b13e-4394-9392-a7753e1a3993.png)

1.   iamid中有`CLOSE_AUTO_ROTATE` 状态，表示该队列默认不轮转
    
2.  使用该产品的队列，有产品未及时更新    [《平台凭证管理服务（PCM）介绍 》](https://alidocs.dingtalk.com/i/nodes/r1R7q3QmWew5lo02fZRn00oKJxkXOEP2?utm_scene=team_space&iframeQuery=anchorId%3Duu_mo8et3bkdnbpoxrkv3)
    
3.  使用该队列的产品中，有产品仍在第7把ak[《平台凭证管理服务（PCM）介绍 》](https://alidocs.dingtalk.com/i/nodes/r1R7q3QmWew5lo02fZRn00oKJxkXOEP2?utm_scene=team_space&iframeQuery=anchorId%3Duu_mo8et3bliy39hgdhkpq)
    

## 日志相关

### AK申请日志

**说明：记录每个IAMID申请派生AK记录，通过pcm-core获取，pcm-core中针对每个IAMID的底表****secretARN的缓存时间为12小时，对于一直在用派生ak的产品，理论上每12小时会有一条记录**

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/87b2b8e6-3b4c-43a2-92fd-558bb7185319.png)

### 平台AK访问日志

:::
当前不完整，可作为辅助查询手段
:::

说明：在网关侧记录使用底表AK的使用情况

例如：底表AK Khz7a1kmKMZDCBXj

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/2c526ecb-bf7c-43cf-9508-ca5a0ded8555.png)

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/NpQlK5jrYgYjBqDv/img/df2ecc48-852c-4984-9fbf-a36cfd14c704.png)

### PCM Core日志查看

*   说明：**pcm部署在两个docker上，日志排查需两个docker都去查询**
    

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1GXn45K9b3p9xqDQ/img/2d3da4e7-219d-4054-a60e-f8324f56a869.png)

##### 排查error日志，确定是否pcm-core报错返回 注意需查看两个docker

###### 如果有具体requestid，可直接查询对应日期的error日志

`grep -rn "0ae6084f17767043979091019e659c" /opt/tengine/logs/error.2026-04-20.log`

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1GXn45K9b3p9xqDQ/img/f2de5eb0-80a3-4e6f-b075-d74584829e1c.png)

###### 如果没有具体requestid，可根据akid、iamid和时间段的进行复合筛选，直接查询对应日期的error日志

`grep "eMG9sv4bKvToGKKR" /opt/tengine/logs/error.2026-04-20.log | grep "yundun-oem" | awk '$1 >= "2026/04/20" && $2 >= "23:59:57" && $2 <= "23:59:58"'`

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1GXn45K9b3p9xqDQ/img/94117c38-e835-4ba0-adcc-0629282bf185.png)

##### 排查access日志，确定是否pcm-core接收到请求 注意需查看两个docker

###### 如果有具体requestid，可直接查询对应日期的access日志

`grep -rn "0ae6084f17767043992011025e659c" /opt/tengine/logs/access.2026-04-20.log`

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1GXn45K9b3p9xqDQ/img/575ee2e1-5466-4b29-8cf3-1929c1ce16cf.png)

###### 如果没有具体requestid，可根据akid、iamid和时间段的进行复合筛选，直接查询对应日期的access日志

`grep -E '"time_local": "(20/Apr/2026:22:59|20/Apr/2026:03:0[0-1])' /opt/tengine/logs/access.2026-04-20.log | grep "UFskQ84ZitYgBacU"`

`grep "UFskQ84ZitYgBacU" /opt/tengine/logs/access.2026-04-20.log | grep -E '"time_local": "20/Apr/2026:23:59:5[8-9]'`

![image.png](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/1GXn45K9b3p9xqDQ/img/4a0cbcfc-e5f5-4f41-8867-7730213e2531.png)

###### access日志参数说明

| 参数名称 | 参数含义 |
| --- | --- |
| remote\_addr | 请求源地址 |
| Gateway-POP-Tunnel-ID | tunnel-id |
| X-Aliyun-Vpc-Id | vpc-id |
| remote\_port | 请求端口 |
| time\_local | 请求完成的时间 |
| request\_uri | 请求的uri，包含imaid、secretname、endpoint等信息 |
| request\_method | 请求方法 |
| status | http返回码 |
| http\_user\_agent | 请求代理客户端信息 |
| request\_time | tengine 收到请求到发完响应的总耗时 |
| SecretName | secretname，包含initakid和pcm\_endpoint信息 |
| IamId | 表示请求服务身份，对应sdk填写的appname，当http报错时可能会为空 |
| x\_acs\_bearer\_token | 请求发送jwt |
| x\_sdk\_client | pcm-sdk版本 |
| limit\_req\_status | 限流状态，未限流显示"PASSED"，限流显示"-" |
| eagleeye\_traceid | 即requestid，可根据此查询对应error\_log是否有错误日志 |

# 常见问题排查

[《PCM排查思路&常见问题》](https://alidocs.dingtalk.com/i/nodes/m9bN7RYPWdyrPBREckdQ5joEVZd1wyK0)

# 应急处置

[《PCM应急处置》](https://alidocs.dingtalk.com/i/nodes/MNDoBb60VLYDGNPytBomBqkPJlemrZQ3)

# 潜在风险