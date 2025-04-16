package mx.utng.finer_back_end.Alumnos.Implement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import mx.utng.finer_back_end.Alumnos.Dao.AlumnoContinuarCursoDao;
import mx.utng.finer_back_end.Alumnos.Dao.CursoAlumnoDao;
import mx.utng.finer_back_end.Alumnos.Documentos.CertificadoDetalleDTO;
import mx.utng.finer_back_end.Alumnos.Documentos.ContinuarCursoDTO;
import mx.utng.finer_back_end.Alumnos.Documentos.CursoDetalleAlumnoDTO;
import mx.utng.finer_back_end.Alumnos.Documentos.CursoFinalizadoDTO;
import mx.utng.finer_back_end.Alumnos.Documentos.CursoInscritoDTO;
import mx.utng.finer_back_end.Alumnos.Documentos.CursoNombreAlumnoDTO;
import mx.utng.finer_back_end.Alumnos.Documentos.PuntuacionAlumnoDTO;
import mx.utng.finer_back_end.Alumnos.Services.CursoAlumnoService;
import mx.utng.finer_back_end.Documentos.TemaDocumento;

@Service
public class CursoAlumnoImplement implements CursoAlumnoService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CursoAlumnoDao cursoDao;

    @Autowired
    AlumnoContinuarCursoDao continuarCursoDao;

    @Override
    @Transactional
    public List<CursoDetalleAlumnoDTO> getCurso(String tituloCurso) {
        List<Object[]> resultados = cursoDao.verCursoDetalles(tituloCurso); // Pasar un String en lugar de Integer
        List<CursoDetalleAlumnoDTO> detalles = new ArrayList<>();

        for (Object[] row : resultados) {
            CursoDetalleAlumnoDTO cursoDetalle = new CursoDetalleAlumnoDTO(
                    (Integer) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (Integer) row[5],
                    (Integer) row[6],
                    (String) row[7]);
            detalles.add(cursoDetalle);
        }
        return detalles;
    }

    @Override
    @Transactional
    public Boolean inscribirseCurso(Integer idUsuario, Integer idCurso) {
        Boolean reinscripcionCurso = cursoDao.validarReinscripcionAlumno(idCurso, idUsuario);
        if (reinscripcionCurso == true) {
            return cursoDao.inscribirseCursoAlumno(idCurso, idUsuario);
        } else {
            return reinscripcionCurso;
        }
    }

    @Override
    @Transactional
    public List<PuntuacionAlumnoDTO> verPuntuacion(Integer idInscripcion) {
        List<Object[]> puntuacionAlumno = cursoDao.verPuntuacion(idInscripcion);
        List<PuntuacionAlumnoDTO> puntuacionAlumnoDTO = new ArrayList<>();

        for (Object[] row : puntuacionAlumno) {
            PuntuacionAlumnoDTO puntacionA = new PuntuacionAlumnoDTO(
                    (Integer) row[0],
                    (Integer) row[1],
                    (BigDecimal) row[2]);
            puntuacionAlumnoDTO.add(puntacionA);
        }
        return puntuacionAlumnoDTO;
    }

    public String bajaCursoAlumno(Integer idInscricpion) {
        return cursoDao.bajaCursoAlumno(idInscricpion);
    }

    @Override
    public CertificadoDetalleDTO obtenerDetallesCertificado(Integer idInscripcion) {
        List<Object[]> resultados = cursoDao.obtenerDetallesCertificado(idInscripcion);

        if (resultados != null && !resultados.isEmpty()) {
            Object[] fila = resultados.get(0);

            if (fila != null && fila.length >= 7){
                try {
                    LocalDate fechaInscripcion;
                    if (fila[6] instanceof java.sql.Timestamp) {
                        // Convertir Timestamp a LocalDate
                        fechaInscripcion = ((java.sql.Timestamp) fila[6]).toLocalDateTime().toLocalDate();
                    } else if (fila[6] instanceof java.sql.Date) {
                        // Por si acaso el tipo cambia en el futuro
                        fechaInscripcion = ((java.sql.Date) fila[6]).toLocalDate();
                    } else {
                        // Si es otro tipo, usar la fecha actual como fallback
                        fechaInscripcion = LocalDate.now();
                        System.out.println("Tipo de fecha no reconocido, usando fecha actual");
                    }

                    CertificadoDetalleDTO certificadoDetalles = new CertificadoDetalleDTO(
                            (Integer) fila[0],
                            (String) fila[1],
                            (String) fila[2],
                            (String) fila[3],
                            (String) fila[4],
                            (String) fila[5],
                            ((Timestamp) fila[6]).toLocalDateTime().toLocalDate(),
                            LocalDate.now());

                    return certificadoDetalles;
                } catch (ClassCastException e) {
                    System.err.println("Error de conversión de tipo: " + e.getMessage());
                    e.printStackTrace(); // Para ver más detalles del error
                    return null;
                }
            } else {
                
                System.out.println("La fila no tiene la cantidad de columnas esperada.");
                return null;
            }
        } else {
            System.out.println("No se encontraron resultados para la inscripción " + idInscripcion);
            return null;
        }
    }

    public List<TemaDocumento> getTemas(Integer idCurso) {
        List<Object[]> result = cursoDao.getTemas(idCurso);
        List<TemaDocumento> temas = new ArrayList<>();

        for (Object[] row : result) {
            TemaDocumento tema = new TemaDocumento(
                    (Integer) row[0],
                    (Integer) idCurso,
                    (String) row[1],
                    (String) row[2]);
            temas.add(tema);
        }
        return temas;
    }

    /**
     * Método que busca cursos por nombre utilizando la función PL/pgSQL
     * 
     * @param nombreCurso Nombre del curso a buscar
     * @return Lista de cursos encontrados
     */
    public List<CursoNombreAlumnoDTO> getCursoNombre(String nombreCurso) {
        String sql = "SELECT * FROM filtrar_cursos_nombre(?)"; // Llamamos la función PL/pgSQL
        return jdbcTemplate.query(sql, new Object[] { nombreCurso }, (rs, rowNum) -> {
            // Mapeo del resultado de la consulta a un objeto CursoDocumento
            CursoNombreAlumnoDTO curso = new CursoNombreAlumnoDTO();
            curso.setTituloCurso(rs.getString("titulo_curso"));
            curso.setDescripcion(rs.getString("descripcion"));
            curso.setNombreInstructor(rs.getString("nombre_instructor"));
            curso.setApellidoPaterno(rs.getString("apellido_paterno"));
            curso.setApellidoMaterno(rs.getString("apellido_materno"));
            curso.setNombreCategoria(rs.getString("nombre_categoria"));
            curso.setImagen(rs.getString("imagen"));
            return curso;
        });
    }

    /**
     * Método que busca cursos por nombre utilizando la función PL/pgSQL
     * 
     * @param nombreCurso Nombre del curso a buscar
     * @return Lista de cursos encontrados
     */
    public List<CursoNombreAlumnoDTO> getCursoCategoria(String categoria) {
        String sql = "SELECT * FROM filtrar_cursos_categoria(?)"; // Llamamos la función PL/pgSQL
        return jdbcTemplate.query(sql, new Object[] { categoria }, (rs, rowNum) -> {
            // Mapeo del resultado de la consulta a un objeto CursoDocumento
            CursoNombreAlumnoDTO curso = new CursoNombreAlumnoDTO();
            curso.setTituloCurso(rs.getString("titulo_curso"));
            curso.setDescripcion(rs.getString("descripcion"));
            curso.setNombreInstructor(rs.getString("nombre_instructor"));
            curso.setApellidoPaterno(rs.getString("apellido_paterno"));
            curso.setApellidoMaterno(rs.getString("apellido_materno"));
            curso.setNombreCategoria(rs.getString("nombre_categoria"));
            curso.setImagen(rs.getString("imagen"));
            return curso;
        });
    }

    public List<ContinuarCursoDTO> continuarCurso(Integer idCurso, Integer idUsuarioAlumno) {
        List<Object[]> result = continuarCursoDao.continuar_curso(idCurso, idUsuarioAlumno);
        List<ContinuarCursoDTO> cursos = new ArrayList<>();

        for (Object[] row : result) {
            ContinuarCursoDTO curso = new ContinuarCursoDTO(
                    (Integer) row[0], // idTema
                    String.valueOf(row[1]), // nombreTema
                    (String) row[2], // contenido
                    (Boolean) row[3] // f
            );
            cursos.add(curso);
        }

        return cursos;
    }

    @Override
    @Transactional
    public List<CursoInscritoDTO> verCursosDelAlumno(Integer idAlumno) {
        List<Object[]> resultados = cursoDao.obtenerCursosAlumno(idAlumno);
        List<CursoInscritoDTO> cursosInscritos = new ArrayList<>();

        for (Object[] row : resultados) {
            CursoInscritoDTO curso = new CursoInscritoDTO(
                    (Integer) row[0], // id_curso
                    (String) row[1], // titulo_curso
                    (String) row[2], // descripcion
                    (Integer) row[3], // id_categoria
                    (String) row[4], // nombre_categoria
                    ((Timestamp) row[5]).toLocalDateTime(), // fecha_inscripcion
                    (String) row[6] // estatus_inscripcion
            );
            cursosInscritos.add(curso);
        }
        return cursosInscritos;
    }

    @Override
    public Boolean esAlumno(Integer idUsuario) {
        return cursoDao.esAlumno(idUsuario);
    }

    @Override
    public List<CursoFinalizadoDTO> obtenerCursosFinalizadosPorAlumno(Integer idUsuarioAlumno) {
        List<Object[]> resultados = cursoDao.obtenerCursosFinalizadosPorAlumno(idUsuarioAlumno);
        List<CursoFinalizadoDTO> cursosFinalizados = new ArrayList<>();

        if (resultados != null && !resultados.isEmpty()) {
            for (Object[] fila : resultados) {
                /*
                 * Orden de columnas según la función:
                 * 0: id_inscripcion (Integer)
                 * 1: matricula (String)
                 * 2: fecha_inscripcion (Timestamp)
                 * 3: estatus_inscripcion (String)
                 * 4: id_usuario_alumno (Integer)
                 * 5: nombre_completo_alumno (String)
                 * 6: id_curso (Integer)
                 * 7: id_usuario_instructor (Integer)
                 * 8: id_categoria (Integer)
                 * 9: titulo_curso (String)
                 * 10: descripcion_curso (String)
                 * 11: nombre_categoria (String)
                 * 12: nombre_completo_instructor (String)
                 */
                try {
                    Integer idInscripcion = (Integer) fila[0];
                    String matricula = (String) fila[1];
                    // Convertir el timestamp a LocalDate:
                    LocalDate fechaInscripcion = ((java.sql.Timestamp) fila[2]).toLocalDateTime().toLocalDate();
                    String estatusInscripcion = (String) fila[3];
                    Integer idUsuarioAlumnoVal = (Integer) fila[4];
                    String nombreCompletoAlumno = (String) fila[5];
                    Integer idCurso = (Integer) fila[6];
                    Integer idUsuarioInstructor = (Integer) fila[7];
                    Integer idCategoria = (Integer) fila[8];
                    String tituloCurso = (String) fila[9];
                    String descripcionCurso = (String) fila[10];
                    String nombreCategoria = (String) fila[11];
                    String nombreCompletoInstructor = (String) fila[12];

                    CursoFinalizadoDTO dto = new CursoFinalizadoDTO(
                            idInscripcion,
                            matricula,
                            fechaInscripcion,
                            estatusInscripcion,
                            idUsuarioAlumnoVal,
                            nombreCompletoAlumno,
                            idCurso,
                            idUsuarioInstructor,
                            idCategoria,
                            tituloCurso,
                            descripcionCurso,
                            nombreCategoria,
                            nombreCompletoInstructor);

                    cursosFinalizados.add(dto);
                } catch (ClassCastException e) {
                    System.err.println("Error al mapear la fila: " + e.getMessage());
                }
            }
        }
        return cursosFinalizados;
    }
}
