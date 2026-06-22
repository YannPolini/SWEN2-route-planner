import { Injectable, signal, computed, inject } from '@angular/core';
import { Tour, TransportType } from '../models/tour.model';
import { TourApiService } from '../models/tour.model.api';

// ═══════════════════════════════════════════════════════
// MODEL (Service) — domain data + business logic ONLY
// No UI state, no form logic, no modal state here.
// ═══════════════════════════════════════════════════════
@Injectable({ providedIn: 'root' })
export class TourService {
  // ── Domain state (Single Source of Truth) ──
  private readonly _tours = signal<Tour[]>([]);

  private readonly _selectedTourId = signal<string | null>(null);

  // ── Public readonly access ──
  readonly tours = this._tours.asReadonly();

  readonly selectedTourId = this._selectedTourId.asReadonly();

  readonly selectedTour = computed(() => {
    const id = this._selectedTourId();
    return id ? (this._tours().find((t) => t.id === id) ?? null) : null;
  });

  // ── Derived domain data ──
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

  // ── CRUD operations (business logic) ──
  addTour(data: Omit<Tour, 'id' | 'createdAt'>): Tour {
    const newTour: Tour = {
      ...data,
      id: crypto.randomUUID(),
      createdAt: new Date(),
    };
    this.api.create(newTour).subscribe({
      next: () => this.loadTours(),
      error: err => console.error('API Fehler beim Create:', err),
    });
    return newTour;
  }

  updateTour(id: string, changes: Partial<Tour>): void {
    const existingTour = this._tours().find(tour => tour.id === id);
    if (!existingTour) {
      console.error('Tour not found:', id);
      return;
    }

    const updatedTour: Tour = { ...existingTour, ...changes };

    // Reload from backend on success so derived state stays in sync.
    this.api.update(updatedTour).subscribe({
      next: () => this.loadTours(),
      error: err => console.error('API Fehler beim Update:', err),
    });
  }

  deleteTour(id: string): void {
    this.api.delete(id).subscribe({
      next: () => {
        this.loadTours();
        if (this._selectedTourId() === id) {
          this._selectedTourId.set(null);
        }
      },
      error: err => console.error('API Fehler beim Delete:', err),
    });
  }

  selectTour(id: string | null): void {
    this._selectedTourId.set(id);
  }

  getTourById(id: string): Tour | undefined {
    return this._tours().find((t) => t.id === id);
  }

  constructor() {
    this.loadTours();
  }

  protected readonly api = inject(TourApiService);

  loadTours(): void {
    this.api.getAll().subscribe({
      next: tours => this._tours.set(tours),
      error: err => console.error('API Fehler:', err),
    });
  }
}
