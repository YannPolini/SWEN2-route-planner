import {
  Component,
  input,
  output,
  signal,
  effect,
  inject,
  PLATFORM_ID,
  OnDestroy,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';

// Backend proxy — the ORS key lives only on the server, never in the browser.
const AUTOCOMPLETE_URL = 'http://localhost:8080/api/geocode/autocomplete';

export interface Coordinates {
  lat: number;
  lng: number;
}

/** A geocoded address: the human-readable label plus exact coordinates. */
interface AddressSuggestion {
  label: string;
  lat: number;
  lng: number;
}

@Component({
  selector: 'app-address-input',
  standalone: true,
  template: `
    <div class="position-relative">
      <input
        type="text"
        class="form-control"
        [class.is-invalid]="invalid()"
        [placeholder]="placeholder()"
        [value]="displayValue()"
        (input)="onInput($event)"
        (blur)="onBlur()"
        autocomplete="off"
      />
      @if (isOpen()) {
        <ul
          class="dropdown-menu show w-100 mt-1 shadow-sm"
          style="z-index: 1055; max-height: 220px; overflow-y: auto;"
        >
          @if (loading()) {
            <li class="dropdown-item text-secondary small disabled">Searching…</li>
          } @else if (suggestions().length === 0) {
            <li class="dropdown-item text-secondary small disabled">No results found</li>
          } @else {
            @for (s of suggestions(); track $index) {
              <li>
                <button
                  type="button"
                  class="dropdown-item small text-truncate"
                  (mousedown)="select(s)"
                >
                  {{ s.label }}
                </button>
              </li>
            }
          }
        </ul>
      }
    </div>
  `,
})
export class AddressInputComponent implements OnDestroy {
  /** Current value — parent should bind this to the form control's value */
  value = input<string>('');
  placeholder = input<string>('');
  /** Pass true to show the input in an error state */
  invalid = input<boolean>(false);

  /** Fires on every keystroke and on dropdown selection */
  valueChange = output<string>();
  /**
   * Fires with the exact ORS coordinates when the user picks a suggestion,
   * and with null when they type freely (the previous coordinates no longer
   * match the text). The parent passes these to the backend so the route is
   * built from the disambiguated point instead of being geocoded again.
   */
  coordinatesChange = output<Coordinates | null>();
  /** Fires when the input loses focus — use to call markAsTouched() */
  blur = output<void>();

  protected displayValue = signal('');
  protected suggestions = signal<AddressSuggestion[]>([]);
  protected isOpen = signal(false);
  protected loading = signal(false);

  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private debounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // Sync visible text when the parent resets or pre-fills the control;
    // the guard avoids overwriting the user's in-progress typing.
    effect(() => {
      const v = this.value();
      if (v !== this.displayValue()) {
        this.displayValue.set(v);
      }
    });
  }

  protected onInput(event: Event): void {
    const text = (event.target as HTMLInputElement).value;
    this.displayValue.set(text);
    this.valueChange.emit(text);
    // Free typing invalidates any previously selected point; the backend will
    // fall back to geocoding the text unless the user picks a suggestion.
    this.coordinatesChange.emit(null);

    if (this.debounce) clearTimeout(this.debounce);

    if (text.length < 3) {
      this.isOpen.set(false);
      this.suggestions.set([]);
      return;
    }

    this.debounce = setTimeout(() => this.fetchSuggestions(text), 300);
  }

  protected onBlur(): void {
    this.blur.emit();
    // Delay so mousedown on a list item fires before the list closes
    setTimeout(() => this.isOpen.set(false), 150);
  }

  protected select(suggestion: AddressSuggestion): void {
    this.displayValue.set(suggestion.label);
    this.valueChange.emit(suggestion.label);
    this.coordinatesChange.emit({ lat: suggestion.lat, lng: suggestion.lng });
    this.isOpen.set(false);
    this.suggestions.set([]);
  }

  private fetchSuggestions(text: string): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.loading.set(true);
    this.isOpen.set(true);

    const url = `${AUTOCOMPLETE_URL}?text=${encodeURIComponent(text)}`;

    this.http.get<AddressSuggestion[]>(url).subscribe({
      next: (items) => {
        this.suggestions.set(items ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.suggestions.set([]);
        this.isOpen.set(false);
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    if (this.debounce) clearTimeout(this.debounce);
  }
}
