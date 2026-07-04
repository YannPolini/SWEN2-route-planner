import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Tour, WeatherForecast } from './tour.model';
import { AuthService } from '../auth/auth.service';

export type DemoSeedResponse = {
  tourCount: number;
  logCount: number;
};

@Injectable({
  providedIn: 'root',
})
export class TourApiService {
  private apiUrl = 'http://localhost:8080/api/tours';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getAll() {
    return this.http.get<Tour[]>(this.apiUrl, {
      headers: this.authService.authHeaders(),
    });
  }

  create(tour: Tour) {
    return this.http.post(this.apiUrl, tour, {
      headers: this.authService.authHeaders(),
      responseType: 'text',
    });
  }

  update(tour: Tour) {
    return this.http.put(`${this.apiUrl}/${tour.id}`, tour, {
      headers: this.authService.authHeaders(),
      responseType: 'text',
    });
  }

  delete(id: string) {
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

  seedDemoData() {
    return this.http.post<DemoSeedResponse>('http://localhost:8080/api/demo-data/seed', {}, {
      headers: this.authService.authHeaders(),
    });
  }
}
