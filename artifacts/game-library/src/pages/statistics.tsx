import { useState } from "react";
import { ArrowDown, ArrowUp } from "lucide-react";
import { useGetLibraryStatistics, type LibraryPlayerStats } from "@workspace/api-client-react";
import { EmptyBlock, ErrorBlock, PageHeader, RowsSkeleton, SummaryStrip, listQuery } from "@/lib/library";

type Key = keyof Pick<LibraryPlayerStats, "username" | "savedGames" | "totalPlayCount" | "averageRating">;

export default function StatisticsPage() {
  const stats = useGetLibraryStatistics({ query: { ...listQuery, queryKey: ["/api/library/statistics"] } });
  const [sort, setSort] = useState<{ key: Key; dir: 1 | -1 }>({ key: "totalPlayCount", dir: -1 });
  const all = stats.data ?? [];
  const maxPlays = Math.max(1, ...all.map((s) => s.totalPlayCount));
  const list = [...all].sort((a, b) => {
    if (sort.key === "username") return a.username.localeCompare(b.username) * sort.dir;
    const av = a[sort.key] ?? -1, bv = b[sort.key] ?? -1;
    return ((av as number) - (bv as number)) * sort.dir || a.username.localeCompare(b.username);
  });
  const toggle = (key: Key) => setSort((s) => ({ key, dir: s.key === key ? (s.dir === 1 ? -1 : 1) : key === "username" ? 1 : -1 }));
  const Th = ({ k, label, right }: { k: Key; label: string; right?: boolean }) => (
    <th className={`px-4 py-2 ${right ? "text-right" : "text-left"}`}>
      <button onClick={() => toggle(k)} className="inline-flex items-center gap-1 font-mono text-[11px] uppercase tracking-[0.15em] text-muted-foreground hover:text-foreground" data-testid={`button-sort-${k}`}>
        {label}{sort.key === k && (sort.dir === 1 ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />)}
      </button>
    </th>
  );
  const idle = all.filter((s) => s.savedGames === 0).length;

  return (
    <div>
      <PageHeader kicker="Drawer D / Ledger" title="Statistics" sub="Per-player totals, including cardholders who have not saved anything yet." />
      <SummaryStrip />
      {stats.isLoading ? <RowsSkeleton rows={8} /> : stats.isError ? <ErrorBlock err={stats.error} onRetry={() => stats.refetch()} /> : all.length === 0 ? (
        <EmptyBlock title="No players to count" body="Statistics appear once players exist." />
      ) : (
        <>
          <div className="font-mono text-xs text-muted-foreground mb-2" data-testid="text-stats-count">{all.length} players / {idle} with zero saves</div>
          <div className="bg-card border border-card-border rounded-md overflow-x-auto">
            <table className="w-full text-sm min-w-[600px]">
              <thead className="border-b border-border bg-muted/50">
                <tr><Th k="username" label="Player" /><Th k="savedGames" label="Saved" right /><Th k="totalPlayCount" label="Total plays" /><Th k="averageRating" label="Avg rating" right /></tr>
              </thead>
              <tbody className="divide-y divide-border">
                {list.map((s) => (
                  <tr key={s.userId} className={s.savedGames === 0 ? "text-muted-foreground" : ""} data-testid={`row-stats-${s.userId}`}>
                    <td className="px-4 py-3 font-semibold">{s.username}{s.savedGames === 0 && <span className="ml-2 font-mono text-[10px] uppercase tracking-wider border border-input rounded px-1.5 py-0.5">no saves</span>}</td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{s.savedGames}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden min-w-[60px]">
                          <div className="h-full bg-accent origin-left" style={{ transform: `scaleX(${s.totalPlayCount / maxPlays})` }} />
                        </div>
                        <span className="font-mono tabular-nums w-14 text-right">{s.totalPlayCount.toLocaleString()}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right font-mono tabular-nums">{s.averageRating == null ? "n/a" : s.averageRating.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
