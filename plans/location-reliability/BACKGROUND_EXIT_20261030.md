# Background navigation exit investigation — 30 October 2026

Branch inspected: `codex/location-reliability`, HEAD `65de832d`. No application source changes made during this investigation. Times below are the phone's local Europe/Vienna times.

## Evidence and conclusion

The 30 October shutdown has system evidence of a **package force-stop**, rather than merely missing location callbacks. The exact requester is not retained. Huawei automatic app management is a likely cause, not a proven caller attribution.

| Evidence | 28 October | 30 October |
| --- | --- | --- |
| Navigation starts | 08:26:17 | 08:32:04 |
| Switch from Fused to GPS + network | 08:32:42 | 08:40:20 |
| Last app record | 08:38:13.877, activity stopping/unbinding | 08:46:07.708, location evaluation received |
| Next process startup | 08:44:12 | 08:49:23 |
| Previous navigation marker | Active, no clean stop, previous PID 14746 | Active, no clean stop, previous PID 11552 |
| Android battery exemption at startup | Already granted | Already granted |

User reports that the navigation notification disappeared in both cases. Neither app log records a Java crash, clean navigation stop, service destruction, or missing-foreground-notification shutdown. Buffered logging means the final app record is not an exact death timestamp.

The captured phone log contains this record, immediately after the 30 October app log ends:

```text
10-30 08:46:09.390 2081 2081 I HwCoreAppHelperService: LocalReceiver receives:android.intent.action.PACKAGE_RESTARTED,pkg:vibro.navigator.debug,uid:10431
```

Android documents `ACTION_PACKAGE_RESTARTED` as removing the package's processes, alarms, notifications, and other runtime state. Its force-stop semantics explain the disappearing notification and absence of an automatic restart. The receiver logging this broadcast is not evidence that this receiver requested the stop. Surrounding ActivityManager records have expired from the retained buffer.

The system log also contains a separate kill at **08:55:41**, for the newly launched PID 13312, because it depended on a dying external-storage provider. That is after the reported navigation failure and must not be substituted as its cause.

The earlier log records `Pause navigation requested from UI` at 08:36:17.230 and `Navigation paused` at 08:36:17.352. The user reports answering a phone call and does not recall pressing Pause; intentional manual pausing is therefore **not established**. The current code emits this message from `NavigationActivityCommands.togglePausedFromUi`; no phone-call listener or audio-focus callback that pauses navigation was found. A paused session still has a foreground service, so pausing alone does not explain the vanished notification. System records for that earlier failure are no longer available.

## Device configuration correction

ADB inspection confirmed `vibro.navigator.debug` is in Android's battery-optimization whitelist. Huawei's Launch screen nevertheless showed **Manage automatically** for both ViBRo variants.

For **ViBRo Navigator Debug**, changed Huawei Launch management to **Manage manually**, with **Auto-launch**, **Secondary launch**, and **Run in background** enabled. Verified that the saved list subtitle changed to **Manage manually**. The production variant remains as observed. Returned the phone to its home screen.

This is a device-setting correction, not a verified resolution of the field failure. A new screen-off navigation trial, ideally including an incoming call, remains necessary. No APK rebuild or installation was performed.

## Code findings distinct from the force-stop

- `android/logging/AndroidProcessExitReader.java:28`: process-exit history is unavailable below Android 11. The Android 8 phone therefore cannot identify the cause from the app's next-launch diagnostic alone.
- `nav/service/NavigationServiceCommandHandler.java:39`: returns `START_STICKY`, but a null restart intent only promotes the service; it does not restore the in-memory session created in `NavigationService.java:54`. This is an ordinary process-recreation resilience gap, **not a way to recover from package force-stop**. Restoring navigation would also need to preserve completed stops, paused state, route/progress and other session state; changing just the restart constant is insufficient.
- `logging/AppLogger.java:183` and `android/logging/AndroidLogFolderWriter.java:50`: selected-folder writes and periodic flushes still occur synchronously on the calling thread, including main-thread navigation callbacks. That is a separate blocking-I/O risk, but the captured force-stop does not prove storage blocking caused this incident.

Do not change location-selection, wake-lock, stationary-sleep, or recovery-alarm policies on the assumption that they caused this force-stop. Capture system evidence during any recurrence before choosing a code remedy.

## Local evidence and remaining validation

Original four supplied logs remain in the user's Downloads directory. Device captures and UI verification are private, ignored artifacts under `.codex-temp/background-exit-20261030/`; they include system information from other apps and are not checked in. No absolute travel coordinates are copied into this report.

Checks performed: inspection of all four app logs, read-only ADB logcat/DropBox/service/battery diagnostics, lifecycle/call-path source review, and saved Huawei Launch setting verification. No automated tests were run because application code did not change. Physical navigation, screen-off persistence, notification continuity and call interaction remain unverified.

References:

- [Android package restart / force-stop broadcast](https://developer.android.com/reference/android/content/Intent#ACTION_PACKAGE_RESTARTED)
- [Android service restart semantics](https://developer.android.com/reference/android/app/Service#START_STICKY)
- [ApplicationExitInfo, available from API 30](https://developer.android.com/reference/android/app/ApplicationExitInfo)
- [Huawei background-app settings](https://consumer.huawei.com/en/support/content/en-us00428704/)
