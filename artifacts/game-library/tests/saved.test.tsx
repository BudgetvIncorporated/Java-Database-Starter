import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import SavedPage from "../src/pages/saved";

type Operation = "create" | "edit" | "remove";
type Completion = { success: true } | { success: false; error: unknown };

const harness = vi.hoisted(() => ({
  calls: { create: vi.fn(), edit: vi.fn(), remove: vi.fn() },
  complete: {} as Record<Operation, (result: Completion) => Promise<void>>,
  invalidate: vi.fn(async () => {}),
  toast: vi.fn(),
  players: [{ id: 701, username: "Synthetic Player", email: "synthetic@example.test" }],
  games: [{
    id: 902, title: "Synthetic Game", genreId: 3, genreName: "Puzzle",
    releaseDate: "2020-01-01", url: "https://example.test/synthetic-game",
  }],
  saved: [{
    userId: 701, gameId: 902, username: "Synthetic Player", gameTitle: "Synthetic Game",
    rating: 4, playCount: 12,
  }],
}));

// Keep the actual page, forms, dialogs, buttons, and analytics implementation.
// Only network hooks and unrelated summary/cache/toast dependencies are replaced.
vi.mock("@workspace/api-client-react", async () => {
  const { useState } = await import("react");
  type Options = {
    mutation: {
      onSuccess: (saved: typeof harness.saved[number]) => unknown;
      onError: (error: unknown) => unknown;
    };
  };
  function useMutation(operation: Operation, options: Options) {
    const [isPending, setPending] = useState(false);
    return {
      isPending,
      mutate(payload: unknown) {
        harness.calls[operation](payload);
        setPending(true);
        harness.complete[operation] = async (result) => {
          try {
            if (result.success) await options.mutation.onSuccess(harness.saved[0]);
            else await options.mutation.onError(result.error);
          } finally {
            setPending(false);
          }
        };
      },
    };
  }
  const query = (data: unknown) => ({ data, isLoading: false, isError: false });
  return {
    useCreateLibrarySavedGame: (options: Options) => useMutation("create", options),
    useUpdateLibrarySavedGame: (options: Options) => useMutation("edit", options),
    useDeleteLibrarySavedGame: (options: Options) => useMutation("remove", options),
    useListLibrarySavedGames: () => query(harness.saved),
    useListLibraryPlayers: () => query(harness.players),
    useListLibraryGames: () => query(harness.games),
    useGetLibrarySummary: () => query({}),
  };
});

vi.mock("@/hooks/use-toast", () => ({
  useToast: () => ({ toast: harness.toast }),
}));
vi.mock("@/lib/library", async (importOriginal) => {
  const original = await importOriginal<typeof import("../src/lib/library")>();
  return {
    ...original,
    useInvalidateLibrary: () => harness.invalidate,
    SummaryStrip: () => null,
  };
});

const statuses = [
  "pending", "success", "validation", "unauthorized", "forbidden", "not_found",
  "conflict", "rate_limited", "server_error", "unknown",
];
const tracker = vi.fn<NonNullable<Window["umami"]>["track"]>();

beforeEach(() => {
  tracker.mockReset();
  window.umami = { track: tracker };
  harness.complete = {} as typeof harness.complete;
});

afterEach(() => {
  // Enforce the privacy contract for every emitted event, including failures.
  for (const [name, payload] of tracker.mock.calls) {
    expect(["saved_game_attempt", "saved_game_success", "saved_game_failure"]).toContain(name);
    if (!payload) throw new Error("Saved-game events must include an allowlisted payload.");
    expect(Object.keys(payload).sort()).toEqual(["operation", "status"]);
    expect(["create", "edit", "remove"]).toContain(payload.operation);
    expect(statuses).toContain(payload.status);
    expect(JSON.stringify(payload)).not.toMatch(
      /701|902|Synthetic|synthetic|example\.test|private|rating|playCount|userId|gameId/,
    );
  }
  delete window.umami;
});

