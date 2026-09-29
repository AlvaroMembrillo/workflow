import { expect, test } from '@playwright/test';

test.describe('Visitante sin cuenta', () => {
  test('la portada muestra ofertas, cada una con su salario', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { level: 1, name: 'Ofertas de empleo' })).toBeVisible();
    const tarjetas = page.locator('app-tarjeta-oferta');
    await expect(tarjetas.first()).toBeVisible();
    expect(await tarjetas.count()).toBeGreaterThanOrEqual(10);
    for (const tarjeta of await tarjetas.all()) {
      await expect(tarjeta).toContainText('brutos/año');
    }
  });

  test('buscar por texto y filtrar por modalidad deja la búsqueda en la URL', async ({
    page,
    isMobile,
  }) => {
    await page.goto('/');

    await page.getByPlaceholder('Puesto, tecnología o empresa').fill('desarrollador');
    await page.getByRole('button', { name: 'Buscar' }).click();
    await expect(page).toHaveURL(/texto=desarrollador/);

    if (isMobile) {
      // En el teléfono los filtros están en una hoja inferior
      await page.getByRole('button', { name: /Filtros/ }).click();
      const hoja = page.getByRole('dialog', { name: 'Filtros' });
      await hoja.getByRole('radio', { name: 'En remoto' }).check();
      await hoja.getByRole('button', { name: /^Ver / }).click();
      await expect(hoja).toBeHidden();
    } else {
      await page
        .getByRole('complementary', { name: 'Filtros' })
        .getByRole('radio', { name: 'En remoto' })
        .check();
    }

    await expect(page).toHaveURL(/modalidad=REMOTO/);
    await expect(page.getByRole('list', { name: 'Filtros aplicados' })).toContainText('En remoto');
    const tarjetas = page.locator('app-tarjeta-oferta');
    await expect(tarjetas.first()).toBeVisible();
    for (const tarjeta of await tarjetas.all()) {
      await expect(tarjeta).toContainText(/desarrollador/i);
      await expect(tarjeta).toContainText('En remoto');
    }

    // La búsqueda sobrevive a una recarga porque vive en la URL
    await page.reload();
    await expect(page.getByPlaceholder('Puesto, tecnología o empresa')).toHaveValue('desarrollador');
  });

  test('la ficha de una oferta muestra el salario completo e invita a crear cuenta', async ({ page }) => {
    await page.goto('/?texto=Desarrollador/a Java Backend');
    await page.getByRole('link', { name: 'Desarrollador/a Java Backend' }).click();

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Desarrollador/a Java Backend');
    await expect(page).toHaveTitle('Desarrollador/a Java Backend · Workflow');
    await expect(page.locator('.hechos')).toContainText('38.000–45.000 €');

    const inscribirme = page.getByRole('link', { name: 'Inscribirme' });
    await expect(inscribirme).toHaveAttribute('href', /^\/registro\?volver=%2Fofertas%2F/);
  });
});
