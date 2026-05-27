# DDoS基础防护

阿里云为部分云产品免费提供500 Mbps~5 Gbps的DDoS基础防护能力，以抵御常见的网络层、传输层DDoS攻击。本文介绍什么是DDoS基础防护。

## 产品介绍

DDoS基础防护是一款为部分阿里云产品免费提供网络层、传输层防御能力的基础安全产品，直接集成到云产品中，默认开启且不支持关闭。提供的DDoS防护能力为500 Mbps~5 Gbps，各云产品的具体防护能力，请参见[DDoS基础防护黑洞阈值](https://help.aliyun.com/zh/anti-ddos/basic-ddos-protection/product-overview/view-the-thresholds-that-trigger-blackhole-filtering-in-anti-ddos-origin-basic)。

**说明**

在遭受频繁攻击的情况下，平台会根据客户的历史攻击记录调整防护能力，以确保平台整体稳定。

在正常情况下，DDoS基础防护一般不会影响用户的正常访问，但鉴于攻击方式（例如HTTP\_Flood、SYN\_Flood，ACK\_Flood等）、攻击手段以及用户自身业务场景（例如超过平台、产品本身规格）可能会对访问造成影响。如果DDoS基础防护无法满足需求，您可以选择DDoS原生防护或DDoS高防等更高级别的防护产品。详细介绍，请参见[什么是DDoS原生防护](https://help.aliyun.com/zh/anti-ddos/anti-ddos-origin/product-overview/what-is-anti-ddos-origin)、[什么是DDoS高防](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/what-are-anti-ddos-pro-and-anti-ddos-premium)以及[如何选择DDoS防护产品](https://help.aliyun.com/zh/anti-ddos/product-overview/scenario-specific-anti-ddos-solutions)。

## 防护原理说明

DDoS基础防护会默认设置清洗阈值，也支持您手动设置清洗阈值，当触发流量清洗条件时，DDoS基础防护通过对所有来自互联网的流量进行过滤清洗，防御一般常见的网络层、传输层攻击，例如UDP反射攻击、SYN/ACK Flood攻击等，但DDoS基础防护不支持抵御应用层攻击，例如HTTP Flood攻击和CC攻击。

DDoS基础防护在清洗判定中除了基于您设置的BPS/PPS清洗阈值外，还采用了AI智能分析的方法，基于阿里云的大数据能力，自学习您的业务流量基线，并结合算法识别异常攻击。只有当AI智能分析检测到DDoS攻击，且请求流量达到您设置的BPS或PPS清洗阈值时，DDoS防护才会触发流量清洗，避免了使用固定阈值可能导致的误清洗（例如，正常业务上涨波动超出固定清洗阈值，引起误清洗）。

如果入方向流量超过防护能力（即黑洞阈值），为避免DDoS攻击对云产品产生更大损害，同时也避免单个云产品被DDoS攻击而影响其他资产正常运行，云产品会进入黑洞，即阿里云会暂时屏蔽云产品的互联网入方向流量。详细介绍，请参见[阿里云黑洞策略](https://help.aliyun.com/zh/anti-ddos/product-overview/blackhole-filtering-policy-of-alibaba-cloud)。

## 支持防护的云产品

ECS、SLB、EIP（包含绑定NAT网关的EIP）、IPv6网关、轻量服务器、WAF、GA、AnyCastEIP

## 支持的地域

DDoS基础防护支持的地域请参见下表。

| **区域** | **地域** |
| --- | --- |
| 亚太 | 泰国（曼谷）、菲律宾（马尼拉）、日本（东京）、印度尼西亚（雅加达）、马来西亚（吉隆坡）、韩国（首尔）、新加坡、中国香港、西南1（成都）、华南3（广州）、华南2（河源）、华南1（深圳）、华北6（乌兰察布）、华北5（呼和浩特）、华北3（张家口）、华北2（北京）、华北1（青岛）、华东6（福州-本地地域）、华东5（南京-本地地域）、华东2（上海）、华东1（杭州）、华东1（网商云） 、郑州（联通云）、华北 2（金融云） |
| 欧洲与美洲 | 英国（伦敦）、德国（法兰福克）、美国（弗吉尼亚）、美国（硅谷） |
| 中东 | 沙特（利雅得）、阿联酋（迪拜） |

## 术语介绍

*   网络层攻击：常见攻击类型包括UDP反射类攻击、大流量SYN、ACK Flood攻击，不符合IP协议的畸形报文。此类攻击以消耗服务器带宽资源从而达到拒绝服务的目的。
    
*   应用层攻击：常见攻击类型包括HTTP Flood攻击、CC攻击以及DNS Flood，是基于业务特征的消耗型攻击。此类攻击以消耗服务器处理性能从而达到拒绝服务的目的。
    

## 相关文档

*   各云产品所支持设置的最大清洗阈值取决于各云产品实例的规格。具体介绍，请参见[云产品规格与清洗阈值](https://help.aliyun.com/zh/anti-ddos/basic-ddos-protection/product-overview/cloud-service-specification-and-cleaning-trigger-value)。
    
*   如何设置清洗阈值，请参见[设置流量清洗阈值](https://help.aliyun.com/zh/anti-ddos/basic-ddos-protection/user-guide/blackhole-filtering-thresholds-and-blackhole-filtering-duration-in-cloud-web-hosting)。