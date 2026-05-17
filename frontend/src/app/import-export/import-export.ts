import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImportResult, ImportExportService } from './importExportService';


@Component({
  selector: 'import-export',
  imports: [CommonModule],
  standalone: true,
  templateUrl: './import-export.html'
})
export class ImportExport {

  // Hier speichern wir die Datei, die der User auswählt
  selectedFile: File | null = null;

  // Tracks whether an import is in progress (used to show a loading indicator)
  isImporting = false;

  // Hier speichern wir das Ergebnis vom Backend nach dem Import
  importResult: ImportResult | null = null;

  constructor(private service: ImportExportService) {}

  // Wird aufgerufen, wenn der User im <input type="file"> eine Datei auswählt,  Validiert die Dateiendung und speichert die Datei für den Import.
  onFileSelected(event: Event): void {
    // Das Event kommt von einem HTMLInputElement
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0]; // Erste ausgewählte Datei holen

    // Exit early wenn keine Datei ausgewählt wurde
    if (!file) return;

    // Nur diese drei Dateitypen erlauben
    const allowed = ['.xlsx', '.csv', '.json'];
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();

    //ist zwar jetzt schon so, aber aktuell werden nur .csv dateien verarbeitet
    if (!allowed.includes(ext)) {
      alert('Nur .xlsx, .csv und .json Dateien werden unterstützt.');
      input.value = '';
      this.selectedFile = null;
      return;
    }

    // Datei speichern (z.B. für einen separaten "Import"-Button)
    this.selectedFile = file;

    /** sending twice
    // Ladeindikator anzeigen
    this.isImporting = true;

    // Datei ans Backend schicken
    this.service.importTours(file).subscribe({
      next: (result) => {
        // Ergebnis speichern für Anzeige im Template
        this.importResult = result;
        this.isImporting = false;
      },
      error: (err) => {
        console.error('Import fehlgeschlagen', err);
        this.isImporting = false;
      }
    });
     */
  }

  // Wird aufgerufen, wenn der User auf "Import Tours" klickt
  importTours(): void {
    // Falls keine Datei ausgewählt wurde, abbrechen
    if (!this.selectedFile) {
      return;
    }

    this.isImporting = true;

    // Datei an das Backend senden
    this.service.importTours(this.selectedFile).subscribe({
      next: result => {
        // Erfolgreiche Antwort vom Backend speichern, Dadurch kann sie im HTML angezeigt werden
        this.importResult = result;
        this.isImporting = false;
      },
      error: err => {
        // Fehler beim Upload oder Import
        console.error('Import failed', err);
        this.isImporting = false;
      }
    });
  }

  //Downloads all tours as a (CSV) file. The backend returns the CSV as a Blob. Angular creates a temporary object URL for that Blob so the browser can download it as a file.
  exportTours(format: 'excel' | 'csv' | 'json') {
    // CSV-Datei vom Backend holen
    this.service.exportTours(format).subscribe(blob => {
      // Aus der Datei-Antwort wird eine temporäre Browser-URL erzeugt
      const url = window.URL.createObjectURL(blob);

      // Ein unsichtbarer Download-Link wird erzeugt
      const link = document.createElement('a');

      // Die temporäre URL wird als Ziel des Links gesetzt
      link.href = url;

      // Map each format to its corresponding download filename
      const extensions: Record<string, string> = {
        csv: 'tours.csv',
        json: 'tours.json',
        excel: 'tours.xlsx'
      };

      // Dateiname, den der User beim Download sieht
      link.download = extensions[format];

      // Download starten
      link.click();

      // Temporäre URL wieder freigeben, damit kein Speicher verschwendet wird
      window.URL.revokeObjectURL(url);
    });
  }

}

