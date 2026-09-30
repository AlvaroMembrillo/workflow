import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Titular } from './titular';

@Component({
  selector: 'app-condiciones',
  imports: [RouterLink],
  templateUrl: './condiciones.html',
  styleUrl: './legal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Condiciones {
  protected readonly titular = inject(Titular);
}
