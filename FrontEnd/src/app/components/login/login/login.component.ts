import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth-service.service';
import { UsuariosService } from '../../../services/usuarios-service.service';

interface LoginResponse {
  idUsuario: number;
  idRol: number;
  correo: string;
  [key: string]: any;
}

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  nombreUsuario: string = '';
  contrasenia: string = '';
  errorMensaje: string = '';
  mostrarContrasena: boolean = false;
  isLoading: boolean = false;
  
  verificationStep: boolean = false;
  verificationCode: string = '';
  maskedEmail: string = '';

  constructor(
    private router: Router,
    private authService: AuthService,
    private usuariosService: UsuariosService
  ) {}

  toggleMostrarContrasena(): void {
    this.mostrarContrasena = !this.mostrarContrasena;
  }

  iniciarSesion(): void {
    this.isLoading = true;
    this.errorMensaje = '';
    
    this.authService.loginStep1(this.nombreUsuario, this.contrasenia).subscribe({
      next: (response: LoginResponse) => {
        if (response.idUsuario) {
          this.verificationStep = true;
          this.maskedEmail = this.maskEmail(response.correo);
          
          this.authService.sendVerificationToken(response.correo).subscribe({
            error: (err: any) => {
              //this.errorMensaje = 'Error al enviar el código de verificación. Intente nuevamente.';
            }
          });
        } else {
          this.errorMensaje = 'Usuario o contraseña incorrectos';
        }
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Error al iniciar sesión:', error);
        this.errorMensaje = error.error?.error || 'Error desconocido al iniciar sesión';
        this.isLoading = false;
      }
    });
  }

  verifyCode() {
    if (!this.verificationCode || this.verificationCode.length !== 6) {
      this.errorMensaje = 'Por favor ingresa un código válido de 6 dígitos';
      return;
    }
  
    this.isLoading = true;
    this.errorMensaje = '';
    const tempUser = JSON.parse(localStorage.getItem('tempUserData') || '{}');
          console.log('Datos del usuario temporal:', tempUser);
  
    console.log('Verificando token:', this.verificationCode); // Verifica que el código se envíe correctamente
  
    this.authService.verifyToken(this.verificationCode).subscribe({
      next: (response: any) => {
        console.log('Respuesta al verificar el token:', response); // Muestra la respuesta del backend
        if (response.resultado) {
          this.authService.completeLogin();
          
           // Verifica los datos del usuario temporal almacenados
          
          this.usuariosService.obtenerUsuarioPorId(tempUser.idUsuario).subscribe({
            next: (usuarioCompleto: any) => {
              const fullUserData = {
                ...tempUser,
                ...usuarioCompleto
              };
              this.usuariosService.setCurrentUser(fullUserData);
              this.redirigirSegunRol(tempUser.idRol);
            },
            error: (error: any) => {
              console.error('Error al obtener datos del usuario:', error);
              this.redirigirSegunRol(tempUser.idRol);
            }
          });
        } else {
          this.errorMensaje = 'Código de verificación incorrecto';
          this.isLoading = false;
        }
      },
      error: (error: any) => {
        console.error('Error al verificar el token:', error); // Verifica el error recibido del servidor
        this.errorMensaje = 'Error al verificar el código. Intente nuevamente.';
        this.isLoading = false;
      }
    });
  }
  

  resendCode(): void {
    const tempUser = JSON.parse(localStorage.getItem('tempUserData') || '{}');
    if (tempUser.correo) {
      this.isLoading = true;
      this.errorMensaje = '';
      
      this.authService.sendVerificationToken(tempUser.correo).subscribe({
        next: () => {
          this.isLoading = false;
        },
        error: (err: any) => {
          this.errorMensaje = 'Error al reenviar el código. Intente nuevamente.';
          this.isLoading = false;
        }
      });
    }
  }

  private redirigirSegunRol(idRol: number): void {
    this.isLoading = false;
    
    switch(idRol) {
      case 1: // Administrador
        this.router.navigate(['/administrador/cursos/ver']);
        break;
      case 2: // Instructor
        this.router.navigate(['/instructor/cursos']);
        break;
      case 3: // Alumno
        this.router.navigate(['/alumnos/cursos']);
        break;
      default:
        this.errorMensaje = 'Rol no reconocido. Comunícate con soporte.';
    }
  }

  private maskEmail(email: string): string {
    if (!email) return '';
    const [name, domain] = email.split('@');
    const maskedName = name.length > 2 
      ? name.substring(0, 2) + '*'.repeat(name.length - 2)
      : '*'.repeat(name.length);
    return maskedName + '@' + domain;
  }

  navigateToRegister() {
    this.router.navigate(['/home/registro']); 
  }
}