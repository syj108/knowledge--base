# QA（高频问答）

**Q：什么是DDoS基础防护？它是免费的吗？**
**A：** DDoS基础防护是一款为部分阿里云产品**免费提供**网络层、传输层防御能力的基础安全产品。它直接集成到云产品中，用于抵御常见的网络层、传输层DDoS攻击。

**Q：DDoS基础防护的防护能力是多少？支持关闭吗？**
**A：** DDoS基础防护提供的防护能力为 500 Mbps~5 Gbps，各云产品的具体防护能力请参见 [DDoS基础防护黑洞阈值](https://help.aliyun.com/zh/anti-ddos/basic-ddos-protection/product-overview/view-the-thresholds-that-trigger-blackhole-filtering-in-anti-ddos-origin-basic)。在遭受频繁攻击的情况下，平台会根据客户的历史攻击记录调整防护能力以确保整体稳定。该产品默认开启且**不支持关闭**。

**Q：DDoS基础防护能防御哪些类型的攻击？能防御CC攻击吗？**
**A：** DDoS基础防护可以防御一般常见的网络层、传输层攻击，例如UDP反射攻击、SYN/ACK Flood攻击等。但**不支持**抵御应用层攻击，例如HTTP Flood攻击和CC攻击。

**Q：流量清洗是如何触发的？会不会误清洗正常业务流量？**
**A：** DDoS基础防护在清洗判定中除了基于您设置的BPS/PPS清洗阈值外，还采用了AI智能分析的方法。它基于阿里云的大数据能力，自学习您的业务流量基线，并结合算法识别异常攻击。只有当AI智能分析检测到DDoS攻击，且请求流量达到您设置的BPS或PPS清洗阈值时，才会触发流量清洗，从而有效避免了使用固定阈值可能导致的误清洗（例如正常业务上涨波动超出固定清洗阈值引起的误清洗）。

**Q：什么是黑洞？触发黑洞后会怎样？**
**A：** 如果入方向流量超过防护能力（即黑洞阈值），为避免DDoS攻击对云产品产生更大损害，同时也避免单个云产品被攻击而影响其他资产正常运行，云产品会进入黑洞状态。进入黑洞后，阿里云会暂时屏蔽该云产品的互联网入方向流量。详细介绍请参见 [阿里云黑洞策略](https://help.aliyun.com/zh/anti-ddos/product-overview/blackhole-filtering-policy-of-alibaba-cloud)。

**Q：DDoS基础防护支持哪些云产品？**
**A：** 目前支持防护的云产品包括：ECS、SLB、EIP（包含绑定NAT网关的EIP）、IPv6网关、轻量服务器、WAF、GA、AnyCastEIP。

**Q：DDoS基础防护支持哪些地域？**
**A：** DDoS基础防护支持全球多个地域，具体包括：

| **区域** | **地域** |
| --- | --- |
| 亚太 | 泰国（曼谷）、菲律宾（马尼拉）、日本（东京）、印度尼西亚（雅加达）、马来西亚（吉隆坡）、韩国（首尔）、新加坡、中国香港、西南1（成都）、华南3（广州）、华南2（河源）、华南1（深圳）、华北6（乌兰察布）、华北5（呼和浩特）、华北3（张家口）、华北2（北京）、华北1（青岛）、华东6（福州-本地地域）、华东5（南京-本地地域）、华东2（上海）、华东1（杭州）、华东1（网商云） 、郑州（联通云）、华北 2（金融云） |
| 欧洲与美洲 | 英国（伦敦）、德国（法兰福克）、美国（弗吉尼亚）、美国（硅谷） |
| 中东 | 沙特（利雅得）、阿联酋（迪拜） |

**Q：如果DDoS基础防护无法满足业务需求，应该怎么办？**
**A：** 如果DDoS基础防护无法满足您的需求，您可以选择DDoS原生防护或DDoS高防等更高级别的防护产品。详细介绍请参见 [什么是DDoS原生防护](https://help.aliyun.com/zh/anti-ddos/anti-ddos-origin/product-overview/what-is-anti-ddos-origin)、[什么是DDoS高防](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/what-are-anti-ddos-pro-and-anti-ddos-premium) 以及 [如何选择DDoS防护产品](https://help.aliyun.com/zh/anti-ddos/product-overview/scenario-specific-anti-ddos-solutions)。