# KMS

密钥管理服务KMS（Key Management Service）是您的一站式密钥管理和数据加密服务平台、一站式凭据安全管理平台，提供简单、可靠、安全、合规的数据加密保护和凭据管理能力。KMS帮助您降低在密码基础设施、数据加解密产品和凭据管理产品上的采购、运维、研发开销，以便您只需关注业务本身。

## 业务组件

KMS主要提供密钥管理、凭据管理两种业务组件。

| **业务组件** | **说明** | **参考文档** |
| --- | --- | --- |
| 密钥管理 | KMS提供密钥安全托管和使用密钥进行密码运算的能力。不仅可为您提供云产品服务端数据加密保护所需的密钥管理功能，还可为您提供在自建应用程序中使用密钥对数据进行数字签名、加密、解密等密码运算。 | [密钥服务概述](https://help.aliyun.com/zh/kms/key-management-service/user-guide/overview-of-key-management#task-2292622) |
| 凭据管理 | KMS提供凭据加密存储、定期轮转、安全分发、中心化管理等能力，使您的应用程序规避明文配置凭据风险，支持轮转进而有效降低凭据泄露事件危害。 | [凭据管理概述](https://help.aliyun.com/zh/kms/key-management-service/user-guide/secret-management-overview#concept-2403154) |

## 功能特性

### 密钥管理

密钥管理可提供的功能如下表所示。

| **功能** | **说明** | **参考文档** |
| --- | --- | --- |
| 丰富的密钥管理类型 | 提供免费的默认密钥用于云产品服务端加密，也提供付费的软件密钥、硬件密钥用于您的自建应用数据加密或云产品服务端加密，满足不同业务和安全合规场景的密钥管理需求。 | [密钥服务概述](https://help.aliyun.com/zh/kms/key-management-service/user-guide/overview-of-key-management#task-2292622) |
| 先进的安全合规能力 | 支持集成经权威认证的硬件安全模块（Hardware Security Module，HSM），满足您对密码技术应用的高安全等级和合规要求。 | [硬件密钥](https://help.aliyun.com/zh/kms/key-management-service/user-guide/overview-of-key-management#section-94o-2vp-13j) |
| 支持[云原生](https://www.aliyun.com/getting-started/what-is/what-is-cloud-native)加密 | 支持广泛的云产品集成，助您轻松使用KMS密钥和加密技术来保护云上敏感数据资产。除支持云产品服务端加密外，也支持对[容器](https://www.aliyun.com/getting-started/what-is/what-is-container)服务ACK Pro集群中的[Kubernetes](https://www.aliyun.com/getting-started/what-is/what-is-kubernetes) Secret密钥数据进行落盘加密。 | [支持集成KMS加密的云产品](https://help.aliyun.com/zh/kms/key-management-service/user-guide/alibaba-cloud-services-that-can-be-integrated-with-kms#concept-2318937) |
| 极简应用接入 | 通过阿里云[SDK](https://www.aliyun.com/getting-started/what-is/what-is-sdk)帮助您轻松使用密钥管理功能，使用KMS实例SDK完成密码运算操作。实现对密钥进行生命周期管理、使用密钥对数据进行加密、解密、签名、验签等密码功能。 | \- [阿里云SDK](https://help.aliyun.com/zh/kms/key-management-service/overview-of-classic-kms-sdk) - [KMS实例SDK](https://help.aliyun.com/zh/kms/key-management-service/developer-reference/kms-instance-sdk-for-java/#task-311381) |
| 中心化规模化管理 | 支持ROS、Terraform等产品，帮助您自动化实施默认加密策略，实现在云服务器ECS（云盘）、[对象存储](https://www.aliyun.com/getting-started/what-is/what-is-object-storage)OSS、关系型[数据库](https://www.aliyun.com/getting-started/what-is/what-is-cloud-database)RDS、大数据计算MaxCompute等产品默认开启服务端加密。 | [Terraform概述](https://help.aliyun.com/zh/kms/key-management-service/developer-reference/kms-3-use-terraform-to-manage-kms-resources#task-2248887) |

### 凭据管理

凭据管理可提供的功能如下表所示。

| **功能** | **说明** | **参考文档** |
| --- | --- | --- |
| 云原生集成 | 云原生集成支持您托管RAM、RDS、ECS凭据和配置轮转周期以实现凭据动态化，帮助您有效应对RAM的AK、RDS和ECS账密泄露的安全威胁。 | [凭据管理概述](https://help.aliyun.com/zh/kms/key-management-service/user-guide/secret-management-overview#concept-2403154) |
| 极简应用接入 | 您的应用可通过凭据管家客户端、RAM凭据插件、凭据管家JDBC客户端，以极简方式接入使用凭据。 | \- [凭据客户端](https://help.aliyun.com/zh/kms/key-management-service/developer-reference/secrets-manager-client#task-2280463) - [凭据JDBC客户端](https://help.aliyun.com/zh/kms/key-management-service/developer-reference/secrets-manager-jdbc#task-2280443) - [RAM凭据插件](https://help.aliyun.com/zh/kms/key-management-service/developer-reference/ram-secret-plug-in#task-2277694) |
| 中心化规模化管理 | 支持ROS、Terraform等产品，帮助您实现凭据的安全托管和运维编排的自动化管理。 | [Terraform概述](https://help.aliyun.com/zh/kms/key-management-service/developer-reference/kms-3-use-terraform-to-manage-kms-resources#task-2248887) |

## 更多参考

如果您想了解更多关于KMS的详细信息，请参见[KMS详情介绍](https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20230922/buhe/%E9%98%BF%E9%87%8C%E4%BA%91%E5%AF%86%E9%92%A5%E7%AE%A1%E7%90%86%E6%9C%8D%E5%8A%A1KMS%E5%AE%98%E7%BD%91%E5%8F%91%E5%B8%83%E7%89%88%E6%9C%AC.pdf)。

![image](https://alidocs.oss-cn-zhangjiakou.aliyuncs.com/res/vBPlN5j4kwGzBOdG/img/dce788f0-1e5e-4656-bc7c-1830ca8b3ef9.png)