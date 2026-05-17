import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Struktur der Antwort vom Backend nach dem Import
export interface ImportResult {
  importedRows: number;
  failedRows: number;
  errors: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ImportExportService {

  // Basis-URL deines Spring-Boot-Backends
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Sendet eine CSV-Datei an das Backend
  importTours(file: File): Observable<ImportResult> {
    // FormData wird verwendet, wenn man Dateien per HTTP hochlädt
    const formData = new FormData();

    // "file" muss gleich heißen wie im Spring Boot Controller:(@RequestParam)
    formData.append('file', file);

    //Das Backend verarbeitet die Datei und gibt ein ImportResult zurück (Verarbeite/Error zeilen)
    return this.http.post<ImportResult>(`${this.apiUrl}/import`, formData);
  }

   //responseType: 'blob' tells Angular to treat the response as a binary file, which is required for triggering a file download in the browser.
   //@returns Observable<Blob> — the raw file content, also ein Objekt für rohe Dateidaten.
  exportTours(format: 'excel' | 'csv' | 'json'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export?format=${format}`, {
      responseType: 'blob' // must be 'blob' to handle binary/file responses, gibt “wartenden HTTP-Request”, der später eine Datei liefert
    });
  }
}