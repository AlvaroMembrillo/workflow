import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { asentar } from '../../../../testing/asentar';
import {
  candidaturaDePrueba,
  curriculumDePrueba,
  ofertaDePrueba,
} from '../../../../testing/datos-de-prueba';
import { iniciarSesionDePrueba } from '../../../../testing/sesion-de-prueba';
import { Curriculum } from '../../../core/api/modelos';
import { Rol } from '../../../core/auth/modelos';
import { Detalle } from './detalle';

describe('Detalle de oferta', () => {
  let fixture: ComponentFixture<Detalle>;
  let backend: HttpTestingController;

  function configurar(rol?: Rol): void {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Detalle],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
    if (rol) {
      iniciarSesionDePrueba({ rol });
    }
  }

  /**
   * Crea la pantalla y responde a sus peticiones. httpResource las lanza al ejecutar la detección de
   * cambios (TestBed.tick) y whenStable() espera a que no quede ninguna abierta, así que se responde a
   * todas antes de esperar. Con sesión de candidato también se consulta si ya se inscribió.
   */
  async function crear(
    oferta: object = ofertaDePrueba(),
    miCandidatura?: { cuerpo: object; estado: number },
    cv?: Curriculum,
  ): Promise<void> {
    fixture = TestBed.createComponent(Detalle);
    fixture.componentRef.setInput('id', 'oferta-1');
    TestBed.tick();
    backend.expectOne('/api/ofertas/oferta-1').flush(oferta);
    if (miCandidatura) {
      backend.expectOne('/api/ofertas/oferta-1/mi-candidatura').flush(miCandidatura.cuerpo, {
        status: miCandidatura.estado,
        statusText: miCandidatura.estado === 200 ? 'OK' : 'Not Found',
      });
      // Si puede inscribirse, el formulario consulta su currículum
      await asentar();
      backend
        .match('/api/candidatos/me/cv')
        .forEach((peticion) =>
          cv
            ? peticion.flush(cv)
            : peticion.flush({ title: 'Sin currículum' }, { status: 404, statusText: 'Not Found' }),
        );
    }
    await fixture.whenStable();
  }

  const NO_INSCRITO = { cuerpo: { title: 'Sin candidatura' }, estado: 404 };

  function pagina(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function panel(): HTMLElement {
    return pagina().querySelector('aside')!;
  }

  afterEach(() => backend.verify());

  it('muestra los datos clave de la oferta y la descripción', async () => {
    configurar();
    await crear();

    expect(pagina().querySelector('h1')!.textContent).toContain('Desarrollador/a Java Backend');
    expect(pagina().querySelector('.hechos')!.textContent).toContain('38.000–45.000 €');
    expect(pagina().querySelector('.hechos')!.textContent).toContain('En remoto');
    expect(pagina().querySelector('.texto')!.textContent).toContain('Trabajarás con Spring Boot');
  });

  it('sin sesión, "Inscribirme" lleva al registro y después vuelve a la oferta', async () => {
    configurar();
    await crear();

    const inscribirme = panel().querySelector<HTMLAnchorElement>('a.btn-primario')!;
    expect(inscribirme.textContent).toContain('Inscribirme');
    expect(inscribirme.getAttribute('href')).toBe('/registro?volver=%2Fofertas%2Foferta-1');
  });

  it('el candidato se inscribe con una carta y ve la confirmación', async () => {
    configurar('CANDIDATO');
    await crear(ofertaDePrueba(), NO_INSCRITO);

    panel().querySelector<HTMLButtonElement>('.enlace-carta')!.click();
    await fixture.whenStable();
    const carta = panel().querySelector<HTMLTextAreaElement>('textarea')!;
    carta.value = 'Me encantaría formar parte del equipo';
    carta.dispatchEvent(new Event('input'));
    panel().querySelector('form')!.dispatchEvent(new Event('submit'));

    const peticion = backend.expectOne('/api/ofertas/oferta-1/candidaturas');
    expect(peticion.request.body).toEqual({
      cartaPresentacion: 'Me encantaría formar parte del equipo',
    });
    peticion.flush(candidaturaDePrueba());
    await fixture.whenStable();

    expect(panel().querySelector('[role="status"]')!.textContent).toContain('Te inscribiste');
    expect(panel().textContent).toContain('Enviada');
    expect(panel().querySelector('a[href="/mis-candidaturas"]')).not.toBeNull();
  });

  it('al inscribirse, el candidato ve el currículum que se enviará con su candidatura', async () => {
    configurar('CANDIDATO');
    await crear(ofertaDePrueba(), NO_INSCRITO, curriculumDePrueba());

    expect(panel().querySelector('app-cv-candidato')!.textContent).toContain('CV Ana García.pdf');
    expect(panel().textContent).toContain('tu currículum si lo has subido');
  });

  it('si el candidato ya se inscribió muestra el estado de su candidatura', async () => {
    configurar('CANDIDATO');
    await crear(ofertaDePrueba(), {
      cuerpo: candidaturaDePrueba({ estado: 'EN_REVISION' }),
      estado: 200,
    });

    expect(panel().textContent).toContain('En revisión');
    expect(panel().querySelector('form')).toBeNull();
  });

  it('si la oferta está cerrada no se puede inscribir', async () => {
    configurar('CANDIDATO');
    await crear(ofertaDePrueba({ estado: 'CERRADA' }), NO_INSCRITO);

    expect(panel().textContent).toContain('Esta oferta ya no admite candidaturas');
    expect(panel().querySelector('form')).toBeNull();
  });

  it('con cuenta de empresa explica que no puede inscribirse y no consulta candidaturas', async () => {
    configurar('EMPRESA');
    await crear();

    expect(panel().textContent).toContain('cuenta de empresa');
    backend.expectNone('/api/ofertas/oferta-1/mi-candidatura');
  });

  it('si la oferta no existe lo dice y ofrece volver al buscador', async () => {
    configurar();
    fixture = TestBed.createComponent(Detalle);
    fixture.componentRef.setInput('id', 'oferta-1');
    TestBed.tick();

    backend.expectOne('/api/ofertas/oferta-1').flush({}, { status: 404, statusText: 'Not Found' });
    await fixture.whenStable();

    expect(pagina().textContent).toContain('Esta oferta no existe');
    expect(pagina().querySelector('a[href="/"]')).not.toBeNull();
  });
});
