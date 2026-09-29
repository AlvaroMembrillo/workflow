import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { empresaDePrueba, ofertaDePrueba } from '../../../../testing/datos-de-prueba';
import { FormularioOferta } from './formulario-oferta';

describe('Formulario de oferta', () => {
  let fixture: ComponentFixture<FormularioOferta>;
  let backend: HttpTestingController;
  let navegar: ReturnType<typeof vi.spyOn>;

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function escribir(id: string, valor: string): void {
    const campo = pagina().querySelector<HTMLInputElement | HTMLTextAreaElement>(`#${id}`)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  function elegir(texto: string): void {
    const opcion = [...pagina().querySelectorAll<HTMLLabelElement>('label.opcion')].find(
      (etiqueta) => etiqueta.textContent?.trim() === texto,
    )!;
    opcion.querySelector('input')!.click();
  }

  async function enviar(): Promise<void> {
    pagina().querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  async function rellenar(): Promise<void> {
    escribir('titulo', 'Backend Java');
    escribir('salarioMinimo', '38000');
    escribir('salarioMaximo', '45000');
    escribir('ubicacion', 'Madrid');
    elegir('En remoto');
    elegir('Indefinido');
    escribir('descripcion', 'Motor de pólizas.');
    await fixture.whenStable();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormularioOferta],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  describe('al publicar', () => {
    beforeEach(async () => {
      fixture = TestBed.createComponent(FormularioOferta);
      TestBed.tick();
      // El nombre de la empresa para la vista previa
      backend.expectOne('/api/empresas/me').flush(empresaDePrueba({ nombre: 'Brisa Logística' }));
      await fixture.whenStable();
    });

    it('explica qué falta, también que el salario es obligatorio, sin llamar a la API', async () => {
      await enviar();

      backend.expectNone('/api/ofertas');
      expect(pagina().querySelector('#titulo-error')!.textContent).toContain('Escribe el título');
      expect(pagina().querySelector('#salarioMinimo-error')!.textContent).toContain('obligatorio');
      expect(pagina().querySelector('#modalidad-error')).not.toBeNull();
    });

    it('avisa si el salario mínimo supera al máximo', async () => {
      await rellenar();
      escribir('salarioMinimo', '50000');

      await enviar();

      backend.expectNone('/api/ofertas');
      expect(pagina().textContent).toContain('El salario mínimo no puede ser mayor que el máximo');
    });

    it('la vista previa muestra la tarjeta tal como la verán los candidatos', async () => {
      await rellenar();

      const vista = pagina().querySelector('.vista-previa')!;
      expect(vista.textContent).toContain('Backend Java');
      expect(vista.textContent).toContain('Brisa Logística');
      expect(vista.textContent).toContain('38–45 k€');
      expect(vista.textContent).toContain('En remoto');
    });

    it('publica la oferta y vuelve a "Mis ofertas" con un aviso', async () => {
      await rellenar();

      await enviar();
      const peticion = backend.expectOne('/api/ofertas');
      expect(peticion.request.method).toBe('POST');
      expect(peticion.request.body).toEqual({
        titulo: 'Backend Java',
        descripcion: 'Motor de pólizas.',
        ubicacion: 'Madrid',
        modalidad: 'REMOTO',
        tipoContrato: 'INDEFINIDO',
        salarioMinimo: 38000,
        salarioMaximo: 45000,
      });
      peticion.flush(ofertaDePrueba({ titulo: 'Backend Java' }));

      expect(navegar).toHaveBeenCalledWith('/empresa', {
        state: { aviso: '"Backend Java" está publicada y ya aparece en el buscador.' },
      });
      expect(fixture.componentInstance.hayCambiosSinGuardar()).toBe(false);
    });

    it('detecta cambios sin guardar para avisar antes de salir', async () => {
      expect(fixture.componentInstance.hayCambiosSinGuardar()).toBe(false);

      escribir('titulo', 'Backend');

      expect(fixture.componentInstance.hayCambiosSinGuardar()).toBe(true);
    });
  });

  describe('al editar', () => {
    async function crearParaEditar(): Promise<void> {
      fixture = TestBed.createComponent(FormularioOferta);
      fixture.componentRef.setInput('id', 'o1');
      TestBed.tick();
    }

    it('rellena el formulario con la oferta y guarda los cambios con PUT', async () => {
      await crearParaEditar();
      backend.expectOne('/api/empresas/me/ofertas/o1').flush({
        oferta: ofertaDePrueba({ id: 'o1', titulo: 'Backend Java' }),
        candidaturas: {
          total: 0,
          pendientes: 0,
          enRevision: 0,
          aceptadas: 0,
          rechazadas: 0,
          retiradas: 0,
        },
      });
      await fixture.whenStable();

      expect(pagina().querySelector<HTMLInputElement>('#titulo')!.value).toBe('Backend Java');
      expect(fixture.componentInstance.hayCambiosSinGuardar()).toBe(false);

      escribir('titulo', 'Backend Java Senior');
      await enviar();
      const peticion = backend.expectOne('/api/ofertas/o1');
      expect(peticion.request.method).toBe('PUT');
      expect(peticion.request.body.titulo).toBe('Backend Java Senior');
      peticion.flush(ofertaDePrueba({ id: 'o1', titulo: 'Backend Java Senior' }));

      expect(navegar).toHaveBeenCalledWith('/empresa', {
        state: { aviso: 'Cambios guardados en "Backend Java Senior".' },
      });
    });

    it('si la oferta es de otra empresa lo explica y no muestra el formulario', async () => {
      await crearParaEditar();
      backend
        .expectOne('/api/empresas/me/ofertas/o1')
        .flush({}, { status: 403, statusText: 'Forbidden' });
      await fixture.whenStable();

      expect(pagina().textContent).toContain('Esta oferta es de otra empresa');
      expect(pagina().querySelector('form')).toBeNull();
    });
  });
});
