(function () {
  'use strict';

  var root = document.documentElement;
  var CONSENT_COOKIE = 'vibro_cookie_consent',
    THEME_COOKIE = 'vibro_theme_override',
    COOKIE_MAX_AGE = 60 * 60 * 24 * 365,
    THEME_CLEANUP_MS = 1000;
  var themeCleanupTimer = 0,
    sessionTheme = null,
    themeControl = null,
    screenshotThemeWord = null;
  var systemThemeMedia = mediaQuery('(prefers-color-scheme: light)'),
    reducedMotionMedia = mediaQuery('(prefers-reduced-motion: reduce)') || { matches: false };

  /* Preference storage: the optional theme cookie requires accepted consent. */
  function readCookie(name) {
    var prefix = name + '=';
    var parts = document.cookie.split(';');
    for (var i = 0; i < parts.length; i += 1) {
      var item = parts[i].trim();
      if (item.indexOf(prefix) !== 0) continue;
      try {
        return decodeURIComponent(item.slice(prefix.length));
      } catch (_error) {
        return null;
      }
    }
    return null;
  }

  function writeCookie(name, value) {
    var secure = location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = name + '=' + encodeURIComponent(value) + '; Path=/; Max-Age=' + COOKIE_MAX_AGE + '; SameSite=Lax' + secure;
  }

  function deleteCookie(name) {
    var secure = location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = name + '=; Path=/; Max-Age=0; SameSite=Lax' + secure;
  }

  function consent() {
    var value = readCookie(CONSENT_COOKIE);
    return value === 'all' || value === 'rejected' ? value : null;
  }

  function savedTheme() {
    if (consent() !== 'all') return null;
    var saved = readCookie(THEME_COOKIE);
    return saved === 'light' || saved === 'dark' ? saved : null;
  }

  /* Older mobile browsers expose addListener rather than addEventListener. */
  function mediaQuery(query) {
    return window.matchMedia ? window.matchMedia(query) : null;
  }

  function listenToMedia(media, handler) {
    if (!media) return false;
    if (media.addEventListener) {
      media.addEventListener('change', handler);
    } else if (media.addListener) {
      media.addListener(handler);
    } else {
      return false;
    }
    return true;
  }

  /* Theme state stays on <html>; only a manual change enables interpolation. */
  function systemTheme() {
    return systemThemeMedia && systemThemeMedia.matches ? 'light' : 'dark';
  }

  function effectiveTheme() {
    return root.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
  }

  function renderThemeUI() {
    var dark = effectiveTheme() === 'dark';
    if (themeControl) themeControl.setAttribute('aria-pressed', String(dark));
    if (screenshotThemeWord) screenshotThemeWord.textContent = dark ? 'light' : 'dark';
  }

  function clearThemeTransition() {
    root.classList.remove('transition');
    themeCleanupTimer = 0;
  }

  function transitionTo(theme) {
    var reduceMotion = reducedMotionMedia.matches;
    window.clearTimeout(themeCleanupTimer);
    if (!reduceMotion) root.classList.add('transition');

    /* Moonwalk changes theme state immediately, without a layout/style flush. */
    root.setAttribute('data-theme', theme);
    sessionTheme = theme;
    renderThemeUI();
    if (consent() === 'all') writeCookie(THEME_COOKIE, theme);

    if (reduceMotion) {
      clearThemeTransition();
    } else {
      themeCleanupTimer = window.setTimeout(clearThemeTransition, THEME_CLEANUP_MS);
    }
  }

  function toggleTheme() {
    transitionTo(effectiveTheme() === 'dark' ? 'light' : 'dark');
  }

  function followSystemTheme() {
    if (sessionTheme) return;
    root.setAttribute('data-theme', systemTheme());
    renderThemeUI();
  }

  function initThemeControls() {
    themeControl = document.querySelector('.theme-switch');
    screenshotThemeWord = document.getElementById('screenshot-theme-word');
    renderThemeUI();
    if (themeControl) themeControl.addEventListener('click', toggleTheme);
    listenToMedia(systemThemeMedia, followSystemTheme);
  }

  /* Consent UI is independent of theme controls and only exists when needed. */
  function initCookieConsent() {
    if (consent()) return;
    var panel = document.createElement('aside');
    panel.className = 'cookie-consent';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Cookie preferences');
    panel.innerHTML = '<div class="cookie-copy">' +
      '<strong>Cookie preferences</strong>' +
      '<p>ViBRo uses a preference cookie only when you accept all, so your manual light/dark choice can follow you across pages and future visits. A necessary consent cookie remembers this choice. No analytics or advertising cookies are used.</p>' +
      '</div>' +
      '<div class="cookie-actions">' +
      '<button class="cookie-button reject" type="button">Reject all</button>' +
      '<button class="cookie-button accept" type="button">Accept all</button>' +
      '</div>';

    var siteRoot = document.querySelector('.site-root') || document.body;
    siteRoot.appendChild(panel);

    panel.querySelector('.accept').addEventListener('click', function () {
      writeCookie(CONSENT_COOKIE, 'all');
      writeCookie(THEME_COOKIE, effectiveTheme());
      panel.classList.remove('is-visible');
    });

    panel.querySelector('.reject').addEventListener('click', function () {
      writeCookie(CONSENT_COOKIE, 'rejected');
      deleteCookie(THEME_COOKIE);
      panel.classList.remove('is-visible');
    });

    panel.classList.add('is-visible');
  }

  /* Homepage navigation: one writer keeps ARIA and visible state in sync. */
  function initNavigation() {
    var toggle = document.querySelector('.menu-toggle');
    var nav = document.getElementById('primary-navigation');
    if (!toggle || !nav) return;
    var desktopMedia = mediaQuery('(min-width: 701px)');

    function menuIsOpen() {
      return toggle.getAttribute('aria-expanded') === 'true';
    }

    function setMenuOpen(open) {
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
      toggle.setAttribute('aria-label', open ? 'Close navigation menu' : 'Open navigation menu');
      if (open) nav.classList.add('is-open');
      else nav.classList.remove('is-open');
    }

    function closeOnLink(event) {
      /* Walk parents so links with nested content work without Element.closest. */
      for (var target = event.target; target && target !== nav; target = target.parentNode) {
        if (target.nodeName === 'A') {
          setMenuOpen(false);
          return;
        }
      }
    }

    function closeOnEscape(event) {
      if ((event.key === 'Escape' || event.keyCode === 27) && menuIsOpen()) {
        setMenuOpen(false);
        toggle.focus();
      }
    }

    function closeOnOutsideClick(event) {
      if (menuIsOpen() && !nav.contains(event.target) && !toggle.contains(event.target)) {
        setMenuOpen(false);
      }
    }

    function closeOnDesktop() {
      var desktop = desktopMedia ? desktopMedia.matches : window.innerWidth > 700;
      if (desktop) setMenuOpen(false);
    }

    toggle.addEventListener('click', function () { setMenuOpen(!menuIsOpen()); });
    nav.addEventListener('click', closeOnLink);
    document.addEventListener('keydown', closeOnEscape);
    document.addEventListener('click', closeOnOutsideClick);
    /* Modern/legacy media listeners fire only at the breakpoint, not every resize. */
    if (!listenToMedia(desktopMedia, closeOnDesktop)) {
      window.addEventListener('resize', closeOnDesktop);
    }
  }

  function initSite() {
    document.removeEventListener('DOMContentLoaded', initSite);
    initThemeControls();
    initCookieConsent();
    initNavigation();
  }

  function startSite() {
    /* Restore synchronously from <head>, then bind controls at DOM readiness. */
    sessionTheme = savedTheme();
    root.setAttribute('data-theme', sessionTheme || systemTheme());
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initSite);
    } else {
      initSite();
    }
  }

  startSite();
}());
