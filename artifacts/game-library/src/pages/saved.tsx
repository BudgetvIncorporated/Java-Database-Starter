import { useEffect, useState, type FormEvent } from "react";
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react";
import {
  useCreateLibrarySavedGame,
  useDeleteLibrarySavedGame,
  useListLibraryGames,
  useListLibraryPlayers,
  useListLibrarySavedGames,
  useUpdateLibrarySavedGame,
  type LibraryGame,
  type LibraryPlayer,
  type LibrarySavedGame,
} from "@workspace/api-client-react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ConfirmDelete } from "@/components/confirm-delete";
import { useToast } from "@/hooks/use-toast";
import {
  EmptyBlock, ErrorBlock, Field, FormError, PageHeader, Rating, RowsSkeleton, SummaryStrip,
  btnGhost, btnIcon, btnPrimary, errorMessage, inputCls, listQuery, useInvalidateLibrary,
} from "@/lib/library";

function SavedDialog({ open, onOpenChange, saved, players, games }: { open: boolean; onOpenChange: (o: boolean) => void; saved: LibrarySavedGame | null; players: LibraryPlayer[]; games: LibraryGame[] }) {
  const { toast } = useToast();
  const invalidate = useInvalidateLibrary();
  const [userId, setUserId] = useState("");
  const [gameId, setGameId] = useState("");
  const [rating, setRating] = useState("");
  const [playCount, setPlayCount] = useState("0");
  const [err, setErr] = useState<string | null>(null);
  useEffect(() => {
    if (open) {
      setUserId(saved ? String(saved.userId) : "");
      setGameId(saved ? String(saved.gameId) : "");
      setRating(saved?.rating != null ? String(saved.rating) : "");
      setPlayCount(saved ? String(saved.playCount) : "0");
      setErr(null);
    }
  }, [open, saved]);
  const done = async (m: string) => { await invalidate(); toast({ title: m }); onOpenChange(false); };
  const create = useCreateLibrarySavedGame({ mutation: { onSuccess: (s) => done(`Saved ${s.gameTitle} for ${s.username}`), onError: (e) => setErr(errorMessage(e)) } });
  const update = useUpdateLibrarySavedGame({ mutation: { onSuccess: (s) => done(`Updated ${s.gameTitle} for ${s.username}`), onError: (e) => setErr(errorMessage(e)) } });
  const pending = create.isPending || update.isPending;
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    if (!saved && (!userId || !gameId)) return setErr("Choose both a player and a game.");
    if (!/^\d+$/.test(playCount.trim()) || Number(playCount) > 2147483647) return setErr("Play count must be a whole number of 0 or more.");
    const data = { rating: rating ? Number(rating) : null, playCount: Number(playCount) };
    if (saved) update.mutate({ userId: saved.userId, gameId: saved.gameId, data });
    else create.mutate({ data: { ...data, userId: Number(userId), gameId: Number(gameId) } });
  };
  return (
    <Dialog open={open} onOpenChange={(o) => !pending && onOpenChange(o)}>
      <DialogContent className="bg-card max-w-lg">
        <DialogHeader>
          <DialogTitle className="text-2xl font-extrabold">{saved ? "Edit saved game" : "Save a game to a player"}</DialogTitle>
          <DialogDescription>{saved ? `${saved.username} / ${saved.gameTitle}` : "Link a player to a game with an optional rating."}</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          {!saved && (
            <div className="grid sm:grid-cols-2 gap-4">
              <Field label="Player">
                <select className={inputCls} value={userId} onChange={(e) => setUserId(e.target.value)} data-testid="select-saved-player">
                  <option value="">Select...</option>
                  {players.map((p) => <option key={p.id} value={p.id}>{p.username}</option>)}
                </select>
              </Field>
              <Field label="Game">
                <select className={inputCls} value={gameId} onChange={(e) => setGameId(e.target.value)} data-testid="select-saved-game">
                  <option value="">Select...</option>
                  {games.map((g) => <option key={g.id} value={g.id}>{g.title}</option>)}
                </select>
              </Field>
            </div>
          )}
          <div className="grid sm:grid-cols-2 gap-4">
            <Field label="Rating">
              <select className={inputCls} value={rating} onChange={(e) => setRating(e.target.value)} data-testid="select-saved-rating">
                <option value="">No rating</option>
                {[1, 2, 3, 4, 5].map((r) => <option key={r} value={r}>{r} / 5</option>)}
              </select>
            </Field>
            <Field label="Play count"><input inputMode="numeric" className={inputCls} value={playCount} onChange={(e) => setPlayCount(e.target.value)} data-testid="input-saved-playcount" /></Field>
          </div>
          <FormError msg={err} />
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnGhost} onClick={() => onOpenChange(false)} disabled={pending}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={pending} data-testid="button-save-saved">{pending ? "Saving..." : saved ? "Save changes" : "Save game"}</button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

type SortKey = "username" | "gameTitle" | "rating" | "playCount";

