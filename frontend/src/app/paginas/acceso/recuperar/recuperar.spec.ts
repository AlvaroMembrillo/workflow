import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { Recuperar } from './recuperar';

describe('Recuperar contraseña', () => {
  let fixture: ComponentFixture<Recuperar>;
  let backend: HttpTestingController;

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function pedirEnlacePara(email: string): Promise<void> {
    const input = pagina().querySelector<HTMLInputElement>('#email')!;
    input.value = email;
    input.dispatchEvent(new Event('input'));
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [Recuperar],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Recuperar);
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  it('pide el enlace y explica que llegará si la cuenta existe', async () => {
    await pedirEnlacePara(' ana@test.com ');

    const peticion = backend.expectOne('/api/auth/recuperacion');
    expect(peticion.request.body).toEqual({ email: 'ana@test.com' });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(pagina().querySelector('h1')!.textContent).toBe('Revisa tu correo');
    expect(pagina().textContent).toContain('Si hay una cuenta con el email ana@test.com');
    expect(pagina().querySelector('form')).toBeNull();
    expect(document.activeElement).toBe(pagina().querySelector('.resultado'));
  });

  it('no envía nada si el email no tiene formato válido', async () => {
    await pedirEnlacePara('no-es-un-email');

    backend.expectNone('/api/auth/recuperacion');
    expect(pagina().querySelector('#email-error')!.textContent).toContain('formato válido');
  });

  it('si la petición falla, lo dice y deja reintentar', async () => {
    await pedirEnlacePara('ana@test.com');
    backend
      .expectOne('/api/auth/recuperacion')
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(pagina().querySelector('[role="alert"]')!.textContent).toContain(
      'Algo ha fallado en el servidor',
    );
    expect(pagina().querySelector('form')).not.toBeNull();
  });
});
