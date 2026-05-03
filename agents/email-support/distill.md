# Distillation Instructions: Email Support

Purpose:
- Convert broad company material into a compact email-support knowledge base that helps draft accurate replies quickly.

What to extract:
- Reusable explanations of what the company sells
- Safe product descriptions and claims that can be repeated in emails
- Common buyer or customer questions and the best supported answers
- Response patterns for pricing requests, lead-time questions, feature questions, and next-step coordination
- Escalation triggers where a human should confirm details before a reply is sent

What to ignore unless clearly relevant:
- Internal brainstorming that does not improve customer communication
- Legal or administrative material with no impact on a reply
- Long background narratives that can be reduced to short factual notes
- Details that are uncertain, outdated, or unsupported by source material

Output expectations:
- Write concise markdown files into `distilled/`
- Prefer short reusable bullet lists and response snippets over long essays
- Keep a clear separation between safe confirmed statements and open questions
- Focus on material that speeds up drafting real email replies

Recommended distilled files:
- `product_facts.md`
- `common_questions.md`
- `response_patterns.md`
- `escalation_rules.md`
- `tone_guidelines.md`
- `open_information_gaps.md`

Quality bar:
- Another agent session should be able to draft a solid first-pass email reply using the distilled files alone
- The distilled context should stay short enough for routine loading and easy manual review
