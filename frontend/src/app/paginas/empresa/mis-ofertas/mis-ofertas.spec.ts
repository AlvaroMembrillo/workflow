import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ofertaDePrueba, paginaDe } from '../../../../testing/datos-de-prueba';
import { OfertaConCandidaturas, ResumenCandidaturas } from '../../../core/api/modelos';
import { MisOfertas } from './mis-ofertas';

const SIN_CANDIDATURAS: ResumenCandidaturas = {
  total: 0,
  pendientes: 0,
  enRevision: 0,
  aceptadas: 0,
  rechazadas: 0,
  retiradas: 0,
};

function fila(
  cambios: Partial<OfertaConCandidaturas['oferta']>,
  candidaturas: Partial<ResumenCandidaturas> = {},
): OfertaConCandidaturas {
  return {
    oferta: ofertaDePrueba(cambios),
    candidaturas: { ...SIN_CANDIDATURAS, ...candidaturas },
  };
}

describe('Mis ofertas', () => {
  let fixture: ComponentFixture<MisOfertas>;
  let backend: HttpTestingController;

  async function crear(filas: OfertaConCandidaturas[]): Promise<void> {
    fixture = TestBed.createComponent(MisOfertas);
    TestBed.tick();
    backend
      .expectOne((peticion) => peticion.url === '/api/empresas/me/ofertas')
      .flush(paginaDe(filas));
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    history.replaceState(null, '');
    TestBed.configureTestingModule({
      imports: [MisOfertas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('muestra cada oferta con su estado, cuántas candidaturas tiene y cuántas esperan respuesta', async () => {
    await crear([
      fila({ id: 'o1', titulo: 'Backend Java' }, { total: 12, pendientes: 3 }),
      fila({ id: 'o2', titulo: 'Frontend Angular', estado: 'CERRADA' }, { total: 1 }),
    ]);

    const filas = pagina().querySelectorAll('li.fila');
    expect(filas[0].querySelector('h2')!.textContent).toContain('Backend Java');
    expect(filas[0].querySelector('.estado')!.textContent).toContain('Abierta');
    expect(filas[0].querySelector('.recuento')!.textContent).toContain('12');
    expect(filas[0].querySelector('.pendientes')!.textContent).toContain('3 sin responder');
    expect(filas[1].querySelector('.recuento')!.textContent).toContain('1 candidatura');
    expect(filas[1].querySelector('.pendientes')).toBeNull();
    expect(filas[1].textContent).toContain('Reabrir');
  });

  it('una oferta retirada por moderación lo explica y no se puede editar ni reabrir', async () => {
    await crear([fila({ id: 'o1', titulo: 'Gana dinero fácil', estado: 'RETIRADA' })]);

    const retirada = pagina().querySelector('li.fila')!;
    expect(retirada.querySelector('.estado')!.textContent).toContain('Retirada');
    expect(retirada.textContent).toContain('La hemos retirado por incumplir las normas');
    expect(retirada.querySelector('a[href="/empresa/ofertas/o1/editar"]')).toBeNull();
    expect(retirada.querySelector('button')).toBeNull();
    expect(retirada.querySelector('a[href="/empresa/ofertas/o1/candidaturas"]')).not.toBeNull();
  });

  it('cierra una oferta abierta y recarga la lista', async () => {
    await crear([fila({ id: 'o1', titulo: 'Backend Java' })]);

    [...pagina().querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent?.trim() === 'Cerrar')!
      .click();
    const peticion = backend.expectOne('/api/ofertas/o1/estado');
    expect(peticion.request.method).toBe('PATCH');
    expect(peticion.request.body).toEqual({ estado: 'CERRADA' });
    peticion.flush(ofertaDePrueba({ id: 'o1', estado: 'CERRADA' }));
    TestBed.tick();
    backend
      .expectOne((p) => p.url === '/api/empresas/me/ofertas')
      .flush(paginaDe([fila({ id: 'o1', titulo: 'Backend Java', estado: 'CERRADA' })]));
    await fixture.whenStable();

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain('ya no admite candidaturas');
    expect(pagina().querySelector('.estado')!.textContent).toContain('Cerrada');
  });

  it('muestra una sola vez el aviso que deja la pantalla anterior', async () => {
    history.replaceState({ aviso: 'Oferta publicada' }, '');

    await crear([fila({})]);

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain('Oferta publicada');
    expect((history.state as Record<string, unknown>)['aviso']).toBeUndefined();
  });

  it('sin ofertas invita a publicar la primera', async () => {
    await crear([]);

    expect(pagina().textContent).toContain('Todavía no has publicado ninguna oferta');
    expect(pagina().querySelector('a[href="/empresa/ofertas/nueva"]')).not.toBeNull();
  });
});
