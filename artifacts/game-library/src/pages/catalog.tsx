import { useEffect, useState, type FormEvent } from "react";
import { ExternalLink, Pencil, Plus, Search, Trash2 } from "lucide-react";
import {
  useCreateLibraryGame,
  useDeleteLibraryGame,
  useListLibraryGames,
  useListLibraryGenres,
  useUpdateLibraryGame,
  type LibraryGame,
  type LibraryGenre,
} from "@workspace/api-client-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { ConfirmDelete } from "@/components/confirm-delete";
import { useToast } from "@/hooks/use-toast";
import {
  EmptyBlock, ErrorBlock, Field, FormError, PageHeader, RowsSkeleton, SummaryStrip,
  btnGhost, btnIcon, btnPrimary, errorMessage, inputCls, listQuery, useInvalidateLibrary,
} from "@/lib/library";

function safeUrl(u: string) {
  try {
    const p = new URL(u);
    return p.protocol === "http:" || p.protocol === "https:" ? p.href : null;
  } catch {
    return null;
  }
}

function GameDialog({ open, onOpenChange, game, genres }: { open: boolean; onOpenChange: (o: boolean) => void; game: LibraryGame | null; genres: LibraryGenre[] }) {
  const { toast } = useToast();
  const invalidate = useInvalidateLibrary();
  const [title, setTitle] = useState("");
  const [genreId, setGenreId] = useState("");
  const [url, setUrl] = useState("");
  const [releaseDate, setReleaseDate] = useState("");
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setTitle(game?.title ?? "");
      setGenreId(game ? String(game.genreId) : "");
      setUrl(game?.url ?? "");
      setReleaseDate(game?.releaseDate?.slice(0, 10) ?? "");
      setErr(null);
    }
  }, [open, game]);

  const done = async (msg: string) => {
    await invalidate();
    toast({ title: msg });
    onOpenChange(false);
  };
  const create = useCreateLibraryGame({ mutation: { onSuccess: (g) => done(`Added "${g.title}"`), onError: (e) => setErr(errorMessage(e)) } });
  const update = useUpdateLibraryGame({ mutation: { onSuccess: (g) => done(`Updated "${g.title}"`), onError: (e) => setErr(errorMessage(e)) } });
  const pending = create.isPending || update.isPending;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    if (!title.trim()) return setErr("Title is required.");
    if (!genreId) return setErr("Choose a genre.");
    if (!url.trim()) return setErr("Link is required.");
    if (!releaseDate) return setErr("Release date is required.");
    const data = { title: title.trim(), genreId: Number(genreId), url: url.trim(), releaseDate };
    if (game) update.mutate({ id: game.id, data });
    else create.mutate({ data });
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !pending && onOpenChange(o)}>
      <DialogContent className="bg-card max-w-lg">
        <DialogHeader>
          <DialogTitle className="text-2xl font-extrabold">{game ? "Edit catalog card" : "New catalog card"}</DialogTitle>
          <DialogDescription>{game ? `Card #${game.id}` : "File a new game into the library."}</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <Field label="Title"><input className={inputCls} value={title} maxLength={200} onChange={(e) => setTitle(e.target.value)} data-testid="input-game-title" /></Field>
          <div className="grid sm:grid-cols-2 gap-4">
            <Field label="Genre">
              <select className={inputCls} value={genreId} onChange={(e) => setGenreId(e.target.value)} data-testid="select-game-genre">
                <option value="">Select...</option>
                {genres.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
              </select>
            </Field>
            <Field label="Release date"><input type="date" className={inputCls} value={releaseDate} onChange={(e) => setReleaseDate(e.target.value)} data-testid="input-game-release" /></Field>
          </div>
          <Field label="Link"><input className={inputCls} value={url} maxLength={2000} placeholder="https://..." onChange={(e) => setUrl(e.target.value)} data-testid="input-game-url" /></Field>
          <FormError msg={err} />
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnGhost} onClick={() => onOpenChange(false)} disabled={pending} data-testid="button-cancel-game">Cancel</button>
            <button type="submit" className={btnPrimary} disabled={pending} data-testid="button-save-game">{pending ? "Saving..." : game ? "Save changes" : "Add game"}</button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function CatalogPage() {
  const { toast } = useToast();
  const invalidate = useInvalidateLibrary();
  const [search, setSearch] = useState("");
  const [debounced, setDebounced] = useState("");
  const [genre, setGenre] = useState("");
  useEffect(() => {
    const t = setTimeout(() => setDebounced(search.trim()), 250);
    return () => clearTimeout(t);
  }, [search]);

  const genres = useListLibraryGenres({ query: { ...listQuery, queryKey: ["/api/library/genres"] } });
  const params = { ...(debounced ? { search: debounced } : {}), ...(genre ? { genreId: Number(genre) } : {}) };
  const games = useListLibraryGames(params, { query: { ...listQuery, queryKey: ["/api/library/games", params] } });

  const [editing, setEditing] = useState<LibraryGame | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [toDelete, setToDelete] = useState<LibraryGame | null>(null);
  const [delErr, setDelErr] = useState<string | null>(null);
  const del = useDeleteLibraryGame({
    mutation: {
      onSuccess: async () => { await invalidate(); toast({ title: `Removed "${toDelete?.title}"` }); setToDelete(null); },
      onError: (e) => setDelErr(errorMessage(e)),
    },
  });
  const filtered = !!debounced || !!genre;
  const list = games.data ?? [];

  return (
    <div>
      <PageHeader
        kicker="Drawer A / Catalog"
        title="The catalog"
        sub="Every game on the shelf. Search by title, narrow by genre, open the link to play."
        action={<button className={btnPrimary} onClick={() => { setEditing(null); setFormOpen(true); }} data-testid="button-add-game"><Plus className="h-4 w-4" /> Add game</button>}
      />
      <SummaryStrip />
      <div className="flex flex-col sm:flex-row gap-3 mb-5">
        <div className="relative flex-1">
          <Search className="h-4 w-4 absolute left-3 top-3 text-muted-foreground" />
          <input type="search" className={`${inputCls} pl-9`} placeholder="Search titles" value={search} onChange={(e) => setSearch(e.target.value)} data-testid="input-search-games" />
        </div>
        <select className={`${inputCls} sm:w-56`} value={genre} onChange={(e) => setGenre(e.target.value)} data-testid="select-filter-genre" disabled={genres.isLoading}>
          <option value="">All genres</option>
          {(genres.data ?? []).map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
        </select>
        {filtered && <button className={btnGhost} onClick={() => { setSearch(""); setGenre(""); }} data-testid="button-clear-filters">Clear</button>}
      </div>
      {genres.isError && <div className="mb-4"><ErrorBlock err={genres.error} onRetry={() => genres.refetch()} /></div>}

      {games.isLoading ? <RowsSkeleton /> : games.isError ? <ErrorBlock err={games.error} onRetry={() => games.refetch()} /> : list.length === 0 ? (
        filtered
          ? <EmptyBlock title="No match" body="Nothing in the catalog fits that search and genre." action={<button className={btnGhost} onClick={() => { setSearch(""); setGenre(""); }}>Clear filters</button>} />
          : <EmptyBlock title="The catalog is empty" body="Add the first game to start the collection." action={<button className={btnPrimary} onClick={() => { setEditing(null); setFormOpen(true); }}>Add game</button>} />
      ) : (
        <>
          <div className="font-mono text-xs text-muted-foreground mb-2" data-testid="text-games-count">{list.length} card{list.length === 1 ? "" : "s"}{games.isFetching ? " / refreshing" : ""}</div>
          <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {list.map((g, i) => {
              const href = safeUrl(g.url);
              return (
                <li key={g.id} className="rise relative bg-card border border-card-border rounded-md p-4 pl-5 overflow-hidden" style={{ animationDelay: `${Math.min(i, 12) * 25}ms` }} data-testid={`card-game-${g.id}`}>
                  <span className="absolute left-0 top-0 bottom-0 w-1 bg-accent" />
                  <div className="flex justify-between items-start gap-2">
                    <span className="font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground">No. {String(g.id).padStart(3, "0")} / {g.genreName}</span>
                    <div className="flex -mr-2 -mt-1">
                      <button className={btnIcon} onClick={() => { setEditing(g); setFormOpen(true); }} aria-label="Edit" data-testid={`button-edit-game-${g.id}`}><Pencil className="h-4 w-4" /></button>
                      <button className={btnIcon} onClick={() => { setDelErr(null); setToDelete(g); }} aria-label="Delete" data-testid={`button-delete-game-${g.id}`}><Trash2 className="h-4 w-4" /></button>
                    </div>
                  </div>
                  <h3 className="text-lg font-bold leading-tight mt-1" data-testid={`text-game-title-${g.id}`}>{g.title}</h3>
                  <div className="mt-3 flex items-center justify-between text-sm">
                    <span className="font-mono text-xs text-muted-foreground">Released {g.releaseDate?.slice(0, 10)}</span>
                    {href ? (
                      <a href={href} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-primary font-semibold hover:underline" data-testid={`link-open-game-${g.id}`}>Open <ExternalLink className="h-3.5 w-3.5" /></a>
                    ) : <span className="font-mono text-xs text-muted-foreground">no valid link</span>}
                  </div>
                </li>
              );
            })}
          </ul>
        </>
      )}

      <GameDialog open={formOpen} onOpenChange={setFormOpen} game={editing} genres={genres.data ?? []} />
      <ConfirmDelete
        open={!!toDelete}
        onOpenChange={(o) => !o && setToDelete(null)}
        title={`Delete "${toDelete?.title ?? ""}"?`}
        description="This removes the game from the catalog. Every saved-game link pointing to it will be removed too (cascade), which changes player statistics. Players themselves are kept."
        confirmLabel="Delete game"
        pending={del.isPending}
        error={delErr}
        onConfirm={() => toDelete && del.mutate({ id: toDelete.id })}
      />
    </div>
  );
}
