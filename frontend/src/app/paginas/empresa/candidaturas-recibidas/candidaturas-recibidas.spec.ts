import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ofertaDePrueba, paginaDe } from '../../../../testing/datos-de-prueba';
import { CandidaturaRecibida } from '../../../core/api/modelos';
import { CandidaturasRecibidas } from './candidaturas-recibidas';

const RESUMEN = {
  total: 4,
  pendientes: 2,
  enRevision: 1,
  aceptadas: 1,
  rechazadas: 0,
  retiradas: 0,
};

function recibida(cambios: Partial<CandidaturaRecibida> = {}): CandidaturaRecibida {
  return {
    id: 'c1',
    candidato: { id: 'u1', nombre: 'Ana García', email: 'ana@test.com' },
    estado: 'PENDIENTE',
    cartaPresentacion: null,
    fechaCreacion: new Date().toISOString(),
    fechaRevision: null,
    fechaResolucion: null,
    fechaActualizacion: new Date().toISOString(),
    ...cambios,
  };
}

describe('Candidaturas recibidas', () => {
  let fixture: ComponentFixture<CandidaturasRecibidas>;
  let backend: HttpTestingController;

  function responderOferta(): void {
    backend.expectOne('/api/empresas/me/ofertas/o1').flush({
      oferta: ofertaDePrueba({ id: 'o1', titulo: 'Backend Java' }),
      candidaturas: RESUMEN,
    });
  }

  function peticionCandidaturas(): TestRequest {
    return backend.expectOne((peticion) => peticion.url === '/api/ofertas/o1/candidaturas');
  }

  async function crear(candidaturas: CandidaturaRecibida[], vista?: string): Promise<TestRequest> {
    fixture = TestBed.createComponent(CandidaturasRecibidas);
    fixture.componentRef.setInput('id', 'o1');
    if (vista) fixture.componentRef.setInput('vista', vista);
    TestBed.tick();
    responderOferta();
    const peticion = peticionCandidaturas();
    peticion.flush(paginaDe(candidaturas));
    await fixture.whenStable();
    return peticion;
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function boton(texto: string, dentro: ParentNode = pagina()): HTMLButtonElement {
    return [...dentro.querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === texto,
    )!;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CandidaturasRecibidas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('por defecto muestra las pendientes, de la más antigua a la más reciente', async () => {
    const peticion = await crear([recibida()]);

    expect(peticion.request.params.getAll('estado')).toEqual(['PENDIENTE']);
    expect(peticion.request.params.get('sort')).toBe('fechaCreacion,asc');
  });

  it('las pestañas indican cuántas candidaturas hay en cada estado', async () => {
    await crear([recibida()]);

    const pestanas = [...pagina().querySelectorAll('.pestanas a')].map((a) =>
      a.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(pestanas).toEqual(['Sin responder 2', 'En revisión 1', 'Decididas 1', 'Todas 4']);
    expect(pagina().querySelector('.pestanas a[aria-current="page"]')!.textContent).toContain(
      'Sin responder',
    );
  });

  it('la pestaña "Decididas" pide los tres estados finales', async () => {
    const peticion = await crear([], 'decididas');

    expect(peticion.request.params.getAll('estado')).toEqual(['ACEPTADA', 'RECHAZADA', 'RETIRADA']);
    expect(pagina().textContent).toContain('No hay candidaturas en esta pestaña');
  });

  it('resalta las candidaturas que llevan varios días sin respuesta', async () => {
    const haceSeisDias = new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString();
    await crear([recibida({ fechaCreacion: haceSeisDias }), recibida({ id: 'c2' })]);

    const esperas = pagina().querySelectorAll('.espera');
    expect(esperas).toHaveLength(1);
    expect(esperas[0].textContent).toContain('Sin respuesta desde hace 6 días');
  });

  it('pasar a revisión no pide confirmación', async () => {
    await crear([recibida()]);

    boton('Pasar a revisión').click();
    const cambio = backend.expectOne('/api/candidaturas/c1/estado');
    expect(cambio.request.body).toEqual({ estado: 'EN_REVISION' });
    cambio.flush(recibida({ estado: 'EN_REVISION' }));
    TestBed.tick();
    peticionCandidaturas().flush(paginaDe([]));
    responderOferta();
    await fixture.whenStable();

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain(
      'Ana García pasa a revisión',
    );
    // El botón pulsado ya no está: el foco va al mensaje para no perderse
    expect(document.activeElement).toBe(pagina().querySelector('.mensajes'));
  });

  it('aceptar es una decisión final y pide confirmación antes de enviarla', async () => {
    await crear([recibida()]);

    boton('Aceptar').click();
    await fixture.whenStable();
    const dialogo = pagina().querySelector('dialog')!;
    expect(dialogo.open).toBe(true);
    expect(dialogo.textContent).toContain('¿Aceptar la candidatura de Ana García?');
    expect(dialogo.textContent).toContain('Seleccionada');
    backend.expectNone('/api/candidaturas/c1/estado');

    boton('Aceptar', dialogo).click();
    const cambio = backend.expectOne('/api/candidaturas/c1/estado');
    expect(cambio.request.body).toEqual({ estado: 'ACEPTADA' });
    cambio.flush(recibida({ estado: 'ACEPTADA' }));
    TestBed.tick();
    peticionCandidaturas().flush(paginaDe([]));
    responderOferta();
    await fixture.whenStable();

    expect(dialogo.open).toBe(false);
    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain(
      'Has aceptado la candidatura',
    );
  });

  it('las candidaturas ya decididas no tienen acciones', async () => {
    await crear([recibida({ estado: 'RECHAZADA' })], 'decididas');

    expect(pagina().querySelector('.candidatura .acciones')).toBeNull();
    expect(pagina().querySelector('app-estado-candidatura')!.textContent).toContain('Rechazada');
  });
});
