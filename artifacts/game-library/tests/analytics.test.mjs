import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import vm from "node:vm";
import ts from "typescript";

const source = readFileSync(new URL("../src/lib/analytics.ts", import.meta.url), "utf8");
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText;

function load(window) {
  const context = { exports: {}, ...(window === undefined ? {} : { window }) };
  vm.runInNewContext(compiled, context);
  return context.exports;
}

test("analytics is safe without a browser, without a tracker, and with broken trackers", async () => {
  for (const window of [
    undefined,
    {},
    { umami: { track() { throw new Error("tracker unavailable"); } } },
    { umami: { track() { return Promise.reject(new Error("network unavailable")); } } },
  ]) {
    const analytics = load(window);
    let mutationRan = false;
    analytics.trackSavedGame({ operation: "create", outcome: "attempt" });
    mutationRan = true;
    assert.equal(mutationRan, true);
  }
  // Allow rejection handlers to run; an unhandled rejection fails this test.
  await new Promise((resolve) => setImmediate(resolve));
});

test("all actions and outcomes send only operation and allowlisted status", () => {
  const calls = [];
  const { trackSavedGame } = load({ umami: { track: (...args) => calls.push(args) } });
  for (const operation of ["create", "edit", "remove"]) {
    for (const outcome of ["attempt", "success", "failure"]) {
      trackSavedGame({
        operation, outcome, status: "conflict",
        userId: 123, gameId: 456, username: "private player",
        gameTitle: "private title", email: "private@example.test",
        url: "/private", formText: "private input",
      });
      const [name, payload] = calls.at(-1);
      assert.equal(name, `saved_game_${outcome}`);
      assert.deepEqual(JSON.parse(JSON.stringify(payload)), {
        operation, status: outcome === "attempt" ? "pending" : outcome === "success" ? "success" : "conflict",
      });
    }
  }
  assert.equal(calls.length, 9);
});

test("failures use numeric HTTP status only and never include error text", () => {
  const { savedGameFailureCategory: category } = load({});
  const statuses = [
    [400, "validation"], [422, "validation"], [401, "unauthorized"],
    [403, "forbidden"], [404, "not_found"], [409, "conflict"],
    [429, "rate_limited"], [500, "server_error"], [599, "server_error"],
    [600, "unknown"], [200, "unknown"], ["409", "unknown"],
  ];
  for (const [status, expected] of statuses) {
    assert.equal(category({ status, message: "private title", data: { error: "private player" } }), expected);
  }
  for (const error of [null, undefined, "private text", new TypeError("private URL"), {}]) {
    assert.equal(category(error), "unknown");
  }
});
