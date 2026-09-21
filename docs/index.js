(() => {
  const input = document.getElementById('theme-toggle');
  const control = document.querySelector('.theme-switch[for="theme-toggle"]');
  if (!input) return;

  const CONSENT_COOKIE = 'vibro_cookie_consent';
  const THEME_COOKIE = 'vibro_theme_override';
  const COOKIE_MAX_AGE = 60 * 60 * 24 * 365;
  const duration = 1650;
  const midpoint = 825;
  let transitioning = false;

  const readCookie = (name) => {
    const prefix = `${name}=`;
    const item = document.cookie.split(';').map((part) => part.trim()).find((part) => part.startsWith(prefix));
    return item ? decodeURIComponent(item.slice(prefix.length)) : null;
  };

  const writeCookie = (name, value) => {
    const secure = location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${name}=${encodeURIComponent(value)}; Path=/; Max-Age=${COOKIE_MAX_AGE}; SameSite=Lax${secure}`;
  };

  const deleteCookie = (name) => {
    const secure = location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${name}=; Path=/; Max-Age=0; SameSite=Lax${secure}`;
  };

  const systemIsLight = () => window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches;
  const effectiveTheme = () => {
    const system = systemIsLight() ? 'light' : 'dark';
    return input.checked ? (system === 'light' ? 'dark' : 'light') : system;
  };
  const checkedForTheme = (theme) => theme !== (systemIsLight() ? 'light' : 'dark');

  const syncThemePictures = () => {
    const useLight = effectiveTheme() === 'light';
    document.querySelectorAll('source[data-theme-source]').forEach((source) => {
      source.media = useLight ? 'all' : 'not all';
    });
  };
  const syncAria = () => {
    if (control) control.setAttribute('aria-checked', String(input.checked));
  };

  const setScreenshotThemeWord = () => {
    const word = document.getElementById('screenshot-theme-word');
    if (!word) return;
    word.textContent = effectiveTheme() === 'dark' ? 'light' : 'dark';
  };

  const flipScreenshotThemeWord = () => {
    const word = document.getElementById('screenshot-theme-word');
    if (!word) return;
    word.textContent = word.textContent.trim() === 'light' ? 'dark' : 'light';
  };

  const positionSolarHotspot = () => {
    const logo = document.querySelector('.hero-icon');
    if (!logo) {
      document.body.style.removeProperty('--solar-x');
      document.body.style.removeProperty('--solar-y');
      return;
    }
    const rect = logo.getBoundingClientRect();
    const x = Math.min(window.innerWidth - 42, Math.max(rect.right + 180, window.innerWidth * 0.86));
    const y = rect.top + (rect.height / 2);
    document.body.style.setProperty('--solar-x', `${Math.round(x)}px`);
    document.body.style.setProperty('--solar-y', `${Math.round(y)}px`);
  };

  const consent = () => readCookie(CONSENT_COOKIE);
  const persistThemeIfAllowed = () => {
    if (consent() === 'all') writeCookie(THEME_COOKIE, effectiveTheme());
  };
  const applySavedTheme = () => {
    if (consent() !== 'all') return;
    const saved = readCookie(THEME_COOKIE);
    if (saved === 'light' || saved === 'dark') input.checked = checkedForTheme(saved);
  };

  const createCookiePanel = () => {
    const panel = document.createElement('aside');
    panel.className = 'cookie-consent';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Cookie preferences');
    panel.innerHTML = `
      <div class="cookie-copy">
        <strong>Cookie preferences</strong>
        <p>ViBRo uses a preference cookie only when you accept all, so your manual light/dark choice can follow you across pages and future visits. A necessary consent cookie remembers this choice. No analytics or advertising cookies are used.</p>
      </div>
      <div class="cookie-actions">
        <button class="cookie-button reject" type="button">Reject all</button>
        <button class="cookie-button accept" type="button">Accept all</button>
      </div>`;

    const root = document.querySelector('.site-root') || document.body;
    root.appendChild(panel);
    const close = () => panel.classList.remove('is-visible');
    panel.querySelector('.accept').addEventListener('click', () => {
      writeCookie(CONSENT_COOKIE, 'all');
      persistThemeIfAllowed();
      close();
    });
    panel.querySelector('.reject').addEventListener('click', () => {
      writeCookie(CONSENT_COOKIE, 'rejected');
      deleteCookie(THEME_COOKIE);
      close();
    });
    if (!consent()) panel.classList.add('is-visible');
  };

  const toggleTheme = () => {
    if (transitioning) return;
    const target = effectiveTheme() === 'light' ? 'dark' : 'light';
    transitioning = true;
    positionSolarHotspot();
    document.body.classList.add('theme-transitioning', target === 'dark' ? 'theme-transition-to-dark' : 'theme-transition-to-light');

    window.setTimeout(() => {
      input.checked = !input.checked;
      syncAria();
      flipScreenshotThemeWord();
      syncThemePictures();
      persistThemeIfAllowed();
    }, midpoint);

    window.setTimeout(() => {
      document.body.classList.remove('theme-transitioning', 'theme-transition-to-dark', 'theme-transition-to-light');
      transitioning = false;
    }, duration + 40);
  };

  if (control) {
    control.setAttribute('role', 'switch');
    control.setAttribute('tabindex', '0');
  }

  applySavedTheme();
  syncAria();
  setScreenshotThemeWord();
  syncThemePictures();
  createCookiePanel();

  if (control) {
    control.addEventListener('click', (event) => {
      event.preventDefault();
      toggleTheme();
    });

    control.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        toggleTheme();
      }
    });
  }

  if (window.matchMedia) {
    const media = window.matchMedia('(prefers-color-scheme: light)');
    media.addEventListener?.('change', () => {
      const saved = readCookie(THEME_COOKIE);
      if (consent() === 'all' && (saved === 'light' || saved === 'dark')) {
        input.checked = checkedForTheme(saved);
        syncAria();
      }
      setScreenshotThemeWord();
      syncThemePictures();
    });
  }
})();
