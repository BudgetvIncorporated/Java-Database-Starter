import { type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ErrorBoundary } from '@/components/error-boundary';
import { Toaster } from '@/components/ui/toaster';
import { TooltipProvider } from '@/components/ui/tooltip';
import NotFound from '@/pages/not-found';
import CatalogPage from '@/pages/catalog';
import PlayersPage from '@/pages/players';
import SavedPage from '@/pages/saved';
import StatisticsPage from '@/pages/statistics';
import { Shell } from '@/lib/library';
import { Route, Switch, useLocation, Router as WouterRouter } from 'wouter';
import { ClerkProvider } from '@clerk/react';
import { publishableKeyFromHost } from '@clerk/react/internal';
import { AccessProvider, ManagerOnly } from '@/lib/access';
import { clerkAppearance, SignInPage, SignUpPage } from '@/lib/auth';

const clerkPubKey = publishableKeyFromHost(
  window.location.hostname,
  import.meta.env.VITE_CLERK_PUBLISHABLE_KEY,
);
const clerkProxyUrl = import.meta.env.VITE_CLERK_PROXY_URL;
const basePath = import.meta.env.BASE_URL.replace(/\/$/, "");
function stripBase(path: string): string {
  return basePath && path.startsWith(basePath) ? path.slice(basePath.length) || "/" : path;
}

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, refetchOnWindowFocus: true, retry: 1 } },
});

function Router() {
  return (
    <Shell>
      <RoutedErrorBoundary>
        <Switch>
          <Route path="/" component={CatalogPage} />
          <Route path="/players"><ManagerOnly><PlayersPage /></ManagerOnly></Route>
          <Route path="/saved"><ManagerOnly><SavedPage /></ManagerOnly></Route>
          <Route path="/statistics" component={StatisticsPage} />
          <Route path="/sign-in/*?" component={SignInPage} />
          <Route path="/sign-up/*?" component={SignUpPage} />
          <Route component={NotFound} />
        </Switch>
      </RoutedErrorBoundary>
    </Shell>
  );
}

function RoutedErrorBoundary({ children }: { children: ReactNode }) {
  const [location] = useLocation();
  return <ErrorBoundary resetKey={location}>{children}</ErrorBoundary>;
}

function AuthenticatedApp() {
  const [, setLocation] = useLocation();
  return (
    <ClerkProvider
      publishableKey={clerkPubKey}
      proxyUrl={clerkProxyUrl}
      appearance={clerkAppearance}
      signInUrl={`${basePath}/sign-in`}
      signUpUrl={`${basePath}/sign-up`}
      routerPush={(to) => setLocation(stripBase(to))}
      routerReplace={(to) => setLocation(stripBase(to), { replace: true })}
      localization={{ signIn: { start: { title: "Game Library sign-in", subtitle: "Only the approved manager can edit the shared library." } }, signUp: { start: { title: "Game Library account", subtitle: "Creating an account does not grant manager access." } } }}
    >
      <QueryClientProvider client={queryClient}>
        <AccessProvider>
          <TooltipProvider>
            <Router />
            <Toaster />
          </TooltipProvider>
        </AccessProvider>
      </QueryClientProvider>
    </ClerkProvider>
  );
}

function App() {
  return <WouterRouter base={basePath}><AuthenticatedApp /></WouterRouter>;
}

export default App;
