import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { iniciarSesionDePrueba } from '../../../testing/sesion-de-prueba';
import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { Sesion } from './sesion';

const PISTA = 'workflow.sesion';

describe('Sesion', () => {
  let http: HttpTestingController;

  function responderRefresco(token: string): void {
    http
      .expectOne('/api/auth/refresco')
      .flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 900 });
  }

  function rechazarRefresco(): void {
    http
      .expectOne('/api/auth/refresco')
      .flush({ title: 'Sesión no válida' }, { status: 401, statusText: 'Unauthorized' });
  }

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

  it('al entrar expone quién ha iniciado sesión sin guardar el token en el navegador', () => {
    const sesion = TestBed.inject(Sesion);
    const token = tokenDePrueba({ rol: 'EMPRESA', email: 'rrhh@acme.es' });

    sesion.entrar({ email: 'rrhh@acme.es', password: 'secreta123' }).subscribe();
    const peticion = http.expectOne('/api/auth/login');
    expect(peticion.request.method).toBe('POST');
    peticion.flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 900 });

    expect(sesion.iniciada()).toBe(true);
    expect(sesion.rol()).toBe('EMPRESA');
    expect(sesion.usuario()?.email).toBe('rrhh@acme.es');
    // Solo queda la marca de que hay sesión: el token vive en memoria
    expect(localStorage.getItem(PISTA)).toBe('1');
    expect(JSON.stringify({ ...localStorage })).not.toContain(token);
  });

  it('al abrir la web recupera la sesión con la cookie de refresco', () => {
    localStorage.setItem(PISTA, '1');
    const sesion = TestBed.inject(Sesion);
    let haySesion: boolean | undefined;

    sesion.restaurar().subscribe((resultado) => (haySesion = resultado));
    responderRefresco(tokenDePrueba({ email: 'ana@test.com' }));

    expect(haySesion).toBe(true);
    expect(sesion.usuario()?.email).toBe('ana@test.com');
  });

  it('quien nunca ha iniciado sesión no hace ninguna petición al abrir la web', () => {
    let haySesion: boolean | undefined;

    TestBed.inject(Sesion)
      .restaurar()
      .subscribe((resultado) => (haySesion = resultado));

    expect(haySesion).toBe(false);
    http.expectNone('/api/auth/refresco');
  });

  it('si la sesión caducó mientras la web estaba cerrada, lo recuerda para explicarlo', () => {
    localStorage.setItem(PISTA, '1');
    const sesion = TestBed.inject(Sesion);
    let haySesion: boolean | undefined;

    sesion.restaurar().subscribe((resultado) => (haySesion = resultado));
    rechazarRefresco();

    expect(haySesion).toBe(false);
    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(true);
    expect(localStorage.getItem(PISTA)).toBeNull();
  });

  it('sin conexión al abrir la web, conserva la marca para recuperar la sesión más tarde', () => {
    localStorage.setItem(PISTA, '1');
    const sesion = TestBed.inject(Sesion);

    sesion.restaurar().subscribe();
    http.expectOne('/api/auth/refresco').error(new ProgressEvent('error'));

    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(false);
    expect(localStorage.getItem(PISTA)).toBe('1');
  });

  it('al cerrar la sesión olvida el token e invalida la cookie en el servidor', () => {
    iniciarSesionDePrueba();
    const sesion = TestBed.inject(Sesion);

    sesion.cerrar();

    expect(http.expectOne('/api/auth/salida').request.method).toBe('POST');
    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(false);
    expect(localStorage.getItem(PISTA)).toBeNull();
  });

  it('renueva el token de acceso poco antes de que caduque', () => {
    vi.useFakeTimers();
    iniciarSesionDePrueba({ caducaEn: 900, email: 'ana@test.com' });
    const sesion = TestBed.inject(Sesion);

    vi.advanceTimersByTime(869_000);
    http.expectNone('/api/auth/refresco');

    vi.advanceTimersByTime(1_000);
    const nuevo = tokenDePrueba({ caducaEn: 900, email: 'ana@test.com' });
    responderRefresco(nuevo);

    expect(sesion.token()).toBe(nuevo);
    expect(sesion.iniciada()).toBe(true);
  });

  it('si no puede renovar porque la sesión ha terminado, la cierra por caducidad', () => {
    vi.useFakeTimers();
    iniciarSesionDePrueba({ caducaEn: 900 });
    const sesion = TestBed.inject(Sesion);

    vi.advanceTimersByTime(870_000);
    rechazarRefresco();

    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(true);
  });

  it('sin conexión al renovar, mantiene la sesión hasta que caduca el token', () => {
    vi.useFakeTimers();
    iniciarSesionDePrueba({ caducaEn: 900 });
    const sesion = TestBed.inject(Sesion);

    vi.advanceTimersByTime(870_000);
    http.expectOne('/api/auth/refresco').error(new ProgressEvent('error'));
    expect(sesion.iniciada()).toBe(true);

    vi.advanceTimersByTime(30_000);
    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(true);
  });

  it('varias renovaciones a la vez comparten una sola petición', () => {
    iniciarSesionDePrueba();
    const sesion = TestBed.inject(Sesion);
    const recibidos: string[] = [];

    sesion.renovar().subscribe((token) => recibidos.push(token));
    sesion.renovar().subscribe((token) => recibidos.push(token));
    const nuevo = tokenDePrueba({ email: 'otra@test.com' });
    responderRefresco(nuevo);

    expect(recibidos).toEqual([nuevo, nuevo]);
  });

  it('se pone al día cuando se inicia o se cierra sesión en otra pestaña', () => {
    const sesion = TestBed.inject(Sesion);

    window.dispatchEvent(new StorageEvent('storage', { key: PISTA, newValue: '1' }));
    localStorage.setItem(PISTA, '1');
    window.dispatchEvent(new StorageEvent('storage', { key: PISTA, newValue: '1' }));
    responderRefresco(tokenDePrueba({ rol: 'EMPRESA' }));
    expect(sesion.rol()).toBe('EMPRESA');

    window.dispatchEvent(new StorageEvent('storage', { key: PISTA, newValue: null }));
    expect(sesion.iniciada()).toBe(false);
    expect(sesion.caducada()).toBe(false);
  });
});
