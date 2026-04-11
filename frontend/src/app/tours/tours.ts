import { Component, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TourService } from '../services/tour.service';
import { Tour, TransportType, TRANSPORT_TYPES } from '../models/tour.model';
import { SearchBarComponent } from '../shared/search-bar/search-bar';
import { TourMapComponent } from '../shared/tour-map/tour-map';
import { TourlogsComponent } from '../tourlogs/tourlogs';

// ═══════════════════════════════════════════════════════
// VIEW-MODEL (Component) — UI state, UI logic, actions
// Connects the Model (TourService) with the View (template).
// ═══════════════════════════════════════════════════════
@Component({
  selector: 'app-tours',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    SearchBarComponent,
    TourMapComponent,
    TourlogsComponent,
  ],
  templateUrl: './tours.html',
  styleUrl: './tours.css',
})
export class ToursComponent {
  // ── Injected Model (domain data + business logic) ──
  private readonly tourService = inject(TourService);
  private readonly fb = inject(FormBuilder);

  // ── Constants ──
  protected readonly transportTypes = TRANSPORT_TYPES;

  // ── UI state (owned by ViewModel) ──
  protected readonly selectedTourId = this.tourService.selectedTourId;
  protected readonly searchTerm = signal<string>('');
  protected readonly filterType = signal<TransportType | null>(null);
  protected readonly showForm = signal(false);
  protected readonly editingTour = signal<Tour | null>(null);
  protected readonly deleteTarget = signal<Tour | null>(null);
  protected readonly formSubmitted = signal(false);

  // ── Reactive Form (owned by ViewModel) ──
  protected readonly tourForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required]],
    from: ['', [Validators.required]],
    to: ['', [Validators.required]],
    transportType: ['hike' as TransportType, [Validators.required]],
    distance: [0, [Validators.required, Validators.min(0.1)]],
    estimatedTimeMinutes: [0, [Validators.required, Validators.min(1)]],
  });

  // ── Derived state (combines Model data + UI state) ──
  protected readonly filteredTours = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const type = this.filterType();
    let result = this.tourService.tours();
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

  // ── Delegated Model data (pass-through to template) ──
  protected readonly selectedTour = this.tourService.selectedTour;
  protected readonly tourCount = this.tourService.tourCount;
  protected readonly stats = this.tourService.stats;

  // ══════════════════════════════════════
  // ACTIONS — called from the template
  // ══════════════════════════════════════

  protected selectTour(id: string): void {
    this.tourService.selectTour(id);
  }

  protected updateSearch(term: string): void {
    this.searchTerm.set(term);
  }

  protected setFilterType(type: TransportType | null): void {
    this.filterType.set(type);
  }

  // ── Form workflow ──
  protected openCreateForm(): void {
    this.editingTour.set(null);
    this.tourForm.reset({
      name: '',
      description: '',
      from: '',
      to: '',
      transportType: 'hike',
      distance: 0,
      estimatedTimeMinutes: 0,
    });
    this.formSubmitted.set(false);
    this.showForm.set(true);
  }

  protected openEditForm(tour: Tour): void {
    this.editingTour.set(tour);
    this.tourForm.setValue({
      name: tour.name,
      description: tour.description,
      from: tour.from,
      to: tour.to,
      transportType: tour.transportType,
      distance: tour.distance,
      estimatedTimeMinutes: Math.round(tour.estimatedTime / 60),
    });
    this.formSubmitted.set(false);
    this.showForm.set(true);
  }

  protected closeForm(): void {
    this.showForm.set(false);
    this.editingTour.set(null);
  }

  protected submitForm(): void {
    this.formSubmitted.set(true);
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
      routeImagePath: this.editingTour()?.routeImagePath ?? '',
    };

    const editing = this.editingTour();
    if (editing) {
      this.tourService.updateTour(editing.id, data); // ← calls Model
    } else {
      const newTour = this.tourService.addTour(data); // ← calls Model
      this.tourService.selectTour(newTour.id);
    }
    this.closeForm();
  }

  // ── Delete workflow ──
  protected confirmDelete(tour: Tour): void {
    this.deleteTarget.set(tour);
  }
  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected executeDelete(): void {
    const t = this.deleteTarget();
    if (t) {
      this.tourService.deleteTour(t.id); // ← calls Model
      if (this.selectedTourId() === t.id) this.tourService.selectTour(null);
    }
    this.deleteTarget.set(null);
  }

  // ── Presentation helpers ──
  protected formatDuration(seconds: number): string {
    if (!seconds || seconds <= 0) return '—';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0 && m > 0) return `${h}h ${m}min`;
    if (h > 0) return `${h}h`;
    return `${m}min`;
  }

  protected badgeClass(type: string): string {
    const map: Record<string, string> = {
      bike: 'text-bg-primary',
      hike: 'text-bg-success',
      running: 'text-bg-warning',
      vacation: 'text-bg-info',
    };
    return map[type] ?? 'text-bg-secondary';
  }
}
