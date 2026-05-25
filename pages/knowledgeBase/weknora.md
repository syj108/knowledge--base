# WeKnora

## 产品对外文档：

### 服务介绍：

*   产品定位与演进历程
    
    *   WeKnora 是腾讯开源的面向知识管理与检索的 RAG（Retrieval-Augmented Generation）应用平台，聚焦于企业/团队私有知识库的构建、理解与智能问答，核心能力覆盖文档解析、向量化存储、语义检索、大模型协同生成及多端（Web / Desktop / Lite / Kubernetes）部署。
        
    *   当前公开版本为 v0.6.0（依据 ISSUE_TEMPLATE 中 `version` 字段示例），已支持 Docker Compose、Kubernetes（Helm）、单二进制 Lite 模式及桌面应用等多种部署形态；具备生产就绪的核心能力。后续演进方向包括增强配置管理能力、提升多模型服务调度灵活性、深化文档 Reader 的格式兼容性（如复杂 PDF 表格/公式识别）、引入细粒度权限控制（RBAC）、强化可观测性（metrics / tracing）及插件化扩展能力（v0.7+ 架构设计中）。
        
    *   能力涉及核心组件：Frontend UI、Backend Service & API、Document Reader Service、Vector Database（默认 PostgreSQL + pgvector 插件）、Model Service（支持 OpenAI 兼容接口及本地 LLM 接入）。

*   对外介绍架构图
    
    *   **中心端与端侧部署架构**：WeKnora 采用松耦合微服务架构。中心端包含 Backend（协调中枢）、Document Reader（异步文档解析）、Vector DB（向量+元数据持久化）、Model Service（LLM 推理代理）；端侧支持 Web 浏览器、跨平台 Desktop App（Tauri 构建）、轻量级 Lite 二进制（含内嵌 SQLite + 简化向量引擎）及云原生 Kubernetes 集群（Helm Chart）。Docker Compose/K8s 部署中，各服务以独立容器运行，共享网络与存储卷（如 `./data` 映射至 docreader/postgres 容器）。
        
    *   **数据流向图**：用户上传文档 → Backend 接收并分发至 Document Reader → Reader 解析文本/结构化内容 → Backend 将 chunked 文本写入 Vector DB（含 embedding 向量与元数据）→ 用户提问 → Backend 调用 Vector DB 检索 Top-K 相关片段 → 组装 Prompt 并转发至 Model Service → 返回生成答案 → 前端渲染。  
        更完整的工作流为：`Upload → [Validate → Queue → Parse → Chunk → Embed → Index] → Search → [Retrieve → Rerank → Generate] → Answer`（所有异步步骤均通过 Redis Queue 或 PG LISTEN/NOTIFY 触发）。
        
    *   **上下游依赖关系**：
        *   无强依赖阿里云原生系统（如 Tianji、OpsApi），属独立开源项目；**不隶属于阿里云产品体系**，与 VPC、ECS、SLB 等阿里云 Top30 产品无直接交互或集成关系。
        *   向量检索层依赖 PostgreSQL + pgvector（或可插拔替换为 Milvus/Chroma/Weaviate）；Redis 可选用于队列与缓存；MinIO/S3 可选用于原始文档对象存储。
        *   大模型调用依赖外部 LLM 服务（OpenAI / Azure OpenAI / Ollama / vLLM / DashScope / Qwen / GLM 等 OpenAI 兼容 API 或本地部署实例）。
        *   日志与监控需用户自行对接（如通过容器日志采集至 ELK/Prometheus/Loki）；WeKnora 提供 `/api/v1/metrics`（Prometheus 格式）用于可观测性对接。

