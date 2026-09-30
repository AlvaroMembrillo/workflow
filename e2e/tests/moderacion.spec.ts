import { APIRequestContext, expect, test } from '@playwright/test';

import { crearCuenta, Cuenta, entrar, PASSWORD_DEMO, unico } from './apoyo';

const ADMIN = 'admin@demo.test';

async function publicarOferta(
  request: APIRequestContext,
  empresa: Cuenta,
  titulo: string,
): Promise<string> {
  const respuesta = await request.post('/api/ofertas', {
    headers: { Authorization: `Bearer ${empresa.token}` },
    data: {
      titulo,
      descripcion: 'Trabajo desde casa sin experiencia. Solo tienes que pagar el material.',
      ubicacion: 'Toda España',
      modalidad: 'REMOTO',
      tipoContrato: 'FREELANCE',
      salarioMinimo: 36000,
      salarioMaximo: 36000,
    },
  });
  expect(respuesta.status()).toBe(201);
  return ((await respuesta.json()) as { id: string }).id;
}

test('un candidato denuncia una oferta, moderación la retira y la empresa lo ve', async ({
  page,
  request,
}) => {
  const empresa = await crearCuenta(request, 'EMPRESA');
  const candidato = await crearCuenta(request, 'CANDIDATO');
  const titulo = `Gana dinero fácil ${unico('e2e')}`;
  const ofertaId = await publicarOferta(request, empresa, titulo);

  // El candidato la denuncia desde la ficha de la oferta
  await entrar(page, candidato.email, candidato.password);
  await page.goto(`/ofertas/${ofertaId}`);
  await page.getByRole('button', { name: 'Denunciar esta oferta' }).click();
  const formulario = page.getByRole('dialog', { name: 'Denunciar esta oferta' });
  await formulario.getByRole('button', { name: 'Enviar denuncia' }).click();
  await expect(formulario).toContainText('Elige un motivo');
  await formulario.getByLabel('Parece una estafa o pide dinero').check();
  await formulario.getByLabel('Cuéntanos más').fill('Piden 200 € por adelantado para el material.');
  await formulario.getByRole('button', { name: 'Enviar denuncia' }).click();
  await expect(page.getByRole('dialog', { name: 'Gracias por avisar' })).toBeVisible();
  await page.getByRole('button', { name: 'Cerrar' }).click();
  await page.getByRole('button', { name: 'Salir' }).click();

  // Moderación la revisa y la retira, con confirmación
  await entrar(page, ADMIN, PASSWORD_DEMO);
  await expect(page).toHaveURL(/\/admin$/);
  const denuncia = page.locator('li.denuncia', { hasText: titulo });
  await expect(denuncia).toContainText('Parece una estafa o pide dinero');
  await expect(denuncia).toContainText('Piden 200 € por adelantado para el material.');
  await expect(denuncia).toContainText(candidato.email);
  await denuncia.getByRole('button', { name: 'Retirar oferta' }).click();
  const confirmacion = page.getByRole('dialog', { name: `¿Retirar la oferta "${titulo}"?` });
  await confirmacion.getByRole('button', { name: 'Retirar oferta' }).click();
  await expect(page.getByText(`Oferta "${titulo}" retirada.`)).toBeVisible();
  await expect(denuncia).toBeHidden();
  await page.getByRole('button', { name: 'Salir' }).click();

  // Ya no está en el buscador ni se puede abrir
  await page.goto(`/?texto=${encodeURIComponent(titulo)}`);
  await expect(page.locator('app-tarjeta-oferta')).toHaveCount(0);
  await page.goto(`/ofertas/${ofertaId}`);
  await expect(page.getByText('Esta oferta no existe')).toBeVisible();

  // La empresa la ve retirada y sin poder reabrirla
  await entrar(page, empresa.email, empresa.password);
  const fila = page.locator('li.fila', { hasText: titulo });
  await expect(fila).toContainText('Retirada');
  await expect(fila).toContainText('La hemos retirado por incumplir las normas');
  await expect(fila.getByRole('button', { name: 'Reabrir' })).toHaveCount(0);
});

test('moderación suspende una empresa y esta ya no puede entrar', async ({ page, request }) => {
  const empresa = await crearCuenta(request, 'EMPRESA');
  const candidato = await crearCuenta(request, 'CANDIDATO');
  const titulo = `Inversión garantizada ${unico('e2e')}`;
  const ofertaId = await publicarOferta(request, empresa, titulo);
  const denuncia = await request.post(`/api/ofertas/${ofertaId}/denuncias`, {
    headers: { Authorization: `Bearer ${candidato.token}` },
    data: { motivo: 'FRAUDE', detalle: null },
  });
  expect(denuncia.status()).toBe(201);

  await entrar(page, ADMIN, PASSWORD_DEMO);
  await page
    .locator('li.denuncia', { hasText: titulo })
    .getByRole('button', { name: 'Suspender empresa' })
    .click();
  const confirmacion = page.getByRole('dialog', {
    name: `¿Suspender la cuenta de ${empresa.nombre}?`,
  });
  await expect(confirmacion).toContainText('Se retirarán todas sus ofertas');
  await confirmacion.getByRole('button', { name: 'Suspender cuenta' }).click();
  await expect(page.getByText(`Cuenta de ${empresa.nombre} suspendida`)).toBeVisible();
  await page.getByRole('button', { name: 'Salir' }).click();

  await page.goto('/entrar');
  await page.getByLabel('Email').fill(empresa.email);
  await page.getByLabel('Contraseña', { exact: true }).fill(empresa.password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('alert')).toContainText('Hemos suspendido esta cuenta');
});
