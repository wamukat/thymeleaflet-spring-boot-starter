(() => {
  const initAccordion = (root = document) => {
    const items = [];
    if (root.matches && root.matches('[data-accordion]')) {
      items.push(root);
    }
    root.querySelectorAll('[data-accordion]').forEach(item => items.push(item));
    items.forEach(item => {
      const trigger = item.querySelector('[data-accordion-trigger]');
      const panel = item.querySelector('[data-accordion-panel]');
      const icon = item.querySelector('[data-accordion-icon]');
      if (!trigger || !panel) return;
      if (trigger.dataset.accordionBound === 'true') return;
      trigger.dataset.accordionBound = 'true';

      trigger.addEventListener('click', () => {
        const expanded = trigger.getAttribute('aria-expanded') === 'true';
        trigger.setAttribute('aria-expanded', (!expanded).toString());
        panel.style.display = expanded ? 'none' : 'block';
        if (icon) {
          icon.classList.toggle('rotate-180');
        }
      });
    });
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => initAccordion());
  } else {
    initAccordion();
  }

  document.addEventListener('htmx:afterSettle', event => {
    initAccordion(event.target);
  });
})();
