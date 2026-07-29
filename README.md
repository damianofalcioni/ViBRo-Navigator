# ViBRo Navigator

<p align="center">
  <img src="./fastlane/metadata/android/en-US/images/icon.png" alt="Logo" width="100"/>
</p>

**ViBRo Navigator** (**Vi**brating/**Vi**be-coded **BRo**uter **Navigator**) is a lightweight, offline-first, and battery-efficient Android GPS navigation app built on top of BRouter. It is designed for **map-free navigation**, delivering directions exclusively through **vibrations and minimal on-screen guidance**, enabling distraction-free and screen-off usage.

---

<table align="center" cellspacing="0" cellpadding="0" border="0">
  <tr>
    <td align="center" width="220">
      <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/01-main-ui.png" alt="Main UI" width="200"/>
    </td>
    <td align="center" width="220">
      <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/02-poi-selection-map.png" alt="POI Selection on Map" width="200"/>
    </td>
    <td align="center" width="220">
      <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/03-navigation-ui.png" alt="Navigation UI" width="200"/>
    </td>
  </tr>
</table>

> [!NOTE]
> **The project is entirely created by AI. No code has been written nor reviewed by humans. Despite that...**

## ❓ Why

I needed an offline GPS navigation for my Android 8 phone with degraded battery and little storage space left, as my preferred app dropped support, so, here we are...yes, I know, but I like to push technology to the limit and avoid waste...a dangerous combination.

## ✨ Key Features

* **Vibration-based navigation**
  Clear directional feedback without relying on visual maps. Navigation directions are provided as notifications with different vibration patterns. Best in combination with a smartwatch or smartband that show them.

* **Minimal speech direction instructions**
  Optional spoken direction instructions keep audio guidance concise and distraction-free.

* **Offline-first routing**
  Uses BRouter for fully offline route calculation and turn instruction generation. An internet connection is required only when searching for a destination.

* **BRouter profile selection**
  Choose any available BRouter routing profile, including custom profiles.

* **Straight-line guidance without BRouter**
  The `Straight-line mode` keeps compass and arrival guidance available even when BRouter is not installed.

* **Round trip mode**
  Plan circular BRouter routes from your current location using a target distance and direction.
  
* **Minimal & dependency-light**
  Built in pure Java with minimal external dependencies. APK size is ~400Kb for the F-Droid version and ~900Kb for the GPlay version !!

* **Smart POI search**

  * History-first suggestions
  * OpenStreetMap search
  * Google Maps redirect search
  * Speech input for POI search
  * POI over OpenStreetMap map selection
  * Open from other maps application

* **Map-free compass navigation**
  Visualizes route direction relative to your position, including surrounding streets extracted from downloaded BRouter segments, without rendering a full map.

* **GPX export**
  Export the planned route, passed route, and collected GPS fixes.

* **Intermediate stops support**
  Easily add, edit, and remove waypoints.

* **Pause and blocked road support**
  Navigation can be paused and rerouted to avoid a blocked street.

* **Dynamic GPS acquisition interval**
  Adjusts GPS fix acquisition timing based on the distance to the next direction point to reduce battery usage.

* **Background navigation**
  Works with screen off via a foreground service.
  
* **GPlay only features**

  * Google Fused Location provider
  * Google Maps Geocoding for POI search
  * Android Auto integration

---

## 💡 Innovations

* **Dynamic GPS fix acquisition**  
  The GPS fix acquisition interval dynamically adapts to the remaining distance to the next direction point, reducing battery usage when frequent fixes are not needed.

* **Time-based distance to the next direction**  
  Notifications are triggered from the estimated time to the next turn instruction, based on current speed. Single instruction mode can reduce maneuver alerts to one notification about 10 seconds before the turn.

* **Streets rendered directly from BRouter segments**  
  No additional OSM data download is required. Nearby streets are extracted directly from the already downloaded BRouter segments.

* **Smartband-friendly turn notifications**  
  Uses non-permanent notifications and simple Unicode characters to render correctly on any smartband.

* **3 easy-to-recognize vibration patterns for turn notification categories**  
  Right turns, left turns, and other instructions can be quickly identified, even with the phone in your pocket.
  
* **Initial turn orientation**  
  At the start of the route, a notification tells you how much to turn to face the route direction.

---

## 🧭 How It Works

1. Select a working mode between Route, Round Trip, and Straight Line.
2. Select any available **routing profile** from BRouter, including custom profiles (not available in Straight Line mode).
3. Enter a **destination** or pick from history/map (not available in Round Trip mode).
4. Optionally add **intermediate stops** (not available in Round Trip mode).
5. Start navigation:

   * In Route and Round Trip mode, the app calculates the route via BRouter.
   * In Straight Line mode, the compass points to the next stop or destination and arrival distance/ETA use straight-line legs.
   * Guidance is delivered through:

     * Vibrations
     * Minimal turn instructions text
     * Turn instructions notifications (smartband-friendly)

ViBRo-Navigator prioritizes **high-confidence guidance**—when accuracy is low, it delays instructions instead of risking incorrect directions.

---

## 🔋 Design Principles

* **Battery conscious**: optimized location polling and no persistent wake locks
* **Offline capable**: routing works without internet
* **Minimal UI**: distraction-free experience
* **Robustness-first**: avoids misleading instructions under uncertainty
* **Orientation-safe**: supports portrait and landscape layouts

---

## 📦 Requirements

* Android device (min SDK 23, Android 6.0)
* BRouter installed for routed turn guidance; straight-line guidance works without BRouter

---

## ⚙️ Developer Notes

* Pure Java implementation
* Minimal architecture with focused components
* Navigation logic split into small, testable modules
* Check [`AGENT.md`](./AGENT.md) and [`SPECIFICATION.md`](./SPECIFICATION.md) for more details

---

## 📄 License

MIT

---

## 🚀 Vision

ViBRo Navigator rethinks navigation:
**no maps, no noise — just reliable, intuitive guidance you can feel.**
