import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../auth/auth.service';

export interface ImportResult {
  importedRows: number;
  failedRows: number;
  errors: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ImportExportService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient, private authService: AuthService) {}

  importTours(file: File): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ImportResult>(`${this.apiUrl}/import`, formData, {
      headers: this.authService.authHeaders()
    });
  }

  exportTours(format: 'excel' | 'csv' | 'json'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export?format=${format}`, {
      headers: this.authService.authHeaders(),
      responseType: 'blob'
    });
  }
}
