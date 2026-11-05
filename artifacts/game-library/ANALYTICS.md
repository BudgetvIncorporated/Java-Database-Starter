# Saved-game analytics

## Tracker ownership

This website uses Replit-hosted Project Analytics. The authoritative integration
contract is `.local/skills/project-analytics/SKILL.md`:

> Replit injects the Umami tracker into published app HTML through the deployment proxy.
> The app must not add or configure the analytics script, website ID, script URL, or analytics environment variables.

The same contract states that the tracker is present only after analytics is
enabled in Publishing settings and the supported website is published or
republished. Analytics access was checked and returned `authorized` before
instrumentation was added; this establishes access, not that collection is
currently enabled or that any events have arrived.

Consequently this app intentionally does not initialize a tracker in `App.tsx`
or add a script to its HTML. Local development, disabled analytics, and sessions
before the injected tracker loads are safe no-ops. There is no queue, retry, or
wait for analytics that could delay a mutation.

Platform reference: https://docs.replit.com/features/publishing/project-analytics

## Events

| Event | When |
| --- | --- |
| `saved_game_attempt` | Submitting create/edit, or confirming removal |
| `saved_game_success` | The mutation reports success, before list refresh |
| `saved_game_failure` | Local validation rejects submission or mutation reports an error |

Each payload contains only `operation` (`create`, `edit`, `remove`) and `status`.
Attempts use `pending`; successes use `success`. Failures use `validation`,
`unauthorized`, `forbidden`, `not_found`, `conflict`, `rate_limited`,
`server_error`, or `unknown`. Error text and record/form contents are never
forwarded. Cancelling a dialog does not send an event.

## Verification and release boundary

Run helper/privacy tests with:

```sh
node --test artifacts/game-library/tests/analytics.test.mjs
```

These tests cover the event payloads, numeric status classification, and absent,
synchronously throwing, and asynchronously rejecting trackers. They mock the
tracker and **do not prove delivery through the publishing proxy**.

The app type check and production build also pass. A public preview screenshot
confirms the app renders, not the protected manager flow or live analytics.

This change was made in a task branch without publishing. Actual event delivery
can only be confirmed after merging and releasing the website with analytics
enabled. On that released site, an authorized manager can use disposable records
to create, edit, and remove a link and trigger a validation or duplicate failure.
The Growth pane should show the matching attempts and outcomes, partitioned by
operation and fixed status, without record details. That production check has
not been performed as part of this task.
