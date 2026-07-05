import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { TourService } from '../services/tour.service';
import { TourLogApiService } from './tourlog.model.api';

export type Log = {
  date: string;
  time: string;
  comment: string;
  difficulty: number;
  totalDistance: number;
  totalTime: number;
  rating: number;
  tourID: string;
  logID: string;
  ownerUserId?: number | null;
  creatorName: string;
};

@Injectable({ providedIn: 'root' })
export class TourlogsModel {
  readonly logList = signal<Log[]>([]);
  readonly searchTerm = signal<string>('');

  private readonly tourService = inject(TourService);
  private readonly authService = inject(AuthService);
  private readonly api = inject(TourLogApiService);

  readonly popularityByTour = computed(() => {
    const counts = new Map<string, number>();
    for (const log of this.logList()) {
      counts.set(log.tourID, (counts.get(log.tourID) ?? 0) + 1);
    }
    return counts;
  });

  readonly filteredLogs = computed(() => {
    const selectedTourId = this.tourService.selectedTourId();
    const term = this.searchTerm().toLowerCase().trim();

    if (!selectedTourId) {
      return [];
    }

    let result = this.logList().filter((log) => log.tourID === selectedTourId);

    if (term) {
      result = result.filter(
        (log) =>
          log.date.toLowerCase().includes(term) ||
          log.time.toLowerCase().includes(term) ||
          log.comment.toLowerCase().includes(term) ||
          String(log.difficulty).includes(term) ||
          String(log.totalDistance).includes(term) ||
          String(log.totalTime).includes(term) ||
          String(log.rating).includes(term),
      );
    }

    return result;
  });

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      this.searchTerm.set('');

      if (!user) {
        this.logList.set([]);
        return;
      }

      this.loadLogs(user.id);
    });
  }

  setSearchTerm(term: string): void {
    this.searchTerm.set(term);
  }

  addLog(newLog: Log): void {
    this.api.create(newLog).subscribe({
      next: () => this.loadLogs(),
      error: (err) => console.error('POST error:', err),
    });
  }

  updateLog(updatedLog: Log): void {
    this.api.update(updatedLog).subscribe({
      next: () => this.loadLogs(),
      error: (err) => console.error('PUT error:', err),
    });
  }

  deleteLog(logID: string): void {
    this.api.delete(logID).subscribe({
      next: () => this.loadLogs(),
      error: (err) => console.error('DELETE error:', err),
    });
  }

  loadLogs(expectedUserId = this.authService.currentUser()?.id): void {
    if (expectedUserId == null) {
      this.logList.set([]);
      return;
    }

    this.api.getAll().subscribe({
      next: (logs) => {
        if (this.authService.currentUser()?.id !== expectedUserId) {
          return;
        }

        this.logList.set(logs);
        this.tourService.loadTours(expectedUserId);
      },
      error: (err) => {
        console.error('API Fehler:', err);
      },
    });
  }

  getPopularity(tourID: string): number {
    return this.popularityByTour().get(tourID) ?? 0;
  }
}
