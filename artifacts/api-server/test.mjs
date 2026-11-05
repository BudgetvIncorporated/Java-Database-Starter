import { build } from "esbuild";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const directory = mkdtempSync(join(tmpdir(), "game-library-tests-"));
try {
  const bundle = join(directory, "library.test.cjs");
  await build({
    entryPoints: ["src/test/library.test.ts"],
    outfile: bundle,
    platform: "node",
    format: "cjs",
    bundle: true,
    logLevel: "warning",
  });
  const result = spawnSync(process.execPath, ["--test", bundle], { stdio: "inherit" });
  process.exitCode = result.status ?? 1;
} finally {
  rmSync(directory, { recursive: true, force: true });
}
