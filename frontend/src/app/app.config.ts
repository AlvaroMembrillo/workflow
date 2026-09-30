import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withInMemoryScrolling,
} from '@angular/router';

import { routes } from './app.routes';
import { autenticacionInterceptor } from './core/auth/autenticacion-interceptor';
import { Sesion } from './core/auth/sesion';
import { TituloPagina } from './core/titulo-pagina';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      // Los parámetros de ruta, de la URL (?volver=) y los datos de la ruta llegan como inputs
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }),
    ),
    provideHttpClient(withFetch(), withInterceptors([autenticacionInterceptor])),
    // Antes de mostrar nada se recupera la sesión, para que los guards de las rutas ya sepan si la hay
    provideAppInitializer(() => inject(Sesion).restaurar()),
    { provide: TitleStrategy, useClass: TituloPagina },
  ],
};
