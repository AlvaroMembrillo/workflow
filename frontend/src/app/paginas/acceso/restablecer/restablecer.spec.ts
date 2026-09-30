import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { Restablecer } from './restablecer';

describe('Restablecer contraseña', () => {
  let fixture: ComponentFixture<Restablecer>;
  let backend: HttpTestingController;

  /** Abre la página como si se llegara desde el enlace del correo: /restablecer#token */
  async function abrir(token: string | null): Promise<void> {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Restablecer],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { fragment: token } } },
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Restablecer);
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function guardar(password: string): Promise<void> {
    const input = pagina().querySelector<HTMLInputElement>('#password')!;
    input.value = password;
    input.dispatchEvent(new Event('input'));
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  afterEach(() => backend.verify());

  it('envía el token del enlace con la contraseña nueva e invita a entrar', async () => {
    await abrir('token-del-correo');

    await guardar('una-clave-nueva');
    const peticion = backend.expectOne('/api/auth/restablecimiento');
    expect(peticion.request.body).toEqual({
      token: 'token-del-correo',
      password: 'una-clave-nueva',
    });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Contraseña cambiada');
    expect(pagina().querySelector('a[href="/entrar"]')).not.toBeNull();
  });

  it('no acepta contraseñas de menos de 8 caracteres', async () => {
    await abrir('token-del-correo');

    await guardar('corta');

    backend.expectNone('/api/auth/restablecimiento');
    expect(pagina().querySelector('#password-ayuda')!.textContent).toContain(
      'al menos 8 caracteres',
    );
  });

  it('si el enlace ha caducado o ya se usó, ofrece pedir otro', async () => {
    await abrir('token-gastado');

    await guardar('una-clave-nueva');
    backend
      .expectOne('/api/auth/restablecimiento')
      .flush(
        { title: 'Enlace no válido', status: 400, detail: 'El enlace no es válido' },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Este enlace ya no vale');
    expect(pagina().querySelector('a[href="/recuperar"]')).not.toBeNull();
  });

  it('sin token en la dirección no muestra el formulario', async () => {
    await abrir(null);

    expect(pagina().querySelector('form')).toBeNull();
    expect(pagina().querySelector('h1')!.textContent).toBe('Este enlace ya no vale');
  });
});
