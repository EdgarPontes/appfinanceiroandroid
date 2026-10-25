# FCM integration plan — Android app

This document is the phased implementation plan for native Android notifications delivered by Firebase Cloud Messaging (FCM). The backend owns scheduling and sending; Android registers its installation, receives FCM data messages, renders notifications, and opens the corresponding history item.

> **Status:** implementation and initial internal-device validation are complete: token sync, FCM receiver, persistent deduplication, reminder time/zone fields, and removal of Android polling. Continue with staged rollout and the remaining device/edge-case checks. Keep this contract aligned with `docs/FCM_INTEGRATION.md` in the backend repository.

> **Validation checkpoint:** a server-generated data-only FCM test was received on an Android installation, and tapping it opened notification history. Firebase Console notification-only tests are supported in debug builds only. Debug and release builds both process the Firebase configuration successfully.

## Target architecture and non-goals

- **FCM is the only delivery channel for native Android notifications.** Remove the Android polling worker and scheduler; do not retain polling as a fallback.
- Normal authenticated API calls remain necessary for login, reminder configuration, token registration, notification history, and marking logs read. “FCM-only” refers to notification delivery, not all app/server communication.
- The PWA continues to use Web Push/VAPID. Never send an FCM token to Web Push or a Web Push subscription to FCM.
- While an account has an active Android FCM registration, the backend uses FCM as that account's push channel and suppresses legacy/unversioned unread-log polling responses. PWA history requests and modern Android history requests remain available; Web Push resumes if the Android registration is removed.
- Android API requests identify the client with `X-App-Client: android` and `X-App-Version`; this lets the server distinguish modern history requests from legacy polling clients.
- FCM is best-effort, not an exact-time or exactly-once transport. The backend must retain notification history; the app must tolerate delayed, expired, duplicated, and out-of-order messages.
- Do not put service-account credentials in the app. Avoid sensitive financial details in push data; FCM is not end-to-end encrypted.

## Shared contract (agree before coding)

### Device registration

- `POST /api/device-push-tokens` with the authenticated JWT and body:
  `{"token":"<fcm-token>","deviceId":"<installation-uuid>","platform":"android","appVersion":"<version>"}`.
- `deviceId` is a random UUID generated once per app installation and persisted in app-private, non-backed-up storage. It is not an Android hardware identifier. It is required so a rotated FCM token can replace the token for the same installation.
- The endpoint is idempotent for the same installation. It returns the registration `id` and an opaque `bindingId` for the current account-to-installation association. Repeated registration for the same account does not rotate `bindingId`; changing the account or creating a new association does.
- `DELETE /api/device-push-tokens/:id` removes only the authenticated account’s registration. Never send a client-supplied `userId`.
- The server upserts by installation, replaces the old token on `onNewToken`, and ensures that a token cannot remain active under two accounts. The exact database constraints and transaction behavior are specified in the backend plan.

### FCM message

Use an FCM **data-only** message; do not include the FCM `notification` block because the app must create the local notification and use its channels. Every data value is a string. Shared fields:

`schemaVersion`, `logId`, `bindingId`, `type`, `title`, `body`, `priority`, `deepLink`.

`logId` is the stable message-log UUID and the idempotency key. `bindingId` must match the current local registration so queued messages from a previous account association are ignored. `deepLink` is an allow-listed in-app route, never an arbitrary URL. Keep the total payload within FCM limits and omit sensitive details where possible. The backend sets Android delivery priority and TTL according to the message’s urgency and expiry; distinct reminder occurrences must not be collapsed into one another.

FCM acceptance by Google is not proof that a device displayed the notification. Do not call a message “delivered to device” solely because the Admin SDK accepted it.

## Current app state

