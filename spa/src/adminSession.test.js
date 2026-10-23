import { afterEach, expect, it, vi } from 'vitest';
import { openAdministration } from './adminSession';

afterEach(() => { document.body.innerHTML = ''; vi.restoreAllMocks(); });

it('el salto administrativo usa POST y nunca pone el código en la dirección', () => {
  const submit = vi.spyOn(HTMLFormElement.prototype, 'submit').mockImplementation(() => {});
  openAdministration('codigo-temporal');
  const form = document.querySelector('form');
  expect(form.method).toBe('post');
  expect(form.action).toMatch(/\/admin\/continuar$/);
  expect(form.action).not.toContain('codigo-temporal');
  expect([...new FormData(form)]).toEqual([['codigo','codigo-temporal']]);
  expect(submit).toHaveBeenCalledOnce();
});
