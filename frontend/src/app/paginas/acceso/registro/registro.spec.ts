import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { tokenDePrueba } from '../../../../testing/token-de-prueba';
import { Registro } from './registro';

describe('Registro', () => {
  let fixture: ComponentFixture<Registro>;
  let backend: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  async function crear(entradas: { tipo?: string; volver?: string } = {}): Promise<void> {
    fixture = TestBed.createComponent(Registro);
    for (const [nombre, valor] of Object.entries(entradas)) {
      fixture.componentRef.setInput(nombre, valor);
    }
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function escribir(id: string, valor: string): void {
    const input = pagina().querySelector<HTMLInputElement>(`#${id}`)!;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  async function enviar(): Promise<void> {
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  async function rellenarYEnviar(): Promise<void> {
    escribir('nombre', 'Acme Software');
    escribir('email', 'rrhh@acme.es');
    escribir('password', 'una-clave-larga');
    pagina().querySelector<HTMLInputElement>('#aceptaCondiciones')!.click();
    await enviar();
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Registro],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('con ?tipo=empresa preselecciona la cuenta de empresa y pide el nombre de la empresa', async () => {
    await crear({ tipo: 'empresa' });

    expect(pagina().querySelector<HTMLInputElement>('input[value="EMPRESA"]')!.checked).toBe(true);
    expect(pagina().querySelector('label[for="nombre"]')!.textContent).toContain(
      'Nombre de la empresa',
    );

    await enviar();
    expect(pagina().querySelector('#nombre-error')!.textContent).toContain(
      'Escribe el nombre de la empresa',
    );
    backend.expectNone('/api/auth/registro');
  });

  it('no envía un formulario incompleto y explica qué falta en cada campo', async () => {
    await crear();

    await enviar();

    backend.expectNone('/api/auth/registro');
    expect(pagina().querySelectorAll('[aria-invalid="true"]')).toHaveLength(4);
    expect(pagina().textContent).toContain('Escribe tu email');
    expect(pagina().textContent).toContain('Elige una contraseña');
  });

  it('no crea la cuenta sin aceptar las condiciones y la política de privacidad', async () => {
    await crear();
    escribir('nombre', 'Ana García');
    escribir('email', 'ana@test.com');
    escribir('password', 'una-clave-larga');

    await enviar();

    backend.expectNone('/api/auth/registro');
    expect(pagina().querySelector('#acepto-error')!.textContent).toContain(
      'tienes que aceptar las condiciones',
    );
    expect(pagina().querySelector('a[href="/condiciones"]')!.getAttribute('target')).toBe('_blank');
    expect(pagina().querySelector('a[href="/privacidad"]')).not.toBeNull();
  });

  it('crea la cuenta y lleva a la empresa a su panel', async () => {
    await crear({ tipo: 'empresa' });

    await rellenarYEnviar();
    const peticion = backend.expectOne('/api/auth/registro');
    expect(peticion.request.body).toEqual({
      rol: 'EMPRESA',
      nombre: 'Acme Software',
      email: 'rrhh@acme.es',
      password: 'una-clave-larga',
      aceptaCondiciones: true,
    });
    peticion.flush({
      accessToken: tokenDePrueba({ rol: 'EMPRESA' }),
      tokenType: 'Bearer',
      expiresIn: 3600,
    });

    expect(navegar).toHaveBeenCalledWith('/empresa');
  });

  it('tras registrarse vuelve a la página de la que venía', async () => {
    await crear({ volver: '/ofertas/42' });

    await rellenarYEnviar();
    backend
      .expectOne('/api/auth/registro')
      .flush({ accessToken: tokenDePrueba(), tokenType: 'Bearer', expiresIn: 3600 });

    expect(navegar).toHaveBeenCalledWith('/ofertas/42');
  });

  it('si el email ya existe lo dice junto al campo y ofrece entrar', async () => {
    await crear();

    await rellenarYEnviar();
    backend.expectOne('/api/auth/registro').flush(
      { title: 'Email ya registrado', detail: 'Ya existe una cuenta con el email rrhh@acme.es' },
      {
        status: 409,
        statusText: 'Conflict',
      },
    );
    await fixture.whenStable();

    const error = pagina().querySelector('#email-error')!;
    expect(error.textContent).toContain('Ya existe una cuenta con este email.');
    expect(error.querySelector('a')!.textContent).toContain('Entra con ella');
  });

  it('muestra en cada campo los errores de validación del servidor', async () => {
    await crear();

    await rellenarYEnviar();
    backend.expectOne('/api/auth/registro').flush(
      {
        title: 'Datos no válidos',
        errores: { nombre: 'El nombre no puede superar los 100 caracteres' },
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(pagina().querySelector('#nombre-error')!.textContent).toContain(
      'El nombre no puede superar los 100 caracteres',
    );
  });

  it('si no hay conexión lo explica en un aviso', async () => {
    await crear();

    await rellenarYEnviar();
    backend.expectOne('/api/auth/registro').error(new ProgressEvent('error'), { status: 0 });
    await fixture.whenStable();

    expect(pagina().querySelector('[role="alert"]')!.textContent).toContain(
      'No hemos podido conectar',
    );
  });
});
