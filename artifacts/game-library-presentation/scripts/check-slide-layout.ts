import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

import { chromium, type Browser } from 'playwright-core';

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
      detached: true,
    },
  );
  let serverOutput = '';
  vite.stdout.on('data', (chunk: Buffer) => { serverOutput += chunk.toString(); });
  vite.stderr.on('data', (chunk: Buffer) => { serverOutput += chunk.toString(); });

  let browser: Browser | undefined;
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
    // Inject plain JS: tsx can add closure helpers that do not exist in the browser.
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
    if (process.argv.includes('--self-test')) {
      const reflection = manifest.find((slide) => slide.position === 9);
      if (!reflection) throw new Error('Missing slide 9 regression fixture.');
      // Mutate only this temporary browser DOM, never the deck source.
      await page.evaluate(`document.querySelector('[data-layout-content="reflection bullets"]').style.maxWidth = '86vw'`);
      const collision = await page.evaluate('window.runSlideLayoutCheck()') as typeof issues;
      if (!collision.overlapIssues.some((issue) =>
        issue.startsWith(reflection.id) && issue.includes('right-side illustration panel'))) {
        throw new Error('Regression check failed: slide 9 graphic collision was not detected.');
      }
      await page.evaluate(`document.querySelector('[data-layout-content="reflection bullets"]').style.maxWidth = '55vw';
        document.querySelector('[data-layout-content="reflection bullets"] li').style.whiteSpace = 'nowrap'`);
      const overflow = await page.evaluate('window.runSlideLayoutCheck()') as typeof issues;
      if (!overflow.overlapIssues.some((issue) =>
        issue.startsWith(reflection.id) && issue.includes('leaves "reflection bullets" column'))) {
        throw new Error('Regression check failed: slide 9 column overflow was not detected.');
      }
      console.log('✓ Regression checks passed: slide 9 graphic collision and column overflow detected');
    }
  } finally {
    await browser?.close();
    if (vite.pid !== undefined) {
      try {
        process.kill(-vite.pid, 'SIGTERM');
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ESRCH') throw error;
      }
    }
  }
}

main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
