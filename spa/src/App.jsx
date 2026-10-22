import { useEffect, useRef, useState } from 'react';
import { api, setSession } from './api';

function Icon({ name = 'leaf', size = 22 }) {
  const paths = {
    leaf: <><path d="M20 4C10 2 3 7 5 14c2 7 12 7 15-10Z"/><path d="m5 20 9-10"/></>,
    grid: <><rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/></>,
    clock: <><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></>,
    check: <path d="m5 12 4 4L19 6"/>,
    arrow: <><path d="M4 12h16m-6-6 6 6-6 6"/></>,
    exit: <><path d="M9 4H4v16h5m6-13 5 5-5 5M9 12h11"/></>,
  };
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function ErrorMessage({ message }) { return message ? <p className="error" role="alert">{message}</p> : null; }
function Loading() { return <div className="state" role="status"><span className="spinner" /> Cargando su espacio…</div>; }
function Pager({ data, page, onChange }) {
  return <nav className="pager" aria-label="Paginación"><button className="secondary" disabled={data.primera} onClick={() => onChange(page - 1)}>← Anterior</button><span>Página {page + 1} de {Math.max(data.totalPaginas, 1)}</span><button className="secondary" disabled={data.ultima} onClick={() => onChange(page + 1)}>Siguiente →</button></nav>;
}

function Login({ onLogin, notice }) {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    const form = new FormData(event.currentTarget);
    try { await onLogin({ correo: form.get('correo'), password: form.get('password') }); }
    catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  return <main className="login"><section className="login-story"><div className="wordmark"><Icon size={30}/> SIRA</div><div><span className="eyebrow">RUTINAS QUE ACOMPAÑAN</span><h1>Cada pequeño paso<br/>cuenta.</h1><p>Un espacio para acompañar el día a día,<br/>a su propio ritmo.</p><div className="growth" aria-hidden="true"><span>01</span><i/><span>02</span><i/><span><Icon name="leaf" size={30}/></span></div></div><small>Sistema de Rutinas y Apoyos</small></section><section className="login-form"><div className="form-inner"><span className="eyebrow">BIENVENIDO A SU ESPACIO</span><h2>Qué bueno tenerle aquí.</h2><p>Ingrese para consultar las rutinas y registrar los avances de hoy.</p>{notice && <p className="notice" role="status">{notice}</p>}<ErrorMessage message={error}/><form onSubmit={submit}><label>Correo electrónico<input name="correo" type="email" autoComplete="username" placeholder="nombre@correo.com" maxLength="160" required/></label><label>Contraseña<input name="password" type="password" autoComplete="current-password" placeholder="Su contraseña" maxLength="72" required/></label><button disabled={busy}>{busy ? 'Ingresando…' : 'Ingresar a mi espacio'} <Icon name="arrow" size={18}/></button></form><p className="login-footnote">¿Necesita acceso? Contacte al profesional que le acompaña.</p></div></section></main>;
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
  return <><div className="section-heading"><h2>Sus rutinas</h2><span>{state.data.totalElementos} en total</span></div>{state.data.contenido.length === 0 ? <div className="state"><Icon name="leaf" size={35}/><h3>Un nuevo comienzo</h3><p>Cuando su profesional publique una rutina, aparecerá aquí.</p></div> : <div className="routine-grid">{state.data.contenido.map((routine, i) => <article className="routine-card" key={routine.id}><div className="card-top"><span className={`routine-icon tone-${i % 3}`}><Icon name={i % 2 ? 'leaf' : 'grid'} size={26}/></span><span className="tag">Publicada</span></div><h3>{routine.nombre}</h3><p className="routine-person">Participante #{routine.participanteId}</p><div className="card-time"><Icon name="clock" size={17}/> {routine.horaInicio.slice(0, 5)} <span>· Desde {routine.vigenciaDesde}</span></div><button className="card-action" onClick={() => onSelect(routine.id)}>Ver pasos y registrar <Icon name="arrow" size={19}/></button></article>)}</div>}<Pager data={state.data} page={page} onChange={setPage}/></>;
}

function localDate() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}

function ExecutionForm({ id, onClose, onSaved }) {
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
    submitting.current = true; setBusy(true); setError('');
    try { const result = await api.cerrar(body); onSaved(result); }
    catch (e) { setError(e.message); }
    finally { submitting.current = false; setBusy(false); }
  }
  return <section className="execution"><button className="back-link" disabled={busy} onClick={onClose}>← Volver a mis rutinas</button>{!routine ? <><ErrorMessage message={error}/>{error ? <button onClick={() => setLoadAttempt(n => n + 1)}>Reintentar</button> : <Loading/>}</> : <><span className="eyebrow">UN PASO A LA VEZ</span><h1>{routine.nombre}</h1><p>Registre cómo salió cada paso. Todos los avances tienen valor.</p><p className="days-note">Días: {routine.diasSemana.slice().sort().map(d => ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'][d - 1]).join(', ')}</p><form onSubmit={submit}><fieldset disabled={busy} className="form-fields"><div className="execution-meta"><label>Fecha<input type="date" value={date} onChange={e => setDate(e.target.value)} min={routine.vigenciaDesde} max={routine.vigenciaHasta && routine.vigenciaHasta < localDate() ? routine.vigenciaHasta : localDate()} required/></label><label>Hora de inicio<input type="time" name="horaInicio" defaultValue={routine.horaInicio.slice(0,5)} required/></label><label>Hora de finalización<input type="time" name="horaFin" required/></label></div>{routine.pasos.map((p, index) => <fieldset className="step" key={p.id}><legend><span className="step-number">{index + 1}</span> {p.descripcion}</legend><p className="step-estimate">Duración estimada: {p.duracionEstimadaMin} min</p><div className="outcomes">{[['LOGRADO', 'Logrado'], ['CON_APOYO', 'Con apoyo'], ['NO_LOGRADO', 'No logrado']].map(([value, text]) => <label key={value}><input type="radio" name={`resultado-${p.id}`} value={value} required/><span>{text}</span></label>)}</div><label className="observation">Observación (opcional)<input name={`observacion-${p.id}`} maxLength="240" placeholder="Algo que quiera compartir con su profesional"/></label></fieldset>)}<ErrorMessage message={error}/><div className="save-row"><p>Se guardarán todos los pasos y se calculará la adherencia.</p><button disabled={busy || routine.pasos.length === 0}>{busy ? 'Guardando…' : 'Guardar registro'} <Icon name="check" size={18}/></button></div></fieldset></form></>}</section>;
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
  return <><div className="page-intro"><span className="eyebrow">EL CAMINO RECORRIDO</span><h1>Mis registros</h1><p>Cada registro nos ayuda a comprender y acompañar mejor.</p></div>{state.loading ? <Loading/> : state.error ? <><ErrorMessage message={state.error}/><button onClick={() => setRetry(n => n + 1)}>Reintentar</button></> : <><div className="history-table"><table><thead><tr><th>Fecha</th><th>Rutina</th><th>Horario</th><th>Adherencia</th><th>Estado</th></tr></thead><tbody>{state.data.contenido.map(e => <tr key={e.id}><td>{e.fecha}</td><td>Rutina #{e.rutinaId}</td><td>{e.horaInicio.slice(0,5)} – {e.horaFin?.slice(0,5) || '—'}</td><td><span className="adherence">{e.adherencia ?? '—'}{e.adherencia != null && '%'}</span></td><td><span className="tag">{e.estado}</span></td></tr>)}{!state.data.contenido.length && <tr><td colSpan="5">Aún no hay registros. Puede crear el primero desde Mis rutinas.</td></tr>}</tbody></table></div><Pager data={state.data} page={page} onChange={setPage}/></>}</>;
}

export default function App() {
  const [user, setUser] = useState(null);
  const [notice, setNotice] = useState('');
  const [tab, setTab] = useState('rutinas');
  const [selected, setSelected] = useState(null);
  const [saved, setSaved] = useState(null);
  const [revision, setRevision] = useState(0);
  function logout(message = '') { setSession(null); setUser(null); setNotice(message); setSelected(null); setSaved(null); setTab('rutinas'); }
  async function login(credentials) {
    const result = await api.login(credentials);
    setSession(result.accessToken, () => logout('Su sesión terminó. Ingrese de nuevo para continuar.'));
    try { const profile = await api.perfil(); setUser(profile); setNotice(''); }
    catch (error) { setSession(null); throw error; }
  }
  if (!user) return <Login onLogin={login} notice={notice}/>;
  const professional = user.rol === 'PROFESIONAL';
  return <div className="app-shell"><aside className="sidebar"><div className="wordmark"><Icon size={28}/> SIRA</div><span className="sidebar-caption">MI ESPACIO</span><nav aria-label="Principal"><button className={tab === 'rutinas' ? 'active' : ''} onClick={() => { setTab('rutinas'); setSelected(null); }}><Icon name="grid"/> Mis rutinas</button><button className={tab === 'historial' ? 'active' : ''} onClick={() => { setTab('historial'); setSelected(null); }}><Icon name="clock"/> Mis registros</button></nav><div className="sidebar-note"><Icon name="leaf" size={28}/><p>El progreso se construye<br/>un día a la vez.</p></div><div className="sidebar-footer"><span className="avatar">{user.nombre[0]}</span><div><strong>{user.nombre.split(' ').slice(0,2).join(' ')}</strong><small>{professional ? 'Profesional' : 'Encargado'}</small></div><button className="icon-button" aria-label="Cerrar sesión" onClick={() => logout()}><Icon name="exit" size={20}/></button></div></aside><div className="workspace"><header className="topbar"><span>Mi espacio <span className="breadcrumb">/ {selected ? 'Registrar rutina' : tab === 'rutinas' ? 'Mis rutinas' : 'Mis registros'}</span></span><span className="today">{new Intl.DateTimeFormat('es-CR', { day:'numeric', month:'long', year:'numeric' }).format(new Date())}</span></header><main className="content">{professional ? <div className="state"><h1>Espacio profesional</h1><p>La creación de participantes y la publicación de rutinas están disponibles en el módulo administrativo.</p><a className="button" href={`${(import.meta.env.VITE_API_URL || '').replace(/\/$/,'')}/admin/participantes`}>Abrir administración</a></div> : selected ? <ExecutionForm key={selected} id={selected} onClose={() => setSelected(null)} onSaved={result => { setSaved(result); setSelected(null); setRevision(n => n + 1); }}/>: tab === 'historial' ? <History reload={revision}/> : <><div className="page-intro"><span className="eyebrow">SU DÍA, PASO A PASO</span><h1>Hola, {user.nombre.split(' ')[0]}<span className="hello-dot">.</span></h1><p>Pequeñas acciones de hoy, grandes avances para mañana.</p></div>{saved && <div className="saved" role="status"><Icon name="check"/><div><strong>Registro guardado</strong><p>Adherencia: {saved.adherencia}%. Gracias por acompañar este proceso.</p></div><button className="text-button" onClick={() => setTab('historial')}>Ver mis registros →</button></div>}<section className="welcome-banner"><div><span className="eyebrow">CADA AVANCE IMPORTA</span><h2>Acompañar también es celebrar.</h2><p>Revise sus rutinas y cuéntenos cómo fue cada paso. <br/>No hay un ritmo correcto: hay un ritmo propio.</p></div><div className="banner-art" aria-hidden="true"><div className="art-ring"/><span className="art-leaf"><Icon name="leaf" size={58}/></span><span className="art-check"><Icon name="check" size={27}/></span><span className="art-dot"/></div></section><RoutineList onSelect={id => { setSaved(null); setSelected(id); }} reload={revision}/><p className="bottom-note"><Icon name="leaf" size={16}/> Un registro honesto es una herramienta para acompañar mejor.</p></>}</main><footer>SIRA <span>·</span> Sistema de Rutinas y Apoyos</footer></div></div>;
}
