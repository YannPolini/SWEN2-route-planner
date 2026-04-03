import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../auth/auth.service';

function passwordChangeValidator(control: AbstractControl) {
  const newPassword = control.get('newPassword')?.value?.trim();
  const confirmNewPassword = control.get('confirmNewPassword')?.value?.trim();

  if (!newPassword && !confirmNewPassword) {
    return null;
  }

  if (!newPassword || !confirmNewPassword) {
    return { passwordChangeIncomplete: true };
  }

  return newPassword === confirmNewPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class ProfileComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly authService = inject(AuthService);

  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly submitted = signal(false);
  readonly profileForm = this.fb.nonNullable.group(
    {
      name: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      currentPassword: ['', [Validators.required, Validators.minLength(6)]],
      newPassword: ['', [Validators.minLength(6)]],
      confirmNewPassword: [''],
    },
    { validators: passwordChangeValidator },
  );

  readonly passwordMismatch = computed(
    () =>
      (this.submitted() || this.profileForm.controls.confirmNewPassword.touched) &&
      this.profileForm.hasError('passwordMismatch'),
  );

  readonly passwordChangeIncomplete = computed(
    () =>
      (this.submitted() ||
        this.profileForm.controls.newPassword.touched ||
        this.profileForm.controls.confirmNewPassword.touched) &&
      this.profileForm.hasError('passwordChangeIncomplete'),
  );

  readonly userInitials = computed(() => {
    const name = this.authService.currentUser()?.name ?? '';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  });

  constructor() {
    this.resetForm();
  }

  protected submit(): void {
    this.submitted.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const { name, email, currentPassword, newPassword } = this.profileForm.getRawValue();
    const result = this.authService.updateProfile({
      name,
      email,
      currentPassword,
      newPassword,
    });

    if (!result.success) {
      this.errorMessage.set(result.message ?? 'Profile update failed.');
      return;
    }

    this.successMessage.set('Profile updated successfully.');
    this.submitted.set(false);
    this.resetForm();
  }

  protected resetForm(): void {
    const currentUser = this.authService.currentUser();

    this.profileForm.reset({
      name: currentUser?.name ?? '',
      email: currentUser?.email ?? '',
      currentPassword: '',
      newPassword: '',
      confirmNewPassword: '',
    });
    this.errorMessage.set('');
  }
}
