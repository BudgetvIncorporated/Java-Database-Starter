import { build } from "esbuild";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const directory = mkdtempSync(join(tmpdir(), "game-library-tests-"));
try {
  await build({
    entryPoints: ["src/test/library.test.ts", "src/test/library-postgres.test.ts"],
    outdir: directory,
    outExtension: { ".js": ".cjs" },
    platform: "node",
    format: "cjs",
    bundle: true,
    logLevel: "warning",
  });
  const result = spawnSync(process.execPath, ["--test",
    join(directory, "library.test.cjs"), join(directory, "library-postgres.test.cjs")], { stdio: "inherit" });
  process.exitCode = result.status ?? 1;
} finally {
  rmSync(directory, { recursive: true, force: true });
}
