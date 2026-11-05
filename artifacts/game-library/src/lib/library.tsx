import { type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Link, useLocation } from "wouter";
import { AlertTriangle, BarChart3, BookMarked, Library, RotateCw, Users } from "lucide-react";
import { useGetLibrarySummary } from "@workspace/api-client-react";
import { Skeleton } from "@/components/ui/skeleton";
import { LibraryBackup } from "@/components/library-backup";

export const listQuery = { staleTime: 30_000, refetchOnWindowFocus: true } as const;

export function useInvalidateLibrary() {
  const qc = useQueryClient();
  return () =>
    qc.invalidateQueries({
      predicate: (q) => typeof q.queryKey[0] === "string" && (q.queryKey[0] as string).startsWith("/api/library"),
    });
}

export function errorMessage(err: unknown, fallback = "Something went wrong."): string {
  if (err && typeof err === "object") {
    const data = (err as { data?: unknown }).data;
    if (data && typeof data === "object" && typeof (data as { error?: unknown }).error === "string") {
      return (data as { error: string }).error;
    }
    const msg = (err as { message?: unknown }).message;
    if (typeof msg === "string" && msg) return msg;
  }
  return fallback;
}

const nav = [
  { href: "/", label: "Catalog", icon: Library },
  { href: "/players", label: "Players", icon: Users },
  { href: "/saved", label: "Saved", icon: BookMarked },
  { href: "/statistics", label: "Statistics", icon: BarChart3 },
];

export function Shell({ children }: { children: ReactNode }) {
  const [loc] = useLocation();
  return (
    <div className="min-h-[100dvh] flex flex-col md:flex-row">
      <aside className="md:w-60 md:min-h-[100dvh] bg-sidebar text-sidebar-foreground md:sticky md:top-0 md:self-start flex md:flex-col">
        <div className="px-5 py-4 md:py-7 flex-shrink-0">
          <div className="font-mono text-[10px] tracking-[0.25em] uppercase opacity-60 hidden md:block">Room 214 / Est. catalog</div>
          <div className="text-xl md:text-2xl font-extrabold leading-none md:mt-2">
            Game<span className="text-sidebar-primary">.</span>Library
          </div>
        </div>
        <nav className="flex md:flex-col gap-1 px-2 md:px-3 overflow-x-auto items-center md:items-stretch flex-1">
          {nav.map((n) => {
            const active = n.href === "/" ? loc === "/" : loc.startsWith(n.href);
            return (
              <Link
                key={n.href}
                href={n.href}
                data-testid={`link-nav-${n.label.toLowerCase()}`}
                className={`flex items-center gap-2.5 px-3 py-2 rounded-md text-sm whitespace-nowrap transition-colors ${
                  active ? "bg-sidebar-primary text-sidebar-primary-foreground font-semibold" : "hover:bg-sidebar-accent"
                }`}
              >
                <n.icon className="h-4 w-4" />
                <span>{n.label}</span>
              </Link>
            );
          })}
        </nav>
        <div className="hidden md:block px-5 py-6 font-mono text-[10px] opacity-50 leading-relaxed">
          Live SQLite records.
          <br />
          Every change is written through.
        </div>
      </aside>
      <main className="flex-1 min-w-0 px-4 sm:px-8 lg:px-12 py-6 md:py-10 max-w-[1280px]">
        <LibraryBackup />
        {children}
      </main>
    </div>
  );
}

