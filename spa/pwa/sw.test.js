// @vitest-environment node
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { expect, it, vi } from 'vitest';

function worker() {
  const handlers = {};
  const shell = { html:'public shell' };
  const fetch = vi.fn().mockRejectedValue(new Error('offline'));
  const match = vi.fn().mockResolvedValue(shell);
  const source = readFileSync('pwa/sw.js', 'utf8')
    .replace('__VERSION__', 'test').replace('__PRECACHE__', JSON.stringify(['/index.html', '/assets/app.js']));
  runInNewContext(source, { URL, Response, fetch, caches:{ match }, self:{
    location:{ origin:'https://sira.example' }, addEventListener:(name, handler) => { handlers[name] = handler; },
  } });
  return { handlers, match, fetch, shell };
}

it('deja la autenticación, datos privados y escrituras fuera de la caché', () => {
  const { handlers, match } = worker();
  for (const [method, url, mode] of [
    ['GET','https://api.example/api/v1/rutinas','cors'],
    ['GET','https://sira.example/api/v1/rutinas','navigate'],
    ['POST','https://sira.example/auth/login','cors'],
    ['GET','https://sira.example/admin/participantes','navigate'],
  ]) {
    const respondWith = vi.fn();
    handlers.fetch({ request:{ method, url, mode }, respondWith });
    expect(respondWith).not.toHaveBeenCalled();
  }
  expect(match).not.toHaveBeenCalled();
});

it('puede abrir la interfaz pública sin conexión', async () => {
  const { handlers, shell } = worker();
  let response;
  handlers.fetch({ request:{ method:'GET', url:'https://sira.example/', mode:'navigate' }, respondWith:value => { response = value; } });
  expect(await response).toBe(shell);
});
