# WeKnora

[//]: # (# 20260520定稿-模版)

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   WeKnora 是腾讯开源的面向知识管理与检索的 RAG（Retrieval-Augmented Generation）应用平台，核心能力覆盖文档解析、向量检索、大模型集成及多端部署（Docker Compose / Lite 单二进制 / Desktop / Kubernetes），聚焦企业级私有知识库场景。
        
    *   当前公开版本为 v0.6.0（见 [[WeKnora/系统信息|系统信息]]），已支持组件化分层架构，各模块可独立升级与观测；后续演进方向包括增强多模态文档解析、细粒度权限控制、联邦RAG协同及可观测性标准化。
        
    *   能力涉及核心组件：Frontend UI、Backend Service & API、Document Reader Service、Vector Database（默认 PostgreSQL + pgvector 扩展）、Model Service（支持 OpenAI 兼容接口及本地 LLM 接入）。

*   对外介绍架构图
    
    *   部署架构为「中心化服务 + 端侧轻量接入」模式：  
        - **中心端**：基于 Docker Compose 或 Helm 部署于 Linux 主机/K8s 集群，含 `app`（后端API）、`docreader`（异步文档解析服务）、`postgres`（向量+元数据混合存储）、`nginx`（静态资源与反向代理）等容器组；  
        - **端侧**：支持 Web 浏览器访问、桌面客户端（Electron 封装）、CLI 工具及 SDK 集成调用。  
        - 各容器组默认共置于同一物理节点或 K8s Node，无跨机强依赖。
        
    *   数据流向：用户上传文档 → Frontend UI → Backend API → 异步投递至 Document Reader Service → 解析后写入 PostgreSQL（文本块 + embedding 向量 + 元数据）→ 用户提问时由 Backend API 调用 Vector Database 检索 → 聚合结果送 Model Service 生成回答 → 返回前端。
        
    *   与上下游系统无强耦合依赖；不直接对接阿里云生态（如 Tianji、OpsApi），属独立开源项目，但可通过标准 API 与 VPC 内网其他服务互通。
        
    *   参考：官方架构说明见 [[WeKnora/架构概览|架构概览]]（GitHub README）

*   各核心组件能力详细说明
    
    *   **Frontend UI**：React 实现的响应式 Web 界面，提供文档上传、知识库管理、对话交互、设置面板（含系统信息查看）；支持主题切换与基础访问控制。
    *   **Backend Service & API**：Go 编写，提供 RESTful 接口（`/api/v1/...`），负责鉴权、路由、任务调度、向量检索编排、LLM 请求中转；内置健康检查端点 `/healthz`。
    *   **Document Reader Service**：Python（FastAPI）实现，专注文档解析（PDF/DOCX/MD/TXT 等），支持 OCR（可选）、分块策略配置、嵌入向量化（调用 Model Service 或内置 embedder）；通过 Redis Queue 与 Backend 解耦。
    *   **Vector Database**：基于 PostgreSQL + pgvector 扩展，统一存储文档元数据（`documents`, `chunks` 表）与向量（`chunk_embeddings` 向量列），支持高效 ANN 检索；不依赖外部向量数据库（如 Milvus、Qdrant）。
    *   **Model Service**：抽象 LLM 接入层，支持 OpenAI 兼容 API（如 Ollama、vLLM、OpenRouter）及本地 HuggingFace 模型；通过环境变量或配置中心动态切换 provider。

*   与阿里云其他产品的关系
    
    *   WeKnora 为独立开源项目，**未预集成或适配阿里云 VPC、ECS、SLB 等产品**；若部署于阿里云 ECS，仅需确保网络连通性与端口开放（如 3000/8080），其行为与其他云厂商或本地环境一致。
    *   产品异常**不会影响**阿里云基础设施稳定性；**不会造成**VPC 网络中断、ECS 实例宕机、SLB 转发失败等——所有影响均限于 WeKnora 自身服务可用性与知识库功能（如文档无法上传、检索无结果、回答超时等）。

### QA（高频问答）：

*   Q：WeKnora 支持哪些文档格式？是否支持图片/PPT？  
    A：当前支持 PDF、DOCX、MD、TXT、HTML；PPT/XLSX 计划在 v0.7+ 支持；原生不支持图片内文字识别（OCR），但 Document Reader Service 提供 OCR 开关（需额外部署 Tesseract）。

*   Q：能否替换为 MySQL 或 MongoDB？  
    A：否。向量检索强依赖 pgvector 的 ANN 算子，数据库层深度绑定 PostgreSQL；MongoDB 不在支持路线图中。

*   Q：如何查看当前运行版本？  
    A：访问 Web 端 **Settings → System Info**，或执行 `docker compose exec app curl -s http://localhost:8080/api/v1/system/info | jq '.version'`。

*   Q：Lite 版本和 Docker Compose 版本有何区别？  
    A：Lite 是单二进制（含内嵌 SQLite + 轻量 HTTP Server），适合个人快速体验；Docker Compose 版本使用 PostgreSQL + Redis + 分离服务，支持高并发与生产级扩展。

### 组件接口人、研发负责人等角色信息：

*   WeKnora 为腾讯开源项目，**无阿里云内部 SRE/产研接口人**；社区维护由 Tencent WeKnora Team 负责。  
    *   GitHub Issues 是唯一官方问题入口（[[WeKnora/Issue Tracker|Issue Tracker]]）；  
    *   L1/L2 运维操作权限分级不适用（非阿里云托管服务）；所有部署、配置、日志排查均由用户自主完成；  
    *   紧急问题应参考 [[WeKnora/文档/QA.md|官方QA]] 或提交 Issue，**不升级至阿里云产研**。

### 告警/风险/异常汇总表

*   WeKnora **未内置 Prometheus/Grafana 告警体系，亦无 EOCC/KB 对接**；告警需用户自行基于日志关键词或 HTTP 健康检查构建。  
    *   常见需关注的异常模式（建议纳入自建监控）：
        - `docreader.*panic` / `docreader.*failed to parse` → Document Reader Service 解析失败
        - `backend.*500.*vector search failed` → 向量检索异常（查 pgvector 扩展状态、索引完整性）
        - `model-service.*timeout` / `model-service.*connection refused` → LLM 服务不可达
        - `postgres.*connection refused` → 数据库连接中断
    *   应急手册：见 [[WeKnora/典型问题排查解决方案|典型问题排查解决方案]]

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `docreader_panic` | P1 | 日志含 `panic:` 或连续 `ERROR` 且无 recovery | Document Reader Service | 解析进程崩溃 | 文档处理链路中断，新上传文档无法入库 | [[WeKnora/典型问题排查解决方案#docreader_panic|docreader_panic]] | 否 |
| `vector_search_failed` | P2 | Backend 日志含 `vector search failed` + HTTP 500 | Backend Service & API | pgvector 查询异常或索引失效 | 检索功能降级，用户提问返回空结果 | [[WeKnora/典型问题排查解决方案#vector_search_failed|vector_search_failed]] | 否 |
| `model_service_unavailable` | P2 | Backend 日志含 `failed to call model service` | Backend Service & API | Model Service 响应超时或连接拒绝 | 生成回答失败，仅返回检索片段 | [[WeKnora/典型问题排查解决方案#model_service_unavailable|model_service_unavailable]] | 否 |
| `postgres_connection_refused` | P1 | `docker compose logs postgres` 无输出或报错 | Vector Database | PostgreSQL 容器未启动或端口冲突 | 全功能不可用（读写均失败） | [[WeKnora/典型问题排查解决方案#postgres_connection_refused|postgres_connection_refused]] | 否 |

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    *   数据库：`weknora`（PostgreSQL）  
    *   `documents`: 存储上传文档元信息（id, name, source_type, created_at）  
    *   `chunks`: 存储解析后的文本块（id, document_id, content, token_count）  
    *   `chunk_embeddings`: 存储向量（chunk_id, embedding vector, updated_at）—— **关键检索表**

*   关键日志路径，组件，内容，轮转策略  
    *   Docker Compose 部署：  
        - `app` 容器：`/app/logs/backend.log`（Backend 服务日志，按天轮转，保留7天）  
        - `docreader` 容器：`/app/logs/docreader.log`（Document Reader 日志，同上）  
        - `postgres` 容器：`/var/lib/postgresql/data/log/`（PostgreSQL CSV 日志，需在 `postgresql.conf` 中启用 `logging_collector=on`）  
    *   Lite 版本：`logs/*.log`（同目录下，无自动轮转，需用户清理）

*   问题排查SOP，通用场景的排查思路和路径  
    *   通用路径：  
        1. 查 `app` 日志确认请求是否到达 Backend；  
        2. 若失败，看是否为 `500` → 查 `docreader` 或 `postgres` 日志；  
        3. 若成功但无结果 → 查 `chunk_embeddings` 表是否有对应 `chunk_id` 向量；  
        4. 若回答异常 → 查 `model-service` 连通性及响应体。  
    *   必查命令：  
        ```bash
        docker compose logs --tail=100 app docreader postgres
        docker compose exec postgres psql -U weknora -c "SELECT COUNT(*) FROM chunk_embeddings;"
        ```

*   版本升级指南（如有）、巡检手册（如有）、相关aone  
    *   升级指南：见 [[WeKnora/Upgrade Guide|Upgrade Guide]]（GitHub `docs/upgrade.md`）  
    *   巡检项（每日）：  
        - `docker compose ps` 确认所有服务 `Up` 状态  
        - `curl -s http://localhost:8080/healthz` 返回 `{"status":"ok"}`  
        - `docker compose exec postgres psql -U weknora -c "SELECT now() - pg_postmaster_start_time();"`（确认 DB 运行时长）  
    *   Aone：不适用（非阿里云内部项目）

### 典型问题排查解决方案

*   文档定位：L2 值班时的核心操作手册，从告警/问题现象出发，提供完整的排查路径和操作步骤。  
*   已知真实case：问题描述、分析路径、解决路径（用于引导智能体）  

```yaml
（针对每一个问题现象）
一、问题描述
● 问题现象：用户上传 PDF 后，知识库列表显示“Processing”，但数小时后仍不完成，且无错误提示。
● 适用范围：所有部署形态（Docker Compose / Lite / Desktop），v0.5.0+
二、排查信息收集
● 必须收集的信息：文档 ID（URL 中 `?id=xxx`）、WeKnora 版本（Settings → System Info）、OS 类型
● 检查终态的方法：登录 `docreader` 容器，执行 `ls -l /app/uploads/` 确认文件存在；执行 `docker compose logs docreader | tail -20`
● 排查问题步骤：
  - 步骤1：`docker compose logs docreader | grep -i "error\|panic\|fail"`
    → 结果含 `tesseract not found` → 场景一
  - 步骤2：`docker compose logs docreader | grep "success"`
    → 无输出 → 场景二
三、解决步骤
 场景一：OCR 依赖缺失（PDF 含扫描件）
 - 适用条件：日志含 `tesseract not found` 且文档为扫描PDF
 - 实施步骤：
     - Docker Compose：修改 `docker-compose.yml`，在 `docreader` service 下添加 `image: tencent/weknora-docreader:with-tesseract`
     - 重启：`docker compose up -d docreader`
 - 结果验证：重新上传同文档，日志出现 `OCR completed for page 1`
 场景二：Redis 队列阻塞
 - 适用条件：`docreader` 日志静默，`redis` 容器 CPU 100%
 - 实施步骤：
     - `docker compose exec redis redis-cli`
     - `> llen weknora:queue:parse` （查看队列长度）
     - `> lrange weknora:queue:parse 0 1` （查看待处理任务）
     - `> lpop weknora:queue:parse` （手动弹出卡住任务）
 - 结果验证：新上传文档进入处理流程
四、非本产品排查
● 明确标注：若 `docker compose logs postgres` 显示 `out of memory`，属宿主机内存不足，需扩容 ECS 或调整 `shared_buffers`
五、快速定位工具
● 脚本位置：`scripts/diagnose.sh`（GitHub 仓库根目录）
● 使用方法：`bash scripts/diagnose.sh --component docreader --verbose`
```

### 紧急场景止血与恢复手册：

*   灾难场景下的一键恢复和应急操作  
    *   **全服务不可用（`docker compose ps` 全红）**：  
        ```bash
        docker compose down && docker compose up -d --force-recreate
        ```
    *   **文档解析积压（Redis 队列 > 1000）**：  
        ```bash
        docker compose exec redis redis-cli DEL weknora:queue:parse
        ```
    *   **向量索引损坏（检索结果为空但 chunks 存在）**：  
        ```bash
        docker compose exec postgres psql -U weknora -c "CREATE INDEX CONCURRENTLY ON chunk_embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);"
        ```

### 横向研发文档：

*   接入指引  
    *   第三方系统可通过 `POST /api/v1/documents/upload` 上传文档，`POST /api/v1/chat/completions` 发起 RAG 问答；详见 [[WeKnora/API Reference|API Reference]]。

*   产品对接方案细节  
    *   支持 OAuth2.0 认证（需配置 `AUTH_PROVIDER`）；Webhook 事件（如 `document.processed`）需自行订阅 Redis Pub/Sub 频道 `weknora:events`。

*   产品对接范围等  
    *   对接边界：仅暴露 REST API 与 Redis 事件通道；不开放数据库直连、不提供 gRPC 接口、不兼容阿里云 RAM/OpenAPI。

## 产品对内文档：

### 完整架构图：

*   系统，架构，调用关系，业务流，模块等深层知识点，供扩展使用，也可以写入一些已知问题  
    *   架构图见 [[WeKnora/Architecture Diagram|Architecture Diagram]]（GitHub `docs/architecture.png`）  
    *   已知问题：  
        - v0.6.0 中，当 `chunk_embeddings` 表超过 100 万条，IVFFLAT 索引召回率下降明显（已提 issue #421）  
        - Lite 版本 SQLite 不支持并发写入，多用户同时上传易触发 `database is locked`

### 业务逻辑时序图

*   用户使用  
    *   时序图见 [[WeKnora/Sequence Diagram/User Flow|User Flow]]（Mermaid 格式，含上传、检索、问答三主干）

*   工作流流转  
    *   文档生命周期：`upload → queue → parse → embed → store → index → retrieve → generate → answer`

### 代码仓库

*   基线仓库  
    *   `https://github.com/Tencent/WeKnora`（主仓库，main 分支为稳定基线）

*   代码仓库  
    *   `frontend/`（React）  
    *   `backend/`（Go）  
    *   `docreader/`（Python）  
    *   `scripts/`（运维脚本）

*   制品仓库  
    *   Docker Hub：`tencent/weknora-app`, `tencent/weknora-docreader`  
    *   GitHub Releases：提供 `weknora-lite` 单二进制下载

*   关联依赖仓库等  
    *   `tencent/weknora-model-service`（独立子项目，已归并至主仓 `backend/internal/model`）  
    *   `tencent/weknora-doc-parser`（历史子模块，代码已迁移）

### 数据表结构

*   `documents` 表：  
    ```sql
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    source_type VARCHAR(20) CHECK (source_type IN ('upload', 'url', 'api')),
    created_at TIMESTAMPTZ DEFAULT NOW()
    ```

*   `chunks` 表：  
    ```sql
    id UUID PRIMARY KEY,
    document_id UUID REFERENCES documents(id),
    content TEXT NOT NULL,
    token_count INT,
    created_at TIMESTAMPTZ DEFAULT NOW()
    ```

*   `chunk_embeddings` 表：  
    ```sql
    chunk_id UUID PRIMARY KEY REFERENCES chunks(id),
    embedding VECTOR(1024), -- 默认维度，可配
    updated_at TIMESTAMPTZ DEFAULT NOW()
    ```