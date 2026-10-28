# Java Database Starter

A small Java 17 project that connects to a local SQLite database with JDBC.
The database file is created automatically the first time the program runs.

## Run it

From this directory, run:

```sh
mvn compile exec:java
```

Maven downloads the SQLite JDBC driver and the project plugins on the first run.

## Add tables

Edit `src/main/resources/schema.sql`. Add one or more SQLite `CREATE TABLE`
statements, ending each statement with a semicolon. They are applied each time
the program starts, so use `CREATE TABLE IF NOT EXISTS` to keep existing tables.

The database file is `database.db` in this directory. It is ignored by Git.

## Java files

- `Database.java` opens the JDBC connection.
- `SchemaInitializer.java` applies the SQL in `schema.sql`.
- `Main.java` starts the program and closes the connection when it finishes.
