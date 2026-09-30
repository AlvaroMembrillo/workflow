import { Routes } from '@angular/router';

import { requiereRol, requiereSesion, soloSinSesion } from './core/auth/guards';
import { avisarCambiosSinGuardar } from './core/cambios-sin-guardar';

const formularioOferta = () =>
  import('./paginas/empresa/formulario-oferta/formulario-oferta').then((m) => m.FormularioOferta);

/**
 * Mapa de pantallas de la guía de diseño. Las rutas privadas vuelven a comprobar la sesión en cada navegación ("always"), así que si el token
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
    path: 'recuperar',
    title: 'Recuperar contraseña',
    loadComponent: () => import('./paginas/acceso/recuperar/recuperar').then((m) => m.Recuperar),
  },
  {
    path: 'restablecer',
    title: 'Contraseña nueva',
    loadComponent: () =>
      import('./paginas/acceso/restablecer/restablecer').then((m) => m.Restablecer),
  },
  {
    path: 'verificar-email',
    title: 'Confirmar email',
    loadComponent: () =>
      import('./paginas/acceso/verificar-email/verificar-email').then((m) => m.VerificarEmail),
  },
  {
    path: 'cuenta',
    title: 'Mi cuenta',
    canActivate: [requiereSesion],
    runGuardsAndResolvers: 'always',
    loadComponent: () => import('./paginas/cuenta/cuenta').then((m) => m.Cuenta),
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
    loadComponent: () => import('./paginas/empresa/panel/panel').then((m) => m.Panel),
    children: [
      {
        path: '',
        title: 'Mis ofertas',
        loadComponent: () =>
          import('./paginas/empresa/mis-ofertas/mis-ofertas').then((m) => m.MisOfertas),
      },
      {
        path: 'ofertas/nueva',
        title: 'Publicar oferta',
        canDeactivate: [avisarCambiosSinGuardar],
        loadComponent: formularioOferta,
      },
      {
        path: 'ofertas/:id/editar',
        title: 'Editar oferta',
        canDeactivate: [avisarCambiosSinGuardar],
        loadComponent: formularioOferta,
      },
      {
        path: 'ofertas/:id/candidaturas',
        title: 'Candidaturas recibidas',
        loadComponent: () =>
          import('./paginas/empresa/candidaturas-recibidas/candidaturas-recibidas').then(
            (m) => m.CandidaturasRecibidas,
          ),
      },
      {
        path: 'perfil',
        title: 'Perfil de empresa',
        canDeactivate: [avisarCambiosSinGuardar],
        loadComponent: () =>
          import('./paginas/empresa/perfil-empresa/perfil-empresa').then((m) => m.PerfilEmpresa),
      },
    ],
  },
  {
    path: 'cuenta-borrada',
    title: 'Cuenta borrada',
    loadComponent: () => import('./paginas/cuenta/cuenta-borrada').then((m) => m.CuentaBorrada),
  },
  {
    path: 'privacidad',
    title: 'Política de privacidad',
    loadComponent: () => import('./paginas/legal/privacidad').then((m) => m.Privacidad),
  },
  {
    path: 'condiciones',
    title: 'Condiciones de uso',
    loadComponent: () => import('./paginas/legal/condiciones').then((m) => m.Condiciones),
  },
  {
    path: 'aviso-legal',
    title: 'Aviso legal',
    loadComponent: () => import('./paginas/legal/aviso-legal').then((m) => m.AvisoLegal),
  },
  {
    path: 'admin',
    title: 'Moderación',
    canActivate: [requiereRol('ADMIN')],
    runGuardsAndResolvers: 'always',
    loadComponent: () => import('./paginas/admin/moderacion/moderacion').then((m) => m.Moderacion),
  },
  {
    path: '**',
    title: 'Página no encontrada',
    loadComponent: () =>
      import('./paginas/no-encontrada/no-encontrada').then((m) => m.NoEncontrada),
  },
];
