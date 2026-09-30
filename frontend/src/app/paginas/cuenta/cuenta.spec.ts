import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { usuarioDePrueba } from '../../../testing/datos-de-prueba';
import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { Usuario } from '../../core/auth/modelos';
import { Cuenta } from './cuenta';

describe('Mi cuenta', () => {
  let fixture: ComponentFixture<Cuenta>;
  let backend: HttpTestingController;

  async function abrir(usuario: Usuario = usuarioDePrueba()): Promise<void> {
    localStorage.clear();
    localStorage.setItem('workflow.token', tokenDePrueba({ rol: usuario.rol }));
    TestBed.configureTestingModule({
      imports: [Cuenta],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Cuenta);
    TestBed.tick();
    backend.expectOne('/api/usuarios/me').flush(usuario);
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function escribir(id: string, valor: string): void {
    const campo = pagina().querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  async function cambiarPassword(actual: string, nueva: string): Promise<void> {
    escribir('passwordActual', actual);
    escribir('passwordNueva', nueva);
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  afterEach(() => backend.verify());

  it('muestra los datos y que el email está confirmado', async () => {
    await abrir();

    expect(pagina().querySelector('.datos')!.textContent).toContain('Ana García');
    expect(pagina().querySelector('.datos')!.textContent).toContain('ana@test.com');
    expect(pagina().querySelector('.marca')!.textContent).toContain('Confirmado');
    expect(pagina().textContent).not.toContain('Enviar otro enlace');
  });

  it('con el email sin confirmar permite pedir otro enlace', async () => {
    await abrir(usuarioDePrueba({ emailVerificado: false }));

    expect(pagina().querySelector('.marca')!.textContent).toContain('Sin confirmar');
    pagina().querySelector<HTMLButtonElement>('.fila button')!.click();
    backend
      .expectOne('/api/auth/verificacion/reenvio')
      .flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(pagina().textContent).toContain('Enviado. Si no lo ves, mira en la carpeta de spam.');
  });

  it('guarda la preferencia de avisos al marcar o desmarcar la casilla', async () => {
    await abrir();
    const casilla = pagina().querySelector<HTMLInputElement>('.casilla input')!;
    expect(casilla.checked).toBe(true);

    casilla.click();
    const peticion = backend.expectOne('/api/usuarios/me');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ avisosPorCorreo: false });
    peticion.flush(usuarioDePrueba({ avisosPorCorreo: false }));
    await fixture.whenStable();

    expect(pagina().textContent).toContain('Guardado: no te enviaremos avisos.');
    expect(pagina().querySelector<HTMLInputElement>('.casilla input')!.checked).toBe(false);
  });

  it('si no se puede guardar la preferencia, la casilla vuelve a como estaba', async () => {
    await abrir();
    const casilla = pagina().querySelector<HTMLInputElement>('.casilla input')!;

    casilla.click();
    backend
      .expectOne('/api/usuarios/me')
      .flush(null, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();

    expect(casilla.checked).toBe(true);
    expect(pagina().textContent).toContain('Algo ha fallado en el servidor');
  });

  it('explica a la empresa qué avisos recibe', async () => {
    await abrir(usuarioDePrueba({ rol: 'EMPRESA', nombre: 'Lumen Seguros' }));

    expect(pagina().querySelector('.casilla')!.textContent).toContain(
      'cuando alguien se inscriba en una de mis ofertas',
    );
  });

  it('cambia la contraseña y vacía el formulario', async () => {
    await abrir();

    await cambiarPassword('la-actual-123', 'la-nueva-12345');
    const peticion = backend.expectOne('/api/usuarios/me/password');
    expect(peticion.request.body).toEqual({
      passwordActual: 'la-actual-123',
      passwordNueva: 'la-nueva-12345',
    });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain('Contraseña cambiada');
    expect(pagina().querySelector<HTMLInputElement>('#passwordActual')!.value).toBe('');
    expect(pagina().querySelector('.error-campo')).toBeNull();
  });

  it('si la contraseña actual no es correcta, lo indica en ese campo', async () => {
    await abrir();

    await cambiarPassword('no-es-esta', 'la-nueva-12345');
    backend.expectOne('/api/usuarios/me/password').flush(
      {
        title: 'Datos no válidos',
        status: 400,
        errores: { passwordActual: 'La contraseña actual no es correcta' },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(pagina().querySelector('#passwordActual-error')!.textContent).toContain(
      'La contraseña actual no es correcta',
    );
    expect(document.activeElement).toBe(pagina().querySelector('#passwordActual'));
  });
});
