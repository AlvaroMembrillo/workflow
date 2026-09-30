import { expect, test } from '@playwright/test';

import { correoPara, crearCuenta, enlaceDe, entrar, unico } from './apoyo';

test('quien se registra confirma su email con el enlace del correo', async ({ page, request }) => {
  const email = `${unico('nueva')}@e2e.test`;

  await page.goto('/registro');
  await page.getByLabel('Nombre', { exact: true }).fill('Nueva E2E');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill('clave-e2e-segura');
  await page.getByRole('button', { name: 'Crear cuenta' }).click();

  // Hasta que lo confirme, la aplicación se lo recuerda en todas las páginas
  const aviso = page.getByRole('complementary', { name: 'Email sin confirmar' });
  await expect(aviso).toContainText(`Te hemos enviado un enlace a ${email}`);

  const correo = await correoPara(request, email, 'Confirma tu email en Workflow');
  expect(correo).toContain('Hola, Nueva E2E');
  await page.goto(enlaceDe(correo, '/verificar-email'));

  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Email confirmado');
  await expect(aviso).toBeHidden();
  // El token desaparece de la barra de direcciones
  await expect(page).toHaveURL(/\/verificar-email$/);

  await page.getByRole('link', { name: `Mi cuenta: ${email}` }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Mi cuenta');
  await expect(page.locator('.datos')).toContainText('Confirmado');
});

test('quien olvida su contraseña la cambia con el enlace del correo', async ({ page, request }) => {
  const cuenta = await crearCuenta(request, 'CANDIDATO');
  const nueva = 'otra-clave-e2e-segura';

  await page.goto('/entrar');
  await page.getByRole('link', { name: '¿Has olvidado tu contraseña?' }).click();
  // La pantalla de acceso también tiene un campo "Email": se espera a la nueva antes de escribir
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Recupera tu contraseña');
  await page.getByLabel('Email').fill(cuenta.email);
  await page.getByRole('button', { name: 'Enviar enlace' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Revisa tu correo');

  const correo = await correoPara(request, cuenta.email, 'Cambia tu contraseña de Workflow');
  const enlace = enlaceDe(correo, '/restablecer');
  await page.goto(enlace);
  await page.getByLabel('Contraseña nueva').fill(nueva);
  await page.getByRole('button', { name: 'Guardar contraseña' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Contraseña cambiada');

  // La contraseña antigua deja de valer y la nueva funciona
  await page.getByRole('link', { name: 'Entrar' }).last().click();
  await page.getByLabel('Email').fill(cuenta.email);
  await page.getByLabel('Contraseña', { exact: true }).fill(cuenta.password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('alert')).toContainText('El email o la contraseña no son correctos');
  await entrar(page, cuenta.email, nueva);

  // El enlace solo se puede usar una vez
  await page.getByRole('button', { name: 'Salir' }).click();
  await page.goto(enlace);
  await page.getByLabel('Contraseña nueva').fill('una-tercera-clave');
  await page.getByRole('button', { name: 'Guardar contraseña' }).click();
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Este enlace ya no vale');
});

test('la empresa y el candidato reciben un correo con cada novedad de una candidatura', async ({
  page,
  request,
}) => {
  // Los avisos solo se envían a emails confirmados
  const verificar = async (email: string) => {
    const correo = await correoPara(request, email, 'Confirma tu email en Workflow');
    const token = enlaceDe(correo, '/verificar-email').split('#')[1];
    expect((await request.post('/api/auth/verificacion', { data: { token } })).status()).toBe(204);
  };
  const empresa = await crearCuenta(request, 'EMPRESA');
  const candidato = await crearCuenta(request, 'CANDIDATO');
  await verificar(empresa.email);
  await verificar(candidato.email);

  const titulo = `Oferta ${unico('avisos')}`;
  const oferta = await request.post('/api/ofertas', {
    headers: { Authorization: `Bearer ${empresa.token}` },
    data: {
      titulo,
      descripcion: 'Oferta para probar los avisos por correo.',
      ubicacion: 'Cádiz',
      modalidad: 'REMOTO',
      tipoContrato: 'INDEFINIDO',
      salarioMinimo: 30000,
      salarioMaximo: 35000,
    },
  });
  const { id: ofertaId } = (await oferta.json()) as { id: string };
  await request.post(`/api/ofertas/${ofertaId}/candidaturas`, {
    headers: { Authorization: `Bearer ${candidato.token}` },
    data: {},
  });

  // La empresa recibe el aviso y su enlace lleva a las candidaturas de la oferta
  const avisoEmpresa = await correoPara(request, empresa.email, `Nueva candidatura para ${titulo}`);
  expect(avisoEmpresa).toContain(`${candidato.nombre} se ha inscrito en tu oferta`);
  await entrar(page, empresa.email, empresa.password);
  await page.goto(`/empresa/ofertas/${ofertaId}/candidaturas`);
  await page
    .locator('li.candidatura', { hasText: candidato.nombre })
    .getByRole('button', { name: 'Pasar a revisión' })
    .click();

  const avisoCandidato = await correoPara(
    request,
    candidato.email,
    `Tu candidatura a ${titulo} está en revisión`,
  );
  expect(avisoCandidato).toContain(`${empresa.nombre} está revisando tu candidatura`);
});
