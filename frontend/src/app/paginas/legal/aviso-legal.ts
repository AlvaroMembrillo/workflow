import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { Titular } from './titular';

@Component({
  selector: 'app-aviso-legal',
  imports: [RouterLink],
  templateUrl: './aviso-legal.html',
  styleUrl: './legal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AvisoLegal {
  protected readonly titular = inject(Titular);
}
