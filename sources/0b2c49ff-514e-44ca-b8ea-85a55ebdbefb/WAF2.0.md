# WAF2.0

Web应用防火墙（Web Application Firewall，简称WAF）为您的网站或App业务提供一站式安全防护。WAF可以有效识别Web业务流量的恶意特征，在对流量清洗和过滤后，将正常、安全的流量返回给服务器，避免网站服务器被恶意入侵导致性能异常等问题，从而保障网站的业务安全和数据安全。

## 功能特性

| **功能类别** |  | **功能说明** |
| --- | --- | --- |
| 业务配置 |  | 支持对网站的HTTP、HTTPS流量进行安全防护。 |
| Web应用安全防护 | 常见Web应用攻击防护 | \- 防御OWASP常见威胁：[SQL](https://www.aliyun.com/getting-started/what-is/what-is-sql)注入、XSS跨站、WebShell上传、后门攻击、命令注入、非法HTTP协议请求、常见Web服务器漏洞攻击、CSRF、核心文件非授权访问、路径穿越、网站被扫描等。 - 网站隐身：不对攻击者暴露站点地址，避免其绕过Web应用防火墙直接攻击。 - 0day补丁及时更新：及时更新漏洞补丁，防护网站安全。 - 友好的观察模式：针对网站新上线的业务开启观察模式，对于匹配中防护规则的疑似攻击只告警不阻断，方便统计业务误报状况。 |
| 深度精确防护 | \- 支持全解析多种常见HTTP协议数据格式：任意头部字段、Form表单、Multipart、JSON、XML。 - 支持解码常见编码类型：URL编码、JavaScript Unicode编码、HEX编码、HTML实体编码、Java序列化编码、PHP序列化编码、Base64编码、UTF-7编码、UTF-8编码、混合嵌套编码。 - 支持预处理机制：空格压缩、注释删减、特殊字符处理，向上层多种检测引擎提供更为精细、准确的数据源。 - 支持复杂格式数据环境下的检测能力；支持合理的检测逻辑复杂度，避免过多检测数据导致的误报，降低误报率；支持多种形式数据编码的自适应解码，避免利用各种编码形式的绕过。 |  |
| CC恶意攻击防护 | \- 控制单一源IP的访问频率，基于重定向跳转验证、人机识别等。 - 针对海量慢速请求攻击，根据统计响应码及URL请求分布、异常Referer及User-Agent特征识别，结合网站精准防护规则综合防护。 - 充分利用阿里云大数据安全优势，建立威胁情报与可信访问分析模型，快速识别恶意流量。 |  |
| 精准访问控制 | \- 提供友好的配置控制台界面，支持IP、URL、Referer、User-Agent等HTTP常见字段的条件组合，配置强大的精准访问控制策略；支持盗链防护、网站后台保护等防护场景。 - 与Web常见攻击防护、CC防护等安全模块结合，搭建多层综合保护机制；依据需求，轻松识别可信与恶意流量。 |  |
| 虚拟补丁 | 在Web应用漏洞补丁发布和修复之前，通过调整Web防护策略实现快速防护。 |  |
| 攻击事件管理 |  | 支持对攻击事件、攻击流量、攻击规模的集中管理统计。 |
| 灵活性、可靠性 |  | \- 支持[负载均衡](https://www.aliyun.com/getting-started/what-is/what-is-load-balance)：以集群方式提供服务，多台服务器负载均衡，支持多种负载均衡策略。 - 支持平滑扩容：可根据实际流量情况，缩减或增加集群服务器的数量，实现服务能力弹性扩容。 - 无单点问题：单台服务器宕机或者维修，均不影响正常服务。 |

更多产品信息，请参见[Web应用防火墙产品页面](http://www.aliyun.com/product/waf/)。

## 产品优势

| **产品优势** | **优势说明** | | 10年以上网络安全经验 | - 建立在阿里巴巴集团10年以上的网络安全经验上，提供与淘宝、天猫、支付宝等成功应用案例同样的安全体验。 - 由专业的安全团队为您提供服务。 - 抵御已知的OWASP漏洞并不断修复披露漏洞。 | | 防御CC攻击和爬虫攻击 | - 帮助您抵御和减缓CC攻击。 - 帮助您防御网络爬虫，避免网络资源消耗。 - 检测和阻挡恶意请求，帮助您减少带宽消耗，防止数据库、SMS、[API](https://www.aliyun.com/getting-started/what-is/what-is-api)资源亏空，减少响应延时，避免宕机等。 - 针对多样业务场景支持自定义防护规则。 | | 集成大数据能力 | - 每天约抵御数亿次网络攻击。 - 拥有丰富的IP数据库。 - 拥有广泛的应用案例，对各类常见网络攻击的模式、方法和签名有大量研究。 - 大数据分析不断整合先进的技术。 | | 简易性、可靠性 | - 5分钟内部署和激活。 - 无需安装任何软硬件或调整[路由](https://www.aliyun.com/getting-started/what-is/what-is-routing)配置。 - 通过防护集群作用，避免单点故障和冗余。 - 防护流量处理性能高。 |

## 应用场景

WAF适用于阿里云以及阿里云外所有用户，主要用于金融、电商、O2O、互联网+、游戏、政府、保险等行业各类网站的Web应用安全防护。

**说明**

WAF仅支持通过域名方式进行防护，不支持使用IP直接接入。

## 如何使用WAF

您购买WAF后，可以通过CNAME接入或透明接入的方式，将网站域名接入到WAF进行防护。

*   CNAME接入
    
    如果您的源站服务器部署在云上、云下，那么可以使用CNAME接入方式接入WAF。
    
    CNAME接入通过添加需要防护的网站信息到WAF控制台，并修改网站域名的[DNS](https://www.aliyun.com/getting-started/what-is/what-is-dns)解析（设置CNAME解析记录），将网站的Web请求转发到WAF进行防护。详细内容，请参见[添加域名](https://help.aliyun.com/zh/waf/web-application-firewall-2-0/user-guide/add-a-domain-name-to-waf#task-1796689)。
    
*   透明接入
    

如果您的源站服务器为ECS服务器或者部署在阿里云公网SLB上，那么除了使用CNAME接入，还可以选择[云原生](https://www.aliyun.com/getting-started/what-is/what-is-cloud-native)的透明接入。

透明接入将需要防护的网站信息添加到WAF控制台后，无需修改域名的[DNS](https://www.aliyun.com/getting-started/what-is/what-is-dns)解析设置，即可将源站请求流量转发到WAF进行防护。详细内容，请参见[透明接入](https://help.aliyun.com/zh/waf/web-application-firewall-2-0/user-guide/transparent-proxy-mode#task-2538763)。

## 合规资质

WAF已通过ISO 9001、ISO 20000、ISO 22301、ISO 27001、ISO 27017、ISO 27018、ISO 27701、ISO 29151、BS 10012、CSA STAR、等保三级、SOC 1/2/3、C5、HK金融、OSPAR、PCI DSS等多项国际权威认证。

WAF作为标准的阿里云云产品，在云平台层面具备与阿里云同等水平的安全合规资质。详细内容，请参见[阿里云信任中心](https://security.aliyun.com/trust)。