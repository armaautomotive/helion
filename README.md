# helion

`helion` is a Java business agent CLI. It can take a business prompt and produce a structured response for planning, analysis, outreach, or general strategy work. It now supports a manager/worker runtime, optional internet browsing, and local llama.cpp workers.

## Features

- CLI entry point: `helion.Helion`
- Modes: `plan`, `analyze`, `email`, `general`
- Manager/worker architecture
- Providers:
  - `demo` provider for offline use
  - `openai` for a hosted manager
  - `llama.cpp` for local workers
- Optional browser search and page fetch tools
- File-backed reusable memory
- File-backed business knowledge base
- Multi-agent filesystem layout with agent-specific workspaces
- Structured manager protocol with canonical final-response sections
- Structured outputs aimed at business tasks

## Requirements

- Java 17+
- Maven 3.9+

## Run

```bash
mvn -q exec:java -Dexec.args="plan Launch a new B2B robotics consultancy"
```

If you omit the prompt, `helion` reads from standard input.

## Session Mode

You can keep `helion` running in an interactive terminal session instead of exiting after one prompt:

```bash
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
java -cp out helion.Helion --session analyze
```

Available session commands:

- `/help`
- `/distill`
- `/mode`
- `/mode plan`
- `/mode analyze`
- `/mode email`
- `/mode general`
- `/quit`

## Web UI

You can also start a local browser UI:

```bash
./run.sh --web
```

To start it and try to open your default browser automatically:

```bash
./run.sh --web-open
```

Then open:

```text
http://127.0.0.1:8421
```

Config keys:

```properties
helion.web.host=127.0.0.1
helion.web.port=8421
```

The web UI currently supports:

- selecting an agent
- viewing agent status, role, distilled context, and workspace context
- sending prompts to the selected agent
- listing, adding, editing, and deleting company data source directories
- viewing per-model usage stats

From terminal session mode, you can also run:

- `/distill`
- `/openweb`
- `/usage`

## Model Usage Stats

`helion` now records model usage per provider/model and aggregates:

- daily tokens
- monthly tokens
- yearly tokens
- request counts

Config key:

```properties
helion.usage.events_file=.helion/usage_events.tsv
```

Notes:

- OpenAI usage is recorded from API response usage fields when available.
- Local models such as `llama.cpp` may not return token accounting, so `helion` falls back to token estimation for those calls.
- The web UI shows usage summaries, and terminal session mode supports `/usage`.

## Config File

`helion` now supports a local `helion.properties` file in the project root. Environment variables still work and override file values when both are present.

Start by copying:

```bash
cp helion.properties.example helion.properties
```

Then edit `helion.properties`:

```properties
helion.manager.provider=openai
helion.manager.model=gpt-4o-mini
helion.openai.api_key=your_key_here
helion.worker.provider=llama.cpp
helion.worker.model=your-loaded-model-name
helion.worker.count=3
helion.llamacpp.url=http://localhost:8080
helion.enable.browser=true
helion.enable.memory=true
```

The real `helion.properties` file is ignored by git. Commit `helion.properties.example`, not your secrets.

## OpenAI manager with llama.cpp workers

Set these environment variables if you prefer env-based config:

```bash
export HELION_MANAGER_PROVIDER=openai
export OPENAI_API_KEY=your_key_here
export HELION_MANAGER_MODEL=gpt-4o-mini
export HELION_WORKER_PROVIDER=llama.cpp
export HELION_WORKER_MODEL=qwen2.5-7b-instruct
export HELION_WORKER_COUNT=3
export HELION_LLAMACPP_URL=http://localhost:8080
```

Then run:

```bash
mvn -q exec:java -Dexec.args="analyze Improve retention for a subscription design agency"
```

## Browser-enabled mode

```bash
export HELION_ENABLE_BROWSER=true
export HELION_BROWSER_RESULT_LIMIT=5
export HELION_BROWSER_FETCH_CHAR_LIMIT=4000
```

With browsing enabled, the manager can issue search and fetch actions during its reasoning loop.

## Memory

By default, `helion` stores reusable notes under `.helion/memory`.

