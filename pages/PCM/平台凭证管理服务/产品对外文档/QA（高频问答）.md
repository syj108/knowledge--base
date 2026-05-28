# QA（高频问答）

### 接入PCM后出现大量报错日志，是否有影响？
2507版本PCM服务端尚未部署，部分适配了PCM的产品可能访问PCM报错，但因降级返回了原始底表AK，不影响业务调用。如果调用非常频繁，可能产生大量错误日志。
此外，部分产品升级至3186-2510及以上版本，但 baseServiceAll 未升级，可能同样出现以上问题。

### 如何判断底表AK是否禁用？
请参考 [《PCM运维手册》](https://alidocs.dingtalk.com/i/nodes/amweZ92PV6DbOdgzUK4on0qD8xEKBD6p?utm_scene=team_space&iframeQuery=anchorId%3Duu_mo8cms9ciyzk8jo83x) 进行查询。

### 如何判断派生AK是否禁用？
当前输出版本（3186、320）默认均不禁用派生AK。

### 接入PCM对时间敏感服务是否有影响？
接入PCM后可能导致部分时间敏感服务延迟加大，且网络可能出现延迟。针对时间敏感服务，增加了超时策略：
在 `1.13-SNAPSHOT`（20250908）版本中，支持 `PCM_TASK_DELAY` 环境变量，用于设置访问PCM的最大超时时间，单位为毫秒（ms）。默认值为 1000ms（即1s）。