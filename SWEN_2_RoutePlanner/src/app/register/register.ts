import {Component, computed, inject, signal} from '@angular/core';
import {AbstractControl, FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {AuthService} from '../auth/auth.service';

function passwordsMatch(control: AbstractControl) {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  return password === confirmPassword ? null : {passwordMismatch: true};
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly errorMessage = signal('');
  readonly submitted = signal(false);
  readonly registerForm = this.fb.nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    {validators: passwordsMatch},
  );

  readonly passwordMismatch = computed(
    () =>
      (this.submitted() || this.registerForm.controls.confirmPassword.touched) &&
      this.registerForm.hasError('passwordMismatch'),
  );

  protected submit(): void {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const {name, email, password} = this.registerForm.getRawValue();
    const result = this.authService.register({name, email, password});

    if (!result.success) {
      this.errorMessage.set(result.message ?? 'Registration failed.');
      return;
    }

    void this.router.navigate(['/tourlogs']);
  }
}