```bash
export HELION_ENABLE_MEMORY=true
export HELION_MEMORY_DIR=.helion/memory
export HELION_MEMORY_NAMESPACE=acme
```

The manager can explicitly read and write memory notes during a run, and `helion` also stores a compact final-answer summary automatically.

## Knowledge Base

`helion` can load business and product context from the local `knowledge/` directory. This is the right place to store facts about your company, products, buyers, differentiators, and pain points.

Starter files are included:

- `knowledge/company_overview.md`
- `knowledge/products/cnc_tube_notcher.md`
- `knowledge/sales/prospecting_notes.md`

Config keys:

```properties
helion.enable.knowledge=true
helion.knowledge.dir=knowledge
helion.knowledge.char_limit=12000
```

The knowledge base is injected automatically into agent runs, so the manager and workers can reason from your local business context before they browse for prospects.

## Multi-Agent Layout

`helion` now supports named agents with their own role, status, distilled context, and workspace files.

Directory layout:

```text
agents/
  prospecting/
    role.md
    distill.md
    status.md
    distilled/
    workspace/

Email configuration is supported through `helion.properties` for a draft-first workflow:

```properties
helion.email.enabled=true
helion.email.provider=imap_smtp
helion.email.display_name=Arma Automotive
helion.email.address=you@example.com
helion.email.imap.host=imap.example.com
helion.email.imap.port=993
helion.email.imap.username=you@example.com
helion.email.imap.password=replace_me
helion.email.imap.ssl=true
helion.email.smtp.host=smtp.example.com
helion.email.smtp.port=465
helion.email.smtp.username=you@example.com
helion.email.smtp.password=replace_me
helion.email.smtp.ssl=true
```

Current email support is draft-only. It does not send or sync mail yet.

In session mode:

```text
/emailconfig
/draftemail customer@example.com | CNC tube notcher question | Thanks for reaching out. Here is a first-pass reply...
```

Drafts are appended to the current agent workspace, for example:

- `agents/email-support/workspace/reply_drafts.md`
company_data/
```

Shared `company_data/` is for broad internal documents. Each agent can keep a cleaner, role-specific view of relevant information under its own `distilled/` directory.

You do not have to copy files into `company_data/`. `helion` can also read from a managed list of real directories elsewhere on disk, using:

```properties
helion.company_data.sources_file=.helion/company_data_sources.txt
```

If source directories are configured there, agents will read from those paths directly. The local `company_data/` directory remains a fallback location.

The first starter agent is:

- `prospecting`

There is also a reusable scaffold for future agents under:

- `agents/_template/`

Run a one-shot prompt against that agent:

```bash
java -cp out helion.Helion --agent prospecting general "Find likely buyer profiles for our CNC tube notcher"
```

Run a session against that agent:

```bash
java -cp out helion.Helion --session --agent prospecting analyze
```

Session commands now include:

- `/distill`
- `/company`
- `/company add <path>`
- `/company edit <index> <path>`
- `/company delete <index>`
- `/agents`
- `/agent`
- `/agent prospecting`
- `/status`

Distillation workflow:

- `role.md` defines the agent’s responsibilities
- `distill.md` defines what relevant facts should be extracted from shared company data
- `/distill` refreshes the selected agent’s `distilled/` files using its role, distillation instructions, shared knowledge, and configured company-data sources
- runnable agents also perform a background distill check every `helion.distill.check_seconds` seconds
- if shared company files, shared knowledge files, or the agent’s role/distill files are newer than the last distill state, Helion refreshes that agent’s `distilled/` files automatically before normal work continues
- background distill refresh uses the agent’s selected execution target and preferred local pool when applicable

## Manager Protocol

Manager turns now work against structured observations and should emit exactly one action block at a time. Final answers are normalized to a canonical schema with:

- `STATUS`
- `TITLE`
- `SUMMARY`
- `DETAILS`
- `NEXT_STEPS`
- `SOURCES`

If the model returns plain text instead, `helion` still coerces it into a stable final output.

## Direct `javac` run

```bash
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
java -cp out helion.Helion plan "Launch a B2B AI operations consultancy for manufacturers"
```

## Demo mode

If no supported live provider is configured, `helion` falls back to a local rule-based business advisor so the program still works.
