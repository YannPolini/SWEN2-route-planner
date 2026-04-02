import { Injectable, signal, computed } from '@angular/core';
import { Tour, TransportType } from '../models/tour.model';

const MOCK_TOURS: Tour[] = [
  {
    id: '1',
    name: 'Donauinsel Rundweg',
    description: 'Gemütliche Radtour entlang der Donauinsel mit Blick auf die Donau.',
    from: 'Wien Floridsdorf',
    to: 'Wien Donaustadt',
    transportType: 'bike',
    distance: 21.5,
    estimatedTime: 4200,
    routeImagePath: '',
    createdAt: new Date('2026-03-01'),
  },
  {
    id: '2',
    name: 'Schneeberg Gipfeltour',
    description: 'Anspruchsvolle Wanderung zum höchsten Berg Niederösterreichs.',
    from: 'Puchberg am Schneeberg',
    to: 'Schneeberg Gipfel',
    transportType: 'hike',
    distance: 12.8,
    estimatedTime: 18000,
    routeImagePath: '',
    createdAt: new Date('2026-02-15'),
  },
  {
    id: '3',
    name: 'Prater Laufrunde',
    description: 'Flache Laufstrecke durch den Wiener Prater, ideal für Intervalltraining.',
    from: 'Praterstern',
    to: 'Lusthaus',
    transportType: 'running',
    distance: 8.2,
    estimatedTime: 2700,
    routeImagePath: '',
    createdAt: new Date('2026-03-10'),
  },
  {
    id: '4',
    name: 'Wachau Radweg',
    description: 'Malerische Tour durch das UNESCO Weltkulturerbe entlang der Donau.',
    from: 'Melk',
    to: 'Krems an der Donau',
    transportType: 'bike',
    distance: 36.0,
    estimatedTime: 7200,
    routeImagePath: '',
    createdAt: new Date('2026-02-28'),
  },
  {
    id: '5',
    name: 'Salzburg Stadtspaziergang',
    description: 'Entspannter Spaziergang durch die Altstadt mit Besuch der Festung.',
    from: 'Salzburg Hauptbahnhof',
    to: 'Festung Hohensalzburg',
    transportType: 'vacation',
    distance: 4.5,
    estimatedTime: 10800,
    routeImagePath: '',
    createdAt: new Date('2026-03-05'),
  },
];

@Injectable({ providedIn: 'root' })
export class TourService {
  // ── Private writable signals (Single Source of Truth) ──
  private readonly _tours = signal<Tour[]>(MOCK_TOURS);
  private readonly _selectedTourId = signal<string | null>(null);
  private readonly _searchTerm = signal<string>('');
  private readonly _filterType = signal<TransportType | null>(null);

  // ── Public readonly signals (View binds to these) ──
  readonly tours = this._tours.asReadonly();
  readonly selectedTourId = this._selectedTourId.asReadonly();
  readonly searchTerm = this._searchTerm.asReadonly();
  readonly filterType = this._filterType.asReadonly();

  // ── Derived state (computed signals) ──
  readonly filteredTours = computed(() => {
    const term = this._searchTerm().toLowerCase().trim();
    const type = this._filterType();
    let result = this._tours();

    // Filter by transport type
    if (type) {
      result = result.filter((t) => t.transportType === type);
    }

    // Filter by search term
    if (term) {
      result = result.filter(
        (t) =>
          t.name.toLowerCase().includes(term) ||
          t.description.toLowerCase().includes(term) ||
          t.from.toLowerCase().includes(term) ||
          t.to.toLowerCase().includes(term) ||
          t.transportType.toLowerCase().includes(term),
      );
    }

    return result;
  });

  readonly selectedTour = computed(() => {
    const id = this._selectedTourId();
    return id ? (this._tours().find((t) => t.id === id) ?? null) : null;
  });

  readonly tourCount = computed(() => this._tours().length);

  // ── Statistics (derived, always up-to-date) ──
  readonly stats = computed(() => {
    const tours = this._tours();
    const count = tours.length;
    const totalDistance = tours.reduce((sum, t) => sum + t.distance, 0);
    const totalTime = tours.reduce((sum, t) => sum + t.estimatedTime, 0);
    const avgDistance = count > 0 ? totalDistance / count : 0;
    const avgTime = count > 0 ? totalTime / count : 0;

    const byType = new Map<TransportType, number>();
    for (const t of tours) {
      byType.set(t.transportType, (byType.get(t.transportType) ?? 0) + 1);
    }

    return { count, totalDistance, totalTime, avgDistance, avgTime, byType };
  });

  // ── Intent methods (controlled writes) ──
  selectTour(id: string | null): void {
    this._selectedTourId.set(id);
  }

  updateSearch(term: string): void {
    this._searchTerm.set(term);
  }

  setFilterType(type: TransportType | null): void {
    this._filterType.set(type);
  }

  addTour(data: Omit<Tour, 'id' | 'createdAt'>): void {
    const newTour: Tour = {
      ...data,
      id: crypto.randomUUID(),
      createdAt: new Date(),
    };
    this._tours.update((tours) => [newTour, ...tours]);
    this._selectedTourId.set(newTour.id);
  }

  updateTour(id: string, changes: Partial<Tour>): void {
    this._tours.update((tours) => tours.map((t) => (t.id === id ? { ...t, ...changes } : t)));
  }

  deleteTour(id: string): void {
    this._tours.update((tours) => tours.filter((t) => t.id !== id));
    if (this._selectedTourId() === id) {
      this._selectedTourId.set(null);
    }
  }
}
