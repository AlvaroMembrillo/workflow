import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { empresaDePrueba, ofertaDePrueba, paginaDe } from '../../../../testing/datos-de-prueba';
import { esWebSegura, PerfilPublico } from './perfil-publico';

describe('Perfil público de empresa', () => {
  let fixture: ComponentFixture<PerfilPublico>;
  let backend: HttpTestingController;

  async function crear(
    empresa = empresaDePrueba(),
    ofertas = paginaDe([ofertaDePrueba()]),
  ): Promise<void> {
    fixture = TestBed.createComponent(PerfilPublico);
    fixture.componentRef.setInput('id', 'empresa-1');
    TestBed.tick();
    backend.expectOne('/api/empresas/empresa-1').flush(empresa);
    const peticionOfertas = backend.expectOne((peticion) => peticion.url === '/api/ofertas');
    expect(peticionOfertas.request.params.get('empresaId')).toBe('empresa-1');
    peticionOfertas.flush(ofertas);
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PerfilPublico],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('muestra la empresa, su web y sus ofertas abiertas', async () => {
    await crear();

    expect(pagina().querySelector('h1')!.textContent).toContain('Lumen Seguros');
    const web = pagina().querySelector<HTMLAnchorElement>('.datos a')!;
    expect(web.getAttribute('href')).toBe('https://lumen.example');
    expect(web.getAttribute('rel')).toContain('noopener');
    expect(web.textContent).toContain('lumen.example');
    expect(pagina().querySelectorAll('app-tarjeta-oferta')).toHaveLength(1);
  });

  it('no enlaza webs que no sean http o https', async () => {
    await crear(empresaDePrueba({ sitioWeb: 'ftp://lumen.example' }));

    expect(pagina().querySelector('.datos a')).toBeNull();
  });

  it('sin ofertas abiertas lo explica', async () => {
    await crear(empresaDePrueba(), paginaDe([]));

    expect(pagina().textContent).toContain('No tiene ofertas abiertas ahora mismo');
  });

  it.each([
    ['https://acme.es', true],
    ['http://acme.es', true],
    ['javascript:alert(1)', false],
    ['ftp://acme.es', false],
    [null, false],
  ])('esWebSegura(%s) → %s', (url, esperado) => {
    expect(esWebSegura(url)).toBe(esperado);
  });
});
