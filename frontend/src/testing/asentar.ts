import { TestBed } from '@angular/core/testing';

/**
 * Deja que la pantalla procese las respuestas ya enviadas y lance las peticiones que dependen de ellas
 * (por ejemplo, un componente que solo aparece cuando llega un dato). Sirve cuando no se puede usar
 * `whenStable()`, que espera a que no quede ninguna petición abierta.
 */
export async function asentar(): Promise<void> {
  await new Promise((resolver) => setTimeout(resolver));
  TestBed.tick();
}
