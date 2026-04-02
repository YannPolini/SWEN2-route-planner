import { Component, inject, signal } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { TourService } from '../services/tour.service';
import { Tour, TransportType, TRANSPORT_TYPES } from '../models/tour.model';
import { SearchBarComponent } from '../shared/search-bar/search-bar';
import { TourMapComponent } from '../shared/tour-map/tour-map';

@Component({
  selector: 'app-tours',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, DatePipe, SearchBarComponent, TourMapComponent],
  templateUrl: './tours.html',
  styleUrl: './tours.css',
})
export class ToursComponent {
  protected readonly tourService = inject(TourService);
  private readonly fb = inject(FormBuilder);

  protected readonly transportTypes = TRANSPORT_TYPES;
  protected readonly showForm = signal(false);
  protected readonly editingTour = signal<Tour | null>(null);
  protected readonly deleteTarget = signal<Tour | null>(null);

  protected readonly tourForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required]],
    from: ['', [Validators.required]],
    to: ['', [Validators.required]],
    transportType: ['hike' as TransportType, [Validators.required]],
    distance: [0, [Validators.required, Validators.min(0.1)]],
    estimatedTimeMinutes: [0, [Validators.required, Validators.min(1)]],
  });
  protected readonly formSubmitted = signal(false);

  // ── Helpers ──
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

  // ── Tour CRUD ──
  protected openCreate(): void {
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

  protected openEdit(tour: Tour): void {
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
      routeImagePath: '',
    };

    const editing = this.editingTour();
    if (editing) {
      this.tourService.updateTour(editing.id, data);
    } else {
      this.tourService.addTour(data);
    }
    this.closeForm();
  }

  protected confirmDelete(tour: Tour): void {
    this.deleteTarget.set(tour);
  }

  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected executeDelete(): void {
    const t = this.deleteTarget();
    if (t) {
      this.tourService.deleteTour(t.id);
    }
    this.deleteTarget.set(null);
  }
}
