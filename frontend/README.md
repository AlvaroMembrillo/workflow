# Frontend de Workflow

Aplicación Angular 22 del portal de empleo. El diseño sigue la guía de diseño de Workflow: salario siempre
visible, candidaturas que nunca se quedan sin respuesta y navegación sin cuenta ni cookies de seguimiento.

## Comandos

Usa Node 24 (`nvm use` lee la versión de `.nvmrc`).

| Comando | Qué hace |
|---|---|
| `npm start` | Servidor de desarrollo en http://localhost:4200. Reenvía `/api` al backend en el puerto 8080 |
| `npm test` | Tests unitarios con Vitest |
| `npm run build` | Compilación de producción en `dist/` |
| `npm run format` | Formatea el código con Prettier |

## Estructura

```
src/
├── styles/            Variables de diseño, base y clases compartidas (botones, campos, avisos)
├── testing/           Utilidades de los tests (no entran en la aplicación)
└── app/
    ├── core/          Sesión, interceptor HTTP, guards, errores de la API y ayudas de formularios
    ├── layout/        Cabecera, pie y logo
    └── paginas/       Una carpeta por pantalla, cargadas bajo demanda
```

## Decisiones

- **Sin zone.js.** El estado vive en signals y la detección de cambios es `OnPush`.
- **Sesión.** El JWT se guarda en `localStorage` para mantener la sesión al recargar. El frontend solo
  lee sus datos (rol y caducidad) para adaptar la interfaz; la firma la valida el backend. Cuando el
  token caduca, la sesión se cierra sola y las pantallas privadas llevan a la de acceso, que después
  devuelve al usuario adonde estaba.
- **Redirecciones seguras.** El parámetro `?volver=` solo acepta rutas internas, para evitar que un
  enlace lleve a otra web tras iniciar sesión.
- **Errores de la API.** Las respuestas Problem Details se muestran junto al campo que falla o, si no
  afectan a un campo, en un aviso dentro de la pantalla.
- **Accesibilidad.** Enlace para saltar al contenido, foco en el contenido nuevo al cambiar de página,
  foco en el primer campo con error y errores enlazados con `aria-describedby`.
- **Fuentes propias.** Atkinson Hyperlegible y Bricolage Grotesque se sirven desde la aplicación con
  Fontsource, sin peticiones a Google Fonts.
