import { Component, computed, input } from '@angular/core';
import { TransportType } from '../../models/tour.model';

const TRANSPORT_ICON_PATHS: Record<string, string> = {
  bike: '/transport-icons/bicycle-3-svgrepo-com.svg',
  hike: '/transport-icons/hiking-solid-svgrepo-com.svg',
  running: '/transport-icons/running-round-svgrepo-com.svg',
  vehicle: '/transport-icons/car-side-svgrepo-com.svg',
};

@Component({
  selector: 'app-transport-icon',
  standalone: true,
  template: `
    <span
      class="transport-icon"
      role="img"
      [attr.aria-label]="label()"
      [style.--transport-icon-url]="iconCssUrl()"
      [title]="label()"
    >
      <span class="transport-icon__glyph" aria-hidden="true"></span>
    </span>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        line-height: 0;
      }

      .transport-icon,
      .transport-icon__glyph {
        display: inline-block;
        width: 1.25rem;
        height: 1.25rem;
      }

      .transport-icon__glyph {
        background: currentColor;
        mask: var(--transport-icon-url) center / contain no-repeat;
        -webkit-mask: var(--transport-icon-url) center / contain no-repeat;
      }
    `,
  ],
})
export class TransportIconComponent {
  type = input<TransportType | string>('hike');
  label = input<string>('Transport type');
  iconCssUrl = computed(
    () => `url("${TRANSPORT_ICON_PATHS[this.type()] ?? TRANSPORT_ICON_PATHS['hike']}")`
  );
}
