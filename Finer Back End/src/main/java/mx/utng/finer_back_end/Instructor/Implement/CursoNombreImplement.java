package mx.utng.finer_back_end.Instructor.Implement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mx.utng.finer_back_end.Instructor.Dao.CursoNombreDao;
import mx.utng.finer_back_end.Instructor.Documentos.CursoDetalleNombreDTO;
import mx.utng.finer_back_end.Instructor.Services.CursoNombreService;

@Service
public class CursoNombreImplement implements CursoNombreService{
    @Autowired
    private CursoNombreDao cursoNombreDao;

    @Override
    public List<CursoDetalleNombreDTO> filtrarCursoNombre(String busqueda) {
        List<Object[]> resultados = cursoNombreDao.filtrarCursosPorNombre(busqueda);
        List<CursoDetalleNombreDTO> detalles = new ArrayList<>();

        for (Object[] row : resultados) {
            CursoDetalleNombreDTO dto = new CursoDetalleNombreDTO(
                (String) row[0], // titulo_curso
                (String) row[1], // descripcion
                (String) row[2], // nombre_instructor
                (String) row[3], // apellido_paterno
                (String) row[4], // apellido_materno
                (String) row[5]  // nombre_categoria
            );
            detalles.add(dto);
        }
        return detalles;
    }
}
