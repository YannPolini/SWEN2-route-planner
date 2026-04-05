import { Component, signal, computed, inject } from '@angular/core';
import { TourlogsList } from '../tourlogs-list/tourlogs-list';
import { FormBuilder, ReactiveFormsModule, FormControl, FormGroup, Validators, FormsModule } from '@angular/forms';
import { TourService } from '../services/tour.service';
import { AuthService } from '../auth/auth.service';

type Log = {
  date: string;
  time: string;
  comment: string;
  difficulty: number;
  totalDistance: number;
  totalTime: number;
  rating: number;
  tourID: string;
  logID: number;
  creatorName: string;
};

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ReactiveFormsModule, TourlogsList, FormsModule],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})
export class Tourlogs {

  private readonly fb = inject(FormBuilder);

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

  readonly logList = signal<Log[]>([
    {
      date: '2026-03-20',
      time: '08:45',
      comment: 'Angenehme Tour mit schönem Wetter und guter Sicht.',
      difficulty: 2,
      totalDistance: 12.4,
      totalTime: 150,
      rating: 4,
      tourID: '1',
      logID: 1,
      creatorName: 'Demo User 2'
    },
    {
      date: '2026-03-21',
      time: '14:10',
      comment: 'Teilweise anstrengender Anstieg, aber insgesamt sehr lohnend.',
      difficulty: 4,
      totalDistance: 18.7,
      totalTime: 245,
      rating: 5,
      tourID: '2',
      logID: 2,
      creatorName: 'Demo User'
    },
    {
    date: '2026-04-01',
    time: '09:15',
    comment: 'Sehr schöne Morgenrunde entlang des Flusses, kaum Verkehr.',
    difficulty: 2,
    totalDistance: 10.5,
    totalTime: 95,
    rating: 4,
    tourID: '3',
    logID: 6,
    creatorName: 'Demo User'
  },
  {
    date: '2026-04-02',
    time: '13:40',
    comment: 'Heißes Wetter, aber tolle Aussicht auf den Bergen.',
    difficulty: 4,
    totalDistance: 21.3,
    totalTime: 280,
    rating: 5,
    tourID: '4',
    logID: 7,
    creatorName: 'Demo User'
  },
  {
    date: '2026-04-03',
    time: '17:20',
    comment: 'Kurze Feierabendtour, entspannend und ruhig.',
    difficulty: 1,
    totalDistance: 5.8,
    totalTime: 60,
    rating: 3,
    tourID: '5',
    logID: 8,
    creatorName: 'Demo User'
  },
  {
    date: '2026-04-04',
    time: '08:00',
    comment: 'Sehr anspruchsvoll, viele steile Abschnitte.',
    difficulty: 5,
    totalDistance: 25.0,
    totalTime: 340,
    rating: 5,
    tourID: '1',
    logID: 9,
    creatorName: 'Demo User'
  },
  {
    date: '2026-04-05',
    time: '11:10',
    comment: 'Gemütliche Tour durch den Wald, ideal zum Abschalten.',
    difficulty: 2,
    totalDistance: 13.2,
    totalTime: 150,
    rating: 4,
    tourID: '2',
    logID: 10,
    creatorName: 'Demo User'
  }
  ]);

  private readonly tourService = inject(TourService);
  private readonly authService = inject(AuthService);

  /*  //only visual verification during initial testing
  readonly currentUsername = computed(() =>
    this.authService.currentUser()?.name ?? ''
  );
  */

  readonly filteredLogs = computed(() => {
    const selectedTourId = this.tourService.selectedTourId();
    const currentUsername = this.authService.currentUser()?.name;

    if (!selectedTourId) {
      return [];
    }

    return this.logList().filter(log => log.tourID === selectedTourId && log.creatorName === currentUsername);
  });

  readonly canAddLog = computed(() => this.tourService.selectedTourId() !== null);

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

  //readonly selectedLog = signal<Log | null>(null);
  //readonly editingLog = signal<Log | null>(null);
  readonly showFormPopup = signal(false);

  readonly selectedLogId = signal<number | null>(null);
  readonly editingLogId = signal<number | null>(null);

  readonly selectedLog = computed(() =>
    this.logList().find(log => log.logID === this.selectedLogId()) ?? null
  );

  readonly editingLog = computed(() =>
    this.logList().find(log => log.logID === this.editingLogId()) ?? null
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
      tourID: log.tourID
    });

    this.formSubmitted.set(false);
    this.showFormPopup.set(true);
  }

  //wird das noch benutzt?
  /*
  closeEditPopup(): void {
    this.showFormPopup.set(false);
    this.editingLogId.set(null);
  }
    */
  /*
  saveEdit(): void {
    const currentLog = this.editingLog();
    if (!currentLog || this.logForm.invalid) return;

    const formValue = this.logForm.getRawValue();

    this.logList.update(logs =>
      logs.map(log =>
        log.logID === currentLog.logID
          ? {
              ...log,
              date: formValue.date,
              time: formValue.time,
              comment: formValue.comment,
              difficulty: formValue.difficulty,
              totalDistance: formValue.totalDistance,
              totalTime: formValue.totalTime,
              rating: formValue.rating,
              tourID: formValue.tourID
            }
          : log
      )
    );

    this.closeEditPopup();
  }
    */

  deleteLog(): void {
    const currentLog = this.selectedLog();  //holt ausgewähltes Log damit benutzt werden kann
    
    if (!currentLog) return;
    
    this.logList.update(logs =>
      logs.filter(log => log.logID !== currentLog.logID)  //behält alle Logs außer dem ausgewählten.
    );

    this.selectedLogId.set(null);//wenn gelöscht wurde, wird das was im ausgewählten log ist zurückgesetzt
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
      this.logList.update(logs =>
        logs.map(log =>
          log.logID === currentLog.logID
            ? {
              ...log,
              date: formValue.date,
              time: formValue.time,
              comment: formValue.comment,
              difficulty: formValue.difficulty,
              totalDistance: formValue.totalDistance,
              totalTime: formValue.totalTime,
              rating: formValue.rating,
              }
            : log
        )
      );

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

      this.logList.update(logs => [newLog, ...logs]);
      this.selectedLogId.set(newLog.logID);
    }

    this.closeFormPopup();
  }
}