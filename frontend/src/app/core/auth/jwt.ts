import { Rol, UsuarioSesion } from './modelos';

const ROLES: readonly Rol[] = ['CANDIDATO', 'EMPRESA', 'ADMIN'];

/**
 * Lee los datos del usuario del payload de un JWT emitido por el backend.
 * No comprueba la firma: eso lo hace siempre el backend. Aquí solo sirve para adaptar la interfaz.
 * Devuelve null si el token no tiene el formato esperado.
 */
export function leerToken(token: string): UsuarioSesion | null {
  const payload = token.split('.')[1];
  if (!payload) {
    return null;
  }
  try {
    const datos: unknown = JSON.parse(decodificarBase64Url(payload));
    if (!esObjeto(datos)) {
      return null;
    }
    const { sub, email, exp, roles } = datos;
    const rol = Array.isArray(roles) ? roles[0] : undefined;
    if (typeof sub !== 'string' || typeof exp !== 'number' || !ROLES.includes(rol as Rol)) {
      return null;
    }
    return {
      id: sub,
      email: typeof email === 'string' ? email : '',
      rol: rol as Rol,
      expira: exp * 1000,
    };
  } catch {
    return null;
  }
}

function decodificarBase64Url(texto: string): string {
  const base64 = texto.replace(/-/g, '+').replace(/_/g, '/');
  const binario = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='));
  const bytes = Uint8Array.from(binario, (caracter) => caracter.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

function esObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null;
}
