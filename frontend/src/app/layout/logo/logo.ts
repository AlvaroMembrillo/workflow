import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Tres pasos unidos: una candidatura que avanza hasta la decisión. */
@Component({
  selector: 'app-logo',
  template: `
    <svg viewBox="0 0 38 20" aria-hidden="true">
      <line x1="5" y1="10" x2="33" y2="10" />
      <circle class="paso" cx="5" cy="10" r="4" />
      <circle class="paso" cx="19" cy="10" r="4" />
      <circle class="final" cx="33" cy="10" r="5" />
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
    }
    svg {
      width: 1.9rem;
      height: 1rem;
    }
    line {
      stroke: var(--line);
      stroke-width: 2;
    }
    .paso {
      fill: var(--muted);
    }
    .final {
      fill: var(--brand);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Logo {}
