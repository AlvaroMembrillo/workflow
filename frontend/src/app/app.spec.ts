import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { usuarioDePrueba } from '../testing/datos-de-prueba';
import { iniciarSesionDePrueba } from '../testing/sesion-de-prueba';
import { OpcionesToken } from '../testing/token-de-prueba';
import { App } from './app';

describe('App', () => {
  /**
   * Con `sesion`, la aplicación se abre con esa sesión iniciada y pide los datos de la cuenta, que se
   * responden con `cuenta`.
   */
  async function renderizar(
    sesion?: OpcionesToken,
    cuenta = usuarioDePrueba(),
  ): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    if (sesion) {
      iniciarSesionDePrueba(sesion);
    }
    const fixture = TestBed.createComponent(App);
    TestBed.tick();
    const backend = TestBed.inject(HttpTestingController);
    backend.match('/api/usuarios/me').forEach((peticion) => peticion.flush(cuenta));
    await fixture.whenStable();
    backend.verify();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => localStorage.clear());

  it('tiene un enlace para saltar directamente al contenido', async () => {
    const pagina = await renderizar();

    const saltar = pagina.querySelector<HTMLAnchorElement>('a.saltar')!;
    expect(saltar.getAttribute('href')).toBe('#contenido');
    expect(pagina.querySelector('main#contenido')).not.toBeNull();
  });

  it('sin sesión ofrece entrar, crear cuenta y la entrada para empresas', async () => {
    const pagina = await renderizar();

    const textos = [...pagina.querySelectorAll('header a')].map((enlace) =>
      enlace.textContent?.trim(),
    );
    expect(textos).toEqual(
      expect.arrayContaining(['Ofertas', 'Para empresas', 'Entrar', 'Crear cuenta']),
    );
  });

  it('con sesión de candidato muestra sus candidaturas, su email y el botón de salir', async () => {
    const pagina = await renderizar({ rol: 'CANDIDATO', email: 'ana@test.com' });

    expect(pagina.querySelector('header')!.textContent).toContain('Mis candidaturas');
    expect(pagina.querySelector('header')!.textContent).toContain('ana@test.com');
    expect(pagina.querySelector('header button')!.textContent).toContain('Salir');
    expect(pagina.querySelector('header a[href="/cuenta"]')!.getAttribute('aria-label')).toBe(
      'Mi cuenta: ana@test.com',
    );
    expect(pagina.textContent).not.toContain('Confirma tu email');
  });

  it('recuerda confirmar el email a quien todavía no lo ha hecho', async () => {
    const pagina = await renderizar({}, usuarioDePrueba({ emailVerificado: false }));

    const aviso = pagina.querySelector('aside[aria-label="Email sin confirmar"]')!;
    expect(aviso.textContent).toContain('Te hemos enviado un enlace a ana@test.com');
    expect(aviso.querySelector('button')!.textContent).toContain('Enviar otro enlace');
  });
});
