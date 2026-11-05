import { sql } from "drizzle-orm";
import { check, index, integer, pgTable, primaryKey, serial, text, unique } from "drizzle-orm/pg-core";

export const libraryGenres = pgTable("genres", {
  id: serial("genre_id").primaryKey(),
  name: text("genre_name").notNull().unique(),
}, (t) => [check("genres_name_check", sql`length(trim(${t.name})) > 0`)]);

export const libraryUsers = pgTable("users", {
  id: serial("user_id").primaryKey(),
  username: text("username").notNull().unique(),
  email: text("email").notNull().unique(),
}, (t) => [
  check("users_name_check", sql`length(trim(${t.username})) > 0`),
  check("users_email_check", sql`position('@' in ${t.email}) > 1`),
]);

export const libraryGames = pgTable("games", {
  id: serial("game_id").primaryKey(),
  genreId: integer("genre_id").notNull().references(() => libraryGenres.id, { onDelete: "restrict", onUpdate: "cascade" }),
  title: text("title").notNull(),
  url: text("launch_url").notNull(),
  releaseDate: text("release_date").notNull(),
}, (t) => [
  unique("games_title_release_date_key").on(t.title, t.releaseDate),
  index("idx_games_genre_id").on(t.genreId),
  check("games_title_check", sql`length(trim(${t.title})) > 0`),
  check("games_url_check", sql`${t.url} like 'https://%' or ${t.url} like 'http://%'`),
  check("games_date_check", sql`${t.releaseDate} ~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' and to_char(${t.releaseDate}::date, 'YYYY-MM-DD') = ${t.releaseDate}`),
]);

export const libraryUserGames = pgTable("user_games", {
  userId: integer("user_id").notNull().references(() => libraryUsers.id, { onDelete: "cascade", onUpdate: "cascade" }),
  gameId: integer("game_id").notNull().references(() => libraryGames.id, { onDelete: "cascade", onUpdate: "cascade" }),
  rating: integer("rating"),
  playCount: integer("play_count").notNull().default(0),
}, (t) => [
  primaryKey({ columns: [t.userId, t.gameId] }),
  index("idx_user_games_game_id").on(t.gameId),
  check("user_games_rating_check", sql`${t.rating} is null or ${t.rating} between 1 and 5`),
  check("user_games_play_count_check", sql`${t.playCount} >= 0`),
]);
