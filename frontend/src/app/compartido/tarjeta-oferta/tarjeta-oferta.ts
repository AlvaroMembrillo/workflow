import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Oferta } from '../../core/api/modelos';
import { TEXTO_MODALIDAD, TEXTO_TIPO_CONTRATO } from '../../core/textos';
import { Fecha } from '../fecha/fecha';
import { Salario } from '../salario/salario';

/**
 * Tarjeta de una oferta en los listados. Orden fijo: título, empresa y ubicación, salario y etiquetas.
 * El enlace real es el título; el resto de la tarjeta también es clicable a través de él.
 */
@Component({
  selector: 'app-tarjeta-oferta',
  imports: [RouterLink, Salario, Fecha],
  templateUrl: './tarjeta-oferta.html',
  styleUrl: './tarjeta-oferta.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TarjetaOferta {
  readonly oferta = input.required<Oferta>();
  /** Nivel del título según la página que contiene la tarjeta. */
  readonly nivelTitulo = input<2 | 3>(2);

  protected readonly inicial = computed(() =>
    this.oferta().empresa.nombre.trim().charAt(0).toUpperCase(),
  );
  protected readonly modalidad = computed(() => TEXTO_MODALIDAD[this.oferta().modalidad]);
  protected readonly contrato = computed(() => TEXTO_TIPO_CONTRATO[this.oferta().tipoContrato]);
}
