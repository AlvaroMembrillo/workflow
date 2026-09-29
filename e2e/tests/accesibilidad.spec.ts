import AxeBuilder from '@axe-core/playwright';
import { expect, Page, test } from '@playwright/test';

import { entrar, ofertaPorTitulo, PASSWORD_DEMO } from './apoyo';

/** Criterios de WCAG 2.2 nivel AA, el objetivo de la guía de diseño. */
const CRITERIOS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

async function auditar(page: Page): Promise<void> {
  const resultado = await new AxeBuilder({ page }).withTags(CRITERIOS).analyze();
  const problemas = resultado.violations.map(
    (violacion) =>
      `${violacion.id} (${violacion.impact}): ${violacion.help} → ${violacion.nodes
        .map((nodo) => nodo.target.join(' '))
        .join(', ')}`,
  );
  expect(problemas).toEqual([]);
}

for (const tema of ['light', 'dark'] as const) {
  test.describe(`Accesibilidad WCAG 2.2 AA con tema ${tema === 'light' ? 'claro' : 'oscuro'}`, () => {
    test.use({ colorScheme: tema });

    test('portada con resultados y filtros aplicados', async ({ page }) => {
      await page.goto('/?modalidad=REMOTO');
      await expect(page.locator('app-tarjeta-oferta').first()).toBeVisible();
      await auditar(page);
    });

    test('ficha de una oferta', async ({ page, request }) => {
      const oferta = await ofertaPorTitulo(request, 'Desarrollador/a Java Backend');
      await page.goto(`/ofertas/${oferta.id}`);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await auditar(page);
    });

    test('registro con errores de validación a la vista', async ({ page }) => {
      await page.goto('/registro');
      await page.getByRole('button', { name: 'Crear cuenta' }).click();
      await expect(page.locator('.error-campo').first()).toBeVisible();
      await auditar(page);
    });

    test('mis candidaturas', async ({ page }) => {
      await entrar(page, 'ana@demo.test', PASSWORD_DEMO);
      await page.goto('/mis-candidaturas');
      await expect(page.locator('li.candidatura').first()).toBeVisible();
      await auditar(page);
    });

    test('panel de empresa y candidaturas recibidas', async ({ page }) => {
      await entrar(page, 'rrhh@lumen.test', PASSWORD_DEMO);
      await expect(page.locator('li.fila').first()).toBeVisible();
      await auditar(page);

      await page.locator('li.fila', { hasText: 'Desarrollador/a Java Backend' }).getByRole('link', { name: 'Candidaturas' }).click();
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Desarrollador/a Java Backend');
      await auditar(page);
    });

    test('formulario para publicar una oferta', async ({ page }) => {
      await entrar(page, 'rrhh@lumen.test', PASSWORD_DEMO);
      await page.goto('/empresa/ofertas/nueva');
      await expect(page.getByLabel('Título del puesto')).toBeVisible();
      await auditar(page);
    });
  });
}
