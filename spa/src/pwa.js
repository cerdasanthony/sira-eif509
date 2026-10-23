import { useEffect, useState } from 'react';

let installEvent = null;
let installed = false;
const listeners = new Set();
const notify = () => listeners.forEach(listener => listener());

// Keep the browser event even if the user signs in before choosing to install.
window.addEventListener('beforeinstallprompt', event => {
  event.preventDefault();
  installEvent = event;
  notify();
});
window.addEventListener('appinstalled', () => {
  installed = true;
  installEvent = null;
  notify();
});

export function useInstall() {
  const [, refresh] = useState(0);
  useEffect(() => {
    const listener = () => refresh(value => value + 1);
    listeners.add(listener);
    return () => listeners.delete(listener);
  }, []);
  return {
    installed: installed || window.matchMedia?.('(display-mode: standalone)').matches || navigator.standalone,
    available: !!installEvent,
    async prompt() {
      if (!installEvent) return null;
      const event = installEvent;
      installEvent = null;
      notify();
      await event.prompt();
      return (await event.userChoice).outcome;
    },
  };
}

export function registerPwa() {
  if (import.meta.env.PROD && 'serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js', { updateViaCache: 'none' }).catch(() => {
      // Installation can be retried on the next visit; online access still works.
    });
  }
}
