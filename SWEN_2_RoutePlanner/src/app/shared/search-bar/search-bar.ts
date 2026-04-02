import { Component, input, output, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="input-group">
      <span class="input-group-text bg-white">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="text-secondary" viewBox="0 0 16 16">
          <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85zm-5.242.656a5 5 0 1 1 0-10 5 5 0 0 1 0 10z"/>
        </svg>
      </span>
      <input
        type="text"
        class="form-control"
        [placeholder]="placeholder()"
        [ngModel]="value()"
        (ngModelChange)="onInput($event)"
      />
      @if (value()) {
        <button class="btn btn-outline-secondary" type="button" (click)="onClear()">✕</button>
      }
    </div>
  `,
})
export class SearchBarComponent implements OnDestroy {
  placeholder = input<string>('Search...');
  value = input<string>('');
  debounceMs = input<number>(300);
  valueChange = output<string>();

  private timer: ReturnType<typeof setTimeout> | null = null;

  onInput(val: string): void {
    if (this.timer) clearTimeout(this.timer);
    this.timer = setTimeout(() => this.valueChange.emit(val), this.debounceMs());
  }

  onClear(): void {
    if (this.timer) clearTimeout(this.timer);
    this.valueChange.emit('');
  }

  ngOnDestroy(): void {
    if (this.timer) clearTimeout(this.timer);
  }
}
