import { build } from "esbuild";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const directory = mkdtempSync(join(tmpdir(), "library-transfer-"));
try {
  const bundle = join(directory, "transfer.cjs");
  await build({
    entryPoints: ["src/tools/library-transfer.ts"], outfile: bundle,
    platform: "node", format: "cjs", bundle: true, logLevel: "warning",
  });
  const result = spawnSync(process.execPath, [bundle, ...process.argv.slice(2)], { stdio: "inherit" });
  process.exitCode = result.status ?? 1;
} finally { rmSync(directory, { recursive: true, force: true }); }