function expectEvents(operation: Operation, outcome?: "success" | "failure", status?: string) {
  const expected = [["saved_game_attempt", { operation, status: "pending" }]];
  if (outcome) expected.push([
    `saved_game_${outcome}`,
    { operation, status: outcome === "success" ? "success" : status! },
  ]);
  expect(tracker.mock.calls).toEqual(expected);
}

async function openAction(operation: Operation) {
  const user = userEvent.setup();
  render(<SavedPage />);
  if (operation === "create") {
    await user.click(screen.getByTestId("button-add-saved"));
    await user.selectOptions(screen.getByTestId("select-saved-player"), "701");
    await user.selectOptions(screen.getByTestId("select-saved-game"), "902");
  } else {
    await user.click(screen.getByTestId(
      operation === "edit" ? "button-edit-saved-701-902" : "button-delete-saved-701-902",
    ));
  }
  expect(tracker).not.toHaveBeenCalled();
  return user;
}

function actionButton(operation: Operation) {
  return screen.getByTestId(operation === "remove" ? "button-confirm-delete" : "button-save-saved");
}

async function settle(operation: Operation, result: Completion) {
  await act(async () => harness.complete[operation](result));
}

describe.each<Operation>(["create", "edit", "remove"])("%s saved-game UI", (operation) => {
  it("emits exactly one attempt and success from the real handler", async () => {
    const user = await openAction(operation);
    await user.click(actionButton(operation));
    expectEvents(operation);
    expect(harness.calls[operation]).toHaveBeenCalledTimes(1);
    if (operation === "create") {
      expect(harness.calls.create).toHaveBeenCalledWith({
        data: { userId: 701, gameId: 902, rating: null, playCount: 0 },
      });
    } else if (operation === "edit") {
      expect(harness.calls.edit).toHaveBeenCalledWith({
        userId: 701, gameId: 902, data: { rating: 4, playCount: 12 },
      });
    } else {
      expect(harness.calls.remove).toHaveBeenCalledWith({ userId: 701, gameId: 902 });
    }
    await settle(operation, { success: true });
    expectEvents(operation, "success");
    expect(harness.invalidate).toHaveBeenCalledTimes(1);
    expect(harness.toast).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole(operation === "remove" ? "alertdialog" : "dialog")).toBeNull();
    expect(harness.calls[operation]).toHaveBeenCalledTimes(1);
  });

  it.each([
    [409, "conflict"], [403, "forbidden"], [500, "server_error"], [undefined, "unknown"],
  ])("emits one accurate failure for HTTP status %s", async (status, category) => {
    const user = await openAction(operation);
    await user.click(actionButton(operation));
    await settle(operation, {
      success: false,
      error: { status, data: { error: "Synthetic private server details 701/902" } },
    });
    expectEvents(operation, "failure", category as string);
    expect(screen.getByText("Synthetic private server details 701/902")).toBeTruthy();
    expect(harness.calls[operation]).toHaveBeenCalledTimes(1);
    expect(harness.invalidate).not.toHaveBeenCalled();
    expect(harness.toast).not.toHaveBeenCalled();
    expect((actionButton(operation) as HTMLButtonElement).disabled).toBe(false);
  });

  it("cancels without emitting events or calling a mutation", async () => {
    const user = await openAction(operation);
    await user.click(operation === "remove"
      ? screen.getByTestId("button-cancel-delete")
      : screen.getByRole("button", { name: /^Cancel$/ }));
    expect(tracker).not.toHaveBeenCalled();
    for (const mutation of Object.values(harness.calls)) expect(mutation).not.toHaveBeenCalled();
    expect(screen.queryByRole(operation === "remove" ? "alertdialog" : "dialog")).toBeNull();
  });

  it("blocks repeat submissions and cancellation while pending", async () => {
    const user = await openAction(operation);
    await user.dblClick(actionButton(operation));
    const button = actionButton(operation) as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    const cancel = operation === "remove"
      ? screen.getByTestId("button-cancel-delete")
      : screen.getByRole("button", { name: /^Cancel$/ });
    expect((cancel as HTMLButtonElement).disabled).toBe(true);
    await user.click(button);
    await user.click(cancel);
    await user.keyboard("{Escape}");
    if (operation !== "remove") {
      // Bypass the disabled button to exercise the submit handler's pending guard.
      fireEvent.submit(button.closest("form")!);
    }
    expect(screen.getByRole(operation === "remove" ? "alertdialog" : "dialog")).toBeTruthy();
    expectEvents(operation);
    expect(harness.calls[operation]).toHaveBeenCalledTimes(1);
    await settle(operation, { success: true });
    expectEvents(operation, "success");
  });

  it("reports a failed retry as a new attempt with just one new outcome", async () => {
    const user = await openAction(operation);
    await user.click(actionButton(operation));
    await settle(operation, { success: false, error: { status: 429 } });
    await user.click(actionButton(operation));
    await settle(operation, { success: true });
    expect(harness.calls[operation]).toHaveBeenCalledTimes(2);
    expect(tracker.mock.calls).toEqual([
      ["saved_game_attempt", { operation, status: "pending" }],
      ["saved_game_failure", { operation, status: "rate_limited" }],
      ["saved_game_attempt", { operation, status: "pending" }],
      ["saved_game_success", { operation, status: "success" }],
    ]);
  });
});

