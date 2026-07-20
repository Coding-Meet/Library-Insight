(() => {
  'use strict';

  const INSTALL_CMD = 'curl -fsSL https://raw.githubusercontent.com/Coding-Meet/Library-Insight/main/install.sh | bash';

  /* ---------------------------------------------------------------------
     Copy-to-clipboard
     Handles both install banners (hero + CTA band). Falls back to a
     manual textarea/execCommand approach if the Clipboard API is
     unavailable (older browsers, insecure contexts).
  --------------------------------------------------------------------- */
  function copyToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text);
    }
    return new Promise((resolve, reject) => {
      const textarea = document.createElement('textarea');
      textarea.value = text;  
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();
      try {
        document.execCommand('copy');
        resolve();
      } catch (err) {
        reject(err);
      } finally {
        document.body.removeChild(textarea);
      }
    });
  }

  function wireCopyButton(button, getText = () => INSTALL_CMD) {
    if (!button) return;
    if (!button.dataset.copyLabel) button.dataset.copyLabel = button.textContent.trim() || 'Copy';
    if (!button.dataset.copyAriaLabel) {
      button.dataset.copyAriaLabel = button.getAttribute('aria-label') || button.dataset.copyLabel;
    }
    const label = button.querySelector('.copy-label');
    let resetTimer = null;

    button.addEventListener('click', () => {
      copyToClipboard(getText())
        .then(() => {
          button.classList.add('is-copied');
          if (label) label.textContent = 'Copied!';
          if (!label) button.textContent = 'Copied';
          button.setAttribute('aria-label', 'Command copied to clipboard');

          clearTimeout(resetTimer);
          resetTimer = setTimeout(() => {
            button.classList.remove('is-copied');
            if (label) label.textContent = 'Copy';
            if (!label) button.textContent = button.dataset.copyLabel || 'Copy';
            button.setAttribute('aria-label', button.dataset.copyAriaLabel);
          }, 2000);
        })
        .catch(() => {
          if (label) label.textContent = 'Press ⌘/Ctrl+C';
          if (!label) button.textContent = 'Copy failed';
          clearTimeout(resetTimer);
          resetTimer = setTimeout(() => {
            if (label) label.textContent = 'Copy';
            if (!label) button.textContent = button.dataset.copyLabel || 'Copy';
          }, 2000);
        });
    });
  }

  wireCopyButton(document.getElementById('copyInstallBtn'));
  wireCopyButton(document.getElementById('copyInstallBtnCta'));

  function plainCodeText(element) {
    return element.innerText
      .replace(/^\$\s+/gm, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  document.querySelectorAll('.code-block, .command-card pre').forEach((block) => {
    if (block.querySelector('.copy-code-btn')) return;
    const code = block.querySelector('code');
    if (!code) return;

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'copy-code-btn';
    button.dataset.copyLabel = 'Copy';
    button.textContent = 'Copy';
    button.setAttribute('aria-label', 'Copy command');
    block.appendChild(button);
    wireCopyButton(button, () => plainCodeText(code));
  });

  const terminalCopyButton = document.getElementById('copyTerminalBtn');
  wireCopyButton(terminalCopyButton, () => {
    const activePanel = document.querySelector('.terminal__panel.is-active code');
    return activePanel ? plainCodeText(activePanel) : '';
  });

  /* ---------------------------------------------------------------------
     Terminal playground — tab state engine
  --------------------------------------------------------------------- */
  const tabs = document.querySelectorAll('.terminal__tab');
  const panels = document.querySelectorAll('.terminal__panel');

  function activateTab(targetKey) {
    tabs.forEach((tab) => {
      const isActive = tab.dataset.tab === targetKey;
      tab.classList.toggle('is-active', isActive);
      tab.setAttribute('aria-selected', String(isActive));
    });
    panels.forEach((panel) => {
      panel.classList.toggle('is-active', panel.dataset.panel === targetKey);
    });
  }

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => activateTab(tab.dataset.tab));
  });

  // Basic keyboard navigation (left/right arrows) across tabs
  const tablist = document.querySelector('.terminal__tabs');
  if (tablist) {
    tablist.addEventListener('keydown', (e) => {
      if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
      const tabArray = Array.from(tabs);
      const currentIndex = tabArray.findIndex((t) => t.classList.contains('is-active'));
      const nextIndex =
        e.key === 'ArrowRight'
          ? (currentIndex + 1) % tabArray.length
          : (currentIndex - 1 + tabArray.length) % tabArray.length;
      tabArray[nextIndex].focus();
      activateTab(tabArray[nextIndex].dataset.tab);
    });
  }

  /* ---------------------------------------------------------------------
     Mobile nav toggle
  --------------------------------------------------------------------- */
  const burgerBtn = document.getElementById('burgerBtn');
  const mobileMenu = document.getElementById('mobileMenu');

  if (burgerBtn && mobileMenu) {
    burgerBtn.addEventListener('click', () => {
      const isOpen = mobileMenu.classList.toggle('is-open');
      burgerBtn.classList.toggle('is-open', isOpen);
      burgerBtn.setAttribute('aria-expanded', String(isOpen));
    });

    mobileMenu.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', () => {
        mobileMenu.classList.remove('is-open');
        burgerBtn.classList.remove('is-open');
        burgerBtn.setAttribute('aria-expanded', 'false');
      });
    });
  }

  /* ---------------------------------------------------------------------
     Scroll-reveal for section headers + cards (respects reduced motion)
  --------------------------------------------------------------------- */
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  if (!prefersReducedMotion && 'IntersectionObserver' in window) {
    const revealTargets = document.querySelectorAll(
      '.compare-card, .feature-card, .step, .terminal, .workflow-card, .command-card, .cta-band'
    );

    revealTargets.forEach((el) => {
      el.style.opacity = '0';
      el.style.transform = 'translateY(16px)';
      el.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
    });

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.style.opacity = '1';
            entry.target.style.transform = 'translateY(0)';
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
    );

    revealTargets.forEach((el) => observer.observe(el));
  }
})();
