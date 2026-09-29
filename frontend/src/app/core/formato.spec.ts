import { fechaCorta, fechaRelativa, salarioCompleto, salarioCorto } from './formato';

describe('formato', () => {
  describe('salarioCorto', () => {
    it.each([
      [38000, 45000, '38–45 k€'],
      [38500, 45000, '38,5–45 k€'],
      [40000, 40000, '40 k€'],
      [42000, null, 'Desde 42 k€'],
      [null, 45000, 'Hasta 45 k€'],
    ])('%s y %s → %s', (minimo, maximo, esperado) => {
      expect(salarioCorto(minimo, maximo)).toBe(esperado);
    });

    it('devuelve null si la oferta no indica salario', () => {
      expect(salarioCorto(null, null)).toBeNull();
    });
  });

  it('salarioCompleto muestra las cifras con separador de miles', () => {
    expect(salarioCompleto(38000, 45000)).toBe('38.000–45.000 €');
    expect(salarioCompleto(42000, null)).toBe('Desde 42.000 €');
  });

  describe('fechaRelativa', () => {
    const ahora = new Date(2026, 8, 29, 12, 0);
    const haceDias = (dias: number, hora = 9) =>
      new Date(2026, 8, 29 - dias, hora, 0).toISOString();

    it('usa hoy, ayer y "hace N días" durante la primera semana', () => {
      expect(fechaRelativa(haceDias(0, 8), ahora)).toBe('hoy');
      expect(fechaRelativa(haceDias(1, 23), ahora)).toBe('ayer');
      expect(fechaRelativa(haceDias(3), ahora)).toBe('hace 3 días');
      expect(fechaRelativa(haceDias(6), ahora)).toBe('hace 6 días');
    });

    it('a partir de una semana muestra el día', () => {
      expect(fechaRelativa(haceDias(10), ahora)).toMatch(/^19 sept?\.?$/);
    });

    it('añade el año si la fecha es de otro año', () => {
      expect(fechaCorta(new Date(2025, 11, 3).toISOString(), ahora)).toMatch(/^3 dic\.? 2025$/);
    });
  });
});
