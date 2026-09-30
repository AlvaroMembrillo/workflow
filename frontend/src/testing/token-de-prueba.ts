import { Rol } from '../app/core/auth/modelos';

export interface OpcionesToken {
  rol?: Rol;
  email?: string;
  id?: string;
  /** Segundos hasta que caduca; negativo para un token ya caducado. */
  caducaEn?: number;
}

/** JWT con la forma de los que emite el backend. La firma es falsa: el frontend no la comprueba. */
export function tokenDePrueba({
  rol = 'CANDIDATO',
  email = 'ana@test.com',
  id = '8f1c2a4e-0000-4000-8000-000000000001',
  caducaEn = 3600,
}: OpcionesToken = {}): string {
  const ahora = Math.floor(Date.now() / 1000);
  const cabecera = { alg: 'RS256' };
  const payload = {
    iss: 'workflow-api',
    sub: id,
    email,
    roles: [rol],
    iat: ahora,
    exp: ahora + caducaEn,
  };
  return `${base64Url(cabecera)}.${base64Url(payload)}.firma`;
}

function base64Url(valor: object): string {
  const bytes = new TextEncoder().encode(JSON.stringify(valor));
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}
