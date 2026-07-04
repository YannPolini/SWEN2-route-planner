import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ImportExport } from '../import-export/import-export';
import { Tour, TransportType, TRANSPORT_TYPES, WeatherForecast } from '../models/tour.model';
import { TourService } from '../services/tour.service';
import { AddressInputComponent, Coordinates } from '../shared/address-input/address-input';
import { SearchBarComponent } from '../shared/search-bar/search-bar';
import { TourMapComponent } from '../shared/tour-map/tour-map';
import { TransportIconComponent } from '../shared/transport-icon/transport-icon';
import { TourlogsModel } from '../tourlogs.model/tourlogs.model';
import { TourlogsComponent } from '../tourlogs/tourlogs';

@Component({
  selector: 'app-tours',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    DecimalPipe,
    SearchBarComponent,
    TourMapComponent,
    TransportIconComponent,
    TourlogsComponent,
    AddressInputComponent,
    ImportExport,
  ],
  templateUrl: './tours.html',
  styleUrl: './tours.css',
})
export class ToursComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly tourService = inject(TourService);
  private readonly fb = inject(FormBuilder);

  protected readonly tourlogsModel = inject(TourlogsModel);
  protected readonly transportTypes = TRANSPORT_TYPES;

  protected readonly detailTourId = signal<string | null>(null);
  protected readonly searchTerm = signal<string>('');
  protected readonly filterType = signal<TransportType | null>(null);
  protected readonly showForm = signal(false);
  protected readonly editingTour = signal<Tour | null>(null);
  protected readonly deleteTarget = signal<Tour | null>(null);
  protected readonly formSubmitted = signal(false);
  protected readonly weatherForecast = signal<WeatherForecast | null>(null);
  protected readonly weatherLoading = signal(false);
  protected readonly weatherError = signal(false);
  protected readonly fromCoords = signal<Coordinates | null>(null);
  protected readonly toCoords = signal<Coordinates | null>(null);
  protected readonly demoSeedLoading = signal(false);
  protected readonly demoSeedError = signal('');

  private weatherRequestTourId: string | null = null;

  protected readonly tourForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required]],
    from: ['', [Validators.required]],
    to: ['', [Validators.required]],
    transportType: ['hike' as TransportType, [Validators.required]],
  });

  protected readonly filteredTours = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const type = this.filterType();
    let result = this.tourService.tours();

    if (type) result = result.filter((tour) => tour.transportType === type);

    if (term) {
      result = result.filter(
        (tour) =>
          tour.name.toLowerCase().includes(term) ||
          tour.description.toLowerCase().includes(term) ||
          tour.from.toLowerCase().includes(term) ||
          tour.to.toLowerCase().includes(term) ||
          tour.transportType.toLowerCase().includes(term) ||
          String(tour.difficulty ?? '').includes(term) ||
          String(tour.childFriendliness).includes(term),
      );
    }

    return result;
  });

  protected readonly selectedTour = computed(() => {
    const id = this.detailTourId();
    return id ? (this.tourService.getTourById(id) ?? null) : null;
  });

  protected readonly tourCount = this.tourService.tourCount;
  protected readonly stats = this.tourService.stats;

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const id = params.get('id');
      this.detailTourId.set(id);
      this.tourService.selectTour(id);

      if (id) {
        this.loadWeather(id);
      } else {
        this.weatherForecast.set(null);
        this.weatherError.set(false);
        this.weatherLoading.set(false);
      }
    });
  }

  protected openTour(id: string): void {
    this.router.navigate(['/tours', id]);
  }

  protected backToTours(): void {
    this.router.navigate(['/tours']);
  }

  protected updateSearch(term: string): void {
    this.searchTerm.set(term);
  }

  protected setFilterType(type: TransportType | null): void {
    this.filterType.set(type);
  }

  protected openCreateForm(): void {
    this.editingTour.set(null);
    this.tourForm.reset({
      name: '',
      description: '',
      from: '',
      to: '',
      transportType: 'hike',
    });
    this.fromCoords.set(null);
    this.toCoords.set(null);
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
    });
    this.fromCoords.set(
      tour.fromLat != null && tour.fromLng != null
        ? { lat: tour.fromLat, lng: tour.fromLng }
        : null,
    );
    this.toCoords.set(
      tour.toLat != null && tour.toLng != null ? { lat: tour.toLat, lng: tour.toLng } : null,
    );
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
    const from = this.fromCoords();
    const to = this.toCoords();
    const data: Omit<Tour, 'id' | 'createdAt'> = {
      name: v.name.trim(),
      description: v.description.trim(),
      from: v.from.trim(),
      to: v.to.trim(),
      transportType: v.transportType,
      fromLat: from?.lat ?? null,
      fromLng: from?.lng ?? null,
      toLat: to?.lat ?? null,
      toLng: to?.lng ?? null,
      distance: 0,
      estimatedTime: 0,
      childFriendliness: 0,
      routeImagePath: this.editingTour()?.routeImagePath ?? '',
      routeGeometry: null,
    };

    const editing = this.editingTour();
    if (editing) {
      this.tourService.updateTour(editing.id, data);
    } else {
      const newTour = this.tourService.addTour(data);
      this.router.navigate(['/tours', newTour.id]);
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
    const target = this.deleteTarget();
    if (!target) return;

    const wasOpen = this.detailTourId() === target.id;
    this.tourService.deleteTour(target.id);
    this.deleteTarget.set(null);

    if (wasOpen) {
      this.router.navigate(['/tours']);
    }
  }

  protected formatDuration(seconds: number): string {
    if (!seconds || seconds <= 0) return '-';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0 && m > 0) return `${h}h ${m}min`;
    if (h > 0) return `${h}h`;
    return `${m}min`;
  }

  protected formatDifficulty(difficulty: number | null | undefined): string {
    if (difficulty == null) return '-';

    return Number.isInteger(difficulty) ? `${difficulty}/5` : `${difficulty.toFixed(1)}/5`;
  }

  protected badgeClass(type: string): string {
    const map: Record<string, string> = {
      bike: 'tour-type-bike',
      hike: 'tour-type-hike',
      running: 'tour-type-running',
      vehicle: 'tour-type-vehicle',
    };
    return map[type] ?? 'tour-type-default';
  }

  protected transportLabel(type: TransportType | string): string {
    return this.transportTypes.find((transport) => transport.value === type)?.label ?? 'Transport';
  }

  protected weatherIconUrl(icon: string): string {
    return `https://openweathermap.org/img/wn/${icon}@2x.png`;
  }

  protected logCount(tourId: string): number {
    return this.tourlogsModel.getPopularity(tourId);
  }

  protected seedDemoData(): void {
    if (this.demoSeedLoading()) return;

    this.demoSeedLoading.set(true);
    this.demoSeedError.set('');

    this.tourService.seedDemoData().subscribe({
      next: () => {
        this.tourlogsModel.loadLogs();
        this.demoSeedLoading.set(false);
      },
      error: (err) => {
        console.error('Demo data seed failed:', err);
        this.demoSeedError.set('Could not add demo data. Please try again.');
        this.demoSeedLoading.set(false);
      },
    });
  }

  private loadWeather(tourId: string): void {
    this.weatherRequestTourId = tourId;
    this.weatherForecast.set(null);
    this.weatherError.set(false);
    this.weatherLoading.set(true);

    this.tourService.getWeatherForecast(tourId).subscribe({
      next: (forecast) => {
        if (this.weatherRequestTourId !== tourId) return;
        this.weatherForecast.set(forecast);
        this.weatherLoading.set(false);
      },
      error: (err) => {
        if (this.weatherRequestTourId !== tourId) return;
        console.error('Weather API error:', err);
        this.weatherError.set(true);
        this.weatherLoading.set(false);
      },
    });
  }
}
