import { expect, test } from '@playwright/test';

import { correoPara, enlaceDe, ofertaPorTitulo, PDF_DE_PRUEBA, unico } from './apoyo';

test('un candidato se registra desde una oferta, se inscribe, sigue su candidatura y la retira', async ({
  page,
  request,
  context,
}) => {
  const oferta = await ofertaPorTitulo(request, 'Ingeniero/a de QA');

  // Sin cuenta, "Inscribirme" lleva al registro y después vuelve a la oferta
  await page.goto(`/ofertas/${oferta.id}`);
  await page.getByRole('link', { name: 'Inscribirme' }).click();
  await expect(page).toHaveURL(/\/registro\?volver=/);

  const email = `${unico('candidata')}@e2e.test`;
  await page.getByLabel('Nombre', { exact: true }).fill('Candidata E2E');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill('clave-e2e-segura');
  await page.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(page).toHaveURL(new RegExp(`/ofertas/${oferta.id}$`));

  // Para inscribirse tiene que confirmar el email. Abre el enlace del correo en otra pestaña y,
  // al volver a esta, la oferta ya le deja inscribirse sin recargar
  const panel = page.getByRole('complementary', { name: 'Inscripción' });
  await expect(panel).toContainText('Confirma tu email para inscribirte');
  const correo = await correoPara(request, email, 'Confirma tu email en Workflow');
  const pestanaDelCorreo = await context.newPage();
  await pestanaDelCorreo.goto(enlaceDe(correo, '/verificar-email'));
  await expect(pestanaDelCorreo.getByRole('heading', { level: 1 })).toHaveText('Email confirmado');
  await pestanaDelCorreo.close();
  await page.evaluate(() => document.dispatchEvent(new Event('visibilitychange')));
  await expect(panel.getByRole('button', { name: 'Inscribirme' })).toBeVisible();

  // Sube su currículum desde la propia oferta: se enviará con esta candidatura y con las siguientes
  await page.locator('app-cv-candidato input[type="file"]').setInputFiles({
    name: 'CV Candidata E2E.pdf',
    mimeType: 'application/pdf',
    buffer: PDF_DE_PRUEBA,
  });
  await expect(page.locator('app-cv-candidato')).toContainText('CV Candidata E2E.pdf');
  await expect(page.locator('app-cv-candidato').getByRole('status')).toHaveText('Currículum subido.');

  // Se inscribe con una carta de presentación
  await page.getByRole('button', { name: 'Añadir carta de presentación (opcional)' }).click();
  await page.getByLabel('Carta de presentación (opcional)').fill('Me encantaría trabajar en calidad.');
  await page.getByRole('button', { name: 'Inscribirme' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Te inscribiste' })).toContainText(
    'Estado: Enviada',
  );

  // En "Mis candidaturas" ve la candidatura en el primer paso
  await page.getByRole('link', { name: 'Ver mis candidaturas' }).click();
  await expect(page).toHaveURL(/\/mis-candidaturas$/);
  const candidatura = page.locator('li.candidatura', { hasText: oferta.titulo });
  await expect(candidatura.locator('app-estado-candidatura')).toHaveText(/Enviada/);
  await expect(candidatura.locator('[aria-current="step"]')).toContainText('Enviada');
  await candidatura.getByText('Tu carta de presentación').click();
  await expect(candidatura).toContainText('Me encantaría trabajar en calidad.');

  // La retira, con confirmación
  await candidatura.getByRole('button', { name: 'Retirar candidatura' }).click();
  const dialogo = page.getByRole('dialog', { name: '¿Retirar tu candidatura?' });
  await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused();
  await dialogo.getByRole('button', { name: 'Retirar' }).click();

  await expect(page.getByText(`Has retirado tu candidatura a "${oferta.titulo}".`)).toBeVisible();
  await expect(candidatura.locator('app-estado-candidatura')).toHaveText(/Retirada/);
  await expect(candidatura.getByRole('button', { name: 'Retirar candidatura' })).toHaveCount(0);
});
