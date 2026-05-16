import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, tap } from 'rxjs';

type RegisterUser = {
  name: string;
  email: string;
  password: string;
};

type SessionUser = {
  id: number;
  name: string;
  email: string;
};

type AuthResponse = {
  token: string;
  user: SessionUser;
};

export type AuthResult = {
  success: boolean;
  message?: string;
};

type ProfileUpdate = {
  name: string;
  email: string;
  currentPassword: string;
  newPassword?: string;
};

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly tokenKey = 'tour-planner-token';
  private readonly sessionKey = 'tour-planner-session-user';

  private readonly tokenSignal = signal<string | null>(null);
  private readonly currentUserSignal = signal<SessionUser | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor() {
    this.loadState();
  }

  register(user: RegisterUser): Observable<AuthResult> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, user).pipe(
      tap((response) => this.setSession(response)),
      map(() => ({ success: true })),
      catchError((error) => of(this.toAuthResult(error, 'Registration failed.'))),
    );
  }

  login(email: string, password: string): Observable<AuthResult> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap((response) => this.setSession(response)),
      map(() => ({ success: true })),
      catchError((error) => of(this.toAuthResult(error, 'Login failed.'))),
    );
  }

  updateProfile(update: ProfileUpdate): Observable<AuthResult> {
    return this.http.put<SessionUser>(`${this.apiUrl}/editUser`, update, {
      headers: this.authHeaders(),
    }).pipe(
      tap((user) => this.setCurrentUser(user)),
      map(() => ({ success: true })),
      catchError((error) => of(this.toAuthResult(error, 'Profile update failed.'))),
    );
  }

  logout(): void {
    const headers = this.authHeaders();
    this.clearSession();

    if (headers.has('Authorization')) {
      this.http.post<void>(`${this.apiUrl}/logout`, {}, { headers }).subscribe({
        error: () => undefined,
      });
    }

    void this.router.navigate(['/login']);
  }

  private loadState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const token = localStorage.getItem(this.tokenKey);
    const storedSession = localStorage.getItem(this.sessionKey);
    const session = storedSession ? (JSON.parse(storedSession) as SessionUser | null) : null;

    this.tokenSignal.set(token);
    this.currentUserSignal.set(session);

    if (token) {
      this.http.get<SessionUser>(`${this.apiUrl}/me`, { headers: this.authHeaders(token) }).subscribe({
        next: (user) => this.setCurrentUser(user),
        error: () => this.clearSession(),
      });
    }
  }

  private setSession(response: AuthResponse): void {
    this.tokenSignal.set(response.token);
    this.setCurrentUser(response.user);

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    localStorage.setItem(this.tokenKey, response.token);
  }

  private setCurrentUser(user: SessionUser): void {
    this.currentUserSignal.set(user);

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    localStorage.setItem(this.sessionKey, JSON.stringify(user));
  }

  private clearSession(): void {
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.sessionKey);
  }

  private authHeaders(token = this.tokenSignal()): HttpHeaders {
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }

  private toAuthResult(error: unknown, fallback: string): AuthResult {
    if (error instanceof HttpErrorResponse) {
      if (typeof error.error === 'string') {
        return { success: false, message: error.error || fallback };
      }

      return {
        success: false,
        message: error.error?.detail ?? error.error?.message ?? error.error?.error ?? fallback,
      };
    }

    return { success: false, message: fallback };
  }
}
