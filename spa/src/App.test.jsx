import React from 'react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App, { RoutineList } from './App';
import { api, setSession } from './api';
vi.mock('./api', () => ({ setSession:vi.fn(), api:{ login:vi.fn(), perfil:vi.fn(), rutinas:vi.fn(), rutina:vi.fn(), cerrar:vi.fn(), historial:vi.fn() } }));
afterEach(cleanup);
beforeEach(() => vi.resetAllMocks());
const routine = { id:1, nombre:'Preparar la mañana', participanteId:1, horaInicio:'07:00:00', vigenciaDesde:'2026-01-01', diasSemana:[1,2,3,4,5,6,7], pasos:[{ id:11, descripcion:'Lavarse las manos', duracionEstimadaMin:3 }] };
const page = (contenido = [routine], extra = {}) => ({ contenido, totalElementos:contenido.length, totalPaginas:1, primera:true, ultima:true, ...extra });

it('presenta carga, error, reintento, datos y paginación', async () => {
  api.rutinas.mockRejectedValueOnce(new Error('No hay conexión')).mockResolvedValueOnce(page([routine], { totalPaginas:2, ultima:false })).mockResolvedValueOnce(page([], { primera:false }));
  const user = userEvent.setup(); render(<RoutineList onSelect={() => {}}/>);
  expect(screen.getByRole('status').textContent).toContain('Cargando');
  expect((await screen.findByRole('alert')).textContent).toBe('No hay conexión');
  await user.click(screen.getByRole('button', { name:'Reintentar' }));
  await screen.findByRole('heading', { name:routine.nombre });
  await user.click(screen.getByRole('button', { name:'Siguiente →' }));
  await screen.findByText('Un nuevo comienzo'); expect(api.rutinas).toHaveBeenLastCalledWith(1, expect.any(AbortSignal));
});

it('inicia sesión, registra todos los pasos y muestra adherencia e historial', async () => {
  api.login.mockResolvedValue({ accessToken:'jwt' }); api.perfil.mockResolvedValue({ nombre:'Laura Méndez', rol:'ENCARGADO' });
  api.rutinas.mockResolvedValue(page()); api.rutina.mockResolvedValue(routine);
  api.cerrar.mockResolvedValue({ ejecucionId:5, adherencia:100 }); api.historial.mockResolvedValue(page([]));
  const user = userEvent.setup(); render(<App/>);
  await user.type(screen.getByLabelText('Correo electrónico'), 'laura@example.org');
  await user.type(screen.getByLabelText('Contraseña'), 'PasswordPrueba2026!');
  await user.click(screen.getByRole('button', { name:'Ingresar a mi espacio' }));
  await user.click(await screen.findByRole('button', { name:'Ver pasos y registrar' }));
  await screen.findByRole('group', { name:'1 Lavarse las manos' });
  await user.type(screen.getByLabelText('Hora de finalización'), '07:10');
  await user.click(screen.getByLabelText('Logrado', { exact:true }));
  await user.click(screen.getByRole('button', { name:'Guardar registro' }));
  await screen.findByText('Registro guardado');
  expect(api.cerrar).toHaveBeenCalledOnce();
  expect(api.cerrar).toHaveBeenCalledWith(expect.objectContaining({ rutinaId:1, horaInicio:'07:00', horaFin:'07:10', resultados:[{ pasoRutinaId:11, resultado:'LOGRADO', observacionCorta:null }] }));
  await user.click(screen.getByRole('button', { name:'Ver mis registros →' }));
  await screen.findByText('Aún no hay registros. Puede crear el primero desde Mis rutinas.');
});

it('un error al guardar conserva los resultados y una sesión vencida vuelve al login', async () => {
  api.login.mockResolvedValue({ accessToken:'jwt' }); api.perfil.mockResolvedValue({ nombre:'Laura', rol:'ENCARGADO' });
  api.rutinas.mockResolvedValue(page()); api.rutina.mockResolvedValue(routine); api.cerrar.mockRejectedValue(new Error('Ya existe un registro para esa fecha'));
  const user = userEvent.setup(); render(<App/>);
  await user.type(screen.getByLabelText('Correo electrónico'), 'laura@example.org'); await user.type(screen.getByLabelText('Contraseña'), 'PasswordPrueba2026!');
  await user.click(screen.getByRole('button', { name:'Ingresar a mi espacio' }));
  await user.click(await screen.findByRole('button', { name:'Ver pasos y registrar' }));
  await user.type(await screen.findByLabelText('Hora de finalización'), '07:10');
  await user.click(screen.getByLabelText('Con apoyo', { exact:true }));
  await user.click(screen.getByRole('button', { name:'Guardar registro' }));
  expect((await screen.findByRole('alert')).textContent).toContain('Ya existe');
  expect(screen.getByLabelText('Con apoyo', { exact:true }).checked).toBe(true);
  const expired = setSession.mock.calls.find(([token]) => token === 'jwt')[1];
  expired(); await waitFor(() => expect(screen.getByRole('heading', { name:'Qué bueno tenerle aquí.' })).toBeTruthy());
});
