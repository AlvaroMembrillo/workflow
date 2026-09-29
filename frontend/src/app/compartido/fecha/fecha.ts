import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { fechaCompleta, fechaCorta, fechaRelativa } from '../../core/formato';

/** Fecha legible ("hace 2 días" o "12 sept") con la fecha completa en el atributo datetime y al pasar el ratón. */
@Component({
  selector: 'app-fecha',
  template: `<time [attr.datetime]="iso()" [title]="completa()">{{ texto() }}</time>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Fecha {
  readonly iso = input.required<string>();
  /** "relativa" para listados, "corta" para mostrar siempre el día. */
  readonly formato = input<'relativa' | 'corta'>('relativa');

  protected readonly texto = computed(() =>
    this.formato() === 'relativa' ? fechaRelativa(this.iso()) : fechaCorta(this.iso()),
  );
  protected readonly completa = computed(() => fechaCompleta(this.iso()));
}
