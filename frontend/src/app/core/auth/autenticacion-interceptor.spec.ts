import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { autenticacionInterceptor } from './autenticacion-interceptor';
import { Sesion } from './sesion';

describe('autenticacionInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  function conSesion(): string {
    const token = tokenDePrueba();
    localStorage.setItem('workflow.token', token);
    return token;
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([autenticacionInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
      ],
    });
  });

  function preparar(): void {
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  }

  afterEach(() => backend.verify());

  it('añade el token a las peticiones a la API', () => {
    const token = conSesion();
    preparar();

    http.get('/api/usuarios/me').subscribe();

    expect(backend.expectOne('/api/usuarios/me').request.headers.get('Authorization')).toBe(
      `Bearer ${token}`,
    );
  });

  it('no envía el token a otras webs', () => {
    conSesion();
    preparar();

    http.get('https://otra-web.example/api/datos').subscribe();

    expect(
      backend.expectOne('https://otra-web.example/api/datos').request.headers.has('Authorization'),
    ).toBe(false);
  });

  it('si la API rechaza el token, cierra la sesión por caducidad', () => {
    conSesion();
    preparar();
    const sesion = TestBed.inject(Sesion);

    http.get('/api/usuarios/me').subscribe({ error: () => undefined });
    backend.expectOne('/api/usuarios/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(true);
  });

  it('un 401 sin token (credenciales incorrectas) no se trata como sesión caducada', () => {
    preparar();
    const sesion = TestBed.inject(Sesion);

    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(sesion.caducada()).toBe(false);
  });
});
