import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { FormError } from "@/lib/library";

export function ConfirmDelete({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel,
  pending,
  error,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (o: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  pending: boolean;
  error: string | null;
  onConfirm: () => void;
}) {
  return (
    <AlertDialog open={open} onOpenChange={(o) => !pending && onOpenChange(o)}>
      <AlertDialogContent className="bg-card">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-2xl font-extrabold">{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <FormError msg={error} />
        <AlertDialogFooter>
          <AlertDialogCancel disabled={pending} data-testid="button-cancel-delete">Keep it</AlertDialogCancel>
          <button
            onClick={onConfirm}
            disabled={pending}
            data-testid="button-confirm-delete"
            className="inline-flex items-center justify-center h-9 px-4 rounded-md bg-destructive text-destructive-foreground text-sm font-semibold disabled:opacity-50"
          >
            {pending ? "Deleting..." : confirmLabel}
          </button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
