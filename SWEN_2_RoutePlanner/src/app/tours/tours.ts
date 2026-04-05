import { Component, inject } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TourService } from '../services/tour.service';
import { SearchBarComponent } from '../shared/search-bar/search-bar';
import { TourMapComponent } from '../shared/tour-map/tour-map';
import { App } from "../app";
import { Tourlogs } from '../tourlogs/tourlogs';

@Component({
  selector: 'app-tours',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, SearchBarComponent, TourMapComponent, App, Tourlogs],
  templateUrl: './tours.html',
  styleUrl: './tours.css',
})
export class ToursComponent {
  // The ViewModel — single point of contact for the template.
  // The View (this component) owns ZERO state and ZERO logic.
  protected readonly vm = inject(TourService);
}
