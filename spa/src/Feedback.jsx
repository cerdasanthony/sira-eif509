import React, { useEffect, useState } from 'react';
import { useInstall } from './pwa';

export function PendingStatus({ message, className = '', slowAfter = 8000 }) {
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), slowAfter);
    return () => clearTimeout(timer);
  }, [slowAfter]);
  return <div className={`pending-status ${className}`} role="status" aria-live="polite">
    <span className="spinner" aria-hidden="true"/>
    <div><strong>{message}</strong>{slow && <p>Está tardando un poco más. El primer acceso puede tomar cerca de un minuto. Mantenga esta pantalla abierta.</p>}</div>
  </div>;
}

export function ConnectionStatus() {
  const [online, setOnline] = useState(navigator.onLine);
  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener('online', update);
    window.addEventListener('offline', update);
    return () => { window.removeEventListener('online', update); window.removeEventListener('offline', update); };
  }, []);
  return online ? null : <div className="connection-status" role="alert">Sin conexión. Conéctese a internet para ingresar, consultar sus rutinas o guardar registros.</div>;
}

export function InstallApp() {
  const install = useInstall();
  const [help, setHelp] = useState(false);
  const [busy, setBusy] = useState(false);
  if (install.installed) return null;
  async function prompt() {
    if (!install.available) { setHelp(value => !value); return; }
    setBusy(true);
    try { await install.prompt(); }
    catch { setHelp(true); }
    finally { setBusy(false); }
  }
  return <div className="install-app">
    <button type="button" className="install-button" disabled={busy} onClick={prompt} aria-expanded={help}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><path d="M12 3v12m-4-4 4 4 4-4M4 16v5h16v-5"/></svg>
      {busy ? 'Abriendo instalación…' : 'Instalar SIRA'}
    </button>
    {help && <p className="install-help" role="status">En Chrome, abra el menú ⋮ y elija «Instalar aplicación» o «Agregar a pantalla principal». En iPhone, abra Compartir y elija «Agregar a pantalla de inicio».</p>}
  </div>;
}
