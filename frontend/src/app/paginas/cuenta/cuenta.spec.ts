import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { asentar } from '../../../testing/asentar';
import { usuarioDePrueba } from '../../../testing/datos-de-prueba';
import { iniciarSesionDePrueba } from '../../../testing/sesion-de-prueba';
import { Usuario } from '../../core/auth/modelos';
import { Sesion } from '../../core/auth/sesion';
import { Cuenta } from './cuenta';

describe('Mi cuenta', () => {
  let fixture: ComponentFixture<Cuenta>;
  let backend: HttpTestingController;

  async function abrir(usuario: Usuario = usuarioDePrueba()): Promise<void> {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Cuenta],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    iniciarSesionDePrueba({ rol: usuario.rol });
    fixture = TestBed.createComponent(Cuenta);
    TestBed.tick();
    backend.expectOne('/api/usuarios/me').flush(usuario);
    // Los candidatos ven además su currículum
    await asentar();
    backend
      .match('/api/candidatos/me/cv')
      .forEach((peticion) =>
        peticion.flush({ title: 'Sin currículum' }, { status: 404, statusText: 'Not Found' }),
      );
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function boton(texto: string): HTMLButtonElement {
    return [...pagina().querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.replace(/\s+/g, ' ').trim() === texto,
    )!;
  }

  function escribir(id: string, valor: string): void {
    const campo = pagina().querySelector<HTMLInputElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  async function cambiarPassword(actual: string, nueva: string): Promise<void> {
    escribir('passwordActual', actual);
    escribir('passwordNueva', nueva);
    pagina().querySelector('form.formulario')!.dispatchEvent(new Event('submit'));
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

  it('el candidato gestiona su currículum desde su cuenta', async () => {
    await abrir();

    expect(pagina().querySelector('#titulo-cv')!.textContent).toBe('Tu currículum');
    expect(pagina().querySelector('app-cv-candidato')!.textContent).toContain('Subir currículum');
  });

  it('explica a la empresa qué avisos recibe y no le pide currículum', async () => {
    await abrir(usuarioDePrueba({ rol: 'EMPRESA', nombre: 'Lumen Seguros' }));

    expect(pagina().querySelector('app-cv-candidato')).toBeNull();

    expect(pagina().querySelector('.casilla')!.textContent).toContain(
      'cuando alguien se inscriba en una de mis ofertas',
    );
  });

  it('corrige el nombre sin salir de la página', async () => {
    await abrir();

    boton('Cambiar el nombre').click();
    await fixture.whenStable();
    const campo = pagina().querySelector<HTMLInputElement>('#nombre')!;
    expect(campo.value).toBe('Ana García');
    campo.value = ' Ana García López ';
    pagina().querySelector('form.editar-nombre')!.dispatchEvent(new Event('submit'));
    const peticion = backend.expectOne('/api/usuarios/me');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ nombre: 'Ana García López' });
    peticion.flush(usuarioDePrueba({ nombre: 'Ana García López' }));
    await fixture.whenStable();

    expect(pagina().querySelector('.nombre')!.textContent).toContain('Ana García López');
    expect(pagina().querySelector('form.editar-nombre')).toBeNull();
    expect(pagina().textContent).toContain('Nombre guardado.');
  });

  it('descarga una copia de los datos en un fichero', async () => {
    const pulsarEnlace = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        expect(this.download).toBe('workflow-mis-datos.json');
      });
    await abrir();

    boton('Descargar').click();
    backend.expectOne('/api/usuarios/me/datos').flush(new Blob(['{}']));
    await fixture.whenStable();

    expect(pulsarEnlace).toHaveBeenCalledOnce();
    pulsarEnlace.mockRestore();
  });

  it('borrar la cuenta explica las consecuencias y pide la contraseña', async () => {
    await abrir();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    boton('Borrar mi cuenta').click();
    await fixture.whenStable();
    const dialogo = pagina().querySelector<HTMLDialogElement>('dialog.baja')!;
    expect(dialogo.open).toBe(true);
    expect(dialogo.textContent).toContain('Se borrarán tu currículum y todas tus candidaturas');
    expect(dialogo.textContent).toContain('No se puede deshacer');

    // Sin contraseña no se envía nada
    dialogo.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    backend.expectNone('/api/usuarios/me/baja');
    expect(dialogo.querySelector('#baja-password-error')!.textContent).toContain(
      'Escribe tu contraseña',
    );

    dialogo.querySelector<HTMLInputElement>('#baja-password')!.value = 'mi-clave-123';
    dialogo.querySelector('form')!.dispatchEvent(new Event('submit'));
    const peticion = backend.expectOne('/api/usuarios/me/baja');
    expect(peticion.request.body).toEqual({ password: 'mi-clave-123' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(TestBed.inject(Sesion).iniciada()).toBe(false);
    expect(navegar).toHaveBeenCalledWith('/cuenta-borrada');
    backend.expectNone('/api/auth/salida');
  });

  it('si la contraseña para borrar la cuenta no es correcta, lo dice y no cierra la sesión', async () => {
    await abrir(usuarioDePrueba({ rol: 'EMPRESA' }));

    boton('Borrar mi cuenta').click();
    await fixture.whenStable();
    const dialogo = pagina().querySelector<HTMLDialogElement>('dialog.baja')!;
    expect(dialogo.textContent).toContain('todas tus ofertas y las candidaturas que has recibido');
    dialogo.querySelector<HTMLInputElement>('#baja-password')!.value = 'no-es-esta';
    dialogo.querySelector('form')!.dispatchEvent(new Event('submit'));
    backend
      .expectOne('/api/usuarios/me/baja')
      .flush(
        { title: 'Datos no válidos', errores: { password: 'La contraseña no es correcta' } },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(dialogo.querySelector('#baja-password-error')!.textContent).toBe(
      'La contraseña no es correcta',
    );
    expect(TestBed.inject(Sesion).iniciada()).toBe(true);
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
