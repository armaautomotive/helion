# Distillation Instructions: Supervisor

Purpose:
- Convert broad system and company information into a compact supervision knowledge base for monitoring agent performance.

What to extract:
- Which agents exist and what they are supposed to do
- What healthy output looks like for each agent
- Which runtime signals indicate success, failure, or stalled work
- Which settings are important for agent execution, cadence, and escalation
- Any recurring operational patterns worth tracking over time

What to ignore unless clearly relevant:
- Detailed business content that does not affect whether an agent is functioning properly
- Long historical notes with no operational value
- Internal background information that does not change supervision decisions

Output expectations:
- Write concise markdown files into `distilled/`
- Prefer checklists, thresholds, and signal summaries over essays
- Focus on what helps evaluate agent health and intervention decisions

Recommended distilled files:
- `agent_expectations.md`
- `health_signals.md`
- `failure_patterns.md`
- `intervention_rules.md`
- `review_checklist.md`

Quality bar:
- The distilled context should let the supervisor agent judge whether another agent is healthy without rereading the full workspace
- The files should remain compact and operationally useful
