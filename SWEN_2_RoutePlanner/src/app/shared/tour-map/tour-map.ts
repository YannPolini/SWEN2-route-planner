import {
  Component,
  input,
  effect,
  AfterViewInit,
  OnDestroy,
  ElementRef,
  viewChild,
  inject,
  PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-tour-map',
  standalone: true,
  template: `
    <div #mapEl class="tour-map rounded-3 border"></div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .tour-map {
        width: 100%;
        height: 300px;
        z-index: 0;
        background: #f1f3f5;
      }
    `,
  ],
})
export class TourMapComponent implements AfterViewInit, OnDestroy {
  /** Display name of start location */
  fromLocation = input<string>('');
  /** Display name of end location */
  toLocation = input<string>('');

  private mapEl = viewChild.required<ElementRef>('mapEl');
  private platformId = inject(PLATFORM_ID);

  private map: any;
  private L: any;

  constructor() {
    // Re-center map when inputs change
    effect(() => {
      const from = this.fromLocation();
      const to = this.toLocation();
      if (this.map) {
        this.map.invalidateSize();
      }
    });
  }

  async ngAfterViewInit(): Promise<void> {
    // Leaflet needs `window` — skip on server
    if (!isPlatformBrowser(this.platformId)) return;

    // Dynamic import keeps Leaflet out of the SSR bundle
    const L = await import('leaflet');
    this.L = L;

    // Fix default marker icons (broken by bundlers)
    delete (L.Icon.Default.prototype as any)._getIconUrl;
    L.Icon.Default.mergeOptions({
      iconRetinaUrl:
        'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      iconUrl:
        'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl:
        'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    });

    const el = this.mapEl().nativeElement;

    // Default center: Vienna
    this.map = L.map(el, {
      center: [48.2082, 16.3738],
      zoom: 12,
      zoomControl: true,
    });

    // OpenStreetMap tile layer
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:
        '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);

    // Default marker
    const marker = L.marker([48.2082, 16.3738]).addTo(this.map);

    const from = this.fromLocation();
    const to = this.toLocation();
    if (from && to) {
      marker.bindPopup(`<strong>${from}</strong> → <strong>${to}</strong>`).openPopup();
    }

    // Fix rendering in dynamically-sized containers
    setTimeout(() => this.map?.invalidateSize(), 250);
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = undefined;
  }
}
