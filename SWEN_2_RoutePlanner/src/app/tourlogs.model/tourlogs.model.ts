import { signal, computed, inject, Injectable } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { TourService } from '../services/tour.service';

export type Log = {
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

@Injectable({ providedIn: 'root' })
export class TourlogsModel {

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

  private readonly authService = inject(AuthService);
  private readonly tourService = inject(TourService);

  readonly filteredLogs = computed(() => {
    const selectedTourId = this.tourService.selectedTourId();
    const currentUsername = this.authService.currentUser()?.name; 

    if (!selectedTourId) {
      return [];
    }

    return this.logList().filter(log => log.tourID === selectedTourId && log.creatorName === currentUsername);
  });

  addLog(newLog: Log): void {
    this.logList.update(logs => [newLog, ...logs]);     //newLog ist erstes im Array
  }

  updateLog(updatedLog: Log): void {
    this.logList.update(logs =>
      logs.map(log => log.logID === updatedLog.logID ? updatedLog : log)  //wenn logID gleich updaeteLog hier speichern, sonst das alte log
    );
  }

  deleteLog(logID: number): void {
    this.logList.update(logs => logs.filter(log => log.logID !== logID));
  }

}
