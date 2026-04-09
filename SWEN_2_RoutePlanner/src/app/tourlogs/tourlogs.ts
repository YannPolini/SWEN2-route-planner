import { Component, signal, computed, inject, effect } from '@angular/core';
import { TourlogsList } from '../tourlogs-list/tourlogs-list';
import { FormBuilder, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { TourService } from '../services/tour.service';
import { AuthService } from '../auth/auth.service';
import { TourlogsModel, Log } from '../tourlogs.model/tourlogs.model';

@Component({
  selector: 'app-tourlogs',
  standalone: true,
  imports: [ReactiveFormsModule, TourlogsList, FormsModule],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})
export class TourlogsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly tourlogsModel = inject(TourlogsModel);

  readonly logList = this.tourlogsModel.logList;    //Ganze liste der TourLogs

  protected readonly logForm = this.fb.nonNullable.group({
    date: ['', [Validators.required]],
    time: ['', [Validators.required]],
    comment: ['', [Validators.required]],
    difficulty: [0, [Validators.required, Validators.min(0), Validators.max(5)]],
    totalDistance: [0, [Validators.required, Validators.min(0)]],
    totalTime: [0, [Validators.required, Validators.min(0)]],
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    tourID: [''],
  });

  readonly formSubmitted = signal(false);

  private readonly tourService = inject(TourService);
  private readonly authService = inject(AuthService);

  //ausgelagert zu tourlogs.model.ts
  readonly filteredLogs = this.tourlogsModel.filteredLogs;    //Filtered Liste der Tourlogs, mit UserName und 

  readonly canAddLog = computed(() => this.tourService.selectedTourId() !== null);  //Wenn eine Tour ausgewählt ist, dann hat es eine ID

  //with addPopup
  openAdd(): void {
    //damit nicht wenn kein tour ausgewählt trotzdem tourlog adden kann
    if (!this.tourService.selectedTour()) {
      return;
    }

    this.editingLogId.set(null);

    this.logForm.reset({
      date: '',
      time: '',
      comment: '',
      difficulty: 1,
      totalDistance: 0,
      totalTime: 0,
      rating: 0,
    });

    this.formSubmitted.set(false);
    this.showFormPopup.set(true);
  }

  readonly showFormPopup = signal(false);

  readonly selectedLogId = signal<number | null>(null);   //welches Log gerade selected ist
  readonly editingLogId = signal<number | null>(null);    //welches Log gerade bearbeitet wird, wenn edit will dann bekommt nummer von dem was edited wird

  readonly selectedLog = computed(
    () => this.tourlogsModel.logList().find((log) => log.logID === this.selectedLogId()) ?? null,
  );

  constructor() {
    effect(() => {
      //damit wenn sich die tour wechselt das selectedLog auf null gesetzt wird
      this.tourService.selectedTourId();
      this.selectedLogId.set(null);
      this.editingLogId.set(null);
      this.showFormPopup.set(false);
    });
  }

  readonly editingLog = computed(
    () => this.tourlogsModel.logList().find((log) => log.logID === this.editingLogId()) ?? null,
  );

  selectLog(log: Log): void {
    this.selectedLogId.set(log.logID);
  }

  openEdit(log: Log): void {
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

  deleteLog(): void {
    const currentLog = this.selectedLog(); //holt ausgewähltes Log damit benutzt werden kann

    if (!currentLog) return;

    this.tourlogsModel.deleteLog(currentLog.logID);
    this.selectedLogId.set(null); //wenn gelöscht wurde, wird das was im ausgewählten log ist zurückgesetzt
  }

  //close the addPopup
  closeFormPopup(): void {
    this.showFormPopup.set(false);
    this.editingLogId.set(null);
    this.formSubmitted.set(false);

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

  //Damit beim popup gleichzeitig adden und editen kann
  saveLog(): void {
    this.formSubmitted.set(true);

    if (this.logForm.invalid) {
      return;
    }

    const formValue = this.logForm.getRawValue();
    const currentLog = this.editingLog();
    const currentUsername = this.authService.currentUser()?.name;
    const selectedTourId = this.tourService.selectedTourId();

    if (!currentUsername || !selectedTourId) {
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
        logID: Date.now(),
        creatorName: currentUsername,
      };

      this.tourlogsModel.addLog(newLog);
      this.selectedLogId.set(newLog.logID);
    }

    this.closeFormPopup();
  }
}
