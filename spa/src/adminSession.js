export function openAdministration(codigo) {
  const base = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = `${base}/admin/continuar`;
  form.hidden = true;
  const input = document.createElement('input');
  input.type = 'hidden';
  input.name = 'codigo';
  input.value = codigo;
  form.append(input);
  document.body.append(form);
  // Top-level navigation sets the API's first-party session cookie, including on mobile.
  // Only the short-lived grant is posted; the password and JWT stay out of the URL.
  form.submit();
}
