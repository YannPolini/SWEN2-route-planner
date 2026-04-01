import {Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TourlogsList} from '../tourlogs-list/tourlogs-list';

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
};

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule, TourlogsList],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})
export class Tourlogs {
  readonly date = signal('');
  readonly time = signal('');
  readonly comment = signal('');
  readonly difficulty = signal('');
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
      tourID: 'tour-001',
      logID: 1
    },
    {
      date: '2026-03-21',
      time: '14:10',
      comment: 'Teilweise anstrengender Anstieg, aber insgesamt sehr lohnend.',
      difficulty: 4,
      totalDistance: 18.7,
      totalTime: 245,
      rating: 5,
      tourID: 'tour-002',
      logID: 2
    },
    {
    date: '2026-04-01',
    time: '09:15',
    comment: 'Sehr schöne Morgenrunde entlang des Flusses, kaum Verkehr.',
    difficulty: 2,
    totalDistance: 10.5,
    totalTime: 95,
    rating: 4,
    tourID: 'tour-006',
    logID: 6
  },
  {
    date: '2026-04-02',
    time: '13:40',
    comment: 'Heißes Wetter, aber tolle Aussicht auf den Bergen.',
    difficulty: 4,
    totalDistance: 21.3,
    totalTime: 280,
    rating: 5,
    tourID: 'tour-007',
    logID: 7
  },
  {
    date: '2026-04-03',
    time: '17:20',
    comment: 'Kurze Feierabendtour, entspannend und ruhig.',
    difficulty: 1,
    totalDistance: 5.8,
    totalTime: 60,
    rating: 3,
    tourID: 'tour-008',
    logID: 8
  },
  {
    date: '2026-04-04',
    time: '08:00',
    comment: 'Sehr anspruchsvoll, viele steile Abschnitte.',
    difficulty: 5,
    totalDistance: 25.0,
    totalTime: 340,
    rating: 5,
    tourID: 'tour-009',
    logID: 9
  },
  {
    date: '2026-04-05',
    time: '11:10',
    comment: 'Gemütliche Tour durch den Wald, ideal zum Abschalten.',
    difficulty: 2,
    totalDistance: 13.2,
    totalTime: 150,
    rating: 4,
    tourID: 'tour-010',
    logID: 10
  }
  ]);

  addLog(): void {
    this.formSubmitted.set(true);

    const newLog: Log = {
      date: this.date(),
      time: this.time(),
      comment: this.comment(),
      difficulty: 0,
      totalDistance: 0,
      totalTime: 0,
      rating: 0,
      tourID: `tour-${this.logList().length + 1}`,
      logID: Date.now()
    };

    if (!newLog.date || !newLog.time || !newLog.comment) {
      return;
    }

    this.logList.update((logs) => [newLog, ...logs]);
    this.date.set('');
    this.time.set('');
    this.comment.set('');
    this.formSubmitted.set(false);
  }

  readonly selectedLog = signal<Log | null>(null);

  selectLog(log: Log): void {
    this.selectedLog.set(log);
  }

  readonly editingLog = signal<Log | null>(null);
  //readonly showForm = signal<boolean>(false);
  readonly showEditPopup = signal(false);

  openEdit(log: Log): void {
    this.editingLog.set(log);

    this.date.set(log.date);
    this.time.set(log.time);
    this.comment.set(log.comment);
    this.difficulty.set('0');

    //this.showForm.set(true);
    this.showEditPopup.set(true);
  }

  closeEditPopup(): void {
    this.showEditPopup.set(false);
    this.editingLog.set(null);
  }

  saveEdit(): void {
  const currentLog = this.editingLog();

  if (!currentLog) return;
  this.logList.update(logs =>
    logs.map(log =>
      log.logID === currentLog.logID
        ? { //Das ist ein ternärer Operator. Das ist einfach eine kurze Schreibweise für if/else.
            ...log, //kopiert alle alten eigenschaften von log, --> danach kann ich die überschreiben mit dem was ich ändern will
            date: this.date(),
            time: this.time(),
            comment: this.comment(),
            difficulty: 0,
          }
        : log // Alle anderen Logs bleiben unverändert
    )
  );

  this.closeEditPopup();
  }

  deleteLog(): void {
    const currentLog = this.selectedLog();  //holt ausgewähltes Log damit benutzt werden kann
    if (!currentLog) return;
    this.logList.update(logs =>
    logs.filter(log => log.logID !== currentLog.logID)  //behält alle Logs außer dem ausgewählten.
  );

  this.selectedLog.set(null); //wenn gelöscht wurde, wird das was im ausgewählten log ist zurückgesetzt
  }
}
