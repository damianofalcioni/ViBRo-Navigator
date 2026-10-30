# Phone/watch and power validation

Prepared 27 October 2026; updated 29 October 2026. A Huawei PRA-LX1 (Android API 26) received an earlier GPlay Debug build and passed an application-startup smoke check. That installation predates the review corrections. No device was attached for the latest review; the route, phone/watch and power checks below remain pending.

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

## Locate notification delay

Use traceId to connect the recorded observation, motion assessment, guidance decision and notification-post completion. Compare measurement age, receipt elapsed time and uptime, generated events and posting duration. `deliveryReceipt=unobserved` means Android/watch presentation remains unmeasured.

Record whether a delay was visible on the phone, watch or both. Capture a system bugreport close to a reproduced incident, with user authorization for its personal diagnostic contents. System notification/location/power diagnostics may help attribute timing; app notify completion alone cannot prove forwarding or vibration receipt.

## Stationary sleep and recovery

On a device with a supported significant-motion wake-up trigger, observe actual sleep/confirmation, motion wake and fallback-check records. Compare requested deadlines with actual dispatch; test walking away during a screen-off stop. Repeat with the sensor unavailable or registration/alarm failure simulated by focused tests. Active acquisition must remain the fallback when prerequisites fail.

Do not assume a 30-second inexact alarm will dispatch on time in idle. If measured departure delay is unacceptable, retain active acquisition for the affected configuration while investigating. Report maximum/typical observed latency and any missed departure, rather than claiming guaranteed wake-up.

## Provider/power comparison

Repeat comparable trips in alternating order: legacy first/fused second, then fused first/legacy second, with several repetitions where practical. Keep starting battery conditions, phone temperature, screen state, route, carrying position, watch forwarding and logging comparable. Compare corresponding route sections and report variability.

Measure independently observed prompt quality, departure latency, fix age/uncertainty, filter displacement, requested subscription exposure and suitable device power/GNSS/CPU-wake evidence. Provider uncertainty and agreement with the planned route are not ground-truth error. A short simultaneous-provider comparison may characterize disagreement, but must be separate from single-mode power attribution.

Use consumption totals only when counters have suitable scale, discharge direction and charging/reset history. Raw vendor current units remain unvalidated. A percentage plateau is allowed. If counters remain unsuitable, report energy unresolved or use a suitable independent measurement setup that does not silently change idle/charging behavior.

## Results to append

For each run: conditions, verified source/build state, route section, physical observations, relevant trace IDs, phone/watch timings, actual sleep/wake latency, valid power readings and limitations. Keep personal coordinates out of committed test resources. Recommend provider defaults or claimed savings only after these observations support them.
