import { createContext, useContext, useEffect, useRef, type ReactNode } from "react";
import { useAuth, useClerk } from "@clerk/react";
import { useQueryClient } from "@tanstack/react-query";
import { Link } from "wouter";
import { useGetLibraryAccess } from "@workspace/api-client-react";

const AccessContext = createContext({ canManage: false });

export function AccessProvider({ children }: { children: ReactNode }) {
  const { isLoaded, userId } = useAuth();
  const client = useQueryClient();
  const { addListener } = useClerk();
  const previousUser = useRef<string | null | undefined>(undefined);
  useEffect(() => addListener(({ user }) => {
    const id = user?.id ?? null;
    if (previousUser.current !== undefined && previousUser.current !== id) {
      void client.cancelQueries();
      client.clear();
    }
    previousUser.current = id;
  }), [addListener, client]);
  const access = useGetLibraryAccess({
    query: {
      queryKey: ["/api/library/access", userId ?? null],
      enabled: isLoaded,
      staleTime: 0,
      refetchInterval: 30_000,
      retry: 1,
    },
  });
  const canManage = !!isLoaded && !!userId && !access.isError && access.data?.canManage === true;
  return (
    <AccessContext.Provider value={{ canManage }}>
      <div key={`${userId ?? "guest"}:${canManage}`}>{children}</div>
    </AccessContext.Provider>
  );
}

export const useLibraryAccess = () => useContext(AccessContext);

export function ManagerOnly({ children }: { children: ReactNode }) {
  const { canManage } = useLibraryAccess();
  return canManage ? children : (
    <section className="border rounded-md bg-card p-6">
      <h1 className="text-2xl font-bold">Manager access required</h1>
      <p className="mt-2 text-muted-foreground">Player details and saved-game management are private. Sign in with the approved manager account.</p>
      <Link href="/sign-in" className="mt-4 inline-block underline">Manager sign-in</Link>
    </section>
  );
}