- Firebase Messaging dependency and `AppFirebaseMessagingService` are present. The build applies Google Services only when the variant has its `../app/google-services.json` file.
- Android notification polling classes and scheduling hooks have been removed. History remains available through authenticated REST calls.
- `NotificationChannels` and `LocalNotificationPublisher` provide reusable local notification infrastructure.
- `POST_NOTIFICATIONS` is declared; preserve the Android 13+ runtime permission flow and handle system-level notification/channel settings.
- Reminder creation now sends date, `HH:mm`, and the phone's IANA timezone. Timezone changes are synchronized on app start and timezone-change broadcasts.

## Phased implementation

### Phase 0 — Freeze the contract and migration gates

1. Review this document with the backend plan and agree on endpoint schemas, payload fields, binding behavior, expiry, and delivery-status semantics before either side ships.
2. Decide the minimum Android version for FCM-only delivery and a forced-update/deprecation strategy for older polling clients. Do not activate FCM for an account while an unsupported legacy client can still poll the same logs.
3. Define that notification history remains available through the existing authenticated API; the history API is not a delivery fallback and must not publish local notifications.
4. Define privacy behavior: do not display FCM messages while logged out; reject messages whose `bindingId` does not match the active installation association. Push text must not expose sensitive account data on a locked screen.

**Exit criteria:** Android and backend contract tests use the same sample request and payload; rollout and legacy-client policy are approved.

### Phase 1 — Firebase client setup

1. Use the Firebase project and Android application ID `br.com.tecpontes.appfinanceiro` agreed with the backend.
2. Add the Google Services Gradle plugin through `gradle/libs.versions.toml`, apply it to the app, and add Firebase Messaging through the Firebase BoM.
3. Place the correct `../app/google-services.json` in the appropriate build-variant location. It is client configuration, not a service-account credential.
4. Do not add Firebase Analytics unless separately required. Build debug and release variants; do not commit private Admin SDK credentials.

**Exit criteria:** debug/release builds resolve Firebase configuration for each supported application ID.

### Phase 2 — Device identity, reminder time, and token lifecycle

1. Add a `TimePicker` to reminder creation/editing. Send `reminderTime` as `HH:mm` and the phone’s IANA zone from `ZoneId.systemDefault().id` (for example, `America/Sao_Paulo`) with the reminder. Keep `specificDate` as a date, not a server-local timestamp.
2. The timezone used for a reminder is the device timezone at create/update time. When the phone timezone changes, detect it on app start/resume and synchronize the affected reminder configurations so the backend recalculates future occurrences. Document that the server cannot know about a timezone change while the app is offline.
3. Generate and persist a random installation UUID in app-private, non-backed-up storage. Fetch the FCM token after authentication and POST token, installation UUID, platform, and app version with the current JWT.
4. On `onNewToken`, enqueue a unique network-constrained retry that updates that same installation. Retry transient failures with bounded exponential backoff; do not block login or log token/JWT values. Re-register after login and app start to repair missed syncs.
5. Persist the returned registration ID and `bindingId`. Do not trust a user ID from the client.
6. On account switch, remove the old association before clearing the session when online, then register the same installation for the new account. The backend rotates `bindingId` on ownership change; the client must ignore queued messages with an old binding.
7. On logout or local notification opt-out, immediately suppress local display and attempt authenticated DELETE before clearing credentials. If offline, keep the app locally disabled and retry revocation safely when possible; never display messages while logged out. Keep push text non-sensitive and short-lived so a stale server registration cannot expose private content.
8. When the user enables notifications again, obtain the current token and register the installation. A denied OS permission must not block saving reminders; explain that delivery may be received but not displayed.

**Exit criteria:** tests cover token rotation, retries, account switching, logout offline, timezone changes, and no token/user leakage in logs.

### Phase 3 — FCM receiver and idempotent local display

