# QA（高频问答）

[《PCM排查思路&常见问题》](https://alidocs.dingtalk.com/i/nodes/m9bN7RYPWdyrPBREckdQ5joEVZd1wyK0)

## 底表AK管理常见疑问

未提供白屏底表AK禁用能力，底表AK禁用请详见变更文档。

## 派生AK管理常见疑问

### 手动创建派生AK适用场景

当某个应用需要使用临时ak登录或者使用的initAK被禁用时，可以创建临时ak使用。

### 创建临时AK注意事项

1. initAKID是托管到pcm的基线或底表ak（要与所使用账号的原始ak对应）
2. 申请者ID即为IAMID是服务的身份标识（常规为集群 + ： + sr拼接而成，如：StandardCloudCluster-A-20250906-00bf:PcmController，这里如果系统中提示已经存在了，那么，可以在后面拼接任意一些字符串）
3. ak类型默认使用临时类型
4. 有效天数范围限制在1~365天
5. 申请者类型分为：ApsaraStackProduct、Other
6. CloudID、ProductName、ClusterName、ServiceName分别为使用该ak的应用归属的CloudID、产品名称、集群名称、service名称（虽然不是必填，能准确填写请准确填写，以便于更准确的判断该临时ak使用方）
7. 该ak对应的sk明文只会在创建成功后弹窗内展示，关闭弹窗后系统内不再显示（创建成功后请立即复制保存，如果不慎关闭弹窗，则需要重新创建临时ak，系统不对外提供sk明文信息能力）

## AK申请详情常见疑问

### AK申请详情适用场景

用于查看派生AK申请记录。

### 认证状态失败

认证状态失败，仅表示IAMID不规范，但不会对申请结果有任何影响。

### 轮转状态已停止

1. iamid中有`CLOSE_AUTO_ROTATE` 状态，表示该队列默认不轮转
2. 使用该产品的队列，有产品未及时更新 [《[[PCM/平台凭证管理服务/index|平台凭证管理服务]]（PCM）介绍 》](https://alidocs.dingtalk.com/i/nodes/r1R7q3QmWew5lo02fZRn00oKJxkXOEP2?utm_scene=team_space&iframeQuery=anchorId%3Duu_mo8et3bkdnbpoxrkv3)
3. 使用该队列的产品中，有产品仍在第7把ak[《平台凭证管理服务（PCM）介绍 》](https://alidocs.dingtalk.com/i/nodes/r1R7q3QmWew5lo02fZRn00oKJxkXOEP2?utm_scene=team_space&iframeQuery=anchorId%3Duu_mo8et3bliy39hgdhkpq)

## 日志相关常见疑问

### AK申请日志记录频率

记录每个IAMID申请派生AK记录，通过pcm-core获取，pcm-core中针对每个IAMID的底表secretARN的缓存时间为12小时，对于一直在用派生ak的产品，理论上每12小时会有一条记录。

### 平台AK访问日志说明

当前不完整，可作为辅助查询手段。

### PCM Core日志排查注意事项

pcm部署在两个docker上，日志排查需两个docker都去查询。