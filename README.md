# ViBRo Navigator

**ViBRo Navigator** is a lightweight, battery-efficient Android navigation app built on top of BRouter. It is designed for **map-free navigation**, delivering directions exclusively through **vibrations and minimal on-screen guidance**, enabling distraction-free and screen-off usage.

---

## ✨ Key Features

* **Vibration-based navigation**
  Clear directional feedback without relying on visual maps.

* **Offline-first routing**
  Uses BRouter for fully offline route calculation.

* **Minimal & dependency-light**
  Built in pure Java with minimal external dependencies.

* **Smart POI search**

  * History-first suggestions
  * OpenStreetMap search
  * Open from other maps application

* **Map-free compass navigation**
  Visualizes route direction relative to your position without rendering a map.

* **Intermediate stops support**
  Easily add, edit, and remove waypoints.

* **Dynamic navigation updates**
  Adaptive polling, rerouting, and turn estimation based on real-time conditions.

* **Background navigation**
  Works with screen off via a foreground service.

---

## 🧭 How It Works

1. Select a **routing profile** (from BRouter).
2. Enter a **destination** (or pick from history/map).
3. Optionally add **intermediate stops**.
4. Start navigation:

   * The app calculates the route via BRouter.
   * Guidance is delivered through:

     * Vibrations
     * Minimal text directions
     * Notifications (smartband-friendly)

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
* BRouter installed (required for routing)

---

## ⚙️ Developer Notes

* Pure Java implementation
* Minimal architecture with focused components
* Navigation logic split into small, testable modules
* Logging and diagnostics available via hidden developer mode (5 rapid taps in the about page)
* Google Maps API available when `GOOGLE_MAPS_API_KEY` is defined in `local.properties` or as an environment variable.

---

## 📄 License

MIT

---

## 🚀 Vision

ViBRo Navigator rethinks navigation:
**no maps, no noise — just reliable, intuitive guidance you can feel.**