*   各核心组件能力详细说明
    
    *   **Frontend UI**：基于 React 的响应式 Web 界面，提供文档上传、知识库管理、对话交互、历史会话查看、设置中心（含 System Info 版本查看、模型/向量库配置）；Desktop App 为 Tauri 构建，复用同一前端代码。
    *   **Backend Service & API**：Go（Gin 框架）编写，提供 RESTful API（文档管理、检索、问答、健康检查等），负责工作流编排、权限抽象（当前为简易 Token 认证，v0.7+ 规划 RBAC）、配置加载与服务发现；统一鉴权、路由分发、状态管理；提供 `/api/v1/kb`, `/api/v1/chat`, `/api/v1/doc`, `/api/v1/healthz` 等核心接口。
    *   **Document Reader Service**：Python 编写，支持 PDF（含扫描件，需启用 OCR）、DOCX、PPTX、TXT、Markdown 等格式；内置文本切片（chunking）、元数据提取（标题/页码/来源）、可选调用 OCR 引擎（Tesseract）；异步文档解析微服务，支持嵌入向量生成（调用 Model Service 或内置 embedding 模型）。
    *   **Vector Database**：默认集成 PostgreSQL + pgvector 扩展，存储文本块向量、原始内容、来源路径、时间戳等；Schema 设计兼顾检索效率与扩展性（如支持多知识库隔离）；关键表包括 `kb_knowledge_bases`, `kb_documents`, `kb_chunks`, `kb_embeddings`（详见 [[数据表结构|WeKnora-数据表结构]]）。
    *   **Model Service**：轻量级代理层，统一处理 LLM 请求（支持 streaming），适配多种后端（OpenAI / Azure OpenAI / Ollama / Local Llama.cpp / vLLM）；负责 Prompt 工程封装、RAG 上下文拼接与响应解析；支持流式响应、系统提示词注入。

*   与阿里云其他产品的关系
    
    *   WeKnora 为腾讯开源项目，**不原生集成或依赖阿里云 VPC、ECS、SLB 等产品**；用户可在阿里云 ECS 上部署（如使用 Docker Compose），此时其网络连通性、安全组策略、负载均衡需求由用户自主配置，WeKnora 本身不感知云厂商基础设施。
    *   **产品异常影响边界清晰**：
        *   *会造成的影响*：文档解析失败 → 新知识无法入库；向量 DB 不可用 → 检索中断，问答返回空结果；Model Service 不可达 → 仅返回检索片段，无生成答案；Backend 宕机 → 全功能不可用。
        *   *不会造成的影响*：WeKnora 异常**不会影响**用户云上其他业务系统（如 RDS、OSS）的稳定性；**不访问**阿里云控制台 API，无权限泄露风险；**不采集**用户文档内容至第三方（所有处理在用户私有环境完成）。
        *   若用户在阿里云 ECS 上部署 WeKnora，仅存在基础设施层运行依赖（如 ECS 提供计算资源、VPC 提供网络隔离），WeKnora 自身逻辑不感知、不调用阿里云 SDK 或 API；其异常不会触发阿里云产品告警，也不会导致 SLB/VPC/ECS 等服务异常；反之亦然——阿里云资源故障仅影响 WeKnora 运行环境，不改变其内部业务逻辑边界。

### QA（高频问答）：

*   Q：WeKnora 支持哪些文档格式？是否支持图片/PDF 扫描件？  
    A：支持 PDF（含扫描件，需启用 OCR）、DOCX、PPTX、TXT、Markdown。OCR 默认关闭，可在 `config.yaml` 中配置 Tesseract 路径启用。

*   Q：能否更换向量数据库？比如用 Milvus 或 Chroma？  
    A：当前主干支持 PostgreSQL/pgvector；社区已有 Milvus 适配 PR；Chroma / Weaviate 需通过自定义 Vector Store Adapter 实现，详见 [[方案支撑文档/横向研发文档/接入指引|接入指引]]。官方计划在 v0.7+ 版本提供标准适配器框架。

*   Q：如何查看当前运行版本？  
    A：Web 端进入「Settings → System Info」，显示 App Version 与 UI Version；命令行部署可执行 `weknora --version`（Lite/Desktop）或 `docker compose ps app` 查看镜像 tag。

*   Q：日志在哪里？如何排查启动失败？  
    A：Docker Compose 下执行 `docker compose logs -f --tail=1000 app docreader postgres`；Lite/Desktop 查看 `logs/*.log`（按天轮转，保留 7 天）；常见原因：PostgreSQL 连接失败（检查 `DB_URL`）、模型 API KEY 无效、OCR 依赖缺失（Linux 需 `apt install tesseract-ocr`）。

