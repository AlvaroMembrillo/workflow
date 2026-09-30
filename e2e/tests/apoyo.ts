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

/** Un PDF mínimo: la API solo comprueba que el fichero empiece como un PDF. */
export const PDF_DE_PRUEBA = Buffer.from('%PDF-1.4\n% currículum de prueba de los tests E2E\n');

/** Sube por la API el currículum de un candidato. */
export async function subirCv(
  request: APIRequestContext,
  candidato: Cuenta,
  nombre: string,
): Promise<void> {
  const respuesta = await request.post('/api/candidatos/me/cv', {
    headers: { Authorization: `Bearer ${candidato.token}` },
    multipart: { fichero: { name: nombre, mimeType: 'application/pdf', buffer: PDF_DE_PRUEBA } },
  });
  expect(respuesta.status()).toBe(200);
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

/** Mailpit recoge los correos que envía la demo; aquí se leen como haría el usuario en su bandeja. */
const URL_CORREO = process.env['URL_CORREO'] ?? 'http://localhost:8026';

/**
 * Espera el último correo enviado a una dirección con ese asunto y devuelve su texto.
 * El envío es asíncrono, así que puede tardar un momento en llegar.
 */
export async function correoPara(
  request: APIRequestContext,
  email: string,
  asunto: string,
): Promise<string> {
  let id: string | undefined;
  await expect
    .poll(
      async () => {
        const respuesta = await request.get(`${URL_CORREO}/api/v1/search`, {
          params: { query: `to:${email} subject:"${asunto}"` },
        });
        const { messages } = (await respuesta.json()) as { messages: { ID: string }[] };
        id = messages[0]?.ID;
        return id;
      },
      { message: `Debería llegar a ${email} el correo "${asunto}"` },
    )
    .toBeDefined();
  const mensaje = await request.get(`${URL_CORREO}/api/v1/message/${id}`);
  return ((await mensaje.json()) as { Text: string }).Text;
}

/** El enlace a la aplicación que contiene un correo, sin el dominio (para usarlo con page.goto). */
export function enlaceDe(texto: string, ruta: string): string {
  const enlace = texto.match(new RegExp(`https?://[^\\s/]+(${ruta}#[\\w-]+)`));
  expect(enlace, `El correo debería tener un enlace a ${ruta}`).not.toBeNull();
  return enlace![1];
}
