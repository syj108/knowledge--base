# DDoS高防

DDoS高防是一项代理防护服务，用于保护业务免受大规模分布式拒绝服务（DDoS）攻击。它通过将业务流量重定向至遍布全球的高防清洗中心，过滤恶意攻击流量，仅将合法的业务流量转发回源站服务器，从而确保业务在攻击下的稳定性和可用性。

## 工作原理

DDoS高防通过以下三个步骤保障您的业务稳定运行：

1.  **流量牵引**：通过修改DNS解析或将业务IP指向高防IP，将来自公网的访问流量全部牵引至高防清洗中心。更多内容，请参见下文[流量牵引方式](#7cc38a8f51gdx)。
    
2.  **流量清洗**：高防清洗中心利用多层检测与过滤引擎，有效防御SYN Flood、UDP Flood等L3/L4流量型攻击，并对HTTP Flood等L7应用层攻击提供防护。恶意攻击流量在此被精准识别并丢弃。
    
3.  **流量回源**：清洗后的正常访问流量，通过端口协议转发的方式安全、稳定地返回给您的源站服务器。
    

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/pLdn55XmA4gy9no8/img/89f7f08c-eecb-46e1-b6be-0564ae273ad9.svg)

### 流量牵引方式

需要通过以下任一方式，将业务流量引导至高防IP进行清洗。

| **引流方式** | **描述** | **适用场景** | **优点** | **缺点** |
| --- | --- | --- | --- | --- |
| **DNS解析** | 将业务域名（如`www.example.com`）的DNS记录修改为DDoS高防提供的CNAME地址。 | 网站、Web应用、API等通过域名访问的业务。 | 配置简单，生效快，便于在攻击时快速切换。 | 无法防护直接针对源站IP的攻击。 |
| **IP直接指向** | 在DDoS高防实例中配置转发规则，将高防IP作为业务入口，回源至真实服务器IP。客户端直接访问高防IP。 | 游戏、App后端服务等通过IP直接访问的非网站业务。 | 可直接防护IP，隐藏源站。 | 切换IP可能影响部分客户端的连接。 |

## 产品优势

*   **快速简便的部署**
    
    提供DNS解析和IP指向两种接入方式，无需安装软硬件或调整路由，通常可在数分钟内完成接入配置（具体耗时取决于 DNS 生效时间等因素），并隐藏源站IP，保护源站安全。
    
*   **AI驱动的智能精准防护**
    
    *   **网络层攻击防护：** 在传统特征识别等技术之上，结合 IP 信誉库与深度包检测（DPI），精准识别并阻断各类流量型攻击。
        
    *   **应用层CC攻击防护：** AI引擎自动学习业务模型，精准识别并过滤CC攻击流量，支持精细至URL级别的防护策略，大幅降低运维难度。
        
*   **全球化海量防御能力**
    
    DDoS高防全球防护网络总带宽超过20 Tbps，其中非中国内地防护带宽超过5 Tbps，能有效抵御网络层、传输层和应用层的各类DDoS攻击，保障业务全球访问体验。
    
*   **灵活的弹性防护**
    
    支持在线自助升级防护带宽，秒级生效。面对突发攻击可随时提升防御能力，且业务无需任何调整，服务不中断。
    
*   **金融级稳定高可用**
    
    采用全冗余架构，对机房、服务器、引擎和链路进行全方位监控，具备完善的自动故障切换和恢复机制，保障99.95%的服务可用性。
    
*   **智能流量调度**
    

可与云上其他产品联动，实现攻击发生时自动将流量调度至DDoS高防，平时则不介入，兼顾成本与安全。

## 产品规格

DDoS高防根据业务服务器所在的物理**地域**，分为\*\*DDoS高防（中国内地）**和**DDoS高防（非中国内地）\*\*两大类。

