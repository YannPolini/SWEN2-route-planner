import { Injectable, signal, computed, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Tour, TransportType, TRANSPORT_TYPES } from '../models/tour.model';

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
  private readonly fb = inject(FormBuilder);

  // ════════════════════════════════════════
  // MODEL — domain data (Single Source of Truth)
  // ════════════════════════════════════════
  private readonly _tours = signal<Tour[]>(MOCK_TOURS);

  // ════════════════════════════════════════
  // VIEW-MODEL — UI state
  // ════════════════════════════════════════
  private readonly _selectedTourId = signal<string | null>(null);
  private readonly _searchTerm = signal<string>('');
  private readonly _filterType = signal<TransportType | null>(null);
  private readonly _showForm = signal(false);
  private readonly _editingTour = signal<Tour | null>(null);
  private readonly _deleteTarget = signal<Tour | null>(null);
  private readonly _formSubmitted = signal(false);

  // ── Exposed readonly state (View binds to these) ──
  readonly selectedTourId = this._selectedTourId.asReadonly();
  readonly searchTerm = this._searchTerm.asReadonly();
  readonly filterType = this._filterType.asReadonly();
  readonly showForm = this._showForm.asReadonly();
  readonly editingTour = this._editingTour.asReadonly();
  readonly deleteTarget = this._deleteTarget.asReadonly();
  readonly formSubmitted = this._formSubmitted.asReadonly();
  readonly transportTypes = TRANSPORT_TYPES;

  // ── Reactive Form (owned by ViewModel) ──
  readonly tourForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required]],
    from: ['', [Validators.required]],
    to: ['', [Validators.required]],
    transportType: ['hike' as TransportType, [Validators.required]],
    distance: [0, [Validators.required, Validators.min(0.1)]],
    estimatedTimeMinutes: [0, [Validators.required, Validators.min(1)]],
  });

  // ════════════════════════════════════════
  // DERIVED STATE (computed signals)
  // ════════════════════════════════════════
  readonly filteredTours = computed(() => {
    const term = this._searchTerm().toLowerCase().trim();
    const type = this._filterType();
    let result = this._tours();
    if (type) result = result.filter((t) => t.transportType === type);
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

  // ════════════════════════════════════════
  // INTENT METHODS — commands from the View
  // ════════════════════════════════════════

  selectTour(id: string | null): void {
    this._selectedTourId.set(id);
  }
  updateSearch(term: string): void {
    this._searchTerm.set(term);
  }
  setFilterType(type: TransportType | null): void {
    this._filterType.set(type);
  }

  openCreateForm(): void {
    this._editingTour.set(null);
    this.tourForm.reset({
      name: '',
      description: '',
      from: '',
      to: '',
      transportType: 'hike',
      distance: 0,
      estimatedTimeMinutes: 0,
    });
    this._formSubmitted.set(false);
    this._showForm.set(true);
  }

  openEditForm(tour: Tour): void {
    this._editingTour.set(tour);
    this.tourForm.setValue({
      name: tour.name,
      description: tour.description,
      from: tour.from,
      to: tour.to,
      transportType: tour.transportType,
      distance: tour.distance,
      estimatedTimeMinutes: Math.round(tour.estimatedTime / 60),
    });
    this._formSubmitted.set(false);
    this._showForm.set(true);
  }

  closeForm(): void {
    this._showForm.set(false);
    this._editingTour.set(null);
  }

  submitForm(): void {
    this._formSubmitted.set(true);
    if (this.tourForm.invalid) {
      this.tourForm.markAllAsTouched();
      return;
    }
    const v = this.tourForm.getRawValue();
    const data: Omit<Tour, 'id' | 'createdAt'> = {
      name: v.name.trim(),
      description: v.description.trim(),
      from: v.from.trim(),
      to: v.to.trim(),
      transportType: v.transportType,
      distance: +v.distance,
      estimatedTime: +v.estimatedTimeMinutes * 60,
      routeImagePath: this._editingTour()?.routeImagePath ?? '',
    };
    const editing = this._editingTour();
    if (editing) {
      this._tours.update((tours) =>
        tours.map((t) => (t.id === editing.id ? { ...t, ...data } : t)),
      );
    } else {
      const newTour: Tour = { ...data, id: crypto.randomUUID(), createdAt: new Date() };
      this._tours.update((tours) => [newTour, ...tours]);
      this._selectedTourId.set(newTour.id);
    }
    this.closeForm();
  }

  confirmDelete(tour: Tour): void {
    this._deleteTarget.set(tour);
  }
  cancelDelete(): void {
    this._deleteTarget.set(null);
  }

  executeDelete(): void {
    const t = this._deleteTarget();
    if (t) {
      this._tours.update((tours) => tours.filter((tour) => tour.id !== t.id));
      if (this._selectedTourId() === t.id) this._selectedTourId.set(null);
    }
    this._deleteTarget.set(null);
  }

  // ════════════════════════════════════════
  // PRESENTATION HELPERS (pure functions)
  // ════════════════════════════════════════
  formatDuration(seconds: number): string {
    if (!seconds || seconds <= 0) return '—';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0 && m > 0) return `${h}h ${m}min`;
    if (h > 0) return `${h}h`;
    return `${m}min`;
  }

  badgeClass(type: string): string {
    const map: Record<string, string> = {
      bike: 'text-bg-primary',
      hike: 'text-bg-success',
      running: 'text-bg-warning',
      vacation: 'text-bg-info',
    };
    return map[type] ?? 'text-bg-secondary';
  }
}
