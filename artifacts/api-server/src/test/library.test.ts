import assert from "node:assert/strict";
import test from "node:test";
import { existsSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { DatabaseSync, backup } from "node:sqlite";
import express from "express";
import { createLibraryRouter } from "../routes/library";
import { findLibraryDatabase, LibraryStore } from "../lib/library-store";

test("browser API uses the existing SQLite model with CRUD, validation, cascades, and persistence", async (t) => {
  const directory = mkdtempSync(join(tmpdir(), "game-library-data-"));
  const databasePath = join(directory, "library.db");
  const source = new DatabaseSync(findLibraryDatabase(), { readOnly: true });
  await backup(source, databasePath);
  source.close();
  const store = new LibraryStore(databasePath);
  const app = express();
  app.use(express.json());
  app.use("/api/library", createLibraryRouter(store));
  const server = app.listen(0, "127.0.0.1");
  await new Promise<void>((resolve) => server.once("listening", resolve));
  const address = server.address();
  assert(address && typeof address === "object");
  const base = `http://127.0.0.1:${address.port}/api/library`;
  // HTTP responses are intentionally untyped here: assertions check the actual wire payload.
  async function request(method: string, path: string, body?: unknown, status = 200): Promise<any> {
    const response = await fetch(`${base}${path}`, {
      method, headers: { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const data = response.status === 204 ? undefined : await response.json();
    assert.equal(response.status, status, JSON.stringify(data));
    return data;
  }
  let gameId = 0;
  let userId = 0;
  let secondUserId = 0;
  try {
    const genres = await request("GET", "/genres");
    assert(genres.length > 0);
    const genreId = genres[0].id;
    const game = { genreId, title: "Browser integration fixture", url: "https://example.com/play", releaseDate: "2020-02-29" };
    const player = { username: "browser-integration-fixture", email: "browser-integration@example.com" };
    await t.test("real catalog, genre filters, and baseline statistics", async () => {
      const summary = await request("GET", "/summary");
      assert(summary.games > 0 && summary.players > 0);
      const games = await request("GET", `/games?genreId=${genreId}`);
      assert(games.every((entry: { genreId: number }) => entry.genreId === genreId));
      assert.deepEqual(await request("GET", "/games?search=%27%20OR%201%3D1--"), []);
      await request("GET", "/statistics");
    });
    await t.test("create, search, edit, and duplicate game validation", async () => {
      const created = await request("POST", "/games", game, 201);
      gameId = created.id;
      assert.equal(created.genreId, genreId);
      await request("POST", "/games", game, 409);
      await request("POST", "/games", { ...game, releaseDate: "2020-02-30" }, 400);
      await request("POST", "/games", { ...game, url: "javascript:alert(1)" }, 400);
      await request("POST", "/games", { ...game, title: " " }, 400);
      await request("POST", "/games", { ...game, title: "Missing genre fixture", genreId: 9999999 }, 400);
      const updated = await request("PUT", `/games/${gameId}`, { ...game, title: `${game.title} updated` });
      assert.equal(updated.title, `${game.title} updated`);
      const matches = await request("GET", `/games?search=Browser%20integration&genreId=${genreId}`);
      assert(matches.some((entry: { id: number }) => entry.id === gameId));
    });
    await t.test("player CRUD, duplicate errors, and zero-save statistics", async () => {
      userId = (await request("POST", "/players", player, 201)).id;
      secondUserId = (await request("POST", "/players", {
        username: `${player.username}-second`, email: "browser-integration-second@example.com",
      }, 201)).id;
      await request("POST", "/players", player, 409);
      await request("POST", "/players", { ...player, email: "invalid" }, 400);
      const updated = await request("PUT", `/players/${userId}`, { ...player, username: `${player.username}-updated` });
      assert.equal(updated.id, userId);
      const stats = await request("GET", "/statistics");
      const empty = stats.find((row: { userId: number }) => row.userId === userId);
      assert.equal(empty.savedGames, 0);
      assert.equal(empty.totalPlayCount, 0);
      assert.equal(empty.averageRating, null);
    });
    await t.test("saved-game composite identity, nullable ratings, validation, and aggregates", async () => {
      await request("POST", "/saved", { userId, gameId, rating: null, playCount: 0 }, 201);
      await request("POST", "/saved", { userId: secondUserId, gameId, rating: 2, playCount: 3 }, 201);
      await request("POST", "/saved", { userId, gameId, rating: 4, playCount: 0 }, 409);
      await request("PUT", `/saved/${userId}/${gameId}`, { rating: 6, playCount: 1 }, 400);
      await request("PUT", `/saved/${userId}/${gameId}`, { rating: 4, playCount: -1 }, 400);
      await request("PUT", `/saved/${userId}/${gameId}`, { rating: 4, playCount: 1.5 }, 400);
      await request("PUT", `/saved/${userId}/${gameId}`, { rating: 5, playCount: 10 });
      const saved = await request("GET", "/saved");
      assert.equal(saved.find((row: { userId: number; gameId: number }) => row.userId === userId && row.gameId === gameId).playCount, 10);
      assert.equal(saved.find((row: { userId: number; gameId: number }) => row.userId === secondUserId && row.gameId === gameId).playCount, 3);
      const stats = (await request("GET", "/statistics")).find((row: { userId: number }) => row.userId === userId);
      assert.equal(stats.totalPlayCount, 10);
      assert.equal(stats.averageRating, 5);
      await request("DELETE", `/saved/${userId}/${gameId}`, undefined, 204);
      assert((await request("GET", "/games")).some((row: { id: number }) => row.id === gameId));
      assert((await request("GET", "/players")).some((row: { id: number }) => row.id === userId));
      await request("POST", "/saved", { userId, gameId, rating: null, playCount: 2 }, 201);
    });
    await t.test("writes survive a new SQLite connection", async () => {
      const reopened = new LibraryStore(databasePath);
      try {
        const row = reopened.db.prepare("SELECT rating, play_count AS plays FROM user_games WHERE user_id = ? AND game_id = ?").get(userId, gameId);
        assert.equal(row?.rating, null);
        assert.equal(row?.plays, 2);
        assert(reopened.db.prepare("SELECT title FROM games WHERE game_id = ?").get(gameId));
      } finally { reopened.db.close(); }
    });
    await t.test("backup access fails closed without private-workspace management access", async () => {
      const previous = process.env.NODE_ENV;
      try {
        for (const environment of ["production", "test", undefined]) {
          if (environment === undefined) delete process.env.NODE_ENV;
          else process.env.NODE_ENV = environment;
          const response = await fetch(`${base}/backup`, { headers: { "Sec-Fetch-Site": "same-origin" } });
          assert.equal(response.status, 403);
          assert.equal(response.headers.get("cache-control"), "no-store");
          assert.equal(response.headers.get("content-disposition"), null);
          const data = await response.json() as { error: string };
          assert.match(data.error, /management access/);
        }
        process.env.NODE_ENV = "development";
        for (const site of [undefined, "cross-site", "same-site", "none"]) {
          const response = await fetch(`${base}/backup`, { headers: site ? { "Sec-Fetch-Site": site } : {} });
          assert.equal(response.status, 403);
        }
      } finally {
        if (previous === undefined) delete process.env.NODE_ENV;
        else process.env.NODE_ENV = previous;
      }
    });
    await t.test("download opens standalone with four intact tables and committed WAL data, without changing the live database", async () => {
      const previous = process.env.NODE_ENV;
      process.env.NODE_ENV = "development";
      store.db.exec("PRAGMA journal_mode = WAL;");
      const writer = new DatabaseSync(databasePath);
      let temporaryPath: string | undefined;
      const createBackup = store.createBackup.bind(store);
      store.createBackup = async () => {
        const result = await createBackup();
        temporaryPath = result.path;
        return result;
      };
      try {
        // A Java-like second connection has an active, uncommitted write.
        // The download must include the committed value, not that write.
        writer.exec("BEGIN IMMEDIATE");
        writer.prepare("UPDATE user_games SET play_count = 999 WHERE user_id = ? AND game_id = ?").run(userId, gameId);
        const expected = new Map(["genres", "games", "users", "user_games"].map(table => [
          table, store.db.prepare(`SELECT * FROM ${table} ORDER BY rowid`).all(),
        ]));
        const response = await fetch(`${base}/backup`, { headers: { "Sec-Fetch-Site": "same-origin" } });
        assert.equal(response.status, 200);
        assert.equal(response.headers.get("content-type"), "application/vnd.sqlite3");
        assert.equal(response.headers.get("cache-control"), "no-store");
        assert.match(response.headers.get("content-disposition") ?? "", /attachment; filename="?game-library-.*\.db"?$/);
        const bytes = Buffer.from(await response.arrayBuffer());
        assert.equal(bytes.subarray(0, 16).toString(), "SQLite format 3\0");
        const downloadedPath = join(directory, "downloaded.db");
        writeFileSync(downloadedPath, bytes);
        const downloaded = new DatabaseSync(downloadedPath);
        try {
          assert.deepEqual(downloaded.prepare("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
            .all().map(row => row.name), ["games", "genres", "user_games", "users"]);
          assert.equal(downloaded.prepare("PRAGMA integrity_check").get()?.integrity_check, "ok");
          assert.deepEqual(downloaded.prepare("PRAGMA foreign_key_check").all(), []);
          for (const [table, rows] of expected) {
            assert.deepEqual(downloaded.prepare(`SELECT * FROM ${table} ORDER BY rowid`).all(), rows);
            assert.deepEqual(downloaded.prepare(`PRAGMA foreign_key_list(${table})`).all(),
              store.db.prepare(`PRAGMA foreign_key_list(${table})`).all());
          }
          const links = downloaded.prepare("SELECT rating, play_count FROM user_games WHERE user_id = ? AND game_id = ?");
          assert.deepEqual({ ...links.get(userId, gameId) }, { rating: null, play_count: 2 });
          assert.deepEqual({ ...links.get(secondUserId, gameId) }, { rating: 2, play_count: 3 });
          downloaded.exec("PRAGMA foreign_keys = ON");
          downloaded.prepare("DELETE FROM users WHERE user_id = ?").run(userId);
          assert.equal(links.get(userId, gameId), undefined);
          assert(links.get(secondUserId, gameId));
          downloaded.prepare("DELETE FROM games WHERE game_id = ?").run(gameId);
          assert.equal(links.get(secondUserId, gameId), undefined);
        } finally { downloaded.close(); }
        writer.exec("ROLLBACK");
        for (const [table, rows] of expected) {
          assert.deepEqual(store.db.prepare(`SELECT * FROM ${table} ORDER BY rowid`).all(), rows);
        }
        // Transfer callback cleanup may finish just after the client consumes it.
        for (let i = 0; i < 50 && temporaryPath && existsSync(temporaryPath); i++) {
          await new Promise(resolve => setTimeout(resolve, 10));
        }
        assert(temporaryPath);
        assert.equal(existsSync(temporaryPath), false);
      } finally {
        if (writer.isTransaction) writer.exec("ROLLBACK");
        writer.close();
        store.createBackup = createBackup;
        if (previous === undefined) delete process.env.NODE_ENV;
        else process.env.NODE_ENV = previous;
      }
    });
    await t.test("parent deletion cascades only the associated saved relationships", async () => {
      await request("DELETE", `/players/${userId}`, undefined, 204);
      const saved = await request("GET", "/saved");
      assert(!saved.some((row: { userId: number }) => row.userId === userId));
      assert(saved.some((row: { userId: number; gameId: number }) => row.userId === secondUserId && row.gameId === gameId));
      await request("DELETE", `/games/${gameId}`, undefined, 204);
      assert(!(await request("GET", "/saved")).some((row: { gameId: number }) => row.gameId === gameId));
      assert((await request("GET", "/players")).some((row: { id: number }) => row.id === secondUserId));
      await request("DELETE", `/players/${secondUserId}`, undefined, 204);
      await request("DELETE", `/games/${gameId}`, undefined, 404);
      await request("GET", "/games?genreId=bad", undefined, 400);
    });
  } finally {
    await new Promise<void>((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
    store.db.close();
    rmSync(directory, { recursive: true, force: true });
  }
});
