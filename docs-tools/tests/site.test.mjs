import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';
import vm from 'node:vm';
import test, { describe } from 'node:test';

for (const scriptName of ['index.js', 'index.min.js']) {
  const sharedScript = await readFile(new URL('../../docs/' + scriptName, import.meta.url), 'utf8');
  describe(scriptName, () => {

    function element() {
      const attributes = new Map();
      const listeners = new Map();
      const classes = new Set();
      return {
        attributes, listeners, classes, focusCount: 0,
        classList: { add: value => classes.add(value), remove: value => classes.delete(value) },
        getAttribute: key => attributes.get(key) ?? null,
        setAttribute: (key, value) => attributes.set(key, value),
        addEventListener(type, handler, options) { listeners.set(type, { handler, options }); },
        removeEventListener(type, handler) {
          if (listeners.get(type)?.handler === handler) listeners.delete(type);
        },
        fire(type, event = {}) {
          const listener = listeners.get(type);
          if (listener?.options?.once) listeners.delete(type);
          listener?.handler(event);
        },
        focus() { this.focusCount += 1; },
        contains(target) { return target === this; }
      };
    }

    function themePage({
      cookie = '', readyState = 'loading', light = false, reducedMotion = false,
      mediaAPI = 'modern', navigation = false, themeControls = true, protocol = 'https:'
    } = {}) {
      const root = element();
      const control = element();
      const word = element();
      const document = element();
      const window = element();
      const media = element();
      media.matches = light;
      const desktopMedia = element();
      desktopMedia.matches = false;
      const toggle = element();
      toggle.setAttribute('aria-expanded', 'false');
      const nav = element();
      nav.nodeName = 'DIV';
      for (const query of [media, desktopMedia]) {
        if (mediaAPI !== 'modern') {
          if (mediaAPI === 'legacy') query.addListener = handler => query.listeners.set('change', { handler });
          delete query.addEventListener;
        }
      }
      const panels = [];
      const writes = [];
      const cookies = new Map(cookie.split(';').filter(Boolean).map(value => {
        const index = value.indexOf('=');
        return [value.slice(0, index).trim(), value.slice(index + 1)];
      }));
      Object.defineProperty(document, 'cookie', {
        get: () => [...cookies].map(([key, value]) => `${key}=${value}`).join('; '),
        set: value => {
          writes.push(value);
          const pair = value.split(';')[0];
          const index = pair.indexOf('=');
          const key = pair.slice(0, index);
          if (value.includes('Max-Age=0;')) cookies.delete(key);
          else cookies.set(key, pair.slice(index + 1));
        }
      });
      document.documentElement = root;
      document.readyState = readyState;
      document.getElementById = id => {
        if (id === 'screenshot-theme-word') return themeControls ? word : null;
        if (id === 'primary-navigation') return navigation ? nav : null;
        return null;
      };
      document.querySelector = selector => {
        if (selector === '.theme-switch') return themeControls ? control : null;
        if (selector === '.menu-toggle') return navigation ? toggle : null;
        if (selector === '.site-root') return { appendChild: panel => panels.push(panel) };
        return null;
      };
      document.createElement = () => {
        const panel = element();
        panel.accept = element();
        panel.reject = element();
        panel.querySelector = selector => selector === '.accept' ? panel.accept : panel.reject;
        return panel;
      };
      if (mediaAPI !== 'absent') {
        window.matchMedia = query => {
          if (query.includes('reduced-motion')) return { matches: reducedMotion };
          if (query.includes('min-width')) return desktopMedia;
          return media;
        };
      }
      const timers = new Map();
      let nextTimer = 1;
      window.cleanup = null;
      window.setTimeout = (handler, delay) => {
        const id = nextTimer++;
        window.cleanup = { handler, delay, id };
        timers.set(id, window.cleanup);
        return id;
      };
      window.clearTimeout = id => {
        timers.delete(id);
        if (window.cleanup?.id === id) window.cleanup = null;
      };
      vm.runInNewContext(sharedScript, { document, window, location: { protocol } });
      return { root, control, word, document, window, media, desktopMedia, toggle, nav, panels, writes, cookies, timers };
    }

    test('theme restores before body paint; controls are ready without waiting for images', () => {
      const page = themePage();
      assert.equal(page.root.getAttribute('data-theme'), 'dark');
      assert.equal(page.panels.length, 0);
      assert.equal(page.window.listeners.has('load'), false);
      page.document.fire('DOMContentLoaded');
      assert.equal(page.panels.length, 1);
      assert.equal(page.control.getAttribute('aria-pressed'), 'true');
      assert.equal(page.word.textContent, 'light');
      page.control.fire('click');
      assert.equal(page.root.getAttribute('data-theme'), 'light');
      assert.equal(page.control.getAttribute('aria-pressed'), 'false');
      assert.equal(page.word.textContent, 'dark');
      assert.equal(page.root.classes.has('transition'), true);
      assert.equal(page.window.cleanup.delay, 1000);
      assert.equal(page.cookies.has('vibro_theme_override'), false);
      page.window.cleanup.handler();
      assert.equal(page.root.classes.has('transition'), false);
      page.document.fire('DOMContentLoaded');
      assert.equal(page.panels.length, 1);
    });

    test('malformed and unknown consent values show a usable consent panel', () => {
      for (const cookie of ['vibro_cookie_consent=%', 'vibro_cookie_consent=unknown']) {
        const page = themePage({ cookie, readyState: 'interactive' });
        assert.equal(page.panels[0].classes.has('is-visible'), true);
        page.control.fire('click');
        assert.equal(page.root.getAttribute('data-theme'), 'light');
        assert.equal(page.cookies.has('vibro_theme_override'), false);
      }
    });

    test('malformed saved theme falls back safely; valid preferences restore synchronously', () => {
      const invalid = themePage({ cookie: 'vibro_cookie_consent=all; vibro_theme_override=%', readyState: 'complete' });
      assert.equal(invalid.root.getAttribute('data-theme'), 'dark');
      invalid.control.fire('click');
      assert.equal(invalid.cookies.get('vibro_theme_override'), 'light');
      const valid = themePage({ cookie: 'vibro_cookie_consent=all; vibro_theme_override=light' });
      assert.equal(valid.root.getAttribute('data-theme'), 'light');
      assert.equal(valid.panels.length, 0);
    });

    test('accept persists the effective theme with the required HTTPS cookie attributes', () => {
      const page = themePage({ readyState: 'interactive' });
      page.control.fire('click');
      page.panels[0].accept.fire('click');
      assert.equal(page.cookies.get('vibro_cookie_consent'), 'all');
      assert.equal(page.cookies.get('vibro_theme_override'), 'light');
      assert.equal(page.panels[0].classes.has('is-visible'), false);
      for (const write of page.writes) assert.match(write, /; Path=\/; Max-Age=31536000; SameSite=Lax; Secure$/);
    });

    test('reject removes the saved preference and keeps switching available for the page', () => {
      const page = themePage({ cookie: 'vibro_theme_override=light', readyState: 'interactive' });
      page.panels[0].reject.fire('click');
      assert.equal(page.cookies.get('vibro_cookie_consent'), 'rejected');
      assert.equal(page.cookies.has('vibro_theme_override'), false);
      page.control.fire('click');
      assert.equal(page.root.getAttribute('data-theme'), 'light');
      assert.equal(page.cookies.has('vibro_theme_override'), false);
    });

    test('system theme follows changes until manually overridden', () => {
      const page = themePage({ readyState: 'interactive' });
      page.media.matches = true;
      page.media.fire('change');
      assert.equal(page.root.getAttribute('data-theme'), 'light');
      page.control.fire('click');
      page.media.fire('change');
      assert.equal(page.root.getAttribute('data-theme'), 'dark');
    });

    test('reduced-motion switching is immediate without a transition timer', () => {
      const page = themePage({ readyState: 'interactive', reducedMotion: true });
      page.control.fire('click');
      assert.equal(page.root.getAttribute('data-theme'), 'light');
      assert.equal(page.root.classes.has('transition'), false);
      assert.equal(page.window.cleanup, null);
    });

    test('rapid theme switches cancel the previous cleanup and retain synchronized UI', () => {
      const page = themePage({ readyState: 'complete' });
      page.control.fire('click');
      const previousTimer = page.window.cleanup.id;
      page.control.fire('click');
      assert.equal(page.timers.has(previousTimer), false);
      assert.equal(page.timers.size, 1);
      assert.equal(page.root.getAttribute('data-theme'), 'dark');
      assert.equal(page.control.getAttribute('aria-pressed'), 'true');
      assert.equal(page.word.textContent, 'light');
      assert.equal(page.root.classes.has('transition'), true);
      page.window.cleanup.handler();
      assert.equal(page.root.classes.has('transition'), false);
    });

    test('shared navigation initializes once after DOM readiness', () => {
      const page = themePage({ navigation: true });
      assert.equal(page.toggle.listeners.has('click'), false);
      page.document.fire('DOMContentLoaded');
      assert.equal(page.document.listeners.has('DOMContentLoaded'), false);
      page.toggle.fire('click');
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'true');
      assert.equal(page.nav.classes.has('is-open'), true);
      assert.equal(page.toggle.getAttribute('aria-label'), 'Close navigation menu');
      page.document.fire('DOMContentLoaded');
      assert.equal(page.panels.length, 1);
    });

    function menuPage(options = {}) {
      return themePage({ readyState: 'interactive', navigation: true, ...options });
    }

    test('Escape preserves focus when closed and restores it only after closing an open menu', () => {
      const page = menuPage();
      page.document.fire('keydown', { key: 'Escape' });
      assert.equal(page.toggle.focusCount, 0);
      page.toggle.fire('click');
      assert.equal(page.nav.classes.has('is-open'), true);
      page.document.fire('keydown', { key: 'Escape' });
      assert.equal(page.toggle.focusCount, 1);
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      assert.equal(page.nav.classes.has('is-open'), false);
    });

    test('menu still closes on link selection, outside clicks and desktop resizing', () => {
      const page = menuPage();
      page.toggle.fire('click');
      page.nav.fire('click', { target: { nodeName: 'A', parentNode: page.nav } });
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      page.toggle.fire('click');
      page.document.fire('click', { target: page.toggle });
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'true');
      page.document.fire('click', { target: {} });
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      page.toggle.fire('click');
      assert.equal(page.window.listeners.has('resize'), false);
      page.desktopMedia.matches = true;
      page.desktopMedia.fire('change');
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
    });

    test('legacy media listeners follow system theme and close the menu at the desktop breakpoint', () => {
      const page = menuPage({ mediaAPI: 'legacy' });
      page.media.matches = true;
      page.media.fire('change');
      assert.equal(page.root.getAttribute('data-theme'), 'light');
      page.control.fire('click');
      page.media.fire('change');
      assert.equal(page.root.getAttribute('data-theme'), 'dark');
      page.toggle.fire('click');
      page.desktopMedia.matches = true;
      page.desktopMedia.fire('change');
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      assert.equal(page.window.listeners.has('resize'), false);
    });

    test('missing media APIs retain switching and resize fallback at the exact breakpoint', () => {
      for (const mediaAPI of ['absent', 'no-listeners']) {
        const page = menuPage({ mediaAPI });
        page.control.fire('click');
        assert.equal(page.root.getAttribute('data-theme'), 'light');
        page.toggle.fire('click');
        page.window.innerWidth = 700;
        page.window.fire('resize');
        assert.equal(page.toggle.getAttribute('aria-expanded'), 'true');
        page.window.innerWidth = 701;
        page.desktopMedia.matches = true;
        page.window.fire('resize');
        assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      }
    });

    test('nested link content and legacy Escape events close the menu without closest()', () => {
      const page = menuPage();
      page.toggle.fire('click');
      page.nav.fire('click', { target: { nodeName: 'SPAN', parentNode: page.nav } });
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'true');
      const link = { nodeName: 'A', parentNode: page.nav };
      page.nav.fire('click', { target: { nodeName: 'SPAN', parentNode: link } });
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      page.toggle.fire('click');
      page.document.fire('keydown', { keyCode: 27 });
      assert.equal(page.toggle.getAttribute('aria-expanded'), 'false');
      assert.equal(page.toggle.focusCount, 1);
    });

    test('document pages initialize consent and theme without homepage controls', () => {
      const page = themePage({ readyState: 'complete', themeControls: false });
      assert.equal(page.panels.length, 1);
      assert.equal(page.document.listeners.has('keydown'), false);
      page.media.matches = true;
      page.media.fire('change');
      assert.equal(page.root.getAttribute('data-theme'), 'light');
      page.panels[0].accept.fire('click');
      assert.equal(page.cookies.get('vibro_theme_override'), 'light');
    });

    test('remembered consent avoids creating an unused panel or handlers', () => {
      for (const value of ['all', 'rejected']) {
        const page = themePage({ cookie: `vibro_cookie_consent=${value}`, readyState: 'complete' });
        assert.equal(page.panels.length, 0);
        page.control.fire('click');
        assert.equal(page.cookies.has('vibro_theme_override'), value === 'all');
      }
    });

    test('HTTP preference cookies omit Secure while keeping their other attributes', () => {
      const page = themePage({ readyState: 'complete', protocol: 'http:' });
      page.panels[0].accept.fire('click');
      for (const write of page.writes) {
        assert.match(write, /; Path=\/; Max-Age=31536000; SameSite=Lax$/);
        assert.equal(write.includes('; Secure'), false);
      }
    });
  });
}

