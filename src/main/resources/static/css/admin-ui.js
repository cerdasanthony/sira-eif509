(() => {
  const pendingForms = new Map();
  let navigationNotice;
  function status(message) {
    const notice = document.createElement('p');
    notice.className = 'request-status';
    notice.setAttribute('role', 'status');
    notice.setAttribute('aria-live', 'polite');
    notice.textContent = message;
    return notice;
  }
  document.addEventListener('submit', event => {
    const form = event.target;
    if (event.defaultPrevented || !(form instanceof HTMLFormElement)) return;
    if (pendingForms.has(form)) { event.preventDefault(); return; }
    const buttons = [...form.querySelectorAll('button:not([type=button]), input[type=submit]')];
    const originals = buttons.map(button => ({ button, text: button.textContent, disabled: button.disabled }));
    const submitter = event.submitter;
    const action = submitter?.textContent.trim() || 'Guardar';
    const label = action.startsWith('Ingresar') ? 'Ingresando…'
      : action.startsWith('Publicar') ? 'Publicando…'
      : action.startsWith('Cerrar') ? 'Cerrando sesión…'
      : action.startsWith('Crear') ? 'Creando…' : 'Guardando…';
    form.setAttribute('aria-busy', 'true');
    // Do not disable inputs: their values and the CSRF token must reach the server.
    buttons.forEach(button => {
      button.disabled = true;
      if (button === submitter) { button.textContent = label; button.classList.add('is-pending'); }
    });
    const notice = status(action.startsWith('Ingresar') ? 'Conectando con SIRA y verificando su cuenta…' : 'Procesando su solicitud. Espere la confirmación.');
    form.append(notice);
    const timer = setTimeout(() => {
      notice.textContent = 'Está tardando un poco más. El primer acceso puede tomar cerca de un minuto. Mantenga esta pantalla abierta.';
    }, 8000);
    pendingForms.set(form, { originals, notice, timer });
  });
  document.addEventListener('click', event => {
    const link = event.target.closest?.('a[href]');
    if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || link.target || link.hasAttribute('download')) return;
    const target = new URL(link.href);
    if (target.origin !== location.origin || (target.pathname === location.pathname && target.search === location.search && target.hash)) return;
    navigationNotice?.remove();
    navigationNotice = status('Abriendo la página…');
    navigationNotice.classList.add('navigation-status');
    document.body.append(navigationNotice);
  });
  window.addEventListener('pageshow', () => {
    for (const [form, pending] of pendingForms) {
      clearTimeout(pending.timer);
      form.removeAttribute('aria-busy');
      pending.notice.remove();
      pending.originals.forEach(({ button, text, disabled }) => {
        button.disabled = disabled;
        button.textContent = text;
        button.classList.remove('is-pending');
      });
    }
    pendingForms.clear();
    navigationNotice?.remove();
  });
})();
