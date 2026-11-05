type AnalyticsData = Record<string, string | number | boolean>;

declare global {
  interface Window {
    umami?: {
      track(name: string, data?: AnalyticsData): void | Promise<unknown>;
    };
  }
}

/**
 * Replit's publishing proxy supplies window.umami when analytics is enabled
 * for a supported published website. Do not add a tracker script or website ID.
 * Development and disabled-analytics sessions intentionally send no events.
 * See ../../ANALYTICS.md for the deployment contract and verification limits.
 */
export function trackEvent(name: string, data?: AnalyticsData): void {
  if (typeof window === "undefined") return;
  try {
    // Handle both synchronous tracker errors and rejected network promises.
    void Promise.resolve(window.umami?.track(name, data)).catch(() => {});
  } catch {
    // Analytics is best-effort only.
  }
}

type SavedGameOperation = "create" | "edit" | "remove";
type FailureCategory =
  | "validation"
  | "unauthorized"
  | "forbidden"
  | "not_found"
  | "conflict"
  | "rate_limited"
  | "server_error"
  | "unknown";
type SavedGameEvent =
  | { outcome: "attempt"; operation: SavedGameOperation }
  | { outcome: "success"; operation: SavedGameOperation }
  | { outcome: "failure"; operation: SavedGameOperation; status: FailureCategory };

/** Only classify numeric HTTP status codes; never forward server text or data. */
export function savedGameFailureCategory(error: unknown): FailureCategory {
  const status = error && typeof error === "object" && "status" in error ? error.status : undefined;
  switch (status) {
    case 400: case 422: return "validation";
    case 401: return "unauthorized";
    case 403: return "forbidden";
    case 404: return "not_found";
    case 409: return "conflict";
    case 429: return "rate_limited";
    default: return typeof status === "number" && status >= 500 && status <= 599 ? "server_error" : "unknown";
  }
}

export function trackSavedGame(event: SavedGameEvent): void {
  // Construct a fresh payload instead of forwarding an object that could hold
  // record IDs, names, form contents, or error details.
  trackEvent(`saved_game_${event.outcome}`, {
    operation: event.operation,
    status: event.outcome === "failure" ? event.status : event.outcome === "success" ? "success" : "pending",
  });
}
