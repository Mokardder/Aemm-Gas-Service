# Account Sync Adapter (Dual Account Ping-Pong) Requirements

## Goal
Run two Android SyncAdapter accounts in sequence:
1. Account A sync runs.
2. Account B sync runs.
3. B triggers A again.
4. Continue with safety limits.

## Required Configuration

- Two distinct account types must exist:
  - `android.iocl.dac_collector.process`
  - `android.iocl.dac_collector`
- Two distinct account names should be used to avoid clashes.
- Both accounts must be configured with:
  - `ContentResolver.setIsSyncable(account, authority, 1)`
  - `ContentResolver.setSyncAutomatically(account, authority, true)`
  - `ContentResolver.addPeriodicSync(account, authority, extras, intervalSeconds)`

## Important Android Rule
`ContentResolver.addPeriodicSync` interval is in **seconds**, not milliseconds.

## Manifest Requirements

Each sync service must include:
- `android:permission="android.permission.BIND_SYNC_ADAPTER"`
- `android.content.SyncAdapter` intent filter
- matching syncadapter XML metadata

Each authenticator service must include:
- `android:permission="android.permission.BIND_ACCOUNT_AUTHENTICATOR"`
- `android.accounts.AccountAuthenticator` intent filter
- matching authenticator XML metadata

## Chain-Sync Logic Requirements

- Use `onPerformSync` of current account to trigger the other account.
- Pass chain metadata in extras:
  - `chain_depth`
  - `chain_source`
- Apply max depth safety guard (`MAX_CHAIN_DEPTH`) to prevent runaway loops.
- On failure, update `SyncResult` and stop chain.

## Initialization Requirements

- Keep only one startup path (`SyncUtils.initialize(context)`) responsible for dual account setup.
- Startup should be idempotent and safe to call multiple times.

## Operational Recommendation

- Keep periodic sync intervals conservative (e.g. 15 minutes or more).
- Use chained sync only after successful sync work.
- Avoid excessive expedited sync usage to reduce throttling/battery impact.
