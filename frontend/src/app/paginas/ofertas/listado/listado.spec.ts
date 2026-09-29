import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { ofertaDePrueba, paginaDe } from '../../../../testing/datos-de-prueba';
import { Listado } from './listado';

describe('Listado de ofertas', () => {
  let fixture: ComponentFixture<Listado>;
  let backend: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  async function crear(parametrosUrl: Record<string, string> = {}): Promise<TestRequest> {
    fixture = TestBed.createComponent(Listado);
    for (const [nombre, valor] of Object.entries(parametrosUrl)) {
      fixture.componentRef.setInput(nombre, valor);
    }
    // httpResource lanza la petición al ejecutar la detección de cambios. No se usa whenStable() aquí:
    // esperaría a que la petición termine, y la respuesta la da cada test
    TestBed.tick();
    return backend.expectOne((peticion) => peticion.url === '/api/ofertas');
  }

  async function responder(peticion: TestRequest, cuerpo: object): Promise<void> {
    peticion.flush(cuerpo);
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Listado],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  it('pide a la API los filtros y la página de la URL, ignorando los valores no válidos', async () => {
    const peticion = await crear({
      texto: 'java',
      modalidad: 'REMOTO',
      tipoContrato: 'INVENTADO',
      pagina: '2',
    });

    const parametros = peticion.request.params;
    expect(parametros.get('texto')).toBe('java');
    expect(parametros.get('modalidad')).toBe('REMOTO');
    expect(parametros.has('tipoContrato')).toBe(false);
    expect(parametros.get('page')).toBe('1');
    peticion.flush(paginaDe([]));
  });

  it('muestra el total y cada oferta con su salario', async () => {
    const peticion = await crear();

    await responder(
      peticion,
      paginaDe([
        ofertaDePrueba(),
        ofertaDePrueba({ id: 'oferta-2', titulo: 'Programador/a Angular' }),
      ]),
    );

    expect(pagina().querySelector('.total')!.textContent).toContain('2 ofertas');
    const titulos = [...pagina().querySelectorAll('app-tarjeta-oferta h3')].map((h) =>
      h.textContent?.trim(),
    );
    expect(titulos).toEqual(['Desarrollador/a Java Backend', 'Programador/a Angular']);
    expect(pagina().textContent).toContain('38–45 k€');
  });

  it('muestra los filtros aplicados y quitar uno lo quita de la URL', async () => {
    const peticion = await crear({ ubicacion: 'Madrid', modalidad: 'HIBRIDO' });
    await responder(peticion, paginaDe([ofertaDePrueba()]));

    const chips = [...pagina().querySelectorAll('.chip')].map((chip) => chip.textContent?.trim());
    expect(chips).toEqual(['Madrid', 'Híbrido']);

    pagina().querySelector<HTMLButtonElement>('[aria-label="Quitar filtro Madrid"]')!.click();

    expect(navegar).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { ubicacion: null, pagina: null },
        queryParamsHandling: 'merge',
      }),
    );
  });

  it('elegir una modalidad la aplica al momento y vuelve a la primera página', async () => {
    const peticion = await crear({ pagina: '3' });
    await responder(peticion, paginaDe([ofertaDePrueba()]));

    const remoto = [
      ...pagina().querySelectorAll<HTMLLabelElement>('.filtros-laterales label'),
    ].find((etiqueta) => etiqueta.textContent?.trim() === 'En remoto')!;
    remoto.querySelector('input')!.click();

    expect(navegar).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { modalidad: 'REMOTO', pagina: null },
      }),
    );
  });

  it('buscar por texto lleva el texto a la URL', async () => {
    const peticion = await crear();
    await responder(peticion, paginaDe([]));

    const campo = pagina().querySelector<HTMLInputElement>('input[name="texto"]')!;
    campo.value = '  react  ';
    campo.dispatchEvent(new Event('input'));
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(navegar).toHaveBeenCalledWith(
      [],
      expect.objectContaining({
        queryParams: { texto: 'react', ubicacion: null, pagina: null },
      }),
    );
  });

  it('sin resultados con filtros ofrece borrarlos', async () => {
    const peticion = await crear({ texto: 'cobol' });
    await responder(peticion, paginaDe([]));

    expect(pagina().textContent).toContain('No hay ofertas con estos filtros');
    const borrar = [...pagina().querySelectorAll<HTMLButtonElement>('app-estado-vacio button')][0];
    borrar.click();

    expect(navegar).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: {} }));
  });

  it('si la API falla lo explica y permite reintentar', async () => {
    const peticion = await crear();

    peticion.flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    const aviso = pagina().querySelector('[role="alert"]')!;
    expect(aviso.textContent).toContain('No hemos podido cargar las ofertas');
    aviso.querySelector('button')!.click();
    TestBed.tick();
    backend.expectOne((peticionNueva) => peticionNueva.url === '/api/ofertas').flush(paginaDe([]));
  });
});
