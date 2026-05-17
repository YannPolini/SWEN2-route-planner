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

const ORS_KEY =
  'eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjhiNTM2ZTk5OGRhNzQ0NGNiNTUyOTBmMzE2YTEwMmM2IiwiaCI6Im11cm11cjY0In0=';

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
            @for (s of suggestions(); track s) {
              <li>
                <button
                  type="button"
                  class="dropdown-item small text-truncate"
                  (mousedown)="select(s)"
                >{{ s }}</button>
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
  value       = input<string>('');
  placeholder = input<string>('');
  /** Pass true to show the input in an error state */
  invalid     = input<boolean>(false);

  /** Fires on every keystroke and on dropdown selection */
  valueChange = output<string>();
  /** Fires when the input loses focus — use to call markAsTouched() */
  blur        = output<void>();

  protected displayValue = signal('');
  protected suggestions  = signal<string[]>([]);
  protected isOpen       = signal(false);
  protected loading      = signal(false);

  private http       = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private debounce:  ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // When the parent resets or pre-fills the form control, update the visible text.
    // The guard prevents overwriting the user's in-progress typing with echoed-back values.
    effect(() => {
      const v = this.value();
      if (v !== this.displayValue()) {
        this.displayValue.set(v);
      }
    }, { allowSignalWrites: true });
  }

  protected onInput(event: Event): void {
    const text = (event.target as HTMLInputElement).value;
    this.displayValue.set(text);
    this.valueChange.emit(text);

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

  protected select(name: string): void {
    this.displayValue.set(name);
    this.valueChange.emit(name);
    this.isOpen.set(false);
    this.suggestions.set([]);
  }

  private fetchSuggestions(text: string): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.loading.set(true);
    this.isOpen.set(true);

    const url =
      `https://api.openrouteservice.org/geocode/autocomplete` +
      `?api_key=${ORS_KEY}&text=${encodeURIComponent(text)}&size=5`;

    this.http
      .get<{ features: { properties: { label: string } }[] }>(url)
      .subscribe({
        next: res => {
          const names = (res.features ?? [])
            .map(f => f.properties?.label ?? '')
            .filter(Boolean);
          this.suggestions.set(names);
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
