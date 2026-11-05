import type { RequestHandler } from "express";

// The current management boundary is the private workspace demo, not a login.
// Fail closed in production (and unknown environments) until the shared
// authenticated management policy is implemented. Fetch Metadata also prevents
// another website from using the permissive read API CORS policy to export PII.
export const requireLibraryManagement: RequestHandler = (req, res, next) => {
  if (process.env.NODE_ENV !== "development" || req.get("Sec-Fetch-Site") !== "same-origin") {
    res.status(403).json({
      error: "Backups are available only from the private workspace demo. Public backups require authenticated management access.",
    });
    return;
  }
  next();
};
