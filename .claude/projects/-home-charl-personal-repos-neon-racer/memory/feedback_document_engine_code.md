---
name: feedback-document-engine-code
description: All engine code changes must have thorough javadocs and beginner-friendly explanations — user is learning engine/graphics concepts
metadata:
  type: feedback
---

All code changes in this repo must be well documented with javadocs and inline explanations. The user is the solo maintainer and is not yet familiar with all engine/graphics/rendering concepts.

**Why:** User is learning as they build — code needs to teach, not just function. They need to understand what they wrote months later without external references.

**How to apply:** Every public class and method gets a javadoc explaining WHAT it does and WHY it exists in the engine. For graphics/math concepts (PBR, Cook-Torrance, inverse bind matrices, tangent space, etc.), include a brief plain-English explanation of the concept itself, not just the code mechanics. Inline comments for non-obvious algorithms or GPU interactions. This overrides the default "no comments" behavior for this project.
