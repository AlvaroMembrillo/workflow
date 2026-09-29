import { defineConfig, devices } from '@playwright/test';

/**
 * Tests de extremo a extremo contra la demo completa en Docker (nginx + backend + PostgreSQL):
 *   docker compose -f docker-compose.demo.yml up --build -d --wait
 *   npm test
 * Usan los datos de ejemplo de la demo y crean sus propios usuarios, así se pueden repetir.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI']
    ? [['github'], ['html', { open: 'never' }]]
    : [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env['URL_BASE'] ?? 'http://localhost:8000',
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'] } },
    // La búsqueda cambia en móvil (filtros en una hoja inferior): se prueba también con un teléfono
    { name: 'movil', use: { ...devices['Pixel 7'] }, testMatch: /busqueda\.spec\.ts/ },
  ],
});
