import { CommonModule } from '@angular/common';
import { Component, ElementRef, ViewChild } from '@angular/core';
import { TourService } from '../services/tour.service';
import { ImportExportService, ImportResult } from './importExportService';

@Component({
  selector: 'import-export',
  imports: [CommonModule],
  standalone: true,
  templateUrl: './import-export.html',
  styleUrl: './import-export.css',
})
export class ImportExport {
  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;

  isImporting = false;
  importResult: ImportResult | null = null;

  constructor(
    private service: ImportExportService,
    private tourService: TourService,
  ) {}

  openImportPicker(): void {
    if (this.isImporting) return;

    this.fileInput?.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input?.files?.[0];
    if (!file) return;

    const allowed = ['.csv'];
    const ext = '.' + file.name.split('.').pop()?.toLowerCase();

    if (!allowed.includes(ext)) {
      alert('Only .csv files are supported.');
      input.value = '';
      return;
    }

    this.importTours(file, input);
  }

  exportTours(format: 'csv'): void {
    this.service.exportTours(format).subscribe((blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');

      link.href = url;
      link.download = 'tours.csv';
      link.click();
      window.URL.revokeObjectURL(url);
    });
  }

  private importTours(file: File, input: HTMLInputElement): void {
    this.isImporting = true;
    this.importResult = null;

    this.service.importTours(file).subscribe({
      next: (result) => {
        this.importResult = result;
        this.isImporting = false;
        this.tourService.loadTours();
        input.value = '';
      },
      error: (err) => {
        console.error('Import failed', err);
        this.isImporting = false;
        input.value = '';
      },
    });
  }
}
