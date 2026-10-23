import React from 'react';
import { act, cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, it, vi } from 'vitest';
import { ConnectionStatus, InstallApp, PendingStatus } from './Feedback';

afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); });

it('explica una espera prolongada sin anunciar que la solicitud terminó', async () => {
  vi.useFakeTimers();
  render(<PendingStatus message="Verificando su cuenta…"/>);
  expect(screen.getByRole('status').textContent).toContain('Verificando');
  await act(async () => vi.advanceTimersByTime(8000));
  expect(screen.getByRole('status').textContent).toContain('Mantenga esta pantalla abierta');
});

it('avisa cuando se pierde la conexión y retira el aviso al recuperarla', () => {
  render(<ConnectionStatus/>);
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
  act(() => window.dispatchEvent(new Event('offline')));
  expect(screen.getByRole('alert').textContent).toContain('Sin conexión');
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
  act(() => window.dispatchEvent(new Event('online')));
  expect(screen.queryByRole('alert')).toBeNull();
});

it('ofrece instrucciones o el diálogo nativo y oculta instalar tras la instalación', async () => {
  const user = userEvent.setup(); render(<InstallApp/>);
  await user.click(screen.getByRole('button', { name:'Instalar SIRA' }));
  expect(screen.getByRole('status').textContent).toContain('Agregar a pantalla');
  const event = new Event('beforeinstallprompt', { cancelable:true });
  event.prompt = vi.fn().mockResolvedValue();
  event.userChoice = Promise.resolve({ outcome:'accepted' });
  act(() => window.dispatchEvent(event));
  await user.click(screen.getByRole('button', { name:'Instalar SIRA' }));
  expect(event.prompt).toHaveBeenCalledOnce();
  act(() => window.dispatchEvent(new Event('appinstalled')));
  expect(screen.queryByRole('button', { name:'Instalar SIRA' })).toBeNull();
});
