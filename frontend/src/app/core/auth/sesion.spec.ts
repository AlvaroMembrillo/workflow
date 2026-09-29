import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { Sesion } from './sesion';

const CLAVE = 'workflow.token';

describe('Sesion', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('al entrar guarda el token y expone quién ha iniciado sesión', () => {
    const sesion = TestBed.inject(Sesion);
    const token = tokenDePrueba({ rol: 'EMPRESA', email: 'rrhh@acme.es' });

    sesion.entrar({ email: 'rrhh@acme.es', password: 'secreta123' }).subscribe();
    const peticion = http.expectOne('/api/auth/login');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 3600 });

    expect(sesion.iniciada()).toBe(true);
    expect(sesion.rol()).toBe('EMPRESA');
    expect(sesion.usuario()?.email).toBe('rrhh@acme.es');
    expect(localStorage.getItem(CLAVE)).toBe(token);
  });

  it('recupera la sesión guardada al recargar la página', () => {
    localStorage.setItem(CLAVE, tokenDePrueba({ email: 'ana@test.com' }));

    expect(TestBed.inject(Sesion).usuario()?.email).toBe('ana@test.com');
  });

  it('descarta un token guardado que ya ha caducado', () => {
    localStorage.setItem(CLAVE, tokenDePrueba({ caducaEn: -10 }));

    const sesion = TestBed.inject(Sesion);

    expect(sesion.iniciada()).toBe(false);
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });

  it('al cerrar la sesión borra el token', () => {
    localStorage.setItem(CLAVE, tokenDePrueba());
    const sesion = TestBed.inject(Sesion);

    sesion.cerrar();

    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(false);
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });

  it('cierra la sesión sola cuando caduca el token', () => {
    vi.useFakeTimers();
    localStorage.setItem(CLAVE, tokenDePrueba({ caducaEn: 60 }));
    const sesion = TestBed.inject(Sesion);
    expect(sesion.iniciada()).toBe(true);

    vi.advanceTimersByTime(60_000);

    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(true);
  });

  it('se pone al día cuando se inicia o se cierra sesión en otra pestaña', () => {
    const sesion = TestBed.inject(Sesion);

    window.dispatchEvent(
      new StorageEvent('storage', { key: CLAVE, newValue: tokenDePrueba({ rol: 'EMPRESA' }) }),
    );
    expect(sesion.rol()).toBe('EMPRESA');

    window.dispatchEvent(new StorageEvent('storage', { key: CLAVE, newValue: null }));
    expect(sesion.iniciada()).toBe(false);
  });
});
