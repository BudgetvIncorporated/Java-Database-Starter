import { useState } from "react";
import { Download } from "lucide-react";
import { downloadLibraryBackup } from "@workspace/api-client-react";
import { toast } from "@/hooks/use-toast";
import { errorMessage } from "@/lib/library";

export function LibraryBackup() {
  const [pending, setPending] = useState(false);

  async function download() {
    setPending(true);
    try {
      const blob = await downloadLibraryBackup({ responseType: "blob" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `game-library-${new Date().toISOString().replace(/[:.]/g, "-")}.db`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      // Give the browser time to start consuming the object URL.
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
      toast({ title: "Backup download started", description: "Keep this file private: it includes player emails. Your library has not changed." });
    } catch (error) {
      toast({ variant: "destructive", title: "Backup not downloaded", description: errorMessage(error, "Please try again.") });
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="mb-6 border border-card-border bg-card rounded-md px-4 py-3 flex flex-col sm:flex-row sm:items-center gap-3">
      <div className="flex-1 text-sm">
        <div className="font-semibold">Back up before major edits</div>
        <p className="text-muted-foreground text-xs mt-1">
          Download all four tables, including ratings, play counts, and player emails. Keep the file private.
          {" "}Available in the private workspace demo; public downloads require management access.
        </p>
      </div>
      <button type="button" onClick={download} disabled={pending}
        data-testid="button-download-backup" aria-busy={pending}
        className="inline-flex items-center justify-center gap-2 h-10 px-4 rounded-md border border-input text-sm hover:bg-muted disabled:opacity-50 shrink-0">
        <Download className="h-4 w-4" />
        {pending ? "Preparing backup…" : "Download backup"}
      </button>
    </section>
  );
}
