import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  GuardResult,
  MaybeAsync,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { tokenDePrueba } from '../../../testing/token-de-prueba';
import { destinoTrasAcceso, requiereRol, soloSinSesion } from './guards';
import { Rol } from './modelos';

describe('guards', () => {
  function configurar(rol?: Rol): void {
    localStorage.clear();
    if (rol) {
      localStorage.setItem('workflow.token', tokenDePrueba({ rol }));
    }
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
  }

  function ejecutar(guard: typeof soloSinSesion, url: string): string | boolean {
    const resultado = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    ) as MaybeAsync<GuardResult>;
    return resultado instanceof UrlTree
      ? TestBed.inject(Router).serializeUrl(resultado)
      : (resultado as boolean);
  }

  describe('requiereRol', () => {
    it('sin sesión lleva a la pantalla de acceso y guarda adónde volver', () => {
      configurar();
      expect(ejecutar(requiereRol('EMPRESA'), '/empresa/perfil')).toBe(
        '/entrar?volver=%2Fempresa%2Fperfil',
      );
    });

    it('deja pasar al usuario con el rol necesario', () => {
      configurar('EMPRESA');
      expect(ejecutar(requiereRol('EMPRESA'), '/empresa')).toBe(true);
    });

    it('con otro rol lleva al inicio de ese usuario', () => {
      configurar('CANDIDATO');
      expect(ejecutar(requiereRol('EMPRESA'), '/empresa')).toBe('/');
    });
  });

  describe('soloSinSesion', () => {
    it('deja pasar sin sesión', () => {
      configurar();
      expect(ejecutar(soloSinSesion, '/entrar')).toBe(true);
    });

    it('con sesión de empresa lleva a su panel', () => {
      configurar('EMPRESA');
      expect(ejecutar(soloSinSesion, '/entrar')).toBe('/empresa');
    });
  });

  describe('destinoTrasAcceso', () => {
    it('vuelve a la ruta interna de la que venía el usuario', () => {
      expect(destinoTrasAcceso('/ofertas/42', 'CANDIDATO')).toBe('/ofertas/42');
    });

    it.each(['https://malo.example', '//malo.example', '/\\malo.example', 'javascript:alert(1)'])(
      'no redirige fuera de la aplicación con volver=%s',
      (volver) => {
        expect(destinoTrasAcceso(volver, 'CANDIDATO')).toBe('/');
      },
    );

    it('sin ruta de vuelta, la empresa va a su panel', () => {
      expect(destinoTrasAcceso(undefined, 'EMPRESA')).toBe('/empresa');
    });
  });
});
