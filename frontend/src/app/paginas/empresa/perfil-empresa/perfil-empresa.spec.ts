import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { empresaDePrueba } from '../../../../testing/datos-de-prueba';
import { PerfilEmpresa } from './perfil-empresa';

describe('Perfil de empresa', () => {
  let fixture: ComponentFixture<PerfilEmpresa>;
  let backend: HttpTestingController;

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function escribir(id: string, valor: string): void {
    const campo = pagina().querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  async function enviar(): Promise<void> {
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [PerfilEmpresa],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PerfilEmpresa);
    TestBed.tick();
    backend.expectOne('/api/empresas/me').flush(empresaDePrueba());
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  it('carga el perfil en el formulario y enlaza el perfil público', () => {
    expect(pagina().querySelector<HTMLInputElement>('#nombre')!.value).toBe('Lumen Seguros');
    expect(pagina().querySelector<HTMLInputElement>('#sitioWeb')!.value).toBe(
      'https://lumen.example',
    );
    expect(pagina().querySelector('a[href="/empresas/empresa-1"]')).not.toBeNull();
  });

  it('pide la web completa, con https://', async () => {
    escribir('sitioWeb', 'lumen.example');

    await enviar();

    backend.expectNone('/api/empresas/me');
    expect(pagina().querySelector('#sitioWeb-error')!.textContent).toContain('https://');
  });

  it('guarda los campos opcionales vacíos como null', async () => {
    escribir('descripcion', '   ');
    escribir('sitioWeb', '');

    await enviar();
    const peticion = backend.expectOne('/api/empresas/me');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      nombre: 'Lumen Seguros',
      descripcion: null,
      sitioWeb: null,
      ubicacion: 'Madrid',
    });
    peticion.flush(empresaDePrueba({ descripcion: null, sitioWeb: null }));
    await fixture.whenStable();

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain('Perfil guardado');
    expect(fixture.componentInstance.hayCambiosSinGuardar()).toBe(false);
  });
});
