# Company Data

This directory is for broad company-wide documents that multiple agents may need.

Examples:
- Product notes
- Website copy
- Sales orders
- Email history summaries
- Contracts
- Support issues
- Internal process notes

Guidelines:
- Prefer text or markdown exports when possible
- Avoid putting secrets here unless you intend local agents to read them
- Large raw material can live here even if it is messy

Agents should distill relevant facts from this folder into their own `agents/<agent-id>/distilled/` directories.
