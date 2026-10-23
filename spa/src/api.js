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

export async function request(path, { body, signal, ...options } = {}) {
  const controller = new AbortController();
  let timedOut = false;
  const abort = () => controller.abort();
  if (signal?.aborted) abort();
  else signal?.addEventListener('abort', abort, { once: true });
  const timer = setTimeout(() => { timedOut = true; controller.abort(); }, 240000);
  try {
    const response = await fetch(`${base}${path}`, {
      ...options,
      signal: controller.signal,
      credentials: 'omit',
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
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

export const api = {
  login: body => request('/auth/login', { method: 'POST', body }),
  perfil: signal => request('/api/v1/usuarios/me', { signal }),
  accesoAdministracion: () => request('/api/v1/acceso/administracion', { method: 'POST' }),
  rutinas: (page, signal) => request(`/api/v1/rutinas?page=${page}&size=6&sort=nombre,asc`, { signal }),
  rutina: (id, signal) => request(`/api/v1/rutinas/${id}`, { signal }),
  historial: (page, signal) => request(`/api/v1/ejecuciones?page=${page}&size=6&sort=fecha,desc`, { signal }),
  cerrar: body => request('/api/v1/ejecuciones/cierres', { method: 'POST', body }),
};
