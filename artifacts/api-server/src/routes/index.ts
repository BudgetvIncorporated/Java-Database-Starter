import { Router, type IRouter } from "express";
import healthRouter from "./health";
import { pool } from "@workspace/db";
import { PostgresLibraryStore } from "../lib/library-postgres";
import { createLibraryRouter } from "./library";

const router: IRouter = Router();

router.use(healthRouter);
export const libraryStore = new PostgresLibraryStore(pool);
router.use("/library", createLibraryRouter(libraryStore));

export default router;
