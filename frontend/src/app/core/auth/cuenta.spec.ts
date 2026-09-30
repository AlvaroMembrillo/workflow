import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { usuarioDePrueba } from '../../../testing/datos-de-prueba';
import { iniciarSesionDePrueba } from '../../../testing/sesion-de-prueba';
import { Cuenta } from './cuenta';

describe('Cuenta', () => {
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  function volverALaPestana(): void {
    document.dispatchEvent(new Event('visibilitychange'));
    TestBed.tick();
  }

  it('sin sesión no pide nada', () => {
    const cuenta = TestBed.inject(Cuenta);
    TestBed.tick();

    backend.expectNone('/api/usuarios/me');
    expect(cuenta.emailSinVerificar()).toBe(false);
  });

  it('al volver a la pestaña comprueba si el email ya se ha confirmado en otra', async () => {
    iniciarSesionDePrueba();
    const cuenta = TestBed.inject(Cuenta);
    TestBed.tick();
    backend.expectOne('/api/usuarios/me').flush(usuarioDePrueba({ emailVerificado: false }));
    await new Promise((resolver) => setTimeout(resolver));
    expect(cuenta.emailSinVerificar()).toBe(true);

    volverALaPestana();
    backend.expectOne('/api/usuarios/me').flush(usuarioDePrueba({ emailVerificado: true }));
    await new Promise((resolver) => setTimeout(resolver));

    expect(cuenta.emailSinVerificar()).toBe(false);
  });

  it('con el email ya confirmado, volver a la pestaña no repite la petición', async () => {
    iniciarSesionDePrueba();
    TestBed.inject(Cuenta);
    TestBed.tick();
    backend.expectOne('/api/usuarios/me').flush(usuarioDePrueba());
    await new Promise((resolver) => setTimeout(resolver));

    volverALaPestana();

    backend.expectNone('/api/usuarios/me');
  });
});
