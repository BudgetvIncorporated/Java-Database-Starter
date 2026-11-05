import { useEffect, useState, type FormEvent } from "react";
import { Pencil, Plus, Search, Trash2 } from "lucide-react";
import {
  useCreateLibraryPlayer,
  useDeleteLibraryPlayer,
  useListLibraryPlayers,
  useUpdateLibraryPlayer,
  type LibraryPlayer,
} from "@workspace/api-client-react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ConfirmDelete } from "@/components/confirm-delete";
import { useToast } from "@/hooks/use-toast";
import {
  EmptyBlock, ErrorBlock, Field, FormError, PageHeader, RowsSkeleton, SummaryStrip,
  btnGhost, btnIcon, btnPrimary, errorMessage, inputCls, listQuery, useInvalidateLibrary,
} from "@/lib/library";

function PlayerDialog({ open, onOpenChange, player }: { open: boolean; onOpenChange: (o: boolean) => void; player: LibraryPlayer | null }) {
  const { toast } = useToast();
  const invalidate = useInvalidateLibrary();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [err, setErr] = useState<string | null>(null);
  useEffect(() => {
    if (open) { setUsername(player?.username ?? ""); setEmail(player?.email ?? ""); setErr(null); }
  }, [open, player]);
  const done = async (m: string) => { await invalidate(); toast({ title: m }); onOpenChange(false); };
  const create = useCreateLibraryPlayer({ mutation: { onSuccess: (p) => done(`Added ${p.username}`), onError: (e) => setErr(errorMessage(e)) } });
  const update = useUpdateLibraryPlayer({ mutation: { onSuccess: (p) => done(`Updated ${p.username}`), onError: (e) => setErr(errorMessage(e)) } });
  const pending = create.isPending || update.isPending;
  const submit = (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    if (!username.trim()) return setErr("Username is required.");
    if (!/^\S+@\S+\.\S+$/.test(email.trim())) return setErr("Enter a valid email address.");
    const data = { username: username.trim(), email: email.trim() };
    if (player) update.mutate({ id: player.id, data }); else create.mutate({ data });
  };
  return (
    <Dialog open={open} onOpenChange={(o) => !pending && onOpenChange(o)}>
      <DialogContent className="bg-card max-w-md">
        <DialogHeader>
          <DialogTitle className="text-2xl font-extrabold">{player ? "Edit player" : "New player"}</DialogTitle>
          <DialogDescription>{player ? `Library card #${player.id}` : "Issue a new library card."}</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} className="space-y-4">
          <Field label="Username"><input className={inputCls} value={username} maxLength={100} onChange={(e) => setUsername(e.target.value)} data-testid="input-player-username" /></Field>
          <Field label="Email"><input type="email" className={inputCls} value={email} maxLength={254} onChange={(e) => setEmail(e.target.value)} data-testid="input-player-email" /></Field>
          <FormError msg={err} />
          <div className="flex justify-end gap-2 pt-2">
            <button type="button" className={btnGhost} onClick={() => onOpenChange(false)} disabled={pending}>Cancel</button>
            <button type="submit" className={btnPrimary} disabled={pending} data-testid="button-save-player">{pending ? "Saving..." : player ? "Save changes" : "Add player"}</button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export default function PlayersPage() {
  const { toast } = useToast();
  const invalidate = useInvalidateLibrary();
  const players = useListLibraryPlayers({ query: { ...listQuery, queryKey: ["/api/library/players"] } });
  const [q, setQ] = useState("");
  const [editing, setEditing] = useState<LibraryPlayer | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [toDelete, setToDelete] = useState<LibraryPlayer | null>(null);
  const [delErr, setDelErr] = useState<string | null>(null);
  const del = useDeleteLibraryPlayer({
    mutation: {
      onSuccess: async () => { await invalidate(); toast({ title: `Removed ${toDelete?.username}` }); setToDelete(null); },
      onError: (e) => setDelErr(errorMessage(e)),
    },
  });
  const all = players.data ?? [];
  const term = q.trim().toLowerCase();
  const list = term ? all.filter((p) => p.username.toLowerCase().includes(term) || p.email.toLowerCase().includes(term)) : all;
  const openNew = () => { setEditing(null); setFormOpen(true); };

  return (
    <div>
      <PageHeader kicker="Drawer B / Cardholders" title="Players" sub="Students holding a library card. Edit details or retire a card." action={<button className={btnPrimary} onClick={openNew} data-testid="button-add-player"><Plus className="h-4 w-4" /> Add player</button>} />
      <SummaryStrip />
      <div className="relative mb-5 max-w-md">
        <Search className="h-4 w-4 absolute left-3 top-3 text-muted-foreground" />
        <input type="search" className={`${inputCls} pl-9`} placeholder="Filter by username or email" value={q} onChange={(e) => setQ(e.target.value)} data-testid="input-filter-players" />
      </div>
      {players.isLoading ? <RowsSkeleton /> : players.isError ? <ErrorBlock err={players.error} onRetry={() => players.refetch()} /> : all.length === 0 ? (
        <EmptyBlock title="No players yet" body="Issue the first library card." action={<button className={btnPrimary} onClick={openNew}>Add player</button>} />
      ) : list.length === 0 ? (
        <EmptyBlock title="No match" body={`No player matches "${q}".`} action={<button className={btnGhost} onClick={() => setQ("")}>Clear filter</button>} />
      ) : (
        <div className="bg-card border border-card-border rounded-md divide-y divide-border">
          {list.map((p) => (
            <div key={p.id} className="flex items-center gap-4 px-4 py-3" data-testid={`row-player-${p.id}`}>
              <div className="h-10 w-10 flex-shrink-0 rounded-md bg-primary text-primary-foreground grid place-items-center font-bold uppercase">{p.username.slice(0, 2)}</div>
              <div className="min-w-0 flex-1">
                <div className="font-semibold truncate" data-testid={`text-player-username-${p.id}`}>{p.username}</div>
                <div className="font-mono text-xs text-muted-foreground truncate">{p.email}</div>
              </div>
              <span className="hidden sm:block font-mono text-xs text-muted-foreground">#{p.id}</span>
              <button className={btnIcon} onClick={() => { setEditing(p); setFormOpen(true); }} aria-label="Edit" data-testid={`button-edit-player-${p.id}`}><Pencil className="h-4 w-4" /></button>
              <button className={btnIcon} onClick={() => { setDelErr(null); setToDelete(p); }} aria-label="Delete" data-testid={`button-delete-player-${p.id}`}><Trash2 className="h-4 w-4" /></button>
            </div>
          ))}
        </div>
      )}
      <PlayerDialog open={formOpen} onOpenChange={setFormOpen} player={editing} />
      <ConfirmDelete
        open={!!toDelete}
        onOpenChange={(o) => !o && setToDelete(null)}
        title={`Delete ${toDelete?.username ?? ""}?`}
        description="This removes the player. All of their saved-game links (ratings and play counts) are removed too (cascade). Games in the catalog are kept."
        confirmLabel="Delete player"
        pending={del.isPending}
        error={delErr}
        onConfirm={() => toDelete && del.mutate({ id: toDelete.id })}
      />
    </div>
  );
}
