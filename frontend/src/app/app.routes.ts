import { Routes } from '@angular/router';

import { requiereRol, soloSinSesion } from './core/auth/guards';

const enConstruccion = () =>
  import('./paginas/en-construccion/en-construccion').then((m) => m.EnConstruccion);

/**
 * Mapa de pantallas de la guía de diseño. Las que todavía no están hechas muestran qué habrá en ellas.
 * Las rutas privadas vuelven a comprobar la sesión en cada navegación ("always"), así que si el token
 * caduca, el guard lleva a la pantalla de acceso.
 */
export const routes: Routes = [
  {
    path: '',
    title: 'Ofertas de empleo',
    loadComponent: () => import('./paginas/ofertas/listado/listado').then((m) => m.Listado),
  },
  {
    path: 'ofertas/:id',
    title: 'Oferta',
    loadComponent: () => import('./paginas/ofertas/detalle/detalle').then((m) => m.Detalle),
  },
  {
    path: 'empresas/:id',
    title: 'Empresa',
    loadComponent: () =>
      import('./paginas/empresas/perfil-publico/perfil-publico').then((m) => m.PerfilPublico),
  },
  {
    path: 'entrar',
    title: 'Entrar',
    canActivate: [soloSinSesion],
    loadComponent: () => import('./paginas/acceso/entrar/entrar').then((m) => m.Entrar),
  },
  {
    path: 'registro',
    title: 'Crear cuenta',
    canActivate: [soloSinSesion],
    loadComponent: () => import('./paginas/acceso/registro/registro').then((m) => m.Registro),
  },
  {
    path: 'mis-candidaturas',
    title: 'Mis candidaturas',
    canActivate: [requiereRol('CANDIDATO')],
    runGuardsAndResolvers: 'always',
    loadComponent: () =>
      import('./paginas/candidaturas/mis-candidaturas/mis-candidaturas').then(
        (m) => m.MisCandidaturas,
      ),
  },
  {
    path: 'empresa',
    canActivate: [requiereRol('EMPRESA')],
    runGuardsAndResolvers: 'always',
    children: [
      {
        path: '',
        title: 'Panel de empresa',
        loadComponent: enConstruccion,
        data: {
          titulo: 'Panel de empresa',
          descripcion: 'Aquí gestionarás tus ofertas, abiertas y cerradas.',
        },
      },
      {
        path: 'ofertas/nueva',
        title: 'Publicar oferta',
        loadComponent: enConstruccion,
        data: { titulo: 'Publicar oferta', descripcion: 'Aquí podrás publicar una oferta nueva.' },
      },
      {
        path: 'ofertas/:id/editar',
        title: 'Editar oferta',
        loadComponent: enConstruccion,
        data: { titulo: 'Editar oferta', descripcion: 'Aquí podrás modificar una de tus ofertas.' },
      },
      {
        path: 'ofertas/:id/candidaturas',
        title: 'Candidaturas recibidas',
        loadComponent: enConstruccion,
        data: {
          titulo: 'Candidaturas recibidas',
          descripcion:
            'Aquí verás quién se ha inscrito en la oferta y podrás responder a cada persona.',
        },
      },
      {
        path: 'perfil',
        title: 'Perfil de empresa',
        loadComponent: enConstruccion,
        data: {
          titulo: 'Perfil de empresa',
          descripcion: 'Aquí podrás editar el perfil público de tu empresa.',
        },
      },
    ],
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () =>
      import('./paginas/no-encontrada/no-encontrada').then((m) => m.NoEncontrada),
  },
];
