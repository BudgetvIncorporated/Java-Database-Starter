import type { Request, RequestHandler } from "express";
import { clerkClient, getAuth } from "@clerk/express";

export type LibraryIdentity = { signedIn: boolean; canManage: boolean };
export type IdentityResolver = (req: Request) => Promise<LibraryIdentity>;

// No player-table lookup, first-signup grant, or client-editable metadata.
// An empty owner configuration denies management to everyone.
export function isManagerEmail(
  emails: { emailAddress: string; verification: { status: string } | null }[],
  configured = process.env.LIBRARY_MANAGER_EMAIL ?? "",
) {
  const owner = configured.trim().toLowerCase();
  return !!owner && emails.some((email) =>
    email.verification?.status === "verified" && email.emailAddress.toLowerCase() === owner);
}

export const resolveLibraryIdentity: IdentityResolver = async (req) => {
  const { userId } = getAuth(req);
  if (!userId) return { signedIn: false, canManage: false };
  // Fetch current verified identity so access changes don't wait for JWT expiry.
  const user = await clerkClient.users.getUser(userId);
  return { signedIn: true, canManage: isManagerEmail(user.emailAddresses) };
};

export function libraryAccess(resolve: IdentityResolver = resolveLibraryIdentity): RequestHandler {
  return async (req, res, next) => {
    try {
      const identity = await resolve(req);
      res.locals.libraryIdentity = identity;
      if (req.path === "/access" && req.method === "GET") {
        res.json(identity);
        return;
      }
      const publicRead = ["GET", "HEAD"].includes(req.method) &&
        ["/summary", "/genres", "/games", "/statistics"].includes(req.path.replace(/\/$/, ""));
      if (!publicRead && !identity.canManage) {
        res.status(identity.signedIn ? 403 : 401).json({
          error: identity.signedIn ? "Only the approved library manager can access this operation." : "Sign in as the library manager to access this operation.",
        });
        return;
      }
      // Cookie-authenticated writes and exports must originate from our app.
      if (!publicRead && req.get("Sec-Fetch-Site") !== "same-origin") {
        res.status(403).json({ error: "Management requests must come from this library." });
        return;
      }
      next();
    } catch (error) {
      req.log?.error({ err: error }, "Library authorization failed");
      res.status(503).json({ error: "Could not verify library access. Please try again." });
    }
  };
}
