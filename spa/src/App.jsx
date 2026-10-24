import React, { useEffect, useRef, useState } from 'react';
import { api, setSession } from './api';
import { openAdministration } from './adminSession';
import { ConnectionStatus, InstallApp, PendingStatus } from './Feedback';

function Icon({ name = 'grid', size = 22 }) {
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    arrow: <><path d="M4 12h16m-6-6 6 6-6 6"/></>,
    exit: <><path d="M9 4H4v16h5m6-13 5 5-5 5M9 12h11"/></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function Brand() {
  return <div className="wordmark"><span className="brand-symbol" aria-hidden="true"><i/><i/><i/></span>SIRA<span className="brand-description">Rutinas<br/>y apoyos</span></div>;
}

function formatDate(value) {
  return new Intl.DateTimeFormat('es-CR', { day:'numeric', month:'short', year:'numeric' }).format(new Date(`${value}T12:00:00`));
}

function ErrorMessage({ message }) { return message ? <p className="error" role="alert">{message}</p> : null; }
function Loading({ message = "Cargando sus rutinas…" }) { return <PendingStatus className="state loading-state" message={message}/>; }
function Pager({ data, page, onChange }) {
  return <nav className="pager" aria-label="Paginación"><button className="secondary" disabled={data.primera} onClick={() => onChange(page - 1)}>← Anterior</button><span>Página {page + 1} de {Math.max(data.totalPaginas, 1)}</span><button className="secondary" disabled={data.ultima} onClick={() => onChange(page + 1)}>Siguiente →</button></nav>;
}

function Login({ onLogin, notice }) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [progress, setProgress] = useState('Conectando con SIRA…');
  const submitting = useRef(false);
  const attempt = useRef(null);
  useEffect(() => () => attempt.current?.abort(), []);
  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    const form = new FormData(event.currentTarget);
    const controller = new AbortController();
    attempt.current = controller;
    submitting.current = true; setBusy(true); setError(''); setProgress('Conectando con SIRA…');
    try { await onLogin({ correo: form.get('correo'), password: form.get('password') }, { signal: controller.signal, onProgress: setProgress }); }
    catch (e) { if (e.name !== 'AbortError') setError(e.message); }
    finally { if (attempt.current === controller) { attempt.current = null; submitting.current = false; setBusy(false); } }
  }
  return <main className="login">
    <section className="login-story"><Brand/><div className="login-editorial"><span className="eyebrow">LA CONSTANCIA SE CONSTRUYE EN CASA</span><h1>Un día.<br/>Una rutina.<br/><em>A su ritmo.</em></h1><p>Las rutinas, los apoyos y lo que sucede entre ambos. Todo en un mismo lugar.</p><ol className="login-index"><li><span>01</span> Consulte la rutina</li><li><span>02</span> Registre cada paso</li><li><span>03</span> Comparta el resultado</li></ol></div><small>Sistema de Rutinas y Apoyos <span>·</span> SIRA</small></section>
    <section className="login-form"><div className="form-inner">
      <span className="eyebrow">ACCESO A SU CUENTA</span><h2>Bienvenido de vuelta.</h2><p>Ingrese para consultar sus rutinas y registrar los resultados.</p>
      {notice && <p className="notice" role="status">{notice}</p>}<ErrorMessage message={error}/>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset className="form-fields" disabled={busy}>
          <label>Correo electrónico<input name="correo" type="email" autoComplete="username" placeholder="nombre@correo.com" maxLength="160" required/></label>
          <label>Contraseña<input name="password" type="password" autoComplete="current-password" placeholder="Su contraseña" maxLength="72" required/></label>
          <button disabled={busy}>{busy ? <><span className="spinner button-spinner" aria-hidden="true"/> Ingresando…</> : <>Ingresar a mi espacio <Icon name="arrow" size={18}/></>}</button>
        </fieldset>
        {busy && <><PendingStatus key={progress} message={progress} slowMessage="El servidor gratuito puede necesitar hasta 4 minutos para iniciar. El ingreso continuará automáticamente cuando esté listo."/><button type="button" className="cancel-login secondary" onClick={() => attempt.current?.abort()}>Cancelar ingreso</button></>}
      </form>
      <div className="login-footnote"><span>¿Es su primera visita?</span><p>Solicite su cuenta al profesional responsable.</p><InstallApp/></div>
    </div><small className="login-form-caption">El seguimiento empieza con un registro.</small></section>
  </main>;
}

