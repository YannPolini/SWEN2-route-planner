import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

type StoredUser = {
  name: string;
  email: string;
  password: string;
};

type SessionUser = {
  name: string;
  email: string;
};

type AuthResult = {
  success: boolean;
  message?: string;
};

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly usersKey = 'tour-planner-users';
  private readonly sessionKey = 'tour-planner-session';

  private readonly usersSignal = signal<StoredUser[]>([]);
  private readonly currentUserSignal = signal<SessionUser | null>(null);

  readonly currentUser = computed(() => this.currentUserSignal());
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    this.loadState();
  }

  register(user: StoredUser): AuthResult {
    const email = user.email.trim().toLowerCase();
    const name = user.name.trim();
    const password = user.password.trim();

    if (!name || !email || !password) {
      return { success: false, message: 'Please fill in all fields.' };
    }

    if (this.usersSignal().some((existingUser) => existingUser.email === email)) {
      return { success: false, message: 'An account with this email already exists.' };
    }

    this.usersSignal.set([...this.usersSignal(), { name, email, password }]);
    this.currentUserSignal.set({ name, email });
    this.persistState();

    return { success: true };
  }

  login(email: string, password: string): AuthResult {
    const normalizedEmail = email.trim().toLowerCase();
    const normalizedPassword = password.trim();
    const user = this.usersSignal().find(
      (existingUser) =>
        existingUser.email === normalizedEmail && existingUser.password === normalizedPassword,
    );

    if (!user) {
      return { success: false, message: 'Invalid email or password.' };
    }

    this.currentUserSignal.set({ name: user.name, email: user.email });
    this.persistState();

    return { success: true };
  }

  logout(): void {
    this.currentUserSignal.set(null);
    this.persistState();
    void this.router.navigate(['/login']);
  }

  private loadState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const storedUsers = localStorage.getItem(this.usersKey);
    const storedSession = localStorage.getItem(this.sessionKey);

    const users = storedUsers ? (JSON.parse(storedUsers) as StoredUser[]) : this.getSeedUsers();
    const session = storedSession ? (JSON.parse(storedSession) as SessionUser | null) : null;

    this.usersSignal.set(users);
    this.currentUserSignal.set(session);
    this.persistState();
  }

  private persistState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    localStorage.setItem(this.usersKey, JSON.stringify(this.usersSignal()));
    localStorage.setItem(this.sessionKey, JSON.stringify(this.currentUserSignal()));
  }

  private getSeedUsers(): StoredUser[] {
    return [
      {
        name: 'Demo User',
        email: 'demo@tourplanner.local',
        password: 'demo1234',
      },
    ];
  }
}
