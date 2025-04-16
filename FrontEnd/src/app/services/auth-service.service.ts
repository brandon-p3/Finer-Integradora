import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<any>;
  public currentUser: Observable<any>;
  private tempUserData: any;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.currentUserSubject = new BehaviorSubject<any>(
      JSON.parse(localStorage.getItem('currentUser') || 'null')
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): any {
    return this.currentUserSubject.value;
  }

  // Método para primer paso de autenticación
  loginStep1(username: string, password: string): Observable<any> {
    return this.http.get<any>('http://localhost:8080/api/token/iniciar-sesion', {
      params: { nombreUsuario: username, contrasenia: password }
    }).pipe(
      tap((response: any) => {
        if (response.idUsuario) {
          this.tempUserData = response;
          localStorage.setItem('tempUserData', JSON.stringify(response));
        }
      })
    );
  }

  // Método para verificar token
  verifyToken(token: string): Observable<any> {
    console.log('Token a verificar:', token); // Verifica que el token no esté vacío y que sea correcto
  
    return this.http.get<any>('http://localhost:8080/api/token/comparar', {
      params: { token: token }
    }).pipe(
      tap(response => {
        console.log('Respuesta del backend:', response); // Verifica la respuesta del backend
      })
    );
  }
  
  
  // Método para enviar token de verificación
  sendVerificationToken(email: string): Observable<any> {
    return this.http.post('http://localhost:8080/api/token/enviar-token/inicioSesion', {
      correoUsuario: email
    });
  }

  // Método para completar el login
  completeLogin(): void {
    const userData = JSON.parse(localStorage.getItem('tempUserData') || '{}');
    if (userData && userData.idUsuario && userData.token) {
      localStorage.setItem('currentUser', JSON.stringify(userData));
      localStorage.removeItem('tempUserData');
      this.currentUserSubject.next(userData);
      this.tempUserData = null;
    }
  }
  

  logout(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('tempUserData');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getTempUserEmail(): string | null {
    const tempUser = JSON.parse(localStorage.getItem('tempUserData') || 'null');
    return tempUser?.correo || null;
  }
}