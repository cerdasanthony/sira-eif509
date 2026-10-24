import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

function pwaShell() {
  return {
    name: 'sira-pwa-shell',
    apply: 'build',
    enforce: 'post',
    generateBundle(_options, bundle) {
      const files = [...new Set(['/index.html', '/favicon.svg', '/manifest.webmanifest',
        '/icons/icon-192.png', '/icons/icon-512.png', '/icons/icon-maskable-512.png',
        ...Object.keys(bundle).map(file => `/${file}`)])];
      const template = readFileSync(new URL('./pwa/sw.js', import.meta.url), 'utf8');
      const hash = createHash('sha256').update(JSON.stringify(files) + template);
      for (const file of Object.values(bundle)) hash.update(file.type === 'chunk' ? file.code : file.source);
      for (const path of ['manifest.webmanifest', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/icon-maskable-512.png']) {
        hash.update(readFileSync(new URL(`./public/${path}`, import.meta.url)));
      }
      const version = hash.digest('hex').slice(0, 12);
      this.emitFile({ type: 'asset', fileName: 'sw.js', source: template.replace('__VERSION__', version).replace('__PRECACHE__', JSON.stringify(files)) });
    },
  };
}

export default defineConfig({
  plugins: [react(), pwaShell()],
  server: { proxy: { '/api': 'http://localhost:8080', '/auth': 'http://localhost:8080', '/actuator': 'http://localhost:8080', '/admin': 'http://localhost:8080', '/css': 'http://localhost:8080' } },
  test: { environment: 'jsdom', restoreMocks: true },
});
