import { afterEach, expect, it, vi } from 'vitest';
import { api, request, setSession } from './api';

afterEach(() => { setSession(null); vi.unstubAllGlobals(); vi.useRealTimers(); });
it('envía JWT y datos JSON; no envía cookies de la sesión MVC', async () => {
  const fetch = vi.fn().mockResolvedValue({ ok:true, status:201, json:async () => ({ ejecucionId:5 }) });
  vi.stubGlobal('fetch', fetch); setSession('token-de-prueba');
  expect(await api.cerrar({ rutinaId:1 })).toEqual({ ejecucionId:5 });
  expect(fetch).toHaveBeenCalledWith('/api/v1/ejecuciones/cierres', expect.objectContaining({
    method:'POST', credentials:'omit', headers:{ Authorization:'Bearer token-de-prueba', 'Content-Type':'application/json' }, body:'{"rutinaId":1}',
  }));
});
it('limpia la sesión y notifica una vez que llega 401', async () => {
  const expired = vi.fn();
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok:false, status:401, json:async () => ({ detail:'Token expirado' }) }));
  setSession('expirado', expired); await expect(api.perfil()).rejects.toMatchObject({ status:401 });
  expect(expired).toHaveBeenCalledOnce();
});
it('un login incorrecto presenta el error sin activar el cierre por expiración', async () => {
  const expired = vi.fn(); setSession(null, expired);
  vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce({ ok:true, status:200, json:async () => ({ status:'UP' }) })
    .mockResolvedValueOnce({ ok:false, status:401, json:async () => ({ detail:'Credenciales invalidas' }) }));
  await expect(api.login({})).rejects.toThrow('Credenciales invalidas'); expect(expired).not.toHaveBeenCalled();
});
it('muestra errores de campos y diferencia fallas de red de cancelaciones', async () => {
  const fetch = vi.fn().mockResolvedValueOnce({ ok:false, status:400, json:async () => ({ errores:[{ campo:'fecha', mensaje:'Fecha inválida' }] }) })
    .mockRejectedValueOnce(new TypeError('Network')).mockRejectedValueOnce(new DOMException('Aborted', 'AbortError'));
  vi.stubGlobal('fetch', fetch);
  await expect(request('/api/v1/rutinas')).rejects.toThrow('fecha: Fecha inválida');
  await expect(request('/api/v1/rutinas')).rejects.toMatchObject({ status:0 });
  await expect(request('/api/v1/rutinas')).rejects.toMatchObject({ name:'AbortError' });
});

it('termina una espera excesiva y diferencia un guardado de resultado incierto', async () => {
  vi.useFakeTimers();
  vi.stubGlobal('fetch', vi.fn((_url, { signal }) => new Promise((_resolve, reject) => {
    signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
  })));
  const login = expect(request('/auth/login', { method:'POST', body:{} })).rejects.toThrow('tardando demasiado');
  await vi.advanceTimersByTimeAsync(240000);
  await login;
  const save = expect(api.cerrar({ rutinaId:1 })).rejects.toThrow('Revise Mis registros');
  await vi.advanceTimersByTimeAsync(240000);
  await save;
});

it('espera el arranque sin enviar credenciales ni JWT; luego autentica una sola vez', async () => {
  vi.useFakeTimers(); setSession('sesion-anterior');
  const fetch = vi.fn().mockRejectedValueOnce(new TypeError('Cold start'))
    .mockResolvedValueOnce({ ok:false, status:503, json:async () => ({ status:'DOWN' }) })
    .mockResolvedValueOnce({ ok:true, status:200, json:async () => ({ status:'UP' }) })
    .mockResolvedValueOnce({ ok:true, status:200, json:async () => ({ accessToken:'nuevo' }) });
  vi.stubGlobal('fetch', fetch);
  const onProgress = vi.fn();
  const login = api.login({ correo:'mariana@example.org', password:'prueba' }, { onProgress });
  await vi.advanceTimersByTimeAsync(0);
  expect(fetch).toHaveBeenCalledOnce();
  expect(fetch).toHaveBeenLastCalledWith('/actuator/health', expect.objectContaining({ headers:{}, credentials:'omit', cache:'no-store' }));
  await vi.advanceTimersByTimeAsync(6000);
  expect(await login).toEqual({ accessToken:'nuevo' });
  expect(fetch.mock.calls.map(([url]) => url)).toEqual(['/actuator/health', '/actuator/health', '/actuator/health', '/auth/login']);
  expect(fetch.mock.calls[3][1].headers.Authorization).toBeUndefined();
  expect(onProgress).toHaveBeenLastCalledWith('Verificando su cuenta…');
});

it('permite cancelar la espera y no envía las credenciales después', async () => {
  vi.useFakeTimers();
  const fetch = vi.fn().mockRejectedValue(new TypeError('Cold start'));
  vi.stubGlobal('fetch', fetch);
  const controller = new AbortController();
  const login = expect(api.login({}, { signal:controller.signal })).rejects.toMatchObject({ name:'AbortError' });
  await vi.advanceTimersByTimeAsync(0);
  controller.abort(); await login;
  await vi.advanceTimersByTimeAsync(300000);
  expect(fetch).toHaveBeenCalledOnce();
});

it('limita el arranque a cuatro minutos y no confunde una página HTML con la API lista', async () => {
  vi.useFakeTimers();
  const fetch = vi.fn().mockResolvedValue({ ok:true, status:200, json:async () => { throw new SyntaxError('HTML de espera'); } });
  vi.stubGlobal('fetch', fetch);
  const login = expect(api.login({})).rejects.toThrow('No se enviaron sus credenciales');
  await vi.advanceTimersByTimeAsync(240000); await login;
  expect(fetch.mock.calls.every(([url]) => url === '/actuator/health')).toBe(true);
});

it('no reenvía un POST de autenticación que supera los treinta segundos', async () => {
  vi.useFakeTimers();
  const fetch = vi.fn().mockResolvedValueOnce({ ok:true, status:200, json:async () => ({ status:'UP' }) })
    .mockImplementationOnce((_url, { signal }) => new Promise((_resolve, reject) => {
      signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')));
    }));
  vi.stubGlobal('fetch', fetch);
  const login = expect(api.login({})).rejects.toThrow('tardando demasiado');
  await vi.advanceTimersByTimeAsync(30000); await login;
  expect(fetch.mock.calls.map(([url]) => url)).toEqual(['/actuator/health', '/auth/login']);
});
