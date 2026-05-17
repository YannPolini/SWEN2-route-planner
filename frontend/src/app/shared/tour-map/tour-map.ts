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
  template: `<div #mapEl class="tour-map rounded-3 border"></div>`,
  styles: [`
    :host { display: block; }
    .tour-map {
      width: 100%;
      height: 320px;
      z-index: 0;
      background: #f1f3f5;
    }
  `],
})
export class TourMapComponent implements AfterViewInit, OnDestroy {
  fromLocation  = input<string>('');
  toLocation    = input<string>('');
  /**
   * JSON string from backend: [[lat,lng],[lat,lng],...]
   * null / empty → Vienna overview, no route drawn.
   */
  routeGeometry = input<string | null>(null);

  private mapEl      = viewChild.required<ElementRef>('mapEl');
  private platformId = inject(PLATFORM_ID);

  private map:         any = null;
  private L:           any = null;
  private polyline:    any = null;
  private startMarker: any = null;
  private endMarker:   any = null;

  constructor() {
    // Runs whenever any input changes; map must exist first
    effect(() => {
      const geometry = this.routeGeometry();
      const from     = this.fromLocation();
      const to       = this.toLocation();
      if (this.map && this.L) {
        this.redrawRoute(geometry, from, to);
      }
    });
  }

  async ngAfterViewInit(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;

    const L = await import('leaflet');
    this.L = L;

    // Fix bundler-broken default icon paths
    delete (L.Icon.Default.prototype as any)._getIconUrl;
    L.Icon.Default.mergeOptions({
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    });

    this.map = L.map(this.mapEl().nativeElement, {
      center: [48.2082, 16.3738],
      zoom: 12,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 19,
    }).addTo(this.map);

    // Container must be in DOM before drawing
    setTimeout(() => {
      this.map?.invalidateSize();
      this.redrawRoute(this.routeGeometry(), this.fromLocation(), this.toLocation());
    }, 250);
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
  }

  private redrawRoute(geometryJson: string | null | undefined, from: string, to: string): void {
    this.clearLayers();

    if (!geometryJson || geometryJson.trim() === '') {
      this.map.setView([48.2082, 16.3738], 10);
      return;
    }

    let coords: number[][];
    try {
      coords = JSON.parse(geometryJson);
    } catch {
      console.warn('[TourMapComponent] routeGeometry is not valid JSON');
      return;
    }

    if (!coords || coords.length === 0) return;

    const L = this.L;

    this.polyline = L.polyline(coords, { color: '#2563eb', weight: 4, opacity: 0.85 })
      .addTo(this.map);

    const shadow = 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png';
    const base   = 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img';

    const greenIcon = L.icon({
      iconUrl: `${base}/marker-icon-2x-green.png`, shadowUrl: shadow,
      iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
    });
    const redIcon = L.icon({
      iconUrl: `${base}/marker-icon-2x-red.png`, shadowUrl: shadow,
      iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
    });

    this.startMarker = L.marker(coords[0], { icon: greenIcon })
      .addTo(this.map)
      .bindPopup(`<strong>Start:</strong> ${from || 'Start'}`);

    this.endMarker = L.marker(coords[coords.length - 1], { icon: redIcon })
      .addTo(this.map)
      .bindPopup(`<strong>End:</strong> ${to || 'End'}`);

    this.map.fitBounds(this.polyline.getBounds(), { padding: [30, 30] });
  }

  private clearLayers(): void {
    if (this.polyline)    { this.map.removeLayer(this.polyline);    this.polyline    = null; }
    if (this.startMarker) { this.map.removeLayer(this.startMarker); this.startMarker = null; }
    if (this.endMarker)   { this.map.removeLayer(this.endMarker);   this.endMarker   = null; }
  }
}
