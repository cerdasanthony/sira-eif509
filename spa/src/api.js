const base = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '');
let token = null;
let onUnauthorized = () => {};

// El token vive en memoria; no se persiste en localStorage ni en cookies.
export function setSession(value, expired = () => {}) {
  token = value;
  onUnauthorized = expired;
}

export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

export async function request(path, { body, signal, timeoutMs = 240000, anonymous = false, ...options } = {}) {
  const controller = new AbortController();
  let timedOut = false;
  const abort = () => controller.abort();
  if (signal?.aborted) abort();
  else signal?.addEventListener('abort', abort, { once: true });
  const timer = setTimeout(() => { timedOut = true; controller.abort(); }, timeoutMs);
  try {
    const response = await fetch(`${base}${path}`, {
      ...options,
      signal: controller.signal,
      credentials: 'omit',
      headers: { ...(!anonymous && token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
    const data = response.status === 204 ? null : await response.json().catch(error => {
      if (error.name === 'AbortError') throw error;
      return null;
    });
    if (!response.ok) {
      if (response.status === 401 && path !== '/auth/login') {
        token = null;
        onUnauthorized();
      }
      const fields = data?.errores?.map(e => `${e.campo}: ${e.mensaje}`).join('. ');
      throw new ApiError(fields || data?.detail || 'No fue posible completar la solicitud.', response.status);
    }
    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (!timedOut && error.name === 'AbortError') throw error;
    if (path === '/api/v1/ejecuciones/cierres') {
      throw new ApiError('No se pudo confirmar el guardado. Revise Mis registros antes de intentarlo otra vez.', 0);
    }
    throw new ApiError(timedOut
      ? 'El servidor está tardando demasiado. Inténtelo de nuevo en unos instantes.'
      : 'No se pudo conectar. Revise su conexión e intente de nuevo.', 0);
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener('abort', abort);
  }
}

function pause(ms, signal) {
  return new Promise((resolve, reject) => {
    const aborted = () => { clearTimeout(timer); reject(new DOMException('Aborted', 'AbortError')); };
    const timer = setTimeout(() => { signal?.removeEventListener('abort', aborted); resolve(); }, ms);
    if (signal?.aborted) aborted();
    else signal?.addEventListener('abort', aborted, { once: true });
  });
}

export async function waitForServer(signal) {
  const deadline = Date.now() + 240000;
  while (Date.now() < deadline) {
    signal?.throwIfAborted();
    try {
      const health = await request('/actuator/health', {
        signal, anonymous: true, cache: 'no-store', timeoutMs: Math.min(15000, deadline - Date.now()),
      });
      if (health?.status === 'UP') return;
    } catch (error) {
      if (error.name === 'AbortError') throw error;
      if (error.status && error.status < 500 && error.status !== 429) throw error;
    }
    const remaining = deadline - Date.now();
    if (remaining > 0) await pause(Math.min(3000, remaining), signal);
  }
  throw new ApiError('El servidor de SIRA no está disponible por ahora. No se enviaron sus credenciales. Inténtelo de nuevo en unos minutos.', 503);
}

export const api = {
  login: async (body, { signal, onProgress = () => {} } = {}) => {
    onProgress('Esperando a que el servidor de SIRA esté listo…');
    await waitForServer(signal);
    signal?.throwIfAborted();
    onProgress('Verificando su cuenta…');
    return request('/auth/login', { method: 'POST', body, signal, anonymous: true, timeoutMs: 30000 });
  },
  perfil: signal => request('/api/v1/usuarios/me', { signal, timeoutMs: 30000 }),
  accesoAdministracion: signal => request('/api/v1/acceso/administracion', { method: 'POST', signal, timeoutMs: 30000 }),
  rutinas: (page, signal) => request(`/api/v1/rutinas?page=${page}&size=6&sort=nombre,asc`, { signal }),
  rutina: (id, signal) => request(`/api/v1/rutinas/${id}`, { signal }),
  historial: (page, signal) => request(`/api/v1/ejecuciones?page=${page}&size=6&sort=fecha,desc`, { signal }),
  cerrar: body => request('/api/v1/ejecuciones/cierres', { method: 'POST', body }),
};
