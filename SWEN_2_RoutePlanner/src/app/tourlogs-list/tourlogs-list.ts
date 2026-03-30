import { Component, Input } from '@angular/core';

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
  selector: 'app-tourlogs-list',
  imports: [],
  templateUrl: './tourlogs-list.html',
  styleUrl: './tourlogs-list.css',
  standalone: true,
})
export class TourlogsList {
  @Input() log!: Log;
}