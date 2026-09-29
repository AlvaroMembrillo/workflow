import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { candidaturaDePrueba, paginaDe } from '../../../../testing/datos-de-prueba';
import { MiCandidatura } from '../../../core/api/modelos';
import { MisCandidaturas } from './mis-candidaturas';

describe('Mis candidaturas', () => {
  let fixture: ComponentFixture<MisCandidaturas>;
  let backend: HttpTestingController;

  // httpResource lanza la petición en la detección de cambios; se responde antes de esperar a whenStable()
  async function crear(candidaturas: MiCandidatura[]): Promise<void> {
    fixture = TestBed.createComponent(MisCandidaturas);
    TestBed.tick();
    backend
      .expectOne((peticion) => peticion.url === '/api/candidaturas/me')
      .flush(paginaDe(candidaturas, candidaturas.length, 10));
    await fixture.whenStable();
  }

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function botonRetirar(): HTMLButtonElement | null {
    return pagina().querySelector<HTMLButtonElement>('button.retirar');
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MisCandidaturas],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('muestra cada candidatura con su estado, su empresa y la línea de progreso', async () => {
    await crear([
      candidaturaDePrueba({ estado: 'EN_REVISION', fechaRevision: '2026-09-28T12:00:00Z' }),
    ]);

    const tarjeta = pagina().querySelector('.candidatura')!;
    expect(tarjeta.querySelector('h2')!.textContent).toContain('Desarrollador/a Java Backend');
    expect(tarjeta.querySelector('.meta')!.textContent).toContain('Lumen Seguros');
    expect(tarjeta.querySelector('app-estado-candidatura')!.textContent).toContain('En revisión');
    expect(tarjeta.querySelector('[aria-current="step"]')!.textContent).toContain('En revisión');
  });

  it('usa un lenguaje cercano para las decisiones y no deja retirar una candidatura ya decidida', async () => {
    await crear([
      candidaturaDePrueba({ estado: 'RECHAZADA', fechaResolucion: '2026-09-28T12:00:00Z' }),
    ]);

    expect(pagina().querySelector('app-estado-candidatura')!.textContent).toContain(
      'No seleccionada',
    );
    expect(botonRetirar()).toBeNull();
  });

  it('muestra la carta de presentación en un desplegable', async () => {
    await crear([
      candidaturaDePrueba({ cartaPresentacion: 'Me encantaría formar parte del equipo' }),
    ]);

    const carta = pagina().querySelector('details')!;
    expect(carta.querySelector('summary')!.textContent).toContain('Tu carta de presentación');
    expect(carta.textContent).toContain('Me encantaría formar parte del equipo');
  });

  it('pide confirmación antes de retirar y, al confirmar, retira y recarga la lista', async () => {
    await crear([candidaturaDePrueba()]);

    botonRetirar()!.click();
    await fixture.whenStable();
    const dialogo = pagina().querySelector('dialog')!;
    expect(dialogo.textContent).toContain('no podrás volver a inscribirte');
    backend.expectNone('/api/candidaturas/candidatura-1/retirada');

    [...dialogo.querySelectorAll('button')]
      .find((boton) => boton.textContent?.trim() === 'Retirar')!
      .click();
    backend
      .expectOne('/api/candidaturas/candidatura-1/retirada')
      .flush(candidaturaDePrueba({ estado: 'RETIRADA' }));
    TestBed.tick();
    backend
      .expectOne((peticion) => peticion.url === '/api/candidaturas/me')
      .flush(paginaDe([candidaturaDePrueba({ estado: 'RETIRADA' })]));
    await fixture.whenStable();

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain(
      'Has retirado tu candidatura',
    );
    expect(pagina().querySelector('app-estado-candidatura')!.textContent).toContain('Retirada');
    expect(botonRetirar()).toBeNull();
  });

  it('sin candidaturas invita a buscar ofertas', async () => {
    await crear([]);

    expect(pagina().textContent).toContain('Todavía no te has inscrito en ninguna oferta');
    expect(pagina().querySelector('a[href="/"]')!.textContent).toContain('Buscar ofertas');
  });
});
