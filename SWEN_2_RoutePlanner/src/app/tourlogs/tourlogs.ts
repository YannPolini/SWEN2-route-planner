import { Component, Input, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TourlogsList } from '../tourlogs-list/tourlogs-list';

type Log = {
  date: string;
  time: string;
  comment: string;
  /*
  difficulty: number;
  totalDistance: number;
  totalTime: number;
  rating: number;
  tourID: string;
  logID: number;
  */
}
@Component({
  selector: 'app-home',
  imports: [FormsModule, TourlogsList],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})

export class Tourlogs {

  @Input() log!: {
    date: string;
    time: string;
    comment: string;
    /*
    difficulty: number;
    totalDistance: number;
    totalTime: number;
    rating: number;
    tourID: string;
    logID: number;
    */
  };

  //für die eingaben
  readonly date = signal<string>('');
  readonly time = signal<string>('');
  readonly comment = signal<string>('');
  readonly difficulty = signal<number>(-1);
  

  /*
  add(): void {
    const ok =
  }
    */

  //nicht private um mit logList() zugreifen zu können?
  readonly logList =signal<Log[]> ([
  {
    date: '2026-03-20',
    time: '08:45',
    comment: 'Angenehme Tour mit schönem Wetter und guter Sicht.'
    /*
    difficulty: 2,
    totalDistance: 12.4,
    totalTime: 150,
    rating: 4,
    tourID: 'tour-001',
    logID: 1
    */
  },
  {
    date: '2026-03-21',
    time: '14:10',
    comment: 'Teilweise anstrengender Anstieg, aber insgesamt sehr lohnend.'
    /*
    difficulty: 4,
    totalDistance: 18.7,
    totalTime: 245,
    rating: 5,
    tourID: 'tour-002',
    logID: 2
    */
  },
  {
    date: '2026-03-22',
    time: '10:30',
    comment: 'Kurze entspannte Runde, ideal für Anfänger.'
    /*
    difficulty: 1,
    totalDistance: 6.2,
    totalTime: 75,
    rating: 3,
    tourID: 'tour-003',
    logID: 3
    */
  },
  {
    date: '2026-03-24',
    time: '07:50',
    comment: 'Sehr schöne Strecke, aber ziemlich fordernd.'
    /*
    difficulty: 5,
    totalDistance: 22.1,
    totalTime: 320,
    rating: 5,
    tourID: 'tour-004',
    logID: 4
    */
  },
  {
    date: '2026-03-25',
    time: '16:20',
    comment: 'Gute Nachmittagstour mit ein paar schwierigen Passagen.'
    /*
    difficulty: 3,
    totalDistance: 14.8,
    totalTime: 180,
    rating: 4,
    tourID: 'tour-005',
    logID: 5
    */
  },
]);

  // For @switch status UI |||| not sure what this does
  readonly viewState = computed<'list' | 'empty'>;


  addLog(): void {
  const newLog: Log = {
    date: this.date(),
    time: this.time(),
    comment: this.comment()
    /*
    difficulty: this.difficulty(),
    totalDistance: 0,
    totalTime: 0,
    rating: 0,
    tourID: `tour-${this.logList().length + 1}`,
    logID: Date.now()
    */
  };

  this.logList.update(logs => [...logs, newLog]);

  this.date.set('');
  this.time.set('');
  this.comment.set('');
  this.difficulty.set(-1);
}
  /*
  addTourLog(): void {

    const newLog: Log = ({
      date: this.date().trim,

    });
  }
    */

}
