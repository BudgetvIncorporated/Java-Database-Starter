import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

import { chromium } from 'playwright-core';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(scriptDir, '..');
const manifest = JSON.parse(
  readFileSync(path.join(projectRoot, 'src/data/slides-manifest.json'), 'utf8'),
) as Array<{ id: string; title: string; position: number }>;

async function availablePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      if (address === null || typeof address === 'string') {
        server.close();
        reject(new Error('Could not select a local port for layout validation.'));
        return;
      }
      server.close(() => resolve(address.port));
    });
  });
}

async function main(): Promise<void> {
  const port = await availablePort();
  const vite = spawn(
    'pnpm',
    ['exec', 'vite', '--config', 'vite.config.ts', '--host', '127.0.0.1'],
    {
      cwd: projectRoot,
      env: { ...process.env, PORT: String(port), BASE_PATH: '/' },
      stdio: ['ignore', 'pipe', 'pipe'],
    },
  );
  let serverOutput = '';
  vite.stdout.on('data', (chunk: Buffer) => { serverOutput += chunk.toString(); });
  vite.stderr.on('data', (chunk: Buffer) => { serverOutput += chunk.toString(); });

  let browser;
  try {
    const url = `http://127.0.0.1:${port}/allslides`;
    let ready = false;
    for (let attempt = 0; attempt < 80; attempt += 1) {
      if (vite.exitCode !== null) {
        throw new Error(`Vite stopped before layout validation:\n${serverOutput}`);
      }
      try {
        const response = await fetch(url);
        if (response.ok) {
          ready = true;
          break;
        }
      } catch {
        // Vite is still starting.
      }
      await delay(250);
    }
    if (!ready) {
      throw new Error(`Vite did not become ready for layout validation:\n${serverOutput}`);
    }

    browser = await chromium.launch({
      executablePath: process.env.CHROMIUM_PATH ?? '/repl/tools/bin/chromium',
      headless: true,
      args: ['--no-sandbox', '--disable-dev-shm-usage'],
    });
    const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
    await page.goto(url, { waitUntil: 'networkidle' });
    await page.evaluate('document.fonts.ready');
    await page.waitForFunction(
      'expectedCount => document.querySelectorAll(".slide").length === expectedCount',
      manifest.length,
    );
    await page.addScriptTag({
      path: path.join(scriptDir, 'layout-check-browser.js'),
    });
    const issues = await page.evaluate('window.runSlideLayoutCheck()') as {
      slideCount: number;
      overlapIssues: string[];
    };

    if (issues.slideCount !== manifest.length) {
      throw new Error(
        `Layout check rendered ${issues.slideCount} slides; manifest has ${manifest.length}.`,
      );
    }
    if (issues.overlapIssues.length > 0) {
      const titles = new Map(manifest.map((slide) => [slide.id, slide.title]));
      console.error(`Slide layout check found ${issues.overlapIssues.length} issue(s):`);
      for (const issue of issues.overlapIssues) {
        const match = issue.match(/^(.*?): (.*)$/);
        const title = match ? titles.get(match[1]) : undefined;
        console.error(`- ${title ? `${title}: ` : ''}${match?.[2] ?? issue}`);
      }
      process.exitCode = 1;
      return;
    }
    console.log(`✓ Slide layout check passed (${issues.slideCount} slides; no text overlaps)`);
  } finally {
    await browser?.close();
    vite.kill('SIGTERM');
  }
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