| **商品类型** | **实例版本** | **核心特点与区别** | **备注** |
| --- | --- | --- | --- |
| **DDoS高防（中国内地）** | **专业版** | 独享IP，多线BGP防护，支持保底与弹性防护。 | \- |
| **高级版** | 每月提供2次[高级防护](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/terms#section-oum-a36-4bo)（每月重置）。 | 开通需联系商务经理。 |  |
| **DDoS高防（非中国内地）** | **保险防护** 、**无限防护** | \- 保险防护与无限防护均适用于纯海外业务，区别在于计费模式、容量及高级防护次数（前者每月2次，后者无限制）。 - 为避免中国内地用户访问境外站点延迟，建议配合安全加速线路使用，更多内容参见[配置DDoS高防（非中国内地）安全加速](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/user-guide/configure-anti-ddos-premium-of-the-sec-cma-mitigation-plan)。 | \- |
| **安全加速线路2.0** | 提供中国内地访问加速、应用层DDoS防护，选择一定数量的DDoS防护次数后，具备电信、联通、移动线路的大流量DDoS攻击防护能力。 | 无 |  |
| **安全加速线路2.0（保险版）**、**安全加速线路2.0（无限版）** | 功能与2.0基本相同，支持禁用**95弹性业务带宽模式**、**95弹性QPS模式**。 | 功能已迁移至**安全加速线路2.0**，不推荐新购，仅保留存量实例。 |  |
| **加速线路**、**安全加速线路\*\*\*\*1.0** | 旧版本，不支持移动线路。 | 不推荐新购，建议升级至**安全加速线路2.0**，开通需联系商务经理。 |  |

## 常见场景及选购建议

| **服务器部署地域** | **用户来源** | **业务需求** | **推荐版本** |
| --- | --- | --- | --- |
| **中国内地** | 中国内地及非中国内地 | 通用DDoS防护。 | **DDoS高防（中国内地）** - **专业版** |
| **非中国内地** | 非中国内地 | 无需跨境访问加速。 | **DDoS高防（非中国内地）** - **保险防护**或**无限防护** |
| **非中国内地** | 中国内地 | 需要跨境访问加速，保障低延迟和稳定性。 | **DDoS高防（非中国内地）**\-**安全加速线路2.0** |
| **非中国内地** | 中国内地及非中国内地 | 在不迁移服务器的情况下，既要满足跨境访问加速，又要保证业务非中国内地访问需求。 | 组合购买： - **DDoS高防（非中国内地）**\-**安全加速线路2.0** - **DDoS高防（非中国内地）** - **保险防护**或**无限防护** |
| **非中国内地** | 中国内地及非中国内地 | 业务可按用户来源进行服务器迁移，实现跨境访问。迁移后，不同地域的用户访问由不同地域的服务器以及防护版本承载。 | \- 中国内地用户的业务：**DDoS高防（中国内地）** - **专业版** - 非中国内地用户的业务：**DDoS高防（非中国内地）** - **保险防护**或**无限防护** |

## 产品计费

DDoS高防的费用由预付费的**实例费用**和后付费的**弹性费用**组成。

*   **实例费用（预付费）**：根据选择的保底防护带宽、业务带宽、QPS等规格按月或按年支付。费用详情，请参见[DDoS高防（非中国内地）保险防护和无限防护计费说明](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-anti-ddos-premium-of-the-insurance-and-unlimited-mitigation-plans)、[DDoS高防（非中国内地）加速线路计费说明](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-anti-ddos-premium-of-the-cma-mitigation-plan)、[DDoS高防（非中国内地）安全加速线路计费说明](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-anti-ddos-premium-of-the-sec-cma-mitigation-plan)。
    
*   **弹性防护费用（后付费）**：仅当DDoS攻击流量超过保底防护带宽时，按天根据实际攻击流量峰值计费。费用详情，请参见[弹性防护带宽计费方式](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-the-burstable-protection-bandwidth-feature)。
    
*   **弹性业务带宽/QPS费用（后付费）**：仅当正常业务流量或QPS超过保底规格时，按日或月95峰值计费。费用详情，请参见[弹性业务带宽计费说明](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-the-burstable-clean-bandwidth-feature)、[弹性QPS计费说明](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-the-burstable-qps-feature)。
    
*   **高级防护资源包：** 针对特定实例，还可以按需购买**高级防护资源包**。费用详情，请参见[高级防护资源包计费说明](https://help.aliyun.com/zh/anti-ddos/anti-ddos-pro-and-premium/product-overview/billing-of-advanced-mitigation-sessions)。