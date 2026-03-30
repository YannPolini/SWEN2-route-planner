import {Component, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TourlogsList} from '../tourlogs-list/tourlogs-list';

type Log = {
  date: string;
  time: string;
  comment: string;
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
  readonly formSubmitted = signal(false);
  readonly logList = signal<Log[]>([
    {
      date: '2026-03-20',
      time: '08:45',
      comment: 'Angenehme Tour mit schoenem Wetter und guter Sicht.',
    },
    {
      date: '2026-03-21',
      time: '14:10',
      comment: 'Teilweise anstrengender Anstieg, aber insgesamt sehr lohnend.',
    },
    {
      date: '2026-03-22',
      time: '10:30',
      comment: 'Kurze entspannte Runde, ideal fuer Anfaenger.',
    },
  ]);

  addLog(): void {
    this.formSubmitted.set(true);

    const newLog: Log = {
      date: this.date().trim(),
      time: this.time().trim(),
      comment: this.comment().trim(),
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
}
