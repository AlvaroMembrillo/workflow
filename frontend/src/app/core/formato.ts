const MILES = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });
const EUROS = new Intl.NumberFormat('es-ES', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
  useGrouping: true,
});
const DIA_MES = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });
const DIA_MES_ANIO = new Intl.DateTimeFormat('es-ES', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});
const FECHA_COMPLETA = new Intl.DateTimeFormat('es-ES', { dateStyle: 'long', timeStyle: 'short' });

const MS_POR_DIA = 24 * 60 * 60 * 1000;

/**
 * Salario bruto anual en formato corto: "38–45 k€", "Desde 42 k€", "Hasta 45 k€" o "40 k€".
 * Devuelve null si la oferta no indica salario.
 */
export function salarioCorto(minimo: number | null, maximo: number | null): string | null {
  const miles = (euros: number) => MILES.format(euros / 1000);
  if (minimo !== null && maximo !== null) {
    return minimo === maximo ? `${miles(minimo)} k€` : `${miles(minimo)}–${miles(maximo)} k€`;
  }
  if (minimo !== null) {
    return `Desde ${miles(minimo)} k€`;
  }
  if (maximo !== null) {
    return `Hasta ${miles(maximo)} k€`;
  }
  return null;
}

/** Salario con las cifras completas, para la ficha de la oferta: "38.000–45.000 €". */
export function salarioCompleto(minimo: number | null, maximo: number | null): string | null {
  const euros = (valor: number) => EUROS.format(valor).replace(/\s?€/, '');
  if (minimo !== null && maximo !== null) {
    return minimo === maximo ? `${euros(minimo)} €` : `${euros(minimo)}–${euros(maximo)} €`;
  }
  if (minimo !== null) {
    return `Desde ${euros(minimo)} €`;
  }
  if (maximo !== null) {
    return `Hasta ${euros(maximo)} €`;
  }
  return null;
}

/** Tamaño de un fichero: "182 kB" o "1,2 MB". */
export function tamanoLegible(bytes: number): string {
  if (bytes < 1_000_000) {
    return `${Math.max(1, Math.round(bytes / 1000))} kB`;
  }
  return `${MILES.format(bytes / 1_000_000)} MB`;
}

/**
 * Fecha relativa para listados: "hoy", "ayer" o "hace 3 días" durante la primera semana; después,
 * la fecha ("12 sept", con el año si no es el actual).
 */
export function fechaRelativa(iso: string, ahora: Date = new Date()): string {
  const fecha = new Date(iso);
  const dias = Math.round((inicioDelDia(ahora) - inicioDelDia(fecha)) / MS_POR_DIA);
  if (dias <= 0) {
    return 'hoy';
  }
  if (dias === 1) {
    return 'ayer';
  }
  if (dias < 7) {
    return `hace ${dias} días`;
  }
  return fechaCorta(iso, ahora);
}

/** "12 sept", o "12 sept 2025" si no es del año actual. */
export function fechaCorta(iso: string, ahora: Date = new Date()): string {
  const fecha = new Date(iso);
  return (fecha.getFullYear() === ahora.getFullYear() ? DIA_MES : DIA_MES_ANIO).format(fecha);
}

/** "29 de septiembre de 2026, 16:30", para el título emergente de una fecha. */
export function fechaCompleta(iso: string): string {
  return FECHA_COMPLETA.format(new Date(iso));
}

function inicioDelDia(fecha: Date): number {
  return new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()).getTime();
}
