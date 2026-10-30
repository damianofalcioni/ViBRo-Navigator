# Connected-phone power review — 30 October 2026

Read-only ADB inspection of the connected Huawei PRA-LX1 / Android 8.0 phone.
No settings, battery statistics, app state or installed packages were changed.
The installed GPlay Debug package remains the pre-review build, last updated
29 October; the new Round Trip correction has not been installed.

## Current state

At the snapshot, the phone is USB-powered, fully charged and screen-on, with
`mStayOn=true` and `mStayOnWhilePluggedInSetting=7`. The display suspend blocker
is held. PowerManager lists zero current wake locks; ViBRo Debug has no running
service. These plugged-in conditions explain the present display/awake state,
but do not establish the cause during the morning route.

## Retained history covering the route

Battery history is still available from 08:05:57. Its relative-time records were
aligned with that reset timestamp; displayed wall-clock times below are
approximate. The history supports the unchanged elapsed-minus-uptime readings
in the application's route log.

| Evidence | Observation | Interpretation |
|---|---|---|
| CPU running state | Continuously running from approximately 08:29:33 until 08:46:23, including screen-off periods | Confirms the route's lack of observable CPU deep sleep |
| Android GPS activity | Set at approximately 08:30:10, cleared at 08:46:09 | Location activity closely overlaps continuous wakefulness; this is not a direct hardware power measurement |
| After GPS activity clears | CPU running state clears roughly 13.4 seconds later; sleep/wake cycles resume | The phone can sleep outside the route; location activity is the main investigation lead, without proving causation |
| Explicit ViBRo route wake lock | Two acquisitions; about 1.178 seconds actual total; maximum 0.733 seconds | No evidence that the route-calculation wake lock was leaked or held throughout navigation |
| Location-related locks attributed to ViBRo UID | `NlpCollectorWakeLock`: 121.396 seconds actual total; `NlpWakeLock`: 26.561 seconds; `LocationManagerService`: 15.229 seconds; `GCoreFlp`: 0.787 seconds | Location services do hold locks on the app's behalf; these are aggregate statistics, not one continuous lock |
| WindowManager full wake lock | 5m 17.589s | Screen-visible navigation also contributes; do not interpret this as a screen-off CPU-lock leak |

The UID statistics cover the entire period since 08:05:57, including diagnostic
and visible-UI use, rather than an isolated provider-comparison segment.
"Actual" and "blamed" wake durations are Android accounting fields, not measured
energy. No mAh estimate from BatteryStats was used to attribute savings.

Android documents that [LocationManager and FusedLocationProviderClient can acquire
wake locks on an application's behalf](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/identify-wls).
Consequently, a lock charged to ViBRo's UID need not be an explicit ViBRo
`PowerManager` lock. Source inspection finds the explicit partial lock around
route calculation; its brief device history agrees with the route log.

## Limits and next test

The non-root shell cannot read `/sys/kernel/debug/wakeup_sources` or
`/sys/power/wake_lock`; `/proc/wakelocks` is absent. Exact kernel suspend-blocker
attribution is therefore unavailable from this snapshot. No root access was
attempted. The history does not identify a single lock explaining the complete
screen-off awake interval.

The Android GPS flag stays set across the short stationary acquisition
suspensions. This does not prove that ViBRo's unsubscribe failed or that the GNSS
hardware remained at constant power: fused/provider tail behavior and other
requests can retain system activity. It does show that removing this app's
subscription is insufficient evidence of actual device sleep or energy savings.

The next useful experiment is an unplugged, screen-off stationary navigation
capture that records the existing subscription-suspension and fallback events,
followed by retrieving BatteryStats again. Compare whether CPU running/GPS
activity clears during suspension, then repeat with fused disabled under the
same conditions. Keep screen-on startup and provider transitions separate from
the stationary test window. Do not change thresholds or add wake locks from this
correlation alone.

## Local evidence

Personal/system dumps remain in ignored `.codex-temp` files:

- `phone-battery-history-20261030.txt`
- `phone-vibro-batterystats-20261030.txt`
- `phone-location-state-20261030.txt`
- `phone-power-review-20261030.cjs` and its derived JSON

Read commands included `dumpsys power`, `dumpsys deviceidle`,
`dumpsys batterystats --history`, `dumpsys batterystats --charged vibro.navigator.debug`,
package/service state and `dumpsys location`. Battery history was not reset and
full-history collection was not enabled. No production code or tests changed.

## Prepared follow-up

DEVICE_VALIDATION.md now has a short stationary protocol: four minutes unplugged
and screen off per provider, with read-only dumps before/after and no polling
during the test. It excludes USB startup, screen-on stopping and reconnection
from the analysis window. Existing app suspension events can now be paired with
explicit alarm/awake-timer/motion-trigger records and elapsed/uptime differences.
No new periodic logging or wake source was added.

At the software follow-up, ADB lists no connected device. No new power capture,
installation or physical trial has occurred. The original conclusion remains:
location activity correlates with wakefulness, while an explicit sustained ViBRo
route wake-lock leak and a hardware cause are unsupported. Actual CPU/GPS
cessation during acquisition suspension still needs the unplugged test.

## Unplugged legacy attempt, 12:32–12:45

The user's timestamps prompted a read-only capture after reconnection. Retained
history confirms unplugged, screen-off operation from approximately 12:32:48.6
to 12:45:18.2. App stopping is logged at 12:45:24.870; USB returns at approximately
12:45:43.6. This used the older installed Debug package (`sourceRevision=unknown`,
last updated 29 October), not the newly verified follow-up build.

No stationary suspension occurred: all 20 accepted fixes are network-only, with
median delivery spacing 40.237 seconds. Sleep correctly rejects network-only
evidence. Sixteen stream-stall recoveries restart active GPS/network acquisition;
GPS accounting clears only 100–165 ms at each restart. These are not sleep windows.

CPU-running never clears in the retained history; 20 app elapsed/uptime pairs end
with the same offset (variation 1 ms). There is no observed added deep sleep.
This demonstrates wakefulness during active acquisition under these conditions,
but cannot answer whether CPU/GPS activity stops during intentional suspension.
The app UID has 15.310 seconds actual partial-wake accounting and 4.001 seconds
full-wake accounting in this history, not a continuous explicit route lock.
GPS accounting is not a direct hardware-energy measurement.

No root, settings change, battery-history reset or installation was performed.
Raw app log SHA-256: `0ae3ea1e5d9aa2ee72820011ba2fb9d5cfed35f6140710799a1fa8f8683b00dc`.
Local dumps, parser and JSON use the ignored `stationary-1232-` prefix. Exact
conditions, limits and the repeat prerequisite are in DEVICE_VALIDATION.md.
