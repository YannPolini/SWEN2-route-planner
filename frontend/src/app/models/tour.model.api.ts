import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Tour, WeatherForecast } from './tour.model';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root',
})
export class TourApiService {
  private apiUrl = 'http://localhost:8080/api/tours';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getAll() {
    console.log('getting all tours');
    return this.http.get<Tour[]>(this.apiUrl, {
      headers: this.authService.authHeaders(),
    });
  }

  create(tour: Tour) {
    console.log('sending', tour);
    return this.http.post(this.apiUrl, tour, {
      headers: this.authService.authHeaders(),
      responseType: 'text',
    });
  }

  update(tour: Tour) {
    console.log('updating', tour);
    return this.http.put(`${this.apiUrl}/${tour.id}`, tour, {
      headers: this.authService.authHeaders(),
      responseType: 'text',
    });
  }

  delete(id: string) {
    console.log('delete:', id);
    return this.http.delete(`${this.apiUrl}/${id}`, {
      headers: this.authService.authHeaders(),
      responseType: 'text',
    });
  }

  getWeather(id: string) {
    return this.http.get<WeatherForecast>(`${this.apiUrl}/${id}/weather`, {
      headers: this.authService.authHeaders(),
    });
  }
}
