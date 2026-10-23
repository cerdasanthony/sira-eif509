// The build inserts only the public application shell. API responses are never cached.
const CACHE = 'sira-shell-__VERSION__';
const FILES = __PRECACHE__;
const PUBLIC_PATHS = new Set(FILES);

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(FILES)));
  // An update waits for open windows to close, preserving unfinished forms.
});
self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter(key => key.startsWith('sira-shell-') && key !== CACHE).map(key => caches.delete(key)));
    await self.clients.claim();
  })());
});
self.addEventListener('fetch', event => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== 'GET' || url.origin !== self.location.origin) return;
  if (/^\/(api|auth|admin)(\/|$)/.test(url.pathname)) return;
  if (request.mode === 'navigate') {
    event.respondWith((async () => {
      try {
        const response = await fetch(request);
        if (response.ok) return response;
      } catch { /* Show the shell and its offline notice when the connection is lost. */ }
      return await caches.match('/index.html', { cacheName: CACHE }) || Response.error();
    })());
  } else if (PUBLIC_PATHS.has(url.pathname)) {
    event.respondWith(caches.match(request, { cacheName: CACHE }).then(cached => cached || fetch(request)));
  }
});
