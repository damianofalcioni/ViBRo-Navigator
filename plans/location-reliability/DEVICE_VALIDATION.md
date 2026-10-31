# Phone/watch and power validation

Prepared 27 October 2026; updated 31 October 2026. The user confirmed the morning Huawei PRA-LX1 (API 26) GPlay Debug recording matches `65de832d`, before the Round Trip correction now in `a455ce9f`. The verified follow-up was installed at 12:51 on 30 October and passed a startup smoke check; navigation, phone/watch and power validation remain pending.

## Build and settings record

Record the installed package, sourceRevision, build type, provider setting, fixed/dynamic interval setting, notification channels, battery optimization status, screen state, carrying position, watch/companion versions, battery level, charging status and enabled logging. Keep the same settings across comparison runs. Preserve old evidence files; use a fresh log for each trip.

Use the verified APK and current hash from the latest STATUS.md handoff. GPlay Debug uses package `vibro.navigator.debug`, and F-Droid Debug uses `vibro.navigator.foss.debug`; release packages are `vibro.navigator` and `vibro.navigator.foss`. Use the Debug launcher for these test APKs. Build revision can be supplied explicitly with `-PsourceRevision=<revision>`; the default is `unknown` for reproducible source builds. Record uncommitted changes separately. A build label does not prove that a dirty checkout exactly matches a commit.

## Guidance and motion checks

1. Repeat the route screen-off with the same carrying position. Independently note actual turn crossings and the times the phone and watch alert. A voice memo or a second clock/device can supply reference observations without changing this phone's screen/wake behavior.
2. Include a genuine stop, ordinary departure, slow walking and a turn after a stop. Confirm departure verification resumes, useful walking is not labelled a sustained stop, and orientation notices occur only with appropriate stationary evidence. With one-second callbacks, check that small adjacent displacements cannot conceal sustained walking, especially when native speed is absent. Where native speed uncertainty is available, include very slow walking near 0.25–0.35 m/s: precise speed metadata must not prevent sustained independent position progress from confirming movement and limiting filter lag. Precise slow speed without position progress must remain UNKNOWN when its uncertainty excludes zero: it must neither establish movement nor confirm a stop, issue stationary orientation advice, add stationary trip time or authorize stationary sleep. Verify rapid departure acquisition continues until independent raw progress confirms movement, and that a subsequent genuine zero-speed stop can still complete normal stop/sleep confirmation. After a speedless walk, allow the preceding raw displacement to leave the ten-second stop-evidence window before expecting stationary advice.
3. At a final/intermediate target, hold a good fix inside the bounded arrival region for multiple observations. A coarse nearby fix should retain approach guidance. Test an intermediate beeline out and back: confirmation should precede target advancement, while return-to-road guidance remains available.
4. Compare regular long-interval fixes on a long straight leg with the poor-quality final approach. Long intervals should remain possible on the former; the latter should request earlier verification. Include a brief reversal during frequent acquisition: backward route progress must discard forward live ETA evidence even when the fix interval is less than two seconds, and subsequent forward progress must rebuild it without old acceleration.
5. Repeat pause/resume, provider toggling, task backgrounding, screen off/on and stopping navigation. Old callbacks must not resume a stopped session or produce old guidance.
6. With fixed one-second acquisition, verify that sustained walking retains live ETA once positional progress clears its uncertainty threshold. Repeat slow walking with three-second callbacks; small per-callback steps must not permanently force route-model timing. A stop or reversal must still suppress old travel timing. On a Round Trip with overlapping start/end geometry, arrival must gather a new eligible confirmation window after the match ceases to qualify as the loop finish.
7. After independent motion confirmation at 0.25–0.35 m/s, verify that GPS display speed and trip maximum retain that supported slow speed in Route and Straight Line modes. Repeat without native speed where the device permits. Unconfirmed slow drift must still be suppressed; wait for sufficient raw progress and usable filtered uncertainty before assessing this display check.

## Locate notification delay

Use traceId to connect the recorded observation, motion assessment, guidance decision and notification-post completion. Compare measurement age, receipt elapsed time and uptime, generated events and posting duration. `deliveryReceipt=unobserved` means Android/watch presentation remains unmeasured.

Record whether a delay was visible on the phone, watch or both. Capture a system bugreport close to a reproduced incident, with user authorization for its personal diagnostic contents. System notification/location/power diagnostics may help attribute timing; app notify completion alone cannot prove forwarding or vibration receipt.

## Stationary sleep and recovery

