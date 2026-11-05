---
name: SQLite backup read-only behavior
description: Standalone WAL-mode downloads can create sidecars even through read-only SQLite connections.
---

Treat downloaded SQLite backups as standalone snapshots; inspect them with
SQLite immutable mode only after rejecting any existing journal/WAL sidecars.
Never use immutable mode to inspect an active database or a preserved multi-file
set: it would ignore committed WAL content.

**Why:** Node's read-only SQLite connection can still create WAL/SHM sidecars
alongside a standalone WAL-mode backup. Validation can otherwise change the input
directory and make a subsequent standalone recovery check reject the download.

**How to apply:** Keep download validation and active-database inspection
separate. Preserve the entire current database/journal set before replacement;
replay journals on a disposable copy when recovering that preserved set.
