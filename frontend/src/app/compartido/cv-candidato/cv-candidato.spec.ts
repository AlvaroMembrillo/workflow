import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { curriculumDePrueba } from '../../../testing/datos-de-prueba';
import { Curriculum } from '../../core/api/modelos';
import { CvCandidato } from './cv-candidato';

describe('Currículum del candidato', () => {
  let fixture: ComponentFixture<CvCandidato>;
  let backend: HttpTestingController;

  /** Crea el componente y responde a la consulta del currículum guardado (404 si no hay). */
  async function crear(guardado?: Curriculum): Promise<void> {
    TestBed.configureTestingModule({
      imports: [CvCandidato],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CvCandidato);
    TestBed.tick();
    const peticion = backend.expectOne('/api/candidatos/me/cv');
    if (guardado) {
      peticion.flush(guardado);
    } else {
      peticion.flush({ title: 'Sin currículum' }, { status: 404, statusText: 'Not Found' });
    }
    await fixture.whenStable();
  }

  function componente(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function boton(texto: string): HTMLButtonElement {
    return [...componente().querySelectorAll<HTMLButtonElement>('button')].find(
      (b) => b.textContent?.trim() === texto,
    )!;
  }

  /** Simula que el usuario elige un fichero en el selector del navegador. */
  async function elegir(fichero: File): Promise<void> {
    const selector = componente().querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(selector, 'files', { value: [fichero], configurable: true });
    selector.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  function pdf(nombre = 'cv.pdf', tamano = 1000): File {
    return new File([new Uint8Array(tamano)], nombre, { type: 'application/pdf' });
  }

  afterEach(() => backend.verify());

  it('sin currículum invita a subirlo e indica el formato y el tamaño', async () => {
    await crear();

    expect(componente().textContent).toContain('Todavía no has subido tu currículum');
    expect(componente().textContent).toContain('En PDF, de 5 MB como máximo');
    expect(boton('Subir currículum')).toBeDefined();
  });

  it('sube el PDF elegido y muestra sus datos', async () => {
    await crear();

    await elegir(pdf('CV Ana García.pdf'));
    const peticion = backend.expectOne('/api/candidatos/me/cv');
    expect(peticion.request.method).toBe('POST');
    expect((peticion.request.body as FormData).get('fichero')).toBeInstanceOf(File);
    peticion.flush(curriculumDePrueba());
    await fixture.whenStable();

    expect(componente().querySelector('.nombre')!.textContent).toBe('CV Ana García.pdf');
    expect(componente().textContent).toContain('182 kB');
    expect(componente().querySelector('[role="status"]')!.textContent).toBe('Currículum subido.');
  });

  it('no sube ficheros que no son PDF ni los de más de 5 MB', async () => {
    await crear();

    await elegir(new File(['hola'], 'cv.docx', { type: 'application/msword' }));
    expect(componente().querySelector('[role="alert"]')!.textContent).toContain(
      'tiene que ser un PDF',
    );

    await elegir(pdf('enorme.pdf', 6 * 1024 * 1024));
    expect(componente().querySelector('[role="alert"]')!.textContent).toContain(
      'ocupa 6,3 MB y el máximo es 5 MB',
    );
    backend.expectNone('/api/candidatos/me/cv');
  });

  it('muestra el motivo si la API rechaza el fichero', async () => {
    await crear();

    await elegir(pdf());
    backend
      .expectOne('/api/candidatos/me/cv')
      .flush(
        { title: 'Datos no válidos', errores: { fichero: 'El currículum tiene que ser un PDF' } },
        { status: 400, statusText: 'Bad Request' },
      );
    await fixture.whenStable();

    expect(componente().querySelector('[role="alert"]')!.textContent).toContain(
      'El currículum tiene que ser un PDF',
    );
  });

  it('descarga el currículum guardado con su nombre', async () => {
    const pulsarEnlace = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        expect(this.download).toBe('CV Ana García.pdf');
      });
    await crear(curriculumDePrueba());

    boton('Descargar').click();
    backend.expectOne('/api/candidatos/me/cv/fichero').flush(new Blob(['%PDF-1.4']));
    await fixture.whenStable();

    expect(pulsarEnlace).toHaveBeenCalledOnce();
    pulsarEnlace.mockRestore();
  });

  it('quitar el currículum pide confirmación', async () => {
    await crear(curriculumDePrueba());

    boton('Quitar').click();
    await fixture.whenStable();
    const dialogo = componente().querySelector('dialog')!;
    expect(dialogo.open).toBe(true);
    expect(dialogo.textContent).toContain('dejarán de poder descargarlo');
    backend.expectNone('/api/candidatos/me/cv');

    [...dialogo.querySelectorAll('button')]
      .find((b) => b.textContent?.trim() === 'Quitar')!
      .click();
    const peticion = backend.expectOne('/api/candidatos/me/cv');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await fixture.whenStable();

    expect(componente().textContent).toContain('Todavía no has subido tu currículum');
    expect(componente().querySelector('[role="status"]')!.textContent).toContain(
      'Currículum quitado',
    );
  });

  it('si no se puede consultar el currículum, permite reintentar', async () => {
    TestBed.configureTestingModule({
      imports: [CvCandidato],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CvCandidato);
    TestBed.tick();
    backend
      .expectOne('/api/candidatos/me/cv')
      .flush(null, { status: 503, statusText: 'Service Unavailable' });
    await fixture.whenStable();

    expect(componente().textContent).toContain('No hemos podido comprobar');
    boton('Reintentar').click();
    TestBed.tick();
    backend.expectOne('/api/candidatos/me/cv').flush(curriculumDePrueba());
    await fixture.whenStable();

    expect(componente().querySelector('.nombre')!.textContent).toBe('CV Ana García.pdf');
  });
});