On a device with a supported significant-motion wake-up trigger, observe actual sleep/confirmation, motion wake and fallback-check records. Compare requested deadlines with actual dispatch; test walking away during a screen-off stop. Repeat with the sensor unavailable or registration/alarm failure simulated by focused tests. Active acquisition must remain the fallback when prerequisites fail.

Include a nearby fused-to-legacy handover during stop confirmation and during a fallback check. Confirm that `provider_change` starts a full new stop window and cadence learning, with active acquisition until the new provider independently qualifies. A raw/filtered pair from different observations must log `observation_mismatch` and cannot authorize sleep. Clustered handovers and mismatched pairs also have deterministic JVM coverage; do not force a physical provider failure merely to manufacture them.

Inspect both measurement and receipt times during stop and fallback confirmation. Delayed delivery must not stretch a short measurement sequence into a complete stop window, and clustered measurements delivered separately must not re-suspend acquisition. A fresh eligible fix outside the fixed stop origin must restart full confirmation even inside the two-second sample-spacing window. JVM/service regressions cover these cases for legacy and fused subscriptions; physical delivery and wake latency still need observation.

Do not assume a 30-second inexact alarm will dispatch on time in idle. If measured departure delay is unacceptable, retain active acquisition for the affected configuration while investigating. Report maximum/typical observed latency and any missed departure, rather than claiming guaranteed wake-up.

## Provider/power comparison

Repeat comparable trips in alternating order: legacy first/fused second, then fused first/legacy second, with several repetitions where practical. Keep starting battery conditions, phone temperature, screen state, route, carrying position, watch forwarding and logging comparable. Compare corresponding route sections and report variability.

Measure independently observed prompt quality, departure latency, fix age/uncertainty, filter displacement, requested subscription exposure and suitable device power/GNSS/CPU-wake evidence. Provider uncertainty and agreement with the planned route are not ground-truth error. A short simultaneous-provider comparison may characterize disagreement, but must be separate from single-mode power attribution.

Use consumption totals only when counters have suitable scale, discharge direction and charging/reset history. Raw vendor current units remain unvalidated. A percentage plateau is allowed. If counters remain unsuitable, report energy unresolved or use a suitable independent measurement setup that does not silently change idle/charging behavior.

## Short unplugged stationary CPU/GPS check

Use the latest verified build from STATUS.md. Allow about five minutes per run,
with four minutes unplugged and screen off. Physical participation is needed for
unplugging, keeping the phone still and ending the test. Do not change power,
stay-awake or battery-optimization settings, reset BatteryStats, or root the phone.

1. While USB is connected, save read-only `dumpsys power`, `dumpsys location`,
   `dumpsys batterystats --history` and app UID statistics into ignored local
   files with a new run label. Record package/build, provider setting, logging,
   battery/charging state and the time. Do not enable extra system recording.
2. At a safe stationary location with good reception, start ordinary navigation
   with dynamic intervals and logging enabled. Let the position stabilize. Keep
   carrying position and all unrelated settings fixed. Use the current provider
   setting for the first run; record whether fused remains primary or falls back.
3. Unplug USB, turn the screen off and leave the phone still for four minutes.
   Note the start/end using another clock. Do not poll ADB, touch the phone or
   open diagnostics during that window.
4. At the end, briefly wake the screen and stop navigation while still unplugged.
   Record that time, then reconnect USB and save fresh copies of the same dumps
   plus the app log. Keep the startup, screen-on stop and reconnection outside
   the stationary analysis window.
5. Align BatteryStats history with each logged subscription suspension and
   `source=alarm`, `source=awake_timer` or `source=motion_trigger` recovery. Compare
   CPU-running and Android GPS activity within suspension windows. Compare app
   elapsed-minus-uptime before/after those windows to measure added deep sleep.
   Report the number and duration of usable windows; early motion/poor fixes or
   no actual suspension make the run inconclusive.
6. Repeat with fused disabled, changing only that app provider setting. Restore
   the original provider choice afterward. A repeated run in reverse order is
   needed before attributing a provider difference.

CPU/GPS flags clearing during suspension would establish device sleep/activity
cessation for those windows. Persistent flags would justify inspecting other
active requests and framework state; they would not prove a hardware fault or an
explicit ViBRo wake-lock leak. GPS activity is system accounting, not measured
GNSS energy. Phone/watch receipt and worst-case idle departure latency need the
separate route tests above. The legacy attempt below reached no qualifying
suspension window, so this check remains incomplete.

## Results to append

For each run: conditions, verified source/build state, route section, physical observations, relevant trace IDs, phone/watch timings, actual sleep/wake latency, valid power readings and limitations. Keep personal coordinates out of committed test resources. Recommend provider defaults or claimed savings only after these observations support them.