export function RoutineList({ onSelect, reload }) {
  const [page, setPage] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState({ loading: true });
  useEffect(() => {
    const controller = new AbortController();
    setState({ loading: true });
    api.rutinas(page, controller.signal).then(data => setState({ data }))
      .catch(e => { if (e.name !== 'AbortError') setState({ error: e.message }); });
    return () => controller.abort();
  }, [page, attempt, reload]);
  if (state.loading) return <Loading/>;
  if (state.error) return <div className="state"><ErrorMessage message={state.error}/><button className="secondary" onClick={() => setAttempt(n => n + 1)}>Reintentar</button></div>;
  return <><div className="section-heading"><div><span className="eyebrow">PLAN DE ACOMPAÑAMIENTO</span><h2>Rutinas publicadas <span className="count">{state.data.totalElementos}</span></h2></div><span>Asignadas por su profesional</span></div>{state.data.contenido.length === 0 ? <div className="state"><Icon name="grid" size={35}/><h3>Un nuevo comienzo</h3><p>Cuando su profesional publique una rutina, aparecerá aquí.</p></div> : <div className="routine-grid">{state.data.contenido.map((routine) => <article className="routine-card" key={routine.id}><div className="routine-schedule"><span className="eyebrow">INICIO</span><time>{routine.horaInicio.slice(0, 5)}</time><span className="schedule-unit">hora programada</span></div><div className="routine-description"><div className="card-top"><span className="routine-person">Participante #{routine.participanteId}</span><span className="tag">Publicada</span></div><h3>{routine.nombre}</h3><p className="card-time">Vigente desde el {formatDate(routine.vigenciaDesde)}</p></div><button className="card-action" aria-label={`Ver pasos y registrar: ${routine.nombre}`} onClick={() => onSelect(routine.id)}><span>Ver pasos y registrar</span><Icon name="arrow" size={21}/></button></article>)}</div>}<Pager data={state.data} page={page} onChange={setPage}/></>;
}

function localDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

function ExecutionForm({ id, onClose, onSaved, onBusyChange }) {
  const [routine, setRoutine] = useState(null);
  const [error, setError] = useState('');
  const [loadAttempt, setLoadAttempt] = useState(0);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const [date, setDate] = useState(localDate());
  useEffect(() => {
    const controller = new AbortController();
    setError(''); setRoutine(null);
    api.rutina(id, controller.signal).then(setRoutine).catch(e => { if (e.name !== 'AbortError') setError(e.message); });
    return () => controller.abort();
  }, [id, loadAttempt]);
  async function submit(event) {
    event.preventDefault();
    if (submitting.current) return;
    const form = new FormData(event.currentTarget);
    const body = { rutinaId: id, fecha: date, horaInicio: form.get('horaInicio'), horaFin: form.get('horaFin'),
      resultados: routine.pasos.map(p => ({ pasoRutinaId: p.id, resultado: form.get(`resultado-${p.id}`), observacionCorta: form.get(`observacion-${p.id}`) || null })) };
    if (body.horaFin < body.horaInicio) { setError('La hora final debe ser posterior o igual a la inicial.'); return; }
    if (!routine.diasSemana.includes(new Date(`${date}T12:00:00`).getDay() || 7)) { setError('La rutina no está programada para este día de la semana.'); return; }
    submitting.current = true; setBusy(true); onBusyChange(true); setError('');
    try { const result = await api.cerrar(body); onSaved(result); }
    catch (e) { setError(e.message); }
    finally { submitting.current = false; setBusy(false); onBusyChange(false); }
  }
  return <section className="execution"><button className="back-link" disabled={busy} onClick={onClose}>← Volver a mis rutinas</button>{!routine ? <><ErrorMessage message={error}/>{error ? <button onClick={() => setLoadAttempt(n => n + 1)}>Reintentar</button> : <Loading message="Cargando los pasos de la rutina…"/>}</> : <><span className="eyebrow">REGISTRO DE RUTINA · {routine.pasos.length} PASOS</span><h1>{routine.nombre}</h1><p>Seleccione el resultado de cada paso y añada una observación si lo necesita.</p><p className="days-note">Días: {routine.diasSemana.slice().sort().map(d => ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'][d - 1]).join(', ')}</p><form onSubmit={submit} aria-busy={busy}><fieldset disabled={busy} className="form-fields"><div className="execution-meta"><label>Fecha<input type="date" value={date} onChange={e => setDate(e.target.value)} min={routine.vigenciaDesde} max={routine.vigenciaHasta && routine.vigenciaHasta < localDate() ? routine.vigenciaHasta : localDate()} required/></label><label>Hora de inicio<input type="time" name="horaInicio" defaultValue={routine.horaInicio.slice(0,5)} required/></label><label>Hora de finalización<input type="time" name="horaFin" required/></label></div>{routine.pasos.map((p, index) => <fieldset className="step" key={p.id}><legend><span className="step-number">{index + 1}</span> {p.descripcion}</legend><p className="step-estimate">Duración estimada: {p.duracionEstimadaMin} min</p><div className="outcomes">{[['LOGRADO', 'Logrado'], ['CON_APOYO', 'Con apoyo'], ['NO_LOGRADO', 'No logrado']].map(([value, text]) => <label key={value}><input type="radio" name={`resultado-${p.id}`} value={value} required/><span>{text}</span></label>)}</div><label className="observation">Observación (opcional)<input name={`observacion-${p.id}`} maxLength="240" placeholder="¿Qué conviene tener en cuenta para la próxima vez?"/></label></fieldset>)}<ErrorMessage message={error}/><div className="save-row"><p>Al guardar, se cierra el registro y se calcula la adherencia.</p><button disabled={busy || routine.pasos.length === 0}>{busy ? <><span className="spinner button-spinner" aria-hidden="true"/> Guardando…</> : <>Guardar registro <Icon name="check" size={18}/></>}</button></div></fieldset>{busy && <PendingStatus message="Guardando los resultados de la rutina…"/>}</form></>}</section>;
}

function History({ reload }) {
  const [page, setPage] = useState(0);
  const [retry, setRetry] = useState(0);
  const [state, setState] = useState({ loading: true });
  useEffect(() => {
    const controller = new AbortController(); setState({ loading: true });
    api.historial(page, controller.signal).then(data => setState({ data })).catch(e => { if (e.name !== 'AbortError') setState({ error: e.message }); });
    return () => controller.abort();
  }, [page, retry, reload]);
  return <><div className="page-intro"><span className="eyebrow">HISTORIAL DE ACOMPAÑAMIENTO</span><h1>Mis registros</h1><p>Consulte las fechas, los horarios y los resultados de sus rutinas.</p></div>{state.loading ? <Loading message="Cargando sus registros…"/> : state.error ? <><ErrorMessage message={state.error}/><button onClick={() => setRetry(n => n + 1)}>Reintentar</button></> : <><div className="history-table"><table><thead><tr><th>Fecha</th><th>Rutina</th><th>Horario</th><th>Adherencia</th><th>Estado</th></tr></thead><tbody>{state.data.contenido.map(e => <tr key={e.id}><td>{formatDate(e.fecha)}</td><td>Rutina #{e.rutinaId}</td><td>{e.horaInicio.slice(0,5)} – {e.horaFin?.slice(0,5) || '—'}</td><td><span className="adherence">{e.adherencia ?? '—'}{e.adherencia != null && '%'}</span></td><td><span className="tag">{{CERRADA:'Cerrada', ABIERTA:'Abierta'}[e.estado] || e.estado}</span></td></tr>)}{!state.data.contenido.length && <tr><td colSpan="5">Aún no hay registros. Puede crear el primero desde Mis rutinas.</td></tr>}</tbody></table></div><Pager data={state.data} page={page} onChange={setPage}/></>}</>;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [notice, setNotice] = useState(() => new URLSearchParams(window.location.search).get('acceso') === 'expirado' ? 'El acceso no se completó a tiempo. Ingrese de nuevo para continuar.' : '');
  const [redirecting, setRedirecting] = useState(false);
  const [tab, setTab] = useState('rutinas');
  const [selected, setSelected] = useState(null);
  const [saved, setSaved] = useState(null);
  const [revision, setRevision] = useState(0);
  const [saving, setSaving] = useState(false);
  function logout(message = '') { setSession(null); setUser(null); setRedirecting(false); setSaving(false); setNotice(message); setSelected(null); setSaved(null); setTab('rutinas'); }
  async function login(credentials, { signal, onProgress }) {
    const result = await api.login(credentials, { signal, onProgress });
    signal.throwIfAborted();
    setSession(result.accessToken, () => logout('Su sesión terminó. Ingrese de nuevo para continuar.'));
    try {
      onProgress('Cargando su perfil…');
      const profile = await api.perfil(signal);
      signal.throwIfAborted();
      if (profile.rol === 'PROFESIONAL') {
        onProgress('Preparando su administración…');
        const access = await api.accesoAdministracion(signal);
        signal.throwIfAborted();
        setRedirecting(true);
        setSession(null);
        openAdministration(access.codigo);
        return;
      }
      setUser(profile); setNotice('');
    } catch (error) { setRedirecting(false); setSession(null); throw error; }
  }
  useEffect(() => {
    const restore = event => { if (event.persisted) logout(); };
    window.addEventListener('pageshow', restore);
    return () => window.removeEventListener('pageshow', restore);
  }, []);
  if (redirecting) return <main className="redirect-page"><Brand/><PendingStatus message="Abriendo su administración…"/></main>;
  if (!user) return <><ConnectionStatus/><Login onLogin={login} notice={notice}/></>;
  return <><ConnectionStatus/><div className="app-shell">
    <a className="skip-link" href="#contenido">Ir al contenido</a>
    <aside className="sidebar">
      <Brand/>
      <span className="sidebar-caption">ACOMPAÑAMIENTO</span>
      <nav aria-label="Principal">
        <button disabled={saving} className={tab === 'rutinas' ? 'active' : ''} aria-current={tab === 'rutinas' ? 'page' : undefined} onClick={() => { setTab('rutinas'); setSelected(null); }}><Icon name="grid"/> Mis rutinas <span className="nav-index">01</span></button>
        <button disabled={saving} className={tab === 'historial' ? 'active' : ''} aria-current={tab === 'historial' ? 'page' : undefined} onClick={() => { setTab('historial'); setSelected(null); }}><Icon name="clock"/> Mis registros <span className="nav-index">02</span></button>
      </nav>
      <div className="sidebar-install"><InstallApp/></div><div className="sidebar-note"><span className="eyebrow">SU CUADERNO DIARIO</span><p>Las rutinas indican el camino.<br/>Sus registros cuentan cómo fue.</p><span className="note-rule"/></div>
      <div className="sidebar-footer"><span className="avatar">{user.nombre[0]}</span><div><strong>{user.nombre.split(' ').slice(0,2).join(' ')}</strong><small>Encargado</small></div><button className="icon-button" aria-label="Cerrar sesión" disabled={saving} onClick={() => logout()}><Icon name="exit" size={20}/></button></div>
    </aside>
    <div className="workspace">
      <header className="topbar"><span>SIRA <span className="breadcrumb">/ {selected ? 'Registrar rutina' : tab === 'rutinas' ? 'Mis rutinas' : 'Mis registros'}</span></span><time className="today">{new Intl.DateTimeFormat('es-CR', { day:'numeric', month:'long', year:'numeric' }).format(new Date())}</time></header>
      <main className="content" id="contenido">
        {selected ? <ExecutionForm onBusyChange={setSaving} key={selected} id={selected} onClose={() => setSelected(null)} onSaved={result => { setSaved(result); setSelected(null); setRevision(n => n + 1); }}/>
        : tab === 'historial' ? <History reload={revision}/>
        : <>
          <div className="page-intro dashboard-intro"><div><span className="eyebrow">CUADERNO DE ACOMPAÑAMIENTO</span><h1>Hola, {user.nombre.split(' ')[0]}<span className="hello-dot">.</span></h1><p>Estas son sus rutinas. Elija una para registrar cómo fue.</p></div><span className="page-number" aria-hidden="true">01 <span>/ RUTINAS</span></span></div>
          {saved && <div className="saved" role="status"><Icon name="check"/><div><strong>Registro guardado</strong><p>Adherencia: {saved.adherencia}%.</p></div><button className="text-button" onClick={() => setTab('historial')}>Ver mis registros →</button></div>}
          <RoutineList onSelect={id => { setSaved(null); setSelected(id); }} reload={revision}/>
          <aside className="record-guide"><span className="guide-number" aria-hidden="true">i</span><div><h3>Registre lo que ocurrió.</h3><p>Un paso logrado con apoyo también cuenta. Puede dejar una observación para su profesional al completar la rutina.</p></div></aside>
        </>}
      </main>
      <footer><strong>SIRA</strong><span>Sistema de Rutinas y Apoyos</span><span className="footer-install"><InstallApp/></span><span>Seguimiento cotidiano</span></footer>
    </div>
  </div></>;
}