async function htmlPages(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const result = [];
  for (const entry of entries) {
    const url = new URL(entry.name + (entry.isDirectory() ? '/' : ''), directory);
    if (entry.isDirectory()) result.push(...await htmlPages(url));
    else if (entry.name.endsWith('.html')) result.push(url);
  }
  return result;
}

test('all public pages share the same explicit CSS and JS cache versions', async () => {
  const versions = { css: new Set(), js: new Set() };
  const pages = await htmlPages(new URL('../../docs/', import.meta.url));
  assert.equal(pages.length, 6);
  for (const url of pages) {
    const html = await readFile(url, 'utf8');
    assert.doesNotMatch(html, /<script\b[^>]*\bsrc="(?:\.\.\/)?index\.js(?:\?|")/);
    for (const [type, pattern] of [
      ['css', /href="(?:\.\.\/)?styles\.min\.css\?v=(\d+)"/g],
      ['js', /src="(?:\.\.\/)?index\.min\.js\?v=(\d+)"/g]
    ]) {
      const matches = [...html.matchAll(pattern)];
      assert.equal(matches.length, 1, `${url.pathname}: missing or repeated ${type} asset`);
      versions[type].add(matches[0][1]);
    }
  }
  assert.equal(versions.css.size, 1);
  assert.equal(versions.js.size, 1);
});
