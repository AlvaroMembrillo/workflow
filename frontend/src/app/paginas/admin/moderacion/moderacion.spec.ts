import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { denunciaDePrueba, paginaDe } from '../../../../testing/datos-de-prueba';
import { Denuncia } from '../../../core/api/modelos';
import { Moderacion } from './moderacion';

describe('Moderación', () => {
  let fixture: ComponentFixture<Moderacion>;
  let backend: HttpTestingController;

  function peticionDenuncias(): TestRequest {
    return backend.expectOne((peticion) => peticion.url === '/api/admin/denuncias');
  }

  async function crear(denuncias: Denuncia[]): Promise<TestRequest> {
    fixture = TestBed.createComponent(Moderacion);
    TestBed.tick();
    const peticion = peticionDenuncias();
    peticion.flush(paginaDe(denuncias));
    await fixture.whenStable();
    return peticion;
  }

  /** Tras cada acción se vuelve a pedir la lista: se responde con lo que queda. */
  async function recargarCon(denuncias: Denuncia[]): Promise<void> {
    TestBed.tick();
    peticionDenuncias().flush(paginaDe(denuncias));
    await fixture.whenStable();
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
      imports: [Moderacion],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('pide las denuncias pendientes y muestra lo necesario para decidir', async () => {
    const peticion = await crear([denunciaDePrueba()]);

    expect(peticion.request.params.get('estado')).toBe('PENDIENTE');
    const tarjeta = pagina().querySelector('.denuncia')!;
    expect(tarjeta.querySelector('h2')!.textContent).toBe('Gana 3000 € desde casa');
    expect(tarjeta.textContent).toContain('Dinero Fácil');
    expect(tarjeta.textContent).toContain('Parece una estafa o pide dinero');
    expect(tarjeta.querySelector('blockquote')!.textContent).toBe('Piden 200 € para el material.');
    expect(tarjeta.textContent).toContain('ana@test.com');
    expect(tarjeta.querySelector('details')!.textContent).toContain(
      'Trabajo fácil sin experiencia.',
    );
    expect(pagina().querySelector('.total')!.textContent).toContain('1 denuncia pendiente');
  });

  it('sin denuncias lo dice', async () => {
    await crear([]);

    expect(pagina().textContent).toContain('No hay denuncias pendientes');
  });

  it('desestimar no pide confirmación', async () => {
    await crear([denunciaDePrueba()]);

    boton('Desestimar').click();
    const peticion = backend.expectOne('/api/admin/denuncias/denuncia-1/resolucion');
    expect(peticion.request.body).toEqual({ accion: 'DESESTIMAR' });
    peticion.flush(denunciaDePrueba({ estado: 'DESESTIMADA' }));
    await recargarCon([]);

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain(
      'Denuncia de "Gana 3000 € desde casa" desestimada.',
    );
    expect(pagina().querySelector('.denuncia')).toBeNull();
  });

  it('retirar la oferta pide confirmación antes de enviarla', async () => {
    await crear([denunciaDePrueba()]);

    boton('Retirar oferta').click();
    await fixture.whenStable();
    const dialogo = pagina().querySelector('dialog')!;
    expect(dialogo.open).toBe(true);
    expect(dialogo.textContent).toContain('¿Retirar la oferta "Gana 3000 € desde casa"?');
    backend.expectNone('/api/admin/denuncias/denuncia-1/resolucion');

    boton('Retirar oferta', dialogo).click();
    const peticion = backend.expectOne('/api/admin/denuncias/denuncia-1/resolucion');
    expect(peticion.request.body).toEqual({ accion: 'RETIRAR_OFERTA' });
    peticion.flush(denunciaDePrueba({ estado: 'ACEPTADA' }));
    await recargarCon([]);

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain(
      'Oferta "Gana 3000 € desde casa" retirada.',
    );
  });

  it('suspender la empresa pide confirmación y explica las consecuencias', async () => {
    await crear([denunciaDePrueba()]);

    boton('Suspender empresa').click();
    await fixture.whenStable();
    const dialogo = pagina().querySelector('dialog')!;
    expect(dialogo.textContent).toContain('¿Suspender la cuenta de Dinero Fácil?');
    expect(dialogo.textContent).toContain('Se retirarán todas sus ofertas');

    boton('Suspender cuenta', dialogo).click();
    backend
      .expectOne('/api/admin/empresas/empresa-1/suspension')
      .flush(null, { status: 204, statusText: 'No Content' });
    await recargarCon([]);

    expect(pagina().querySelector('.aviso-ok')!.textContent).toContain(
      'Cuenta de Dinero Fácil suspendida',
    );
  });

  it('si otra persona ya resolvió la denuncia, lo explica y recarga la lista', async () => {
    await crear([denunciaDePrueba()]);

    boton('Desestimar').click();
    backend
      .expectOne('/api/admin/denuncias/denuncia-1/resolucion')
      .flush(
        { title: 'Denuncia ya resuelta', detail: 'Otra persona ya ha resuelto esta denuncia' },
        { status: 409, statusText: 'Conflict' },
      );
    await recargarCon([]);

    expect(pagina().querySelector('[role="alert"]')!.textContent).toContain(
      'Otra persona ya ha resuelto esta denuncia',
    );
  });
});
