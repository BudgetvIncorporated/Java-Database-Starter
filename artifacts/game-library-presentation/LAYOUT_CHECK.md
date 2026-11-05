# Check layout before exporting

Run from the workspace root:

```sh
pnpm --filter @workspace/game-library-presentation run validate-slides
```

This validates the manifest, starts a temporary Vite instance of the actual app,
and uses headless Chromium to measure all nine rendered slides on `/allslides`
at the export size of 1920 × 1080. It waits for fonts before measuring.
The command exits unsuccessfully and names the slide, content column or graphic,
and offending text when it finds a collision or overflow.

To verify that the checker still catches the known slide 9 regression:

```sh
pnpm --filter @workspace/game-library-presentation run test:slide-layout
```

The regression check temporarily widens slide 9's bullets and then introduces
unwrapped text in the browser only. It must catch both a graphic collision and
content-column overflow. No slide files are changed.

## Maintaining coverage

- Mark bounded text groups with `data-layout-content="descriptive column name"`.
- Mark graphics that must remain free of text with
  `data-layout-reserved="descriptive graphic name"`.
- Do not mark intentional backgrounds, gradients or underlays as reserved:
  text over a background is not a collision.
- Every slide must have at least one marked content column. All slide text,
  including SVG labels, is also checked against the slide frame.
- Small vertical font-metric overhang is allowed; horizontal column overflow
  and intersections with reserved graphic rectangles are not.

The checker uses `/repl/tools/bin/chromium` in Replit. Elsewhere, set
`CHROMIUM_PATH` to a Chromium executable. It starts and stops its own temporary
server; the preview workflow does not need to be running. This is a pre-export
check, not an override of the platform's export controls.
