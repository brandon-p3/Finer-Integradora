import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Curso } from '../../../documentos/cursosDocumento';
import { CursosServiceService } from '../../../services/cursos-service';
import { UsuariosService } from '../../../services/usuarios-service.service';
import { AlumnoService } from '../../../services/alumno.service';

@Component({
  selector: 'app-descripcion-curso-alumno',
  templateUrl: './descripcion-curso-alumno.component.html',
  styleUrls: ['./descripcion-curso-alumno.component.css']
})
export class DescripcionCursoAlumnoComponent implements OnInit {
  cursos: Curso[] = [];
  idAlumno: number = 0;
  cursosInscritos: number[] = [];

  constructor(
    private route: ActivatedRoute,
    private cursosService: CursosServiceService,
    private alumnosService: AlumnoService,
    private usuariosService: UsuariosService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Obtener el ID del curso desde la URL
    const tituloCurso = this.route.snapshot.paramMap.get('id');
    if (tituloCurso) {
      // Obtener detalles del curso
      this.cursosService.obtenerDetalles(tituloCurso).subscribe(
        (data: Curso[]) => {
          this.cursos = data;
          console.log('Detalles del curso:', this.cursos);
        },
        (error: any) => console.error('Error al obtener detalles:', error)
      );
    }

    // Obtener el ID del alumno
    const usuario = this.usuariosService.currentUserValue;
    this.idAlumno = usuario?.idUsuario;
    console.log('ID de alumno usado:', this.idAlumno);

    // Obtener los cursos inscritos del alumno (se asegura de actualizar siempre que el componente se cargue)
    this.alumnosService.obtenerMisCursos(this.idAlumno).subscribe(
      (cursosInscritos: any[]) => {
        this.cursosInscritos = cursosInscritos.map((curso: any) => curso.idCurso);
        console.log('Cursos en los que está inscrito:', this.cursosInscritos);
      },
      (error: any) => console.error('Error al obtener cursos inscritos:', error)
    );
  }

  // Función para inscribir al alumno en un curso
  inscribirse(curso: Curso): void {
    if (!this.idAlumno || !curso?.idCurso) {
      alert('Faltan datos para inscribirse.');
      return;
    }

    this.cursosService.inscribirAlumno(this.idAlumno, curso.idCurso).subscribe(
      () => {
        alert('¡Te has inscrito exitosamente al curso!');
        this.cursosInscritos.push(curso.idCurso);
      },
      (error: any) => {
        console.error('Error al inscribirse:', error);
        const mensaje = error.error?.mensaje || 'Ocurrió un error al intentar inscribirte.';
        alert(mensaje);
      }
    );
  }

  // Función para acceder al contenido del curso
  accederCurso(curso: any): void {
    console.log('Accediendo al curso:', curso);
    if (curso.idCurso) {
      this.router.navigate([`/alumnos/contenido`, curso.idCurso]);
    } else {
      console.error("ID del curso inválido");
    }
  }

  // Función para verificar si el alumno está inscrito en el curso
  estaInscrito(curso: Curso): boolean {
    return this.cursosInscritos.includes(curso.idCurso);
  }
}