1. Implement `FirebaseMessagingService`, declare it with `android:exported="false"` and `com.google.firebase.MESSAGING_EVENT`, and handle both `onNewToken` and `onMessageReceived`.
2. Accept only the documented schema/version, required fields, valid UUID `logId`, current `bindingId`, supported `type`/`priority`, bounded text, and allow-listed `deepLink`. Ignore malformed, stale, logged-out, or wrong-account messages safely.
3. Use a durable local table (Room or equivalent) with a unique constraint on `logId` for received notifications. Do not rely on the current capped `SharedPreferences` set for correctness. Use `logId` as the stable notification tag/identity so an FCM retry updates the same system notification instead of creating a second alert.
4. Publish immediately from `onMessageReceived` using `LocalNotificationPublisher`/`NotificationChannels`; check `POST_NOTIFICATIONS`, notification enablement, and channel settings. Do not make a network request before displaying a valid push.
5. Put `logId` in the `PendingIntent`, open `NotificationHistoryActivity`, and mark that same log read through the authenticated API. Deep-link routing must not launch arbitrary external content.
6. Treat `onMessageReceived` as time-limited. Do not perform long work there; use a short bounded operation and enqueue non-display follow-up work if required. A force-stopped app may not receive FCM until the user launches it again; do not promise otherwise.

**Exit criteria:** duplicate deliveries for the same `logId` produce one visible notification; invalid or stale bindings never display; tap opens and marks the correct history item.

### Phase 4 — Remove Android polling (required cutover)

1. Polling removal is implemented. Preserve this invariant when changing notification startup, login, or screen code: do not reintroduce periodic unread-log fetching for delivery.
2. Remove only deduplication state used exclusively by polling. Keep device notification preferences and history behavior that remain relevant.
3. Do not add a silent REST polling fallback for missing/late FCM. Show the server-backed history when the user opens it; explain that push delivery is best effort.
4. Coordinate release with the backend’s minimum-version/legacy-client policy. Native Android sends use FCM only; PWA sends remain Web Push. Do not send both transports to the same registered Android installation.

**Exit criteria:** static search and tests confirm no Android notification polling work is scheduled or used; all supported native clients have FCM registration before backend activation.

### Phase 5 — Verification and production rollout

Test on physical devices with Google Play services, not just Firebase Console:

- registration after login, token rotation, reinstall, offline retry, logout, account switch, and stale `bindingId`;
- reminder creation/edit with device time and IANA zone, timezone change while app is active, DST gaps/overlaps, and server recurrence behavior;
- valid/invalid payload, duplicate/out-of-order FCM delivery, process recreation, foreground/background, force-stop limitation, and notification permission/channel disabled;
- taps, deep links, read-state update, and history consistency;
- no polling work, no FCM/Web Push double-send to an Android installation, and no sensitive tokens/payloads in logs;
- debug and release builds, staged internal rollout, monitoring of FCM acceptance/errors and invalid-token cleanup.

The app accepts Firebase Console notification-only test messages as a display smoke test in debug builds. Production delivery uses the validated data-only payload from the backend; a Console test is not an end-to-end history/binding test. Use only non-sensitive test content. Never put an Admin SDK key in the app or use it for client-side tests.

## Android references

- `app/src/main/AndroidManifest.xml` — permissions and service declaration.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/NotificationChannels.java` — notification channels.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/LocalNotificationPublisher.java` — local notification rendering and navigation.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/AppFirebaseMessagingService.java` — validated data-message receiver.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/PushRegistrationManager.java` — token registration, timezone synchronization, and logout/opt-out cleanup.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/FcmMessageDeduplicator.java` — durable, non-backed-up local `logId` deduplication.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/NotificationPreferences.java` — device preference state.
- `app/src/main/res/layout/dialog_create_reminder.xml` and `model/MessageConfigDto.java` — reminder time and device-zone fields.
- `docs/fcm_manual_testing_report.artifact.md` — manual device-test procedure; record each scenario's actual result before broad rollout.
- Backend `docs/FCM_INTEGRATION.md` — token API, scheduler, delivery records, and shared payload contract.
