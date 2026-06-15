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

## ✨ Key Features

* **Vibration-based navigation**
  Clear directional feedback without relying on visual maps. Navigation directions are provided as notifications with different vibration patterns. Best in combination with a smartwatch or smartband that show them.

* **Offline-first routing**
  Uses BRouter for fully offline route calculation and turn instruction generation.

* **Straight-line guidance without BRouter**
  The `Straight Line - No BRouter` profile keeps compass and arrival guidance available even when BRouter is not installed.

* **Minimal & dependency-light**
  Built in pure Java with minimal external dependencies. APK size is ~250Kb for the F-Droid version and ~650Kb for the GPlay version !!

* **Smart POI search**

  * History-first suggestions
  * OpenStreetMap search
  * POI over OpenStreetMap map selection
  * Open from other maps application

* **Map-free compass navigation**
  Visualizes route direction relative to your position without rendering a map.

* **Intermediate stops support**
  Easily add, edit, and remove waypoints.

* **Dynamic navigation updates**
  Adaptive polling, rerouting, and turn estimation based on real-time conditions.

* **Pause and blocked road support**
  Navigation can be paused and rerouted to avoid a blocked street.

* **Background navigation**
  Works with screen off via a foreground service.
  
* **GPlay only features**

  * Google Fused Location provider
  * Google Maps Geocoding for POI search
  * Android Auto integration

---

## 🧭 How It Works

1. Select a **routing profile** from BRouter, or `Straight Line - No BRouter` for direct compass guidance.
2. Enter a **destination** (or pick from history/map).
3. Optionally add **intermediate stops**.
4. Start navigation:

   * With a BRouter profile, the app calculates the route via BRouter.
   * With the straight-line profile, the compass points to the next stop or destination and arrival distance/ETA use straight-line legs.
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

* Android device (min SDK 21)
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
