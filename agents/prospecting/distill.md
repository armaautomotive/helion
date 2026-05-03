# Distillation Instructions: Prospecting

Purpose:
- Convert broad company documents into a compact prospecting-specific knowledge base.

What to extract:
- Which industries and company types are likely buyers
- What operational pain the CNC tube notcher solves
- What product claims are credible and safe to repeat
- What buyer signals suggest urgency or strong fit
- What disqualification signals suggest poor fit
- Any past sales patterns that indicate where the product resonates

What to ignore unless clearly relevant:
- General admin documents
- Legal details with no sales impact
- Internal operational notes that do not change buyer fit or messaging
- Long-form text that can be reduced to short bullet points

Output expectations:
- Write concise, factual markdown files into `distilled/`
- Prefer short bullet lists over essays
- Separate stable truths from tentative assumptions
- Do not invent claims or specs that are not supported by source material

Recommended distilled files:
- `ideal_customer_profile.md`
- `pain_points.md`
- `buyer_signals.md`
- `disqualifiers.md`
- `product_claims.md`
- `sales_patterns.md`

Quality bar:
- The distilled context should be short enough to load routinely
- Another agent session should be able to use the distilled files without rereading the full company corpus
