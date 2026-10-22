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

export async function request(path, { body, ...options } = {}) {
  let response;
  try {
    response = await fetch(`${base}${path}`, {
      ...options,
      credentials: 'omit',
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}) },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError('No se pudo conectar. Revise su conexión e intente de nuevo.', 0);
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login') {
      token = null;
      onUnauthorized();
    }
    const fields = data?.errores?.map(e => `${e.campo}: ${e.mensaje}`).join('. ');
    throw new ApiError(fields || data?.detail || 'No fue posible completar la solicitud.', response.status);
  }
  return data;
}

export const api = {
  login: body => request('/auth/login', { method: 'POST', body }),
  perfil: signal => request('/api/v1/usuarios/me', { signal }),
  rutinas: (page, signal) => request(`/api/v1/rutinas?page=${page}&size=6&sort=nombre,asc`, { signal }),
  rutina: (id, signal) => request(`/api/v1/rutinas/${id}`, { signal }),
  historial: (page, signal) => request(`/api/v1/ejecuciones?page=${page}&size=6&sort=fecha,desc`, { signal }),
  cerrar: body => request('/api/v1/ejecuciones/cierres', { method: 'POST', body }),
};
