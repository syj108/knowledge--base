# QA（高频问答）

**Q：遇到访问报错时，如何判断是否由PCM禁用AK导致？**

A：当遇到访问报错且怀疑是PCM禁用AK导致时，请优先通过拦截日志进行判定。具体步骤如下：
1. 提取拦截日志中的请求AK。
2. 通过PCM服务查询该AK的状态。
3. 如果查询结果显示AK已被禁用，则确认为此原因。此时需采用应急处置方案进行处置，并反馈研发侧排查具体原因。

**Q：各类网关在AK被禁用时的拦截日志特征及AK提取方法是什么？**

A：不同网关的拦截日志特征和AK提取字段有所不同，常见网关的判定特征及提取方法如下表所示：

| 网关类型 | 拦截日志特征 | AK提取字段 |
| :--- | :--- | :--- |
| **OSS** | `"error_code": "InvalidAccessKeyId"`<br>`"status": "403"` | `access_id` |
| **SLS_INNER** | `"Status": "401"` | `AccessKeyId` |
| **SLS_PUB** | `"Status": "401"`<br>`"ErrorCode": "Unauthorized"`<br>`"ErrorMsg": "AccessKeyId is disabled: <AK>"` | `AccessKeyId` |
| **ASAPI** | `"errorCode": "asapi.server.request.parameter.accesskeyid.error"`<br>`"errorMessage": "The specified AccessKey ID (<AK>) is invalid. Details: (The Access Key is disabled.)."` | `accessKeyId` <br>或 `parameters` 中的 `AccessKeyId` |

**Q：确认AK被PCM禁用后，标准的处置流程是什么？**

A：确认AK被禁用后，请立即采取以下措施：
1. **应急处置**：按照 应急处置方案 进行紧急恢复或业务切换，优先保障业务可用性。
2. **原因排查**：将禁用事件及相关AK信息反馈至研发侧，排查AK被禁用的根本原因，避免问题再次发生。