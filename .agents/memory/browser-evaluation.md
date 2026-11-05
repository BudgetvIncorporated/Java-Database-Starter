---
name: Browser evaluation isolation
description: A tsx/esbuild quirk when passing complex closures to browser automation.
---

Browser-side validation code must not depend on helpers injected by the Node-side TypeScript transpiler.

**Why:** Running a complex DOM-check closure through Playwright from tsx produced `ReferenceError: __name is not defined`: esbuild had inserted a naming helper outside the serialized browser closure.

**How to apply:** For substantial browser-side checks, inject self-contained plain JavaScript and evaluate a simple expression. Do not assume a closure that runs in Node will serialize with its surrounding compiler helpers.