### 30 October recording

The user supplied a Huawei PRA-LX1 GPlay Debug route recording and confirmed it
matches branch `65de832d`. The log records three stationary suspensions, two
fallback checks, one hardware motion wake and continuing screen-off location
processing. Twelve correlated imminent alerts complete the app posting call in
21–26 ms. These observations are partial field evidence, not independent turn
crossings, phone/watch receipt times, worst-case wake latency or a controlled
provider/power comparison. All 13 charge-counter samples remain implausible.
See BRANCH_REVIEW_20261030.md for source attribution, counts and limitations.
The review's subsequent Round Trip correction still needs installation and its
own device check; the supplied recording precedes that correction.

A follow-up clock audit found no added CPU deep-sleep time across the 187 paired
elapsed/uptime readings during the recording (their difference varies by only
1 ms and ends unchanged). Acquisition suspension and the motion-trigger callback
were exercised, but deep-sleep wake latency remains untested. The first fallback
has an explicit recovery service action; the second may have used the awake timer.
This recording does not identify which process or device condition kept the CPU
awake. The follow-up findings in BRANCH_REVIEW_20261030.md also record rapid
notification replacement, late provider churn and the reroute-duration attribution.

The connected-phone read-only inspection subsequently recovered the morning
BatteryStats history. It confirms continuous CPU running during the route and
resumed sleep shortly after GPS activity ended. Explicit route-calculation wake
locks were brief (two acquisitions, about 1.178 seconds actual total, maximum
0.733 seconds); no continuous explicit ViBRo CPU lock was identified. System
location locks were attributed to the app, but the available aggregate history
does not identify the exact kernel suspend blocker. The currently USB-powered
phone also has stay-awake-while-plugged-in enabled. See PHONE_POWER_REVIEW_20261030.md
for evidence, attribution limits and the next unplugged screen-off test.

### 30 October, 12:32–12:45 stationary attempt

The user reported screen off at 12:32 and navigation ended at 12:45 (Vienna time).
Read-only capture after reconnection found:

- Unplugged/discharging history starts at approximately 12:32:46. Screen off is
  recorded at approximately 12:32:48.6, screen on at 12:45:18.2, GPS activity ends
  at 12:45:24.7 and USB reconnects at 12:45:43.6. History wall times have roughly
  one-second anchor precision. App stop records are 12:45:24.870 and 12:45:25.637.
- This is the older installed GPlay Debug package, last updated 29 October at
  17:51:41, with `sourceRevision=unknown`. The newly verified follow-up APK was
  not installed. This run cannot validate that follow-up or its new diagnostics.
- Fused is disabled (`fusedPreferred=false`). GPS and network are requested, but
  all 20 accepted observations are network fixes; none are GPS fixes. Median
  receipt spacing is 40,237 ms despite a 3-second request. Accuracy is roughly
  19.3–20.0 m. Stationary sleep records `network_only` 19 times and never arms
  subscription suspension. There are zero stationary fallback checks.
- Sixteen stream-stall recoveries restart acquisition. Recovery dispatch is
  9,289–9,598 ms after the requested deadline. The GPS activity flag clears for
  only 100–165 ms at those restarts, rather than for intentional sleep windows.
- All 20 elapsed/uptime pairs retain an offset of 7,003,295–7,003,296 ms, ending
  unchanged. Battery history has no CPU-running clear. Thus no added deep sleep
  is observed during the sampled interval; the requested suspension experiment
  is **inconclusive because acquisition never suspended**.

The battery percentage goes from 96 to 95; discharging charge counters remain
implausible, so no energy attribution is made. No settings, history or app state
were changed by the capture. Raw evidence and derived JSON remain ignored in
`.codex-temp/stationary-1232-*-20261030.*`; no personal coordinates are committed.
Next: use the verified build and obtain stable eligible GPS/fused fixes before
repeating the unplugged window. Lack of GPS fixes is not evidence of a hardware
fault; reception and provider behavior remain unestablished.

### Verified-build installation, 30 October at 12:51

With user authorization, installed the exact verified GPlay Debug APK in place
using `adb install -r`, preserving app data. Package `vibro.navigator.debug` now
logs `sourceRevision=a455ce9f-reliability-handoff-20261030`. Its APK SHA-256 matches
the latest STATUS.md artifact table. The main activity started successfully and
the process remained running, with no process-filtered crash-buffer records at
the smoke check. No navigation or power trial was started automatically.

For the repeat, obtain stable eligible GPS/fused fixes before starting the
unplugged, screen-off window. The previous network-only run cannot validate
stationary suspension. Keep power settings and battery history unchanged.
