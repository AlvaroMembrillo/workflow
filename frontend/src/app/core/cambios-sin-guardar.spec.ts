import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import { avisarCambiosSinGuardar, ConCambiosSinGuardar } from './cambios-sin-guardar';

describe('avisarCambiosSinGuardar', () => {
  function salir(pantalla: ConCambiosSinGuardar): unknown {
    return TestBed.runInInjectionContext(() =>
      avisarCambiosSinGuardar(
        pantalla,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );
  }

  afterEach(() => vi.restoreAllMocks());

  it('deja salir sin preguntar si no hay cambios', () => {
    const confirmar = vi.spyOn(window, 'confirm');

    expect(salir({ hayCambiosSinGuardar: () => false })).toBe(true);
    expect(confirmar).not.toHaveBeenCalled();
  });

  it('con cambios pregunta y respeta la respuesta', () => {
    vi.spyOn(window, 'confirm').mockReturnValueOnce(false).mockReturnValueOnce(true);
    const pantalla = { hayCambiosSinGuardar: () => true };

    expect(salir(pantalla)).toBe(false);
    expect(salir(pantalla)).toBe(true);
  });
});
