import AxeBuilder from '@axe-core/playwright';
import { expect, Page, test } from '@playwright/test';

import { crearCuenta, entrar, ofertaPorTitulo, PASSWORD_DEMO } from './apoyo';

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

    test('recuperar la contraseña y enlace que ya no vale', async ({ page }) => {
      await page.goto('/recuperar');
      await page.getByRole('button', { name: 'Enviar enlace' }).click();
      await expect(page.locator('.error-campo')).toBeVisible();
      await auditar(page);

      await page.goto('/restablecer');
      await expect(page.getByRole('heading', { level: 1 })).toHaveText('Este enlace ya no vale');
      await auditar(page);
    });

    test('mi cuenta, con el aviso de email sin confirmar', async ({ page, request }) => {
      const cuenta = await crearCuenta(request, 'CANDIDATO', { verificada: false });
      await entrar(page, cuenta.email, cuenta.password);
      await page.goto('/cuenta');
      await expect(page.getByRole('complementary', { name: 'Email sin confirmar' })).toBeVisible();
      await expect(page.locator('.datos')).toContainText('Sin confirmar');
      await auditar(page);
    });

    test('inscripción con el currículum y mi cuenta de candidata', async ({ page, request }) => {
      // Marta tiene currículum y no se ha inscrito en esta oferta
      const oferta = await ofertaPorTitulo(request, 'Ingeniero/a de QA');
      await entrar(page, 'marta@demo.test', PASSWORD_DEMO);
      await page.goto(`/ofertas/${oferta.id}`);
      await expect(page.locator('app-cv-candidato')).toContainText('CV Marta Sanz.pdf');
      await auditar(page);

      await page.goto('/cuenta');
      await expect(page.locator('app-cv-candidato')).toContainText('CV Marta Sanz.pdf');
      await auditar(page);
    });

    test('denunciar una oferta y el panel de moderación', async ({ page, request }) => {
      const oferta = await ofertaPorTitulo(request, 'Carretillero/a');
      await entrar(page, 'luis@demo.test', PASSWORD_DEMO);
      await page.goto(`/ofertas/${oferta.id}`);
      await page.getByRole('button', { name: 'Denunciar esta oferta' }).click();
      const formulario = page.getByRole('dialog', { name: 'Denunciar esta oferta' });
      await formulario.getByRole('button', { name: 'Enviar denuncia' }).click();
      await expect(formulario).toContainText('Elige un motivo');
      await auditar(page);
      await formulario.getByRole('button', { name: 'Cancelar' }).click();
      await page.getByRole('button', { name: 'Salir' }).click();

      await entrar(page, 'admin@demo.test', PASSWORD_DEMO);
      await expect(page.locator('li.denuncia').first()).toBeVisible();
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
