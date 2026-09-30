import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';

import { Rol } from './modelos';
import { Sesion } from './sesion';

/** La ruta necesita un rol concreto. Sin sesión, lleva a la pantalla de acceso y después vuelve aquí. */
export function requiereRol(rol: Rol): CanActivateFn {
  return (_ruta, estado) => {
    const sesion = inject(Sesion);
    if (!sesion.iniciada()) {
      return pantallaDeAcceso(estado.url);
    }
    return sesion.rol() === rol || inject(Router).createUrlTree([inicioPara(sesion.rol())]);
  };
}

/** La ruta necesita sesión, con cualquier tipo de cuenta. */
export const requiereSesion: CanActivateFn = (_ruta, estado) =>
  inject(Sesion).iniciada() || pantallaDeAcceso(estado.url);

/** Acceso y registro: con la sesión ya iniciada no tienen sentido, así que se va al inicio del usuario. */
export const soloSinSesion: CanActivateFn = () => {
  const sesion = inject(Sesion);
  return !sesion.iniciada() || inject(Router).createUrlTree([inicioPara(sesion.rol())]);
};

export function inicioPara(rol: Rol | null): string {
  switch (rol) {
    case 'EMPRESA':
      return '/empresa';
    case 'ADMIN':
      return '/admin';
    default:
      return '/';
  }
}

/**
 * Adónde ir tras iniciar sesión. Solo acepta rutas internas de la aplicación en el parámetro "volver",
 * para que un enlace malicioso no pueda llevar al usuario a otra web (open redirect).
 */
export function destinoTrasAcceso(volver: string | null | undefined, rol: Rol): string {
  const esRutaInterna =
    !!volver && volver.startsWith('/') && !volver.startsWith('//') && !volver.startsWith('/\\');
  return esRutaInterna ? volver : inicioPara(rol);
}

function pantallaDeAcceso(volver: string): UrlTree {
  return inject(Router).createUrlTree(['/entrar'], { queryParams: { volver } });
}
