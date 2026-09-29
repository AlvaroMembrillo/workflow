import { APIRequestContext, expect, Page } from '@playwright/test';

/** Contraseña de las cuentas de ejemplo de la demo (ver README). */
export const PASSWORD_DEMO = 'demo-workflow';

export interface Cuenta {
  email: string;
  password: string;
  nombre: string;
  token: string;
}

/** Texto distinto en cada ejecución, para que los tests no choquen con datos de ejecuciones anteriores. */
export function unico(prefijo: string): string {
  return `${prefijo}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

/** Crea una cuenta por la API (más rápido que por la interfaz cuando no es lo que se prueba). */
export async function crearCuenta(
  request: APIRequestContext,
  rol: 'CANDIDATO' | 'EMPRESA',
): Promise<Cuenta> {
  const nombre = rol === 'CANDIDATO' ? `Candidato ${unico('e2e')}` : `Empresa ${unico('e2e')}`;
  const email = `${unico('e2e')}@e2e.test`;
  const password = 'clave-e2e-segura';
  const respuesta = await request.post('/api/auth/registro', {
    data: { email, password, nombre, rol },
  });
  expect(respuesta.status()).toBe(201);
  const { accessToken } = (await respuesta.json()) as { accessToken: string };
  return { email, password, nombre, token: accessToken };
}

/** Busca una oferta abierta por su título exacto. */
export async function ofertaPorTitulo(
  request: APIRequestContext,
  titulo: string,
): Promise<{ id: string; titulo: string }> {
  const respuesta = await request.get('/api/ofertas', { params: { texto: titulo, size: 50 } });
  const pagina = (await respuesta.json()) as { content: { id: string; titulo: string }[] };
  const oferta = pagina.content.find((candidata) => candidata.titulo === titulo);
  expect(oferta, `La demo debería tener la oferta "${titulo}"`).toBeDefined();
  return oferta!;
}

/** Entra por la pantalla de acceso, como lo haría una persona. */
export async function entrar(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/entrar');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
}
