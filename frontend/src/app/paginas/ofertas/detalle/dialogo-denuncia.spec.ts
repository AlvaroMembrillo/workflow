import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogoDenuncia } from './dialogo-denuncia';

describe('Diálogo de denuncia', () => {
  let fixture: ComponentFixture<DialogoDenuncia>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [DialogoDenuncia],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DialogoDenuncia);
    fixture.componentRef.setInput('ofertaId', 'oferta-1');
    fixture.componentRef.setInput('abierto', true);
    await fixture.whenStable();
  });

  afterEach(() => backend.verify());

  function dialogo(): HTMLDialogElement {
    return (fixture.nativeElement as HTMLElement).querySelector('dialog')!;
  }

  function elegir(texto: string): void {
    const opcion = [...dialogo().querySelectorAll<HTMLLabelElement>('label.opcion')].find(
      (etiqueta) => etiqueta.textContent?.trim() === texto,
    )!;
    opcion.querySelector('input')!.dispatchEvent(new Event('change'));
  }

  async function enviar(): Promise<void> {
    dialogo().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('se abre con los motivos y sin ninguno marcado', () => {
    expect(dialogo().open).toBe(true);
    expect(dialogo().querySelectorAll('input[type="radio"]').length).toBe(4);
    expect(dialogo().querySelector('input[type="radio"]:checked')).toBeNull();
  });

  it('pide un motivo antes de enviar', async () => {
    await enviar();

    backend.expectNone('/api/ofertas/oferta-1/denuncias');
    expect(dialogo().querySelector('#motivo-error')!.textContent).toBe('Elige un motivo');
  });

  it('envía el motivo y la explicación, y da las gracias', async () => {
    elegir('Parece una estafa o pide dinero');
    const detalle = dialogo().querySelector<HTMLTextAreaElement>('textarea')!;
    detalle.value = '  Piden 200 € para el material.  ';
    detalle.dispatchEvent(new Event('input'));

    await enviar();
    const peticion = backend.expectOne('/api/ofertas/oferta-1/denuncias');
    expect(peticion.request.body).toEqual({
      motivo: 'FRAUDE',
      detalle: 'Piden 200 € para el material.',
    });
    peticion.flush(null, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(dialogo().querySelector('h2')!.textContent).toBe('Gracias por avisar');
    expect(dialogo().querySelector('form')).toBeNull();
  });

  it('si ya la había denunciado, también da las gracias', async () => {
    elegir('Otro motivo');

    await enviar();
    backend
      .expectOne('/api/ofertas/oferta-1/denuncias')
      .flush({ title: 'Denuncia repetida' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(dialogo().querySelector('h2')!.textContent).toBe('Gracias por avisar');
  });

  it('si el envío falla, lo dice y deja reintentar', async () => {
    elegir('Otro motivo');

    await enviar();
    backend
      .expectOne('/api/ofertas/oferta-1/denuncias')
      .flush(null, { status: 500, statusText: 'Internal Server Error' });
    await fixture.whenStable();

    expect(dialogo().querySelector('[role="alert"]')!.textContent).toContain(
      'Algo ha fallado en el servidor',
    );
    expect(dialogo().querySelector('form')).not.toBeNull();
  });

  it('avisa al cerrarse, para que la página sepa que ya no está abierto', async () => {
    let cerrado = false;
    fixture.componentInstance.cerrar.subscribe(() => (cerrado = true));

    [...dialogo().querySelectorAll('button')]
      .find((b) => b.textContent?.trim() === 'Cancelar')!
      .click();
    await fixture.whenStable();

    expect(cerrado).toBe(true);
  });
});
