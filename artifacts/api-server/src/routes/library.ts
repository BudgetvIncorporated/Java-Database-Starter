import { Router, type ErrorRequestHandler, type RequestHandler } from "express";
import {
  CreateLibraryGameBody, CreateLibraryPlayerBody, CreateLibrarySavedGameBody,
  UpdateLibrarySavedGameBody, ListLibraryGamesQueryParams,
} from "@workspace/api-zod";
import { LibraryStore, gameSelect, playerSelect, savedSelect } from "../lib/library-store";
import { libraryAccess, type IdentityResolver } from "../lib/library-management";

class HttpError extends Error {
  constructor(readonly status: number, message: string) { super(message); }
}

function id(value: unknown): number {
  const result = Number(value);
  if (!Number.isSafeInteger(result) || result < 1) throw new HttpError(400, "Choose a valid record ID.");
  return result;
}

function gameInput(body: unknown) {
  const parsed = CreateLibraryGameBody.safeParse(body);
  if (!parsed.success) throw new HttpError(400, "Enter a title, genre, game URL, and release date.");
  const result = { ...parsed.data, title: parsed.data.title.trim(), url: parsed.data.url.trim() };
  if (!result.title) throw new HttpError(400, "Game title cannot be blank.");
  try {
    const url = new URL(result.url);
    if (!["http:", "https:"].includes(url.protocol)) throw new Error("protocol");
  } catch { throw new HttpError(400, "Game URL must be a valid http or https link."); }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(result.releaseDate) ||
      !Number.isFinite(Date.parse(result.releaseDate)) ||
      new Date(result.releaseDate).toISOString().slice(0, 10) !== result.releaseDate) {
    throw new HttpError(400, "Release date must be a real date in YYYY-MM-DD format.");
  }
  return result;
}

function playerInput(body: unknown) {
  const parsed = CreateLibraryPlayerBody.safeParse(body);
  if (!parsed.success) throw new HttpError(400, "Enter a username and email address.");
  const result = { username: parsed.data.username.trim(), email: parsed.data.email.trim() };
  if (!result.username) throw new HttpError(400, "Username cannot be blank.");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(result.email)) {
    throw new HttpError(400, "Enter a valid email address.");
  }
  return result;
}

function savedInput(body: unknown, creating: boolean) {
  const result = creating
    ? CreateLibrarySavedGameBody.safeParse(body)
    : UpdateLibrarySavedGameBody.safeParse(body);
  if (!result.success) throw new HttpError(400, "Choose a player and game, use a whole-number rating from 1 to 5 (or leave it blank), and a nonnegative whole-number play count.");
  return result.data;
}

