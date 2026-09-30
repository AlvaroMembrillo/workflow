import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { usuarioDePrueba } from '../../../../testing/datos-de-prueba';
import { tokenDePrueba } from '../../../../testing/token-de-prueba';
import { VerificarEmail } from './verificar-email';

describe('Verificar email', () => {
  let fixture: ComponentFixture<VerificarEmail>;
  let backend: HttpTestingController;

  /** Abre la página como si se llegara desde el enlace del correo: /verificar-email#token */
  function abrir(token: string | null, conSesion = false): void {
    localStorage.clear();
    if (conSesion) {
      localStorage.setItem('workflow.token', tokenDePrueba());
    }
    TestBed.configureTestingModule({
      imports: [VerificarEmail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { fragment: token } } },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(VerificarEmail);
    TestBed.tick();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => backend.verify());

  it('confirma el email con el token del enlace nada más abrirse', async () => {
    abrir('token-del-correo');

    const peticion = backend.expectOne('/api/auth/verificacion');
    expect(peticion.request.body).toEqual({ token: 'token-del-correo' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Email confirmado');
    expect(pagina().querySelector('a[href="/entrar"]')).not.toBeNull();
  });

  it('con sesión, vuelve a pedir los datos de la cuenta para quitar el aviso', async () => {
    abrir('token-del-correo', true);
    backend.expectOne('/api/usuarios/me').flush(usuarioDePrueba({ emailVerificado: false }));

    backend
      .expectOne('/api/auth/verificacion')
      .flush(null, { status: 204, statusText: 'No Content' });
    TestBed.tick();
    backend.expectOne('/api/usuarios/me').flush(usuarioDePrueba());
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Email confirmado');
    expect(pagina().querySelector('a.btn')!.textContent).toContain('Continuar');
  });

  it('si el enlace no vale y hay sesión, permite pedir otro', async () => {
    abrir('token-caducado', true);
    backend.expectOne('/api/usuarios/me').flush(usuarioDePrueba({ emailVerificado: false }));
    backend
      .expectOne('/api/auth/verificacion')
      .flush({ title: 'Enlace no válido' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Este enlace ya no vale');
    pagina().querySelector<HTMLButtonElement>('button.btn')!.click();
    backend
      .expectOne('/api/auth/verificacion/reenvio')
      .flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(pagina().querySelector('[role="status"]')!.textContent).toContain('Enviado');
  });

  it('sin token y sin sesión, invita a entrar para pedir otro enlace', async () => {
    abrir(null);
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Este enlace ya no vale');
    expect(pagina().querySelector('a[href="/entrar?volver=%2Fcuenta"]')).not.toBeNull();
  });
});
