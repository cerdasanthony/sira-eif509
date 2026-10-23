import { readFileSync } from 'node:fs';
import { expect, it } from 'vitest';

it('el loader MVC conserva los datos y CSRF, bloquea reenvíos y se restablece al volver', () => {
  document.body.innerHTML = '<form><input name="_csrf" value="token-de-prueba"><input name="nombre" value="Mateo"><button>Guardar participante</button></form>';
  const form = document.querySelector('form');
  const button = form.querySelector('button');
  new Function(readFileSync('../src/main/resources/static/css/admin-ui.js', 'utf8'))();
  form.dispatchEvent(new SubmitEvent('submit', { bubbles:true, cancelable:true, submitter:button }));
  expect(new FormData(form).get('_csrf')).toBe('token-de-prueba');
  expect(new FormData(form).get('nombre')).toBe('Mateo');
  expect(button.disabled).toBe(true);
  const repeated = new SubmitEvent('submit', { bubbles:true, cancelable:true, submitter:button });
  form.dispatchEvent(repeated);
  expect(repeated.defaultPrevented).toBe(true);
  window.dispatchEvent(new Event('pageshow'));
  expect(button.disabled).toBe(false);
  expect(form.getAttribute('aria-busy')).toBeNull();
  document.body.innerHTML = '';
});
