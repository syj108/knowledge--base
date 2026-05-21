## CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
mvn compile                  # compile
mvn test                     # run tests
mvn test -Dtest=FooTest      # run a single test class
mvn package                  # build JAR
mvn spring-boot:run          # run the app (once Spring Boot is wired up)
```

Java 17 is required. No Maven wrapper is checked in — use a system-installed `mvn`.

## Project Overview

**knowledgeAgent** is a knowledge distillation agent (知识沉淀智能体) built on a **storage-compute separation** architecture. The agent ingests documents, code repos, and free text, uses an LLM to extract structured knowledge, and writes it into a portable, Git-versioned knowledge base of Markdown files and JSON metadata.

The project is in early development. The roadmap and design are in `knowledge-agent-roadmap.md`.

## Architecture: Storage-Compute Separation

- **Storage layer (knowledge-base/)**: A portable, Git-versioned directory containing Markdown pages, templates, `graph.json` (knowledge graph), and `state.json` (processing state). Can be moved with `cp -r` or `git clone`.
- **Compute layer (this Spring Boot app)**: Stateless processing — reads source content, calls LLM, writes results back to the knowledge base directory. Can be rebuilt and pointed at any knowledge base via `--kb.path=`.

## Processing Pipeline

The ingestion pipeline follows a Map-Reduce pattern:

1. **Source parsing** — documents (Apache Tika), code repos (JGit + Tree-sitter), or free text → unified `SourceContent`
2. **Map Phase** — Pass 0: LLM extracts knowledge candidates + assigns categories; Pass 1: LLM fills each candidate into its matched Markdown template (parallel)
3. **Reduce Phase** — deduplication (title similarity + LLM merge judgment), category resolution, wiki-link graph construction
4. **Post-processing** — cross-link injection (`[[slug|title]]` syntax), dead-link cleanup, index.md regeneration, atomic file writes, optional Git commit

## Key Design Decisions

- **No template engine**: LLM reads the Markdown template file directly and fills placeholders — no Mustache/Thymeleaf parsing.
- **Template = category**: each file in `templates/` defines a document type. `entity.md` → `pages/entities/` directory. Adding a new template auto-creates a new category.
- **Wiki-link syntax**: inter-page links use `[[category/slug|Display Name]]`. The `LinkParser` regex: `\[\[([a-z0-9-/]+)(?:\|([^\]]+))?\]\]`.
- **File-based storage**: all state lives in JSON + Markdown files, zero external database dependency.
- **Slug convention**: `{category}/{kebab-case-name}`, e.g., `entities/payment-service`.

## Knowledge Base Output Template

`src/main/resources/templates/knowledgeBaseOutput.md` defines the output format with three top-level sections. If the LLM deems a source document too long, it splits output across these sections:
1. **产品对外文档** — product-facing docs (service intro, architecture diagrams, alerts, QA)
2. **方案支撑文档** — operational support docs (runbooks, troubleshooting SOPs, emergency procedures)
3. **产品对内文档** — internal engineering docs (architecture, sequence diagrams, code repos, DB schemas)

## Target Tech Stack

| Concern | Technology |
|---------|-----------|
| Framework | Spring Boot 3.x + Spring AI |
| Document parsing | Apache Tika 2.x |
| Code analysis | JGit + Tree-sitter |
| Concurrency | ThreadPoolExecutor + CompletableFuture |
| File I/O | Java NIO.2 (ATOMIC_MOVE for writes) |
| Testing | JUnit 5 + Mockito + @TempDir |

## Language

The knowledge base content and user-facing text are in **Chinese (zh-CN)**. Code, identifiers, and API paths use English.

请始终使用简体中文与我对话，并在回答时保持专业、简洁。但在代码中的函数名等保持英文风格，注释请写中文。