export function SummaryStrip() {
  const { data, isLoading, isError, refetch } = useGetLibrarySummary({
    query: { ...listQuery, queryKey: ["/api/library/summary"] },
  });
  const items: [string, keyof NonNullable<typeof data>][] = [
    ["Games", "games"],
    ["Players", "players"],
    ["Saved", "savedGames"],
    ["Total plays", "totalPlays"],
    ["Genres", "genres"],
  ];
  if (isError)
    return (
      <div className="mb-8 flex items-center gap-3 text-sm text-destructive font-mono" data-testid="status-summary-error">
        Summary unavailable.
        <button className="underline" onClick={() => refetch()} data-testid="button-retry-summary">Retry</button>
      </div>
    );
  return (
    <div className="mb-8 grid grid-cols-2 sm:grid-cols-5 border border-card-border bg-card rounded-md overflow-hidden">
      {items.map(([label, key], i) => (
        <div key={key} className={`px-4 py-3 ${i ? "sm:border-l" : ""} ${i > 1 ? "border-t sm:border-t-0" : i === 1 ? "border-l sm:border-l" : ""} border-card-border`}>
          <div className="font-mono text-[10px] uppercase tracking-[0.18em] text-muted-foreground">{label}</div>
          {isLoading || !data ? (
            <Skeleton className="h-7 w-14 mt-1" />
          ) : (
            <div className="text-2xl font-extrabold tabular-nums" data-testid={`text-summary-${key}`}>
              {data[key].toLocaleString()}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

export function PageHeader({ kicker, title, sub, action }: { kicker: string; title: string; sub: string; action?: ReactNode }) {
  return (
    <header className="rise mb-6 flex flex-col sm:flex-row sm:items-end justify-between gap-4">
      <div>
        <div className="font-mono text-xs uppercase tracking-[0.22em] text-accent">{kicker}</div>
        <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight leading-[0.95] mt-1">{title}</h1>
        <p className="text-muted-foreground mt-2 max-w-xl">{sub}</p>
      </div>
      {action}
    </header>
  );
}

export function ErrorBlock({ err, onRetry }: { err: unknown; onRetry: () => void }) {
  return (
    <div className="border border-destructive/40 bg-destructive/5 rounded-md p-6 flex flex-col sm:flex-row sm:items-center gap-4" data-testid="status-error">
      <AlertTriangle className="h-6 w-6 text-destructive" />
      <div className="flex-1">
        <div className="font-semibold">Could not load records</div>
        <div className="text-sm text-muted-foreground font-mono">{errorMessage(err)}</div>
      </div>
      <button onClick={onRetry} data-testid="button-retry" className="inline-flex items-center gap-2 px-3 py-2 rounded-md border border-input hover:bg-muted text-sm">
        <RotateCw className="h-4 w-4" /> Retry
      </button>
    </div>
  );
}

export function EmptyBlock({ title, body, action }: { title: string; body: string; action?: ReactNode }) {
  return (
    <div className="ruled border border-dashed border-input rounded-md px-6 py-14 text-center bg-card/60" data-testid="status-empty">
      <div className="font-mono text-[10px] uppercase tracking-[0.25em] text-muted-foreground">No cards in this drawer</div>
      <div className="text-2xl font-extrabold mt-2">{title}</div>
      <p className="text-muted-foreground mt-1 max-w-md mx-auto">{body}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function RowsSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="space-y-2" data-testid="status-loading">
      {Array.from({ length: rows }).map((_, i) => (
        <Skeleton key={i} className="h-14 w-full" />
      ))}
    </div>
  );
}

export const inputCls =
  "w-full h-10 px-3 rounded-md border border-input bg-card text-sm focus:outline-none focus:ring-2 focus:ring-ring/50 focus:border-ring";
export const btnPrimary =
  "inline-flex items-center justify-center gap-2 h-10 px-4 rounded-md bg-primary text-primary-foreground text-sm font-semibold hover:opacity-90 disabled:opacity-50 transition-opacity";
export const btnGhost =
  "inline-flex items-center justify-center gap-2 h-10 px-4 rounded-md border border-input text-sm hover:bg-muted disabled:opacity-50";
export const btnIcon = "inline-flex items-center justify-center h-8 w-8 rounded-md hover:bg-muted text-muted-foreground hover:text-foreground";

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <span className="font-mono text-[11px] uppercase tracking-[0.15em] text-muted-foreground">{label}</span>
      <div className="mt-1">{children}</div>
      {hint && <span className="text-xs text-muted-foreground">{hint}</span>}
    </label>
  );
}

export function FormError({ msg }: { msg: string | null }) {
  if (!msg) return null;
  return (
    <div className="text-sm text-destructive border border-destructive/40 bg-destructive/5 rounded-md px-3 py-2" role="alert" data-testid="text-form-error">
      {msg}
    </div>
  );
}

export function Rating({ value }: { value: number | null }) {
  if (value == null) return <span className="font-mono text-xs text-muted-foreground">unrated</span>;
  return (
    <span className="inline-flex gap-0.5" aria-label={`${value} of 5`}>
      {[1, 2, 3, 4, 5].map((i) => (
        <span key={i} className={`h-2.5 w-2.5 rounded-full ${i <= value ? "bg-accent" : "bg-muted border border-input"}`} />
      ))}
    </span>
  );
}