*   Q：WeKnora 是否支持中文文档解析与问答？  
    A：是。Document Reader Service 内置中文分词与语义理解能力，Model Service 可接入中文大模型（如 Qwen、GLM），完整支持中文知识库构建与问答。

*   Q：能否替换默认向量数据库为 Milvus / Chroma / Weaviate？  
    A：当前架构支持插件化扩展，Vector DB 层已抽象为 interface；社区已有第三方适配器实验（见 Discussions），官方计划在 v0.7+ 版本提供标准适配器框架。

*   Q：如何离线部署？是否需要联网调用外部大模型？  
    A：支持完全离线部署。通过配置 Model Service 指向本地 Ollama/vLLM 实例（如 `http://localhost:11434`），即可脱离公网运行；Document Reader Service 的 OCR 与嵌入模型亦支持离线加载。

*   Q：是否支持多租户与权限隔离？  
    A：v0.6.0 提供基础知识库级隔离（不同 KB 间数据物理分离），RBAC 权限模型已在 v0.7 Roadmap 中，当前可通过反向代理 + 多实例方式实现租户隔离。

### 组件接口人、研发负责人等角色信息：

*   WeKnora 为腾讯开源项目，**无官方指定 SRE/研发接口人**；问题响应依赖 GitHub 社区：
    *   Bug/Issue：提交至 [GitHub Issues](https://github.com/Tencent/WeKnora/issues)，按模板填写，标签 `bug` 自动触发 triage。
    *   技术咨询：优先查阅 [[产品对外文档/QA（高频问答）|QA]] 或 [官方 FAQ](https://github.com/Tencent/WeKnora/blob/main/docs/QA.md)；未解决则提 [Question Issue](https://github.com/Tencent/WeKnora/issues/new?assignees=&labels=question%2Cneeds-triage&template=question.yml)。
    *   紧急故障（如线上数据损坏）：需用户自行修复（L1/L2 可操作重启服务、回滚配置、清理异常文档任务）；涉及代码缺陷必须升级至社区产研（通过 Issue 提交复现步骤+日志）。
*   本项目为腾讯开源（GitHub: Tencent/WeKnora），**无阿里云内部研发负责人或运维权限分级定义**；所有组件由社区维护，接口人信息请参考 GitHub 仓库的 [MAINTAINERS.md](https://github.com/Tencent/WeKnora/blob/main/MAINTAINERS.md)（若存在）或 Discussions 中的 Committer 公示。  
    *注：L1/L2 运维职责不适用；故障升级路径为 GitHub Issue → Community Discussion → Core Maintainer Review。*

### 告警/风险/异常汇总表

| 告警名 | 级别 | 识别方式 | 所属组件 | 触发条件 | 含义 | 应急手册链接 | 是否有EOCC/KB |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `backend_health_check_failed` | P1 | HTTP GET `/healthz` 返回非 200 | Backend Service & API | Backend 进程存活但 API 不响应（如 DB 连接池耗尽、goroutine 死锁） | 核心服务不可用，所有请求失败 | [[方案支撑文档/典型问题排查解决方案#backend_health_check_failed|backend_health_check_failed]] | 否 |
| `docreader_task_timeout` | P2 | 日志出现 `task timeout after XXXs` | Document Reader Service | 单文档解析超时（>300s），常见于超大 PDF 或 OCR 卡死 | 该文档入库失败，不影响其他任务 | [[方案支撑文档/典型问题排查解决方案#docreader_task_timeout|docreader_task_timeout]] | 否 |
| `vector_db_connection_refused` | P1 | Backend 日志报 `failed to connect to pg: ...` | Vector Database | PostgreSQL 容器未启动、端口被占、认证失败 | 新文档无法入库，历史检索仍可缓存命中 | [[方案支撑文档/典型问题排查解决方案#vector_db_connection_refused|vector_db_connection_refused]] | 否 |
| `model_service_unavailable` | P2 | Backend 日志报 `model service unreachable` | Model Service | Model Service 进程崩溃、API 地址配置错误、网络不通 | 问答仅返回检索片段，无 LLM 生成答案 | [[方案支撑文档/典型问题排查解决方案#model_service_unavailable|model_service_unavailable]] | 否 |

*   WeKnora 官方未发布标准化告警体系（无 Prometheus metrics / Alertmanager 集成开箱即用）；常见异常均通过日志暴露，需结合部署方式采集分析。  
    *注：该表格暂不适用，建议用户基于以下日志关键路径自行建设监控（参见 [[运维指导/运维手册|WeKnora-运维手册]]）。*

## 方案支撑文档：

### 运维指导/运维手册

*   常用的数据库，常用表（写明存储什么信息）  
    *   **数据库**：PostgreSQL（默认 `weknora` DB）  
    *   **关键表**：  
        *   `kb_knowledge_bases`: 知识库元信息（id, name, description, created_at）  
        *   `kb_documents`: 文档元数据（id, kb_id, file_name, status, parsed_at）  
        *   `kb_chunks`: 文本块（id, doc_id, content, token_count）  
        *   `kb_embeddings`: 向量存储（id, chunk_id, embedding vector, updated_at）  

*   关键日志路径，组件，内容，轮转策略  
    *   **Docker Compose**：  
        *   `app` 容器：`/app/logs/backend.log`（Backend 服务日志，JSON 格式，按天轮转，保留 7 天）  
        *   `docreader` 容器：`/app/logs/docreader.log`（Reader 服务日志，文本格式，按大小轮转 100MB，保留 5 个）  
        *   `postgres` 容器：`/var/lib/postgresql/data/log/`（PostgreSQL 日志，需在 `postgresql.conf` 中开启 `logging_collector = on`）  
    *   **Lite/Desktop**：`./logs/*.log`（同上，路径相对可执行文件；按天轮转，保留 7 天）  
    *   **Kubernetes**：通过 `kubectl logs -l app=weknora-backend` 等标签获取，依赖集群日志采集方案（如 Loki+Promtail）

*   问题排查SOP，通用场景的排查思路和路径  
    1. **确认现象**：是前端无响应？API 报错？文档不显示？答案质量差？  
    2. **定位组件**：根据现象选择对应日志（见上表）→ 搜索关键词（`error`, `panic`, `timeout`, `failed`）  
    3. **验证依赖**：  
        *   Backend → `curl -v http://localhost:8000/healthz`  
        *   Vector DB → `docker exec -it weknora-postgres psql -U weknora -c "SELECT 1"`  
        *   Model Service → `curl -v http://localhost:8001/v1/models`（若启用）  
    4. **复现最小化**：使用 `curl` 直接调用 API，排除前端干扰。  
    5. **专项排查路径**：  
        *   **问答无响应/超时** → 查 Backend 日志是否有 `chat timeout` → 检查 Model Service 连通性与响应延迟 → 验证 `curl -X POST http://model-svc:8000/v1/chat/completions`  
        *   **文档上传后无检索结果** → 查 DocReader 日志是否完成 `parsed & embedded` → 检查 `kb_chunks` 表记录数 → 查询 `kb_embeddings` 是否有对应 `chunk_id` 向量  
        *   **UI 显示空白/报 500** → 查 Backend 日志 HTTP 5xx 错误 → 检查 PostgreSQL 连接池是否耗尽 → 验证 `SELECT 1` 是否成功  

*   版本升级指南（如有）、巡检手册（如有）、相关 aone  
    *   升级指南：参考 [GitHub Releases](https://github.com/Tencent/WeKnora/releases)，Docker Compose 用户修改 `docker-compose.yml` 中 `image` tag 并 `docker compose up -d`；Lite 用户下载新二进制覆盖旧版。  
    *   巡检项：每日检查 `docker compose ps` 状态、`docker compose logs -t --tail=100 app` 最新错误、`SELECT COUNT(*) FROM kb_embeddings` 增长趋势、DB 连接数、Model Service P99 延迟、DocReader 队列积压。  
    *   Aone：不适用（非阿里内部项目）；*注：WeKnora 为开源项目，无 aone 系统关联。*

### 典型问题排查解决方案

```yaml
（针对 backend_health_check_failed）
一、问题描述
● 问题现象：Web 页面白屏/报 502；curl http://localhost:8000/healthz 返回 500 或超时；文档上传按钮灰显。
● 适用范围：所有部署形态（Docker Compose/K8s/Lite/Desktop），v0.5.0+
二、排查信息收集
● 必须收集的信息：`docker compose ps app`（Docker）、`ps aux | grep weknora`（Lite）、`weknora --version`
● 检查终态的方法：登录 app 容器执行 `curl -v http://localhost:8000/healthz`；查看 `logs/backend.log`
● 排查问题步骤：
  - 步骤1：检查 PostgreSQL 是否就绪 → `docker compose exec postgres pg_isready -U weknora`
  - 步骤2：检查 Backend 日志末尾 → `tail -n 50 logs/backend.log | grep -i "error\|panic\|failed"`
  - 步骤3：检查内存占用 → `docker stats weknora-app`（OOM 会 kill 进程）
三、解决步骤
 场景一：PostgreSQL 连接失败
 - 适用条件：`pg_isready` 返回 `no response` 或日志含 `dial tcp: i/o timeout`
 - 实施步骤：`docker compose restart postgres` → 等待 10s → `docker compose restart app`
 - 结果验证：`curl http://localhost:8000/healthz` 返回 `{"status":"ok"}`
 场景二：Backend 内存溢出（OOMKilled）
 - 适用条件：`docker inspect weknora-app | grep OOMKilled` 为 true；日志含 `runtime: out of memory`
 - 实施步骤：编辑 `docker-compose.yml`，为 app 服务增加 `mem_limit: 2g`；`docker compose up -d`
 - 结果验证：`docker stats` 显示内存使用 < 1.8g
四、非本产品排查
● 若 `pg_isready` 失败且 `postgres` 容器状态为 Exited：属 PostgreSQL 镜像或存储卷损坏，需重建数据目录（备份 `./data/postgres` 后 `docker compose down -v`）。
五、快速定位工具
● 脚本位置：`scripts/diagnose.sh`（仓库根目录，需自行维护）
● 使用方法：`bash scripts/diagnose.sh health`（自动执行上述步骤1-2并输出摘要）
```

```yaml
（针对“文档上传后无法被检索到”）
一、问题描述
● 问题现象：用户上传 PDF 后，在问答界面输入相关内容，返回空结果或无关答案；Document Reader 日志显示 “parsed successfully”，但无 embedding 写入。
● 适用范围：v0.6.0，Docker Compose / Lite 部署，使用默认 PostgreSQL + pgvector。
二、排查信息收集
● 必须收集的信息：知识库 ID（kb_id）、文档 ID（doc_id）、WeKnora App Version、PostgreSQL 版本（`SELECT version();`）
● 检查终态的方法：登录 PostgreSQL 容器，执行 `psql -U weknora weknora`
● 排查问题步骤：
  - 查询文档状态：`SELECT id, status, parsed_at FROM kb_documents WHERE id = 'xxx';` → 应为 'processed'
  - 查询分块数量：`SELECT COUNT(*) FROM kb_chunks WHERE doc_id = 'xxx';` → 应 > 0
  - 查询向量数量：`SELECT COUNT(*) FROM kb_embeddings WHERE chunk_id IN (SELECT id FROM kb_chunks WHERE doc_id = 'xxx');` → 若为 0，则 embedding 未生成
三、解决步骤
 场景一：Model Service 返回 4xx/5xx 导致 embedding 失败
 - 适用条件：DocReader 日志含 `failed to call model service: 503` 或 `embedding generation error`
 - 实施步骤：
     1. 登录 DocReader 容器：`docker exec -it weknora-docreader sh`
     2. 测试模型服务：`curl -X POST http://weknora-model:8000/v1/embeddings -H "Content-Type: application/json" -d '{"input":"test"}'`
 - 结果验证：返回 `{"data":[{"embedding":[...]}]}` 即正常
 场景二：pgvector 扩展未启用
 - 适用条件：PostgreSQL 日志含 `function vec_add does not exist` 或 `column "embedding" has type vector`
 - 实施步骤：
     1. 进入 PostgreSQL：`docker exec -it weknora-postgres psql -U weknora weknora`
     2. 执行：`CREATE EXTENSION IF NOT EXISTS vector;`
 - 结果验证：`SELECT * FROM pg_extension WHERE extname = 'vector';` 返回一行
四、非本产品排查
● 若 `curl` 测试 Model Service 超时，需排查模型服务自身健康状态（非 WeKnora 组件问题），联系对应 LLM 服务维护方。
五、快速定位工具
● 脚本位置：`scripts/diagnose_doc_embedding.sh`（社区贡献脚本，见 [[诊断脚本|WeKnora-诊断脚本]]）
● 使用方法：`bash scripts/diagnose_doc_embedding.sh <kb_id> <doc_id>`
```

### 紧急场景止血与恢复手册：

*   **知识库误删恢复**：  
    *   止血：立即停止 Backend 服务（`docker compose stop app`），防止进一步写入。  
    *   恢复：从 `./data/postgres/` 备份目录（若启用）或 `pg_dump` 全量备份中恢复 `weknora` DB；若无备份，仅能从 `./data/documents/` 原始文件重新上传（丢失 chunk 元数据）。  
*   **向量索引损坏**：  
    *   止血：`docker compose exec postgres psql -U weknora -c "DROP INDEX IF EXISTS idx_chunks_vector;"`  
    *   恢复：`docker compose exec postgres psql -U weknora -c "CREATE INDEX idx_chunks_vector ON chunks USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);"`（重建索引）  
*   **向量库损坏导致全部检索失效**：  
    *   止血：立即停写（关闭 DocReader 服务），切换 Backend 配置 `search.enabled=false`，降级为关键词搜索（需提前开启 `fts_enabled`）  
    *   恢复：从备份恢复 `kb_embeddings` 表；或重建知识库（重传文档，确保 Model Service 可用）  
*   **Model Service 宕机引发问答雪崩超时**：  
    *   止血：Backend 配置 `model.fallback_to_empty_response=true`，返回空答案避免前端卡死  
    *   恢复：重启 Model Service，检查 GPU 内存/OOM 日志，扩容实例  

### 横向研发文档：

*   接入指引  
    *   新增向量数据库：实现 `vectorstore/Store` 接口（Go），注册到 `vectorstore.NewStore()` 工厂；参考 `vectorstore/pgvector/store.go`。  
    *   新增向量数据库适配：实现 `vectorstore/Store` interface，注册至 `vectorstore/factory.go`  
    *   新增文档解析器：实现 `document/Reader` interface，注入 `document/manager.go`  
*   产品对接方案细节  
    *   对接 LLM：在 `config.yaml` 中配置 `model_provider`（openai/ollama/custom）及 `api_base`；Backend 通过 `model/client.go` 统一调用。  
    *   支持通过 Webhook 接收外部系统推送的文档变更事件（需启用 `webhook.enabled`）  
    *   提供 `/api/v1/healthz` 和 `/api/v1/metrics`（Prometheus 格式）用于可观测性对接  
*   产品对接范围等  
    *   WeKnora 定位为 RAG 应用框架，**不提供**：用户身份认证（LDAP/OIDC）、审计日志（需对接 SIEM）、高可用集群（需用户 K8s 自行部署多副本）；**专注提供**：RAG 核心链路（Ingest → Retrieve → Generate）的开箱即用实现。  
    *   对接范围：仅限知识生命周期（上传→解析→索引→检索→生成）链路；不涉及用户认证（AuthN/AuthZ 交由前置网关）、计费、审计日志等企业级能力（v0.7+ 规划中）  

## 产品对内文档：

### 完整架构图：

*   系统，架构，调用关系，业务流，模块等深层知识点，供扩展使用，也可以写入一些已知问题  
    *   架构图源文件：`docs/architecture.drawio`（仓库中）；核心模块：`pkg/backend`（路由/服务）、`pkg/reader`（解析器抽象）、`pkg/vectorstore`（向量存储抽象）、`pkg/model`（模型客户端）。  
    *   已知问题：PDF 表格识别准确率受限于 `pdfplumber`；高并发下 pgvector IVFFLAT 索引召回率下降（需调优 `probes` 参数）；Tauri Desktop 在 macOS M1 上部分字体渲染异常（[#128](https://github.com/Tencent/WeKnora/issues/128)）。  
*   系统采用分层架构：  
    *   **Client Layer**：Web UI / Mobile SDK（社区孵化） / CLI  
    *   **API Layer**：Backend（Gin）统一入口，含 Auth、RateLimit、Trace  
    *   **Service Layer**：DocReader（Worker Pool）、Model Proxy（Adapter Pattern）、Vector Store（Abstraction）  
    *   **Data Layer**：PostgreSQL（KB Meta + Chunks + Embeddings）、Redis（Session/Cache）、MinIO/S3（原始文档存储，可选）  
    *   **Extensibility**：Plugin System（v0.7 架构设计中，支持动态加载 Reader/Embedder/Retriever）  

### 业务逻辑时序图

*   用户使用  
    *   上传文档：`UI → POST /api/v1/documents → Backend → Queue → DocReader → Save to PG → Notify Backend → Update UI`  
    *   问答请求：`UI → POST /api/v1/chat → Backend → VectorDB Search → ModelService Call → Stream Response → UI Render`  
    *   更完整流程：  
        1. 用户登录 → 前端获取 JWT Token → 后端校验 → 返回用户可访问 KB 列表  
        2. 用户选择 KB → 上传文档 → 前端调用 `/api/v1/kb/{id}/doc` → 后端入队 → DocReader 消费 → 解析+向量化 → 更新 DB  
        3. 用户输入 Query → 前端调用 `/api/v1/chat` → 后端检索 Top-K chunks → 拼装 Prompt → 调用 Model Service → 流式返回答案  

### 代码仓库

*   基线仓库  
    *   https://github.com/Tencent/WeKnora （主仓库，main 分支为稳定基线）  
*   代码仓库  
    *   同上（Go + React）  
*   制品仓库  
    *   GitHub Releases：https://github.com/Tencent/WeKnora/releases （含 Docker 镜像、Lite 二进制、Desktop 安装包、`.tar.gz`, `.exe`, `.dmg`, Docker Images on GHCR）  
*   关联依赖仓库等  
    *   `tencent/weknora-reader`（文档解析子项目，已归档至主仓）  
    *   `tencent/weknora-model-proxy`（模型代理，已合并至主仓 `pkg/model`）  
    *   `tencent/WeKnora-docs`（文档站点）  
    *   `tencent/WeKnora-helm`（Kubernetes Chart）  
    *   `tencent/WeKnora-cli`（命令行工具）  

### 数据表结构

*   `kb_knowledge_bases`  
    | Column | Type | Description |  
    |---|---|---|  
    | `id` | UUID PK | 知识库唯一标识 |  
    | `name` | VARCHAR(255) | 名称 |  
    | `description` | TEXT | 描述 |  
    | `created_at` | TIMESTAMPTZ | 创建时间 |  

*   `kb_documents`  
    | Column | Type | Description |  
    |---|---|---|  
    | `id` | UUID PK | 文档唯一标识 |  
    | `kb_id` | UUID FK → kb_knowledge_bases.id | 所属知识库 |  
    | `file_name` | VARCHAR(512) | 原文件名 |  
    | `status` | VARCHAR(32) | `uploading`/`parsing`/`processed`/`failed` |  
    | `parsed_at` | TIMESTAMPTZ | 解析完成时间 |  

*   `kb_chunks`  
    | Column | Type | Description |  
    |---|---|---|  
    | `id` | UUID PK | 分块唯一标识 |  
    | `doc_id` | UUID FK → kb_documents.id | 来源文档 |  
    | `content` | TEXT | 清洗后文本 |  
    | `token_count` | INT | 估算 token 数 |  

*   `kb_embeddings`  
    | Column | Type | Description |  
    |---|---|---|  
    | `id` | UUID PK | 向量唯一标识 |  
    | `chunk_id` | UUID FK → kb_chunks.id | 对应分块 |  
    | `embedding` | VECTOR(1024) | pgvector 类型向量（维度依模型而定） |  
    | `updated_at` | TIMESTAMPTZ | 更新时间 |