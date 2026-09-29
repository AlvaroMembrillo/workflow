import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { tokenDePrueba } from '../../../../testing/token-de-prueba';
import { Sesion } from '../../../core/auth/sesion';
import { Entrar } from './entrar';

describe('Entrar', () => {
  let fixture: ComponentFixture<Entrar>;
  let backend: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  async function crear(volver?: string): Promise<void> {
    fixture = TestBed.createComponent(Entrar);
    if (volver) {
      fixture.componentRef.setInput('volver', volver);
    }
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function entrarCon(email: string, password: string): Promise<void> {
    for (const [id, valor] of [
      ['email', email],
      ['password', password],
    ]) {
      const input = pagina().querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
    }
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Entrar],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
      ],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('entra y vuelve a la página de la que venía', async () => {
    await crear('/ofertas/42');

    await entrarCon('ana@test.com', 'una-clave-larga');
    backend
      .expectOne('/api/auth/login')
      .flush({ accessToken: tokenDePrueba(), tokenType: 'Bearer', expiresIn: 3600 });

    expect(navegar).toHaveBeenCalledWith('/ofertas/42');
  });

  it('con credenciales incorrectas muestra el mensaje de la API', async () => {
    await crear();

    await entrarCon('ana@test.com', 'otra');
    backend
      .expectOne('/api/auth/login')
      .flush(
        { title: 'Credenciales incorrectas', detail: 'El email o la contraseña no son correctos' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(pagina().querySelector('[role="alert"]')!.textContent).toContain(
      'El email o la contraseña no son correctos',
    );
    expect(navegar).not.toHaveBeenCalled();
  });

  it('explica que la sesión caducó cuando se llega por ese motivo', async () => {
    TestBed.inject(Sesion).caducar();

    await crear('/mis-candidaturas');

    expect(pagina().textContent).toContain('Tu sesión ha caducado');
  });
});
