export type Rol = 'CANDIDATO' | 'EMPRESA' | 'ADMIN';

/** Roles que se pueden elegir al registrarse. Las cuentas ADMIN no se crean desde la web. */
export type RolRegistrable = Exclude<Rol, 'ADMIN'>;

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  /** Segundos de validez desde que se emitió. */
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegistroRequest {
  email: string;
  password: string;
  nombre: string;
  rol: RolRegistrable;
  /** El usuario ha leído y acepta las condiciones de uso y la política de privacidad. */
  aceptaCondiciones: true;
}

/** Lo que la interfaz necesita saber del usuario, sacado del JWT. */
export interface UsuarioSesion {
  id: string;
  email: string;
  rol: Rol;
  /** Momento de caducidad del token, en milisegundos desde epoch. */
  expira: number;
}

/** La cuenta del usuario, tal como la devuelve GET /api/usuarios/me. */
export interface Usuario {
  id: string;
  email: string;
  nombre: string;
  rol: Rol;
  fechaCreacion: string;
  /** Ha confirmado que el email es suyo con el enlace enviado por correo. */
  emailVerificado: boolean;
  /** Quiere recibir avisos por correo (nueva candidatura, cambio de estado). */
  avisosPorCorreo: boolean;
}
