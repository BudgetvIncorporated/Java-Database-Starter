import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { DatabaseSync } from "node:sqlite";

export function findLibraryDatabase(start = process.cwd()): string {
  let directory = start;
  while (true) {
    const candidate = join(directory, "java-database-starter", "game-library.db");
    if (existsSync(candidate)) return candidate;
    const parent = dirname(directory);
    if (parent === directory) {
      throw new Error("Existing java-database-starter/game-library.db not found. The browser app will not create a replacement database.");
    }
    directory = parent;
  }
}

export class LibraryStore {
  readonly db: DatabaseSync;

  constructor(path: string) {
    if (!existsSync(path)) throw new Error("The existing Game Library database is missing.");
    this.db = new DatabaseSync(path);
    this.db.exec("PRAGMA foreign_keys = ON; PRAGMA busy_timeout = 5000;");
    // Fail at startup for the wrong database instead of serving an empty replacement.
    this.db.prepare("SELECT game_id, genre_id, title, launch_url, release_date FROM games LIMIT 0").all();
    this.db.prepare("SELECT user_id, username, email FROM users LIMIT 0").all();
    this.db.prepare("SELECT user_id, game_id, rating, play_count FROM user_games LIMIT 0").all();
  }
}

export const gameSelect = `SELECT g.game_id AS id, g.genre_id AS genreId, g.title,
  g.launch_url AS url, g.release_date AS releaseDate, ge.genre_name AS genreName
  FROM games g JOIN genres ge ON ge.genre_id = g.genre_id`;

export const playerSelect = `SELECT user_id AS id, username, email FROM users`;

export const savedSelect = `SELECT ug.user_id AS userId, ug.game_id AS gameId,
  ug.rating, ug.play_count AS playCount, u.username, g.title AS gameTitle, ge.genre_name AS genreName
  FROM user_games ug JOIN users u ON u.user_id = ug.user_id
  JOIN games g ON g.game_id = ug.game_id JOIN genres ge ON ge.genre_id = g.genre_id`;
