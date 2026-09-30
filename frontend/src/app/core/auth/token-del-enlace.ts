import { Location } from '@angular/common';
import { inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Lee el token de un enlace enviado por correo, que viaja detrás de # para que no llegue al servidor
 * ni quede en sus registros, y lo quita de la barra de direcciones y del historial del navegador.
 * Hay que llamarla al crear el componente.
 */
export function tokenDelEnlace(): string | null {
  const token = inject(ActivatedRoute).snapshot.fragment;
  if (token) {
    const location = inject(Location);
    location.replaceState(location.path(false));
  }
  return token || null;
}
