import { Router, type IRouter } from "express";
import healthRouter from "./health";
import { findLibraryDatabase, LibraryStore } from "../lib/library-store";
import { createLibraryRouter } from "./library";

const router: IRouter = Router();

router.use(healthRouter);
router.use("/library", createLibraryRouter(new LibraryStore(findLibraryDatabase())));

export default router;
