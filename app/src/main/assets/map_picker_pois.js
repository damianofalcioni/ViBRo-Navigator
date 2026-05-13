(function () {
    "use strict";

    const TILE_SIZE = 256;
    const LAYER_ID = "poi-layer";
    const STYLE_ID = "poi-layer-style";
    const LABEL_MIN_ZOOM = 17;

    let pois = [];
    let layer = null;

    function clamp(value, min, max) {
        return Math.min(max, Math.max(min, value));
    }

    function project(lat, lon, zoom) {
        const boundedLat = clamp(lat, -85.05112878, 85.05112878);
        const sinLat = Math.sin(boundedLat * Math.PI / 180);
        const scale = TILE_SIZE * Math.pow(2, zoom);
        return {
            x: (lon + 180) / 360 * scale,
            y: (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * scale
        };
    }

    function currentState() {
        if (!window.mapPicker || !window.mapPicker.getState) {
            return null;
        }
        return window.mapPicker.getState();
    }

    function injectStyle() {
        if (document.getElementById(STYLE_ID)) {
            return;
        }
        const style = document.createElement("style");
        style.id = STYLE_ID;
        style.textContent = [
            "#poi-layer{position:absolute;inset:0;z-index:1;pointer-events:none;}",
            ".poi-marker{position:absolute;width:34px;height:40px;margin:-34px 0 0 -17px;",
            "border:0;background:transparent;padding:0;pointer-events:auto;touch-action:none;",
            "user-select:none;will-change:transform;}",
            ".poi-marker::before{content:\"\";position:absolute;left:7px;top:1px;width:18px;height:18px;",
            "border:2px solid #fff;border-radius:50% 50% 50% 0;background:#151b22;",
            "box-shadow:0 2px 7px rgba(0,0,0,.5);transform:rotate(-45deg);}",
            ".poi-marker::after{content:\"\";position:absolute;left:14px;top:8px;width:6px;height:6px;",
            "border-radius:50%;background:#32d074;}",
            ".poi-label{display:none;position:absolute;left:27px;top:3px;max-width:180px;padding:4px 7px;",
            "border-radius:7px;background:rgba(0,0,0,.78);color:#fff;font:12px/1.2 sans-serif;",
            "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;box-shadow:0 1px 5px rgba(0,0,0,.45);}",
            ".poi-marker.show-label .poi-label{display:block;}"
        ].join("");
        document.head.appendChild(style);
    }

    function ensureLayer() {
        if (layer) {
            return layer;
        }
        injectStyle();
        layer = document.getElementById(LAYER_ID);
        if (!layer) {
            layer = document.createElement("div");
            layer.id = LAYER_ID;
            const overlay = document.getElementById("overlay");
            if (overlay) {
                overlay.appendChild(layer);
            }
        }
        return layer;
    }

    function labelFor(poi) {
        if (poi.name) {
            return poi.name;
        }
        if (poi.categoryLabel) {
            return poi.categoryLabel;
        }
        return "POI";
    }

    function markerFor(poi, showLabel) {
        const marker = document.createElement("button");
        marker.type = "button";
        marker.className = showLabel && poi.name ? "poi-marker show-label" : "poi-marker";
        marker.title = labelFor(poi);
        marker.setAttribute("aria-label", labelFor(poi));
        const label = document.createElement("span");
        label.className = "poi-label";
        label.textContent = labelFor(poi);
        marker.appendChild(label);
        marker.addEventListener("pointerdown", function (event) {
            event.stopPropagation();
        });
        marker.addEventListener("click", function (event) {
            event.stopPropagation();
            if (window.mapPicker && window.mapPicker.select) {
                window.mapPicker.select(poi.lat, poi.lon);
            }
            if (window.AndroidBridge && window.AndroidBridge.onPoiSelected) {
                window.AndroidBridge.onPoiSelected(labelFor(poi), poi.lat, poi.lon);
            }
        });
        return marker;
    }

    function render() {
        const state = currentState();
        const targetLayer = ensureLayer();
        if (!state || !targetLayer) {
            return;
        }

        const center = project(state.centerLat, state.centerLon, state.zoom);
        const topLeftX = center.x - state.width / 2;
        const topLeftY = center.y - state.height / 2;
        const showLabels = state.zoom >= LABEL_MIN_ZOOM;
        targetLayer.textContent = "";
        pois.forEach(function (poi) {
            if (typeof poi.lat !== "number" || typeof poi.lon !== "number") {
                return;
            }
            const point = project(poi.lat, poi.lon, state.zoom);
            const marker = markerFor(poi, showLabels);
            marker.style.transform = "translate3d("
                + (point.x - topLeftX) + "px,"
                + (point.y - topLeftY) + "px,0)";
            targetLayer.appendChild(marker);
        });
    }

    window.mapPickerPoiLayer = {
        setPois: function (nextPois) {
            pois = Array.isArray(nextPois) ? nextPois : [];
            render();
        },
        clear: function () {
            pois = [];
            if (layer) {
                layer.textContent = "";
            }
        }
    };

    window.addEventListener("mapPickerViewChanged", render);
})();
