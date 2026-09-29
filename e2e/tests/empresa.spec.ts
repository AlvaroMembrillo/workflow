import { expect, test } from '@playwright/test';

import { crearCuenta, entrar, ofertaPorTitulo, PASSWORD_DEMO, unico } from './apoyo';

test('la empresa pasa a revisión y acepta una candidatura, y el candidato lo ve', async ({
  page,
  request,
}) => {
  // Un candidato nuevo se inscribe en la oferta de Java de Lumen Seguros (empresa de la demo)
  const oferta = await ofertaPorTitulo(request, 'Desarrollador/a Java Backend');
  const candidato = await crearCuenta(request, 'CANDIDATO');
  const inscripcion = await request.post(`/api/ofertas/${oferta.id}/candidaturas`, {
    headers: { Authorization: `Bearer ${candidato.token}` },
    data: { cartaPresentacion: 'Tengo experiencia con Spring Boot.' },
  });
  expect(inscripcion.status()).toBe(201);

  await entrar(page, 'rrhh@lumen.test', PASSWORD_DEMO);
  await expect(page).toHaveURL(/\/empresa$/);

  const fila = page.locator('li.fila', { hasText: oferta.titulo });
  await expect(fila).toContainText('sin responder');
  await fila.getByRole('link', { name: 'Candidaturas' }).click();

  // Sin responder → En revisión
  const tarjeta = page.locator('li.candidatura', { hasText: candidato.nombre });
  await tarjeta.getByRole('button', { name: 'Pasar a revisión' }).click();
  await expect(page.getByText(`La candidatura de ${candidato.nombre} pasa a revisión.`)).toBeVisible();

  // En revisión → Aceptada, con confirmación porque es una decisión final
  await page.getByRole('link', { name: /^En revisión/ }).click();
  await page
    .locator('li.candidatura', { hasText: candidato.nombre })
    .getByRole('button', { name: 'Aceptar' })
    .click();
  const dialogo = page.getByRole('dialog', {
    name: `¿Aceptar la candidatura de ${candidato.nombre}?`,
  });
  await expect(dialogo).toContainText('"Seleccionada"');
  await dialogo.getByRole('button', { name: 'Aceptar' }).click();
  await expect(page.getByText(`Has aceptado la candidatura de ${candidato.nombre}.`)).toBeVisible();

  // El candidato ve la decisión con la fecha de cada paso
  const respuesta = await request.get('/api/candidaturas/me', {
    headers: { Authorization: `Bearer ${candidato.token}` },
  });
  const [candidatura] = ((await respuesta.json()) as {
    content: { estado: string; fechaRevision: string | null; fechaResolucion: string | null }[];
  }).content;
  expect(candidatura.estado).toBe('ACEPTADA');
  expect(candidatura.fechaRevision).not.toBeNull();
  expect(candidatura.fechaResolucion).not.toBeNull();
});

test('una empresa nueva publica una oferta y aparece en el buscador', async ({ page, request }) => {
  const empresa = await crearCuenta(request, 'EMPRESA');
  const titulo = `Oferta ${unico('e2e')}`;

  await entrar(page, empresa.email, empresa.password);
  await expect(page.getByText('Todavía no has publicado ninguna oferta')).toBeVisible();
  await page.getByRole('link', { name: 'Publicar oferta' }).first().click();

  await page.getByLabel('Título del puesto').fill(titulo);
  await page.getByLabel('Mínimo (€)').fill('30000');
  await page.getByLabel('Máximo (€)').fill('36000');
  await page.getByLabel('Ubicación').fill('Granada');
  await page.getByLabel('Presencial').check();
  await page.getByLabel('Indefinido').check();
  await page.getByLabel('Descripción').fill('Oferta creada por los tests de extremo a extremo.');

  // La vista previa muestra la tarjeta tal como la verán los candidatos
  const vistaPrevia = page.getByRole('complementary', { name: 'Vista previa' });
  await expect(vistaPrevia).toContainText(titulo);
  await expect(vistaPrevia).toContainText(empresa.nombre);
  await expect(vistaPrevia).toContainText('30–36 k€');

  await page.getByRole('button', { name: 'Publicar oferta' }).click();
  await expect(page).toHaveURL(/\/empresa$/);
  await expect(page.getByText(`"${titulo}" está publicada`)).toBeVisible();

  // Cualquiera la encuentra en el buscador
  await page.getByRole('button', { name: 'Salir' }).click();
  await page.goto(`/?texto=${encodeURIComponent(titulo)}`);
  const tarjetas = page.locator('app-tarjeta-oferta');
  await expect(tarjetas).toHaveCount(1);
  await expect(tarjetas).toContainText(empresa.nombre);
  await expect(tarjetas).toContainText('30–36 k€');
});
