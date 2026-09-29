import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { leerToken } from './jwt';
import { Rol } from './modelos';

describe('leerToken', () => {
  it('lee el id, el email, el rol y la caducidad del token', () => {
    const token = tokenDePrueba({ id: 'abc', email: 'ana@test.com', rol: 'EMPRESA', caducaEn: 60 });

    const usuario = leerToken(token);

    expect(usuario).toEqual({
      id: 'abc',
      email: 'ana@test.com',
      rol: 'EMPRESA',
      expira: expect.any(Number),
    });
    expect(usuario!.expira).toBeGreaterThan(Date.now());
  });

  it('lee bien los caracteres no ASCII', () => {
    expect(leerToken(tokenDePrueba({ email: 'maría.núñez@correo.es' }))?.email).toBe(
      'maría.núñez@correo.es',
    );
  });

  it('devuelve null si el texto no tiene formato de JWT', () => {
    expect(leerToken('no-es-un-token')).toBeNull();
    expect(leerToken('a.b.c')).toBeNull();
  });

  it('devuelve null si el rol no es uno de los de la aplicación', () => {
    expect(leerToken(tokenDePrueba({ rol: 'SUPERUSUARIO' as Rol }))).toBeNull();
  });
});