export default function SavedPage() {
  const { toast } = useToast();
  const invalidate = useInvalidateLibrary();
  const saved = useListLibrarySavedGames({ query: { ...listQuery, queryKey: ["/api/library/saved"] } });
  const players = useListLibraryPlayers({ query: { ...listQuery, queryKey: ["/api/library/players"] } });
  const games = useListLibraryGames(undefined, { query: { ...listQuery, queryKey: ["/api/library/games"] } });
  const [player, setPlayer] = useState("");
  const [title, setTitle] = useState("");
  const [sort, setSort] = useState<{ key: SortKey; dir: 1 | -1 }>({ key: "username", dir: 1 });
  const [editing, setEditing] = useState<LibrarySavedGame | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [toDelete, setToDelete] = useState<LibrarySavedGame | null>(null);
  const [delErr, setDelErr] = useState<string | null>(null);
  const del = useDeleteLibrarySavedGame({
    mutation: {
      onSuccess: async () => { await invalidate(); toast({ title: "Saved link removed" }); setToDelete(null); },
      onError: (e) => setDelErr(errorMessage(e)),
    },
  });

  const all = saved.data ?? [];
  const t = title.trim().toLowerCase();
  const list = all
    .filter((s) => (!player || s.userId === Number(player)) && (!t || s.gameTitle.toLowerCase().includes(t)))
    .sort((a, b) => {
      const k = sort.key;
      if (k === "rating" || k === "playCount") {
        const av = a[k] ?? -1, bv = b[k] ?? -1;
        return (av - bv) * sort.dir;
      }
      return a[k].localeCompare(b[k]) * sort.dir;
    });
  const filtered = !!player || !!t;
  const toggle = (key: SortKey) => setSort((s) => ({ key, dir: s.key === key ? (s.dir === 1 ? -1 : 1) : key === "playCount" || key === "rating" ? -1 : 1 }));
  const Th = ({ k, label, right }: { k: SortKey; label: string; right?: boolean }) => (
    <th className={`px-4 py-2 ${right ? "text-right" : "text-left"}`}>
      <button onClick={() => toggle(k)} className="inline-flex items-center gap-1 font-mono text-[11px] uppercase tracking-[0.15em] text-muted-foreground hover:text-foreground" data-testid={`button-sort-${k}`}>
        {label}{sort.key === k && (sort.dir === 1 ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />)}
      </button>
    </th>
  );
  const openNew = () => { setEditing(null); setFormOpen(true); };
  const canAdd = (players.data?.length ?? 0) > 0 && (games.data?.length ?? 0) > 0;

  return (
    <div>
      <PageHeader kicker="Drawer C / Checkouts" title="Saved games" sub="Which player saved which game, how they rated it, and how often they played." action={<button className={btnPrimary} onClick={openNew} disabled={!canAdd} data-testid="button-add-saved"><Plus className="h-4 w-4" /> Save a game</button>} />
      <SummaryStrip />
      <div className="flex flex-col sm:flex-row gap-3 mb-5">
        <select className={`${inputCls} sm:w-60`} value={player} onChange={(e) => setPlayer(e.target.value)} data-testid="select-filter-player">
          <option value="">All players</option>
          {(players.data ?? []).map((p) => <option key={p.id} value={p.id}>{p.username}</option>)}
        </select>
        <input type="search" className={`${inputCls} flex-1`} placeholder="Filter by game title" value={title} onChange={(e) => setTitle(e.target.value)} data-testid="input-filter-title" />
        {filtered && <button className={btnGhost} onClick={() => { setPlayer(""); setTitle(""); }} data-testid="button-clear-saved-filters">Clear</button>}
      </div>
      {saved.isLoading ? <RowsSkeleton /> : saved.isError ? <ErrorBlock err={saved.error} onRetry={() => saved.refetch()} /> : all.length === 0 ? (
        <EmptyBlock title="Nothing saved yet" body="Link a player to a game to start tracking plays and ratings." action={canAdd ? <button className={btnPrimary} onClick={openNew}>Save a game</button> : undefined} />
      ) : list.length === 0 ? (
        <EmptyBlock title="No match" body="No saved games fit those filters." action={<button className={btnGhost} onClick={() => { setPlayer(""); setTitle(""); }}>Clear filters</button>} />
      ) : (
        <div className="bg-card border border-card-border rounded-md overflow-x-auto">
          <table className="w-full text-sm min-w-[620px]">
            <thead className="border-b border-border bg-muted/50">
              <tr><Th k="username" label="Player" /><Th k="gameTitle" label="Game" /><Th k="rating" label="Rating" /><Th k="playCount" label="Plays" right /><th className="w-20" /></tr>
            </thead>
            <tbody className="divide-y divide-border">
              {list.map((s) => (
                <tr key={`${s.userId}-${s.gameId}`} data-testid={`row-saved-${s.userId}-${s.gameId}`}>
                  <td className="px-4 py-3 font-semibold">{s.username}</td>
                  <td className="px-4 py-3"><div>{s.gameTitle}</div><div className="font-mono text-[11px] text-muted-foreground">{s.genreName}</div></td>
                  <td className="px-4 py-3"><Rating value={s.rating} /></td>
                  <td className="px-4 py-3 text-right font-mono tabular-nums">{s.playCount.toLocaleString()}</td>
                  <td className="px-2 py-3 whitespace-nowrap text-right">
                    <button className={btnIcon} onClick={() => { setEditing(s); setFormOpen(true); }} aria-label="Edit" data-testid={`button-edit-saved-${s.userId}-${s.gameId}`}><Pencil className="h-4 w-4" /></button>
                    <button className={btnIcon} onClick={() => { setDelErr(null); setToDelete(s); }} aria-label="Remove" data-testid={`button-delete-saved-${s.userId}-${s.gameId}`}><Trash2 className="h-4 w-4" /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <SavedDialog open={formOpen} onOpenChange={setFormOpen} saved={editing} players={players.data ?? []} games={games.data ?? []} />
      <ConfirmDelete
        open={!!toDelete}
        onOpenChange={(o) => !o && setToDelete(null)}
        title="Remove this saved game?"
        description={`Unlinks "${toDelete?.gameTitle ?? ""}" from ${toDelete?.username ?? ""}, discarding its rating and play count. The game and the player both stay in the library.`}
        confirmLabel="Remove link"
        pending={del.isPending}
        error={delErr}
        onConfirm={() => toDelete && del.mutate({ userId: toDelete.userId, gameId: toDelete.gameId })}
      />
    </div>
  );
}
