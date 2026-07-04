import { Component, Input } from '@angular/core';
import { Log } from '../tourlogs.model/tourlogs.model';

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
