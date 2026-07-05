import { Injectable } from '@angular/core';
import { Log } from './tourlog.model';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root',
})
export class TourLogApiService {
  private apiUrl = 'http://localhost:8080/api/logs';

  constructor(private http: HttpClient, private authService: AuthService) {}

  getAll() {
    return this.http.get<Log[]>(this.apiUrl, {
      headers: this.authService.authHeaders(),
    });
  }

  create(log: Log) {
    return this.http.post<string>(this.apiUrl, log, {
      headers: this.authService.authHeaders(),
      responseType: 'text' as 'json',
    });
  }

  update(log: Log) {
    return this.http.put<string>(`${this.apiUrl}/${log.logID}`, log, {
      headers: this.authService.authHeaders(),
      responseType: 'text' as 'json',
    });
  }

  delete(id: string) {
    return this.http.delete<string>(`${this.apiUrl}/${id}`, {
      headers: this.authService.authHeaders(),
      responseType: 'text' as 'json',
    });
  }
}
