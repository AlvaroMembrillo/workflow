import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Sesion } from '../app/core/auth/sesion';
import { OpcionesToken, tokenDePrueba } from './token-de-prueba';

/**
 * Inicia sesión en un test como lo hace la aplicación al abrirse: el navegador tiene la marca de sesión
 * y la API devuelve un token de acceso a cambio de la cookie de refresco.
 * Hay que llamarla después de configurar TestBed (con provideHttpClientTesting). Devuelve el token.
 */
export function iniciarSesionDePrueba(opciones: OpcionesToken = {}): string {
  const token = tokenDePrueba(opciones);
  localStorage.setItem('workflow.sesion', '1');
  TestBed.inject(Sesion).restaurar().subscribe();
  TestBed.inject(HttpTestingController)
    .expectOne('/api/auth/refresco')
    .flush({ accessToken: token, tokenType: 'Bearer', expiresIn: 900 });
  return token;
}
