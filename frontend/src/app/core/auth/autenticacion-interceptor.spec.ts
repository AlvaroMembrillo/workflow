import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { iniciarSesionDePrueba } from '../../../testing/sesion-de-prueba';
import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { autenticacionInterceptor } from './autenticacion-interceptor';
import { Sesion } from './sesion';

describe('autenticacionInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  const NO_AUTORIZADO = { status: 401, statusText: 'Unauthorized' };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([autenticacionInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('añade el token a las peticiones a la API', () => {
    const token = iniciarSesionDePrueba();

    http.get('/api/usuarios/me').subscribe();

    expect(backend.expectOne('/api/usuarios/me').request.headers.get('Authorization')).toBe(
      `Bearer ${token}`,
    );
  });

  it('no envía el token a otras webs', () => {
    iniciarSesionDePrueba();

    http.get('https://otra-web.example/api/datos').subscribe();

    expect(
      backend.expectOne('https://otra-web.example/api/datos').request.headers.has('Authorization'),
    ).toBe(false);
  });

  it('no envía el token de acceso a los endpoints que abren, renuevan o cierran la sesión', () => {
    iniciarSesionDePrueba();

    TestBed.inject(Sesion).renovar().subscribe();

    const peticion = backend.expectOne('/api/auth/refresco');
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({ accessToken: tokenDePrueba(), tokenType: 'Bearer', expiresIn: 900 });
  });

  it('si la API rechaza el token, pide otro y repite la petición sin que se note', () => {
    iniciarSesionDePrueba();
    let respuesta: unknown;

    http.get('/api/usuarios/me').subscribe((cuerpo) => (respuesta = cuerpo));
    backend.expectOne('/api/usuarios/me').flush(null, NO_AUTORIZADO);
    const nuevo = tokenDePrueba({ email: 'ana@test.com', caducaEn: 1800 });
    backend
      .expectOne('/api/auth/refresco')
      .flush({ accessToken: nuevo, tokenType: 'Bearer', expiresIn: 900 });
    const repetida = backend.expectOne('/api/usuarios/me');
    expect(repetida.request.headers.get('Authorization')).toBe(`Bearer ${nuevo}`);
    repetida.flush({ email: 'ana@test.com' });

    expect(respuesta).toEqual({ email: 'ana@test.com' });
    expect(TestBed.inject(Sesion).iniciada()).toBe(true);
  });

  it('si tampoco puede renovar, cierra la sesión por caducidad', () => {
    iniciarSesionDePrueba();
    const sesion = TestBed.inject(Sesion);
    let fallo: unknown;

    http.get('/api/usuarios/me').subscribe({ error: (error: unknown) => (fallo = error) });
    backend.expectOne('/api/usuarios/me').flush(null, NO_AUTORIZADO);
    backend.expectOne('/api/auth/refresco').flush(null, NO_AUTORIZADO);

    expect(fallo).toBeDefined();
    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(true);
  });

  it('si la petición repetida vuelve a fallar, no entra en un bucle', () => {
    iniciarSesionDePrueba();
    let fallo: unknown;

    http.get('/api/usuarios/me').subscribe({ error: (error: unknown) => (fallo = error) });
    backend.expectOne('/api/usuarios/me').flush(null, NO_AUTORIZADO);
    backend
      .expectOne('/api/auth/refresco')
      .flush({ accessToken: tokenDePrueba(), tokenType: 'Bearer', expiresIn: 900 });
    backend.expectOne('/api/usuarios/me').flush(null, NO_AUTORIZADO);

    expect(fallo).toBeDefined();
    backend.expectNone('/api/auth/refresco');
  });

  it('un 401 sin sesión (credenciales incorrectas) no intenta renovar nada', () => {
    const sesion = TestBed.inject(Sesion);

    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/login').flush(null, NO_AUTORIZADO);

    backend.expectNone('/api/auth/refresco');
    expect(sesion.caducada()).toBe(false);
  });
});