describe.each<"create" | "edit">(["create", "edit"])("%s local validation", (operation) => {
  it.each(["-1", "1.5", "2147483648", "not a number"])(
    "rejects play count %s with failure and no mutation",
    async (value) => {
      const user = await openAction(operation);
      const input = screen.getByTestId("input-saved-playcount");
      await user.clear(input);
      await user.type(input, value);
      await user.click(actionButton(operation));
      expectEvents(operation, "failure", "validation");
      for (const mutation of Object.values(harness.calls)) expect(mutation).not.toHaveBeenCalled();
      expect(screen.getByText("Play count must be a whole number of 0 or more.")).toBeTruthy();
      expect((screen.getByTestId("input-saved-playcount") as HTMLInputElement).value).toBe(value);
    },
  );
});

it("rejects missing player/game choices without mutating or leaking form values", async () => {
  const user = userEvent.setup();
  render(<SavedPage />);
  await user.click(screen.getByTestId("button-add-saved"));
  await user.click(actionButton("create"));
  expectEvents("create", "failure", "validation");
  expect(screen.getByText("Choose both a player and a game.")).toBeTruthy();
  for (const mutation of Object.values(harness.calls)) expect(mutation).not.toHaveBeenCalled();
});

it.each(["absent", "throws", "rejects"])(
  "still saves and closes the real dialog when the optional tracker %s",
  async (mode) => {
    if (mode === "absent") delete window.umami;
    if (mode === "throws") tracker.mockImplementation(() => { throw new Error("private tracker failure"); });
    if (mode === "rejects") tracker.mockRejectedValue(new Error("private tracker failure"));
    const user = await openAction("create");
    await user.click(actionButton("create"));
    await settle("create", { success: true });
    expect(harness.calls.create).toHaveBeenCalledTimes(1);
    expect(harness.invalidate).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    if (mode === "absent") expect(tracker).not.toHaveBeenCalled();
    else expectEvents("create", "success");
  },
);

it("opening and closing the edit dialog with Escape emits no events", async () => {
  const user = await openAction("edit");
  const dialog = screen.getByRole("dialog");
  expect(within(dialog).getByText("Synthetic Player / Synthetic Game")).toBeTruthy();
  await user.keyboard("{Escape}");
  expect(screen.queryByRole("dialog")).toBeNull();
  expect(tracker).not.toHaveBeenCalled();
  expect(harness.calls.edit).not.toHaveBeenCalled();
});
