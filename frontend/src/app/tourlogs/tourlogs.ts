import { Component, computed, effect, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { TourlogsList } from '../tourlogs-list/tourlogs-list';
import { TourService } from '../services/tour.service';
import { Log, TourlogsModel } from '../tourlogs.model/tourlogs.model';
import { SearchBarComponent } from '../shared/search-bar/search-bar';

@Component({
  selector: 'app-tourlogs',
  standalone: true,
  imports: [ReactiveFormsModule, TourlogsList, SearchBarComponent],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})
export class TourlogsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly tourlogsModel = inject(TourlogsModel);
  private readonly tourService = inject(TourService);

  protected readonly logList = this.tourlogsModel.logList;
  protected readonly today = new Date().toISOString().split('T')[0];
  protected readonly minDate = '1900-01-01';
  protected readonly formSubmitted = signal(false);
  protected readonly filteredLogs = this.tourlogsModel.filteredLogs;
  protected readonly searchTerm = this.tourlogsModel.searchTerm;
  protected readonly canAddLog = computed(() => this.tourService.selectedTourId() !== null);
  protected readonly showFormPopup = signal(false);

  private readonly selectedLogId = signal<string | null>(null);
  private readonly editingLogId = signal<string | null>(null);

  readonly logForm = this.fb.nonNullable.group({
    date: ['', [Validators.required, this.maxDateValidator(this.today), this.minDateValidator(this.minDate)]],
    time: ['', [Validators.required]],
    comment: ['', [Validators.required]],
    difficulty: [0, [Validators.required, Validators.min(0), Validators.max(5)]],
    totalDistance: [0, [Validators.required, Validators.min(0)]],
    totalTime: [0, [Validators.required, Validators.min(0)]],
    rating: [0, [Validators.required, Validators.min(0), Validators.max(5)]],
    tourID: [''],
  });

  protected readonly selectedLog = computed(
    () => this.tourlogsModel.logList().find((log) => log.logID === this.selectedLogId()) ?? null,
  );

  protected readonly editingLog = computed(
    () => this.tourlogsModel.logList().find((log) => log.logID === this.editingLogId()) ?? null,
  );

  constructor() {
    effect(() => {
      this.tourService.selectedTourId();
      this.selectedLogId.set(null);
      this.editingLogId.set(null);
      this.showFormPopup.set(false);
      this.resetForm();
    });
  }

  protected updateSearch(term: string): void {
    this.tourlogsModel.setSearchTerm(term);
  }

  protected openAdd(): void {
    if (!this.tourService.selectedTour()) {
      return;
    }

    this.editingLogId.set(null);
    this.resetForm();
    this.formSubmitted.set(false);
    this.showFormPopup.set(true);
  }

  protected selectLog(log: Log): void {
    this.selectedLogId.set(log.logID);
  }

  protected openEdit(log: Log): void {
    this.editingLogId.set(log.logID);
    this.logForm.setValue({
      date: log.date,
      time: log.time,
      comment: log.comment,
      difficulty: log.difficulty,
      totalDistance: log.totalDistance,
      totalTime: log.totalTime,
      rating: log.rating,
      tourID: log.tourID,
    });

    this.formSubmitted.set(false);
    this.showFormPopup.set(true);
  }

  protected deleteLog(): void {
    const currentLog = this.selectedLog();

    if (!currentLog) return;

    this.tourlogsModel.deleteLog(currentLog.logID);
    this.selectedLogId.set(null);
  }

  protected closeFormPopup(): void {
    this.showFormPopup.set(false);
    this.editingLogId.set(null);
    this.formSubmitted.set(false);
    this.resetForm();
  }

  protected saveLog(): void {
    this.formSubmitted.set(true);

    if (this.logForm.invalid) {
      return;
    }

    const formValue = this.logForm.getRawValue();
    const currentLog = this.editingLog();
    const selectedTourId = this.tourService.selectedTourId();

    if (!selectedTourId) {
      return;
    }

    if (currentLog) {
      this.tourlogsModel.updateLog({ ...currentLog, ...formValue });
      this.selectedLogId.set(currentLog.logID);
    } else {
      const newLog: Log = {
        date: formValue.date,
        time: formValue.time,
        comment: formValue.comment,
        difficulty: formValue.difficulty,
        totalDistance: formValue.totalDistance,
        totalTime: formValue.totalTime,
        rating: formValue.rating,
        tourID: selectedTourId,
        logID: Date.now().toString(),
        ownerUserId: null,
        creatorName: '',
      };

      this.tourlogsModel.addLog(newLog);
      this.selectedLogId.set(newLog.logID);
    }

    this.closeFormPopup();
  }

  private maxDateValidator(maxDate: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      return control.value > maxDate ? { maxDate: true } : null;
    };
  }

  private minDateValidator(minDate: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      return control.value < minDate ? { minDate: true } : null;
    };
  }

  private resetForm(): void {
    this.logForm.reset({
      date: '',
      time: '',
      comment: '',
      difficulty: 1,
      totalDistance: 0,
      totalTime: 0,
      rating: 0,
      tourID: '',
    });
  }
}
