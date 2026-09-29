/**
 * Genera las capturas del README a partir de la demo recién arrancada (sin datos de los tests E2E):
 *   docker compose -f docker-compose.demo.yml down -v && docker compose -f docker-compose.demo.yml up -d --wait
 *   cd e2e && npm run capturas
 */
import { mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

import { chromium, devices } from '@playwright/test';
import type { Browser, BrowserContextOptions, Page } from '@playwright/test';

const URL_BASE = process.env['URL_BASE'] ?? 'http://localhost:8000';
const CARPETA = new URL('../../docs/capturas/', import.meta.url);
const PASSWORD_DEMO = 'demo-workflow';

const ESCRITORIO: BrowserContextOptions = {
  viewport: { width: 1280, height: 800 },
  deviceScaleFactor: 2,
  locale: 'es-ES',
  timezoneId: 'Europe/Madrid',
};

async function abrir(
  navegador: Browser,
  opciones: BrowserContextOptions,
  usuario?: string,
): Promise<Page> {
  const contexto = await navegador.newContext({ ...opciones, baseURL: URL_BASE });
  const pagina = await contexto.newPage();
  if (usuario) {
    await pagina.goto('/entrar');
    await pagina.getByLabel('Email').fill(usuario);
    await pagina.getByLabel('Contraseña', { exact: true }).fill(PASSWORD_DEMO);
    await pagina.getByRole('button', { name: 'Entrar' }).click();
    await pagina.getByRole('button', { name: 'Salir' }).waitFor();
  }
  return pagina;
}

async function guardar(pagina: Page, nombre: string): Promise<void> {
  // Espera a que las fuentes estén cargadas para que la captura sea la definitiva
  await pagina.evaluate(() => document.fonts.ready);
  await pagina.screenshot({ path: fileURLToPath(new URL(`${nombre}.png`, CARPETA)) });
  console.log(`docs/capturas/${nombre}.png`);
  await pagina.context().close();
}

const navegador = await chromium.launch();
await mkdir(CARPETA, { recursive: true });

// Buscador con un filtro aplicado
let pagina = await abrir(navegador, ESCRITORIO);
await pagina.goto('/?modalidad=HIBRIDO');
await pagina.locator('app-tarjeta-oferta').first().waitFor();
await guardar(pagina, 'buscador');

// Ficha de una oferta
pagina = await abrir(navegador, ESCRITORIO);
await pagina.goto('/?texto=Desarrollador/a Java Backend');
await pagina.getByRole('link', { name: 'Desarrollador/a Java Backend' }).click();
await pagina.getByRole('heading', { level: 1 }).waitFor();
await guardar(pagina, 'ficha-oferta');

// Mis candidaturas de la candidata de ejemplo
pagina = await abrir(navegador, ESCRITORIO, 'ana@demo.test');
await pagina.goto('/mis-candidaturas');
await pagina.locator('li.candidatura').first().waitFor();
await guardar(pagina, 'mis-candidaturas');

// Candidaturas recibidas en el panel de la empresa de ejemplo
pagina = await abrir(navegador, ESCRITORIO, 'rrhh@lumen.test');
await pagina
  .locator('li.fila', { hasText: 'Desarrollador/a Java Backend' })
  .getByRole('link', { name: 'Candidaturas' })
  .click();
await pagina.locator('li.candidatura').first().waitFor();
await guardar(pagina, 'candidaturas-recibidas');

// Móvil con tema oscuro: la hoja de filtros
pagina = await abrir(navegador, { ...devices['Pixel 7'], colorScheme: 'dark', locale: 'es-ES' });
await pagina.goto('/?modalidad=REMOTO');
await pagina.locator('app-tarjeta-oferta').first().waitFor();
await pagina.getByRole('button', { name: /Filtros/ }).click();
await pagina.getByRole('dialog', { name: 'Filtros' }).waitFor();
await guardar(pagina, 'movil-filtros');

await navegador.close();
