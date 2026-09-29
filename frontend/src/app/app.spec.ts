import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { tokenDePrueba } from '../testing/token-de-prueba';
import { App } from './app';

describe('App', () => {
  async function renderizar(): Promise<HTMLElement> {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => localStorage.clear());

  it('tiene un enlace para saltar directamente al contenido', async () => {
    const pagina = await renderizar();

    const saltar = pagina.querySelector<HTMLAnchorElement>('a.saltar')!;
    expect(saltar.getAttribute('href')).toBe('#contenido');
    expect(pagina.querySelector('main#contenido')).not.toBeNull();
  });

  it('sin sesión ofrece entrar, crear cuenta y la entrada para empresas', async () => {
    const pagina = await renderizar();

    const textos = [...pagina.querySelectorAll('header a')].map((enlace) =>
      enlace.textContent?.trim(),
    );
    expect(textos).toEqual(
      expect.arrayContaining(['Ofertas', 'Para empresas', 'Entrar', 'Crear cuenta']),
    );
  });

  it('con sesión de candidato muestra sus candidaturas, su email y el botón de salir', async () => {
    localStorage.setItem(
      'workflow.token',
      tokenDePrueba({ rol: 'CANDIDATO', email: 'ana@test.com' }),
    );

    const pagina = await renderizar();

    expect(pagina.querySelector('header')!.textContent).toContain('Mis candidaturas');
    expect(pagina.querySelector('header')!.textContent).toContain('ana@test.com');
    expect(pagina.querySelector('header button')!.textContent).toContain('Salir');
  });
});
