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

## OpenAI manager with llama.cpp workers

Set these environment variables:

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