export function createLibraryRouter(store: LibraryStore, resolveIdentity?: IdentityResolver) {
  const router = Router();
  const db = store.db;
  router.use((_req, res, next) => { res.setHeader("Cache-Control", "no-store"); next(); });
  router.use(libraryAccess(resolveIdentity));
  router.get("/backup", async (req, res, next) => {
    // Express also maps HEAD to GET; don't generate a sensitive export for it.
    if (req.method !== "GET") { res.status(405).end(); return; }
    try {
      const snapshot = await store.createBackup();
      if (res.destroyed) { await snapshot.cleanup(); return; }
      res.setHeader("Content-Type", "application/vnd.sqlite3");
      res.setHeader("X-Content-Type-Options", "nosniff");
      const filename = `game-library-${new Date().toISOString().replace(/[:.]/g, "-")}.db`;
      res.download(snapshot.path, filename, async (error) => {
        try { await snapshot.cleanup(); }
        catch (cleanupError) { req.log.error({ err: cleanupError }, "Could not remove temporary library backup"); }
        if (error && !res.destroyed) next(error);
      });
    } catch (error) { next(error); }
  });
  const route = (handler: RequestHandler): RequestHandler => (req, res, next) => {
    try { handler(req, res, next); } catch (error) { next(error); }
  };
  function game(gameId: number) {
    const row = db.prepare(`${gameSelect} WHERE g.game_id = ?`).get(gameId);
    if (!row) throw new HttpError(404, "Game not found. Refresh the catalog and try again.");
    return row;
  }
  function player(userId: number) {
    const row = db.prepare(`${playerSelect} WHERE user_id = ?`).get(userId);
    if (!row) throw new HttpError(404, "Player not found. Refresh the player list and try again.");
    return row;
  }
  function saved(userId: number, gameId: number) {
    const row = db.prepare(`${savedSelect} WHERE ug.user_id = ? AND ug.game_id = ?`).get(userId, gameId);
    if (!row) throw new HttpError(404, "Saved game not found. Refresh the library and try again.");
    return row;
  }
  router.get("/summary", route((_req, res) => {
    res.json(db.prepare(`SELECT
      (SELECT COUNT(*) FROM games) AS games,
      (SELECT COUNT(*) FROM users) AS players,
      (SELECT COUNT(*) FROM user_games) AS savedGames,
      (SELECT COALESCE(SUM(play_count), 0) FROM user_games) AS totalPlays,
      (SELECT COUNT(*) FROM genres) AS genres`).get());
  }));
  router.get("/genres", route((_req, res) => {
    res.json(db.prepare("SELECT genre_id AS id, genre_name AS name FROM genres ORDER BY genre_name COLLATE NOCASE").all());
  }));
  router.get("/games", route((req, res) => {
    const parsed = ListLibraryGamesQueryParams.safeParse(req.query);
    if (!parsed.success) throw new HttpError(400, "Invalid catalog filters.");
    const search = parsed.data.search?.trim() ?? "";
    const genreId = parsed.data.genreId;
    if (genreId !== undefined) id(genreId);
    res.json(db.prepare(`${gameSelect}
      WHERE (? = '' OR instr(lower(g.title), lower(?)) > 0)
      AND (? IS NULL OR g.genre_id = ?) ORDER BY g.title COLLATE NOCASE, g.game_id`)
      .all(search, search, genreId ?? null, genreId ?? null));
  }));
  router.post("/games", route((req, res) => {
    const input = gameInput(req.body);
    const result = db.prepare("INSERT INTO games (genre_id, title, launch_url, release_date) VALUES (?, ?, ?, ?)")
      .run(input.genreId, input.title, input.url, input.releaseDate);
    res.status(201).json(game(Number(result.lastInsertRowid)));
  }));
  router.put("/games/:id", route((req, res) => {
    const gameId = id(req.params.id);
    game(gameId);
    const input = gameInput(req.body);
    db.prepare("UPDATE games SET genre_id = ?, title = ?, launch_url = ?, release_date = ? WHERE game_id = ?")
      .run(input.genreId, input.title, input.url, input.releaseDate, gameId);
    res.json(game(gameId));
  }));
  router.delete("/games/:id", route((req, res) => {
    const gameId = id(req.params.id);
    game(gameId);
    db.prepare("DELETE FROM games WHERE game_id = ?").run(gameId);
    res.status(204).end();
  }));
  router.get("/players", route((_req, res) => {
    res.json(db.prepare(`${playerSelect} ORDER BY username COLLATE NOCASE, user_id`).all());
  }));
  router.post("/players", route((req, res) => {
    const input = playerInput(req.body);
    const result = db.prepare("INSERT INTO users (username, email) VALUES (?, ?)").run(input.username, input.email);
    res.status(201).json(player(Number(result.lastInsertRowid)));
  }));
  router.put("/players/:id", route((req, res) => {
    const userId = id(req.params.id);
    player(userId);
    const input = playerInput(req.body);
    db.prepare("UPDATE users SET username = ?, email = ? WHERE user_id = ?").run(input.username, input.email, userId);
    res.json(player(userId));
  }));
  router.delete("/players/:id", route((req, res) => {
    const userId = id(req.params.id);
    player(userId);
    db.prepare("DELETE FROM users WHERE user_id = ?").run(userId);
    res.status(204).end();
  }));
  router.get("/saved", route((_req, res) => {
    res.json(db.prepare(`${savedSelect} ORDER BY u.username COLLATE NOCASE, g.title COLLATE NOCASE`).all());
  }));
  router.post("/saved", route((req, res) => {
    const input = savedInput(req.body, true);
    // The create schema requires both IDs; validate them independently at this boundary.
    const userId = id(req.body.userId);
    const gameId = id(req.body.gameId);
    player(userId);
    game(gameId);
    db.prepare("INSERT INTO user_games (user_id, game_id, rating, play_count) VALUES (?, ?, ?, ?)")
      .run(userId, gameId, input.rating, input.playCount);
    res.status(201).json(saved(userId, gameId));
  }));
  router.put("/saved/:userId/:gameId", route((req, res) => {
    const userId = id(req.params.userId);
    const gameId = id(req.params.gameId);
    saved(userId, gameId);
    const input = savedInput(req.body, false);
    db.prepare("UPDATE user_games SET rating = ?, play_count = ? WHERE user_id = ? AND game_id = ?")
      .run(input.rating, input.playCount, userId, gameId);
    res.json(saved(userId, gameId));
  }));
  router.delete("/saved/:userId/:gameId", route((req, res) => {
    const userId = id(req.params.userId);
    const gameId = id(req.params.gameId);
    saved(userId, gameId);
    db.prepare("DELETE FROM user_games WHERE user_id = ? AND game_id = ?").run(userId, gameId);
    res.status(204).end();
  }));
  router.get("/statistics", route((_req, res) => {
    res.json(db.prepare(`SELECT u.user_id AS userId, u.username, COUNT(ug.game_id) AS savedGames,
      COALESCE(SUM(ug.play_count), 0) AS totalPlayCount, AVG(ug.rating) AS averageRating
      FROM users u LEFT JOIN user_games ug ON ug.user_id = u.user_id
      GROUP BY u.user_id ORDER BY totalPlayCount DESC, u.username COLLATE NOCASE`).all());
  }));
  const errors: ErrorRequestHandler = (error: unknown, req, res, _next) => {
    if (res.headersSent) { _next(error); return; }
    if (error instanceof HttpError) { res.status(error.status).json({ error: error.message }); return; }
    const message = error instanceof Error ? error.message : "";
    if (message.includes("UNIQUE constraint failed")) {
      const friendly = message.includes("user_games")
        ? "This game is already saved for this player."
        : message.includes("users.")
          ? "That username or email is already in use."
          : "A game with this title and release date already exists.";
      res.status(409).json({ error: friendly }); return;
    }
    if (message.includes("FOREIGN KEY constraint") || message.includes("CHECK constraint")) {
      res.status(400).json({ error: "Check your values and choose existing library records." }); return;
    }
    if (message.includes("database is locked") || message.includes("SQLITE_BUSY")) {
      res.status(503).json({ error: "The library is busy. Please try again in a moment." }); return;
    }
    req.log.error({ err: error }, "Game Library request failed");
    res.status(500).json({ error: "The library could not complete that request. Please try again." });
  };
  router.use(errors);
  return router;
}
