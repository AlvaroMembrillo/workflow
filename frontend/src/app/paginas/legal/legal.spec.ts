import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AvisoLegal } from './aviso-legal';
import { Condiciones } from './condiciones';
import { Privacidad } from './privacidad';

describe('Páginas legales', () => {
  let backend: HttpTestingController;

  const TITULAR = {
    titular: 'Workflow Empleo, S. L.',
    nif: 'B00000000',
    domicilio: 'Calle Mayor 1, Madrid',
    contacto: 'hola@workflow.example',
  };

  async function abrir(pagina: Type<unknown>, titular: object = TITULAR): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(pagina);
    TestBed.tick();
    backend.expectOne('/api/legal').flush(titular);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('la política de privacidad identifica al responsable y explica los derechos', async () => {
    const pagina = await abrir(Privacidad);

    expect(pagina.querySelector('h1')!.textContent).toBe('Política de privacidad');
    expect(pagina.querySelector('dl')!.textContent).toContain('Workflow Empleo, S. L.');
    expect(pagina.querySelector('dl')!.textContent).toContain('B00000000');
    expect(pagina.querySelector('a[href="mailto:hola@workflow.example"]')).not.toBeNull();
    expect(pagina.textContent).toContain('Agencia Española de Protección de Datos');
    expect(pagina.querySelector('a[href="/cuenta"]')).not.toBeNull();
  });

  it('las condiciones de uso recogen las normas de publicación y la moderación', async () => {
    const pagina = await abrir(Condiciones);

    expect(pagina.querySelector('h1')!.textContent).toBe('Condiciones de uso');
    expect(pagina.textContent).toContain(
      'El salario que se publica tiene que ser el que se va a pagar',
    );
    expect(pagina.textContent).toContain('suspender la');
    expect(pagina.querySelector('a[href="mailto:hola@workflow.example"]')).not.toBeNull();
  });

  it('el aviso legal muestra los datos del titular', async () => {
    const pagina = await abrir(AvisoLegal);

    expect(pagina.querySelector('h1')!.textContent).toBe('Aviso legal');
    expect(pagina.querySelector('dl')!.textContent).toContain('Calle Mayor 1, Madrid');
  });

  it('si el titular no está configurado, lo deja a la vista en lugar de dejar un hueco', async () => {
    const pagina = await abrir(AvisoLegal, {
      titular: '',
      nif: null,
      domicilio: '',
      contacto: 'soporte@workflow.localhost',
    });

    expect(pagina.querySelectorAll('dd')[0].textContent).toBe('[pendiente de configurar]');
    expect(pagina.querySelectorAll('dd')[1].textContent).toBe('[pendiente de configurar]');
  });
});
