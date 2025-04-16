package mx.utng.finer_back_end.Publicos.Controller;

import mx.utng.finer_back_end.Publicos.Documentos.CursoDetalleDTO;
import mx.utng.finer_back_end.Publicos.Documentos.RecuperarContraseniaDTO;
import mx.utng.finer_back_end.Publicos.Documentos.VerCategoriasDTO;
import mx.utng.finer_back_end.Publicos.Documentos.verIdInscripcionDTO;
import mx.utng.finer_back_end.Publicos.Services.EmailService;
import mx.utng.finer_back_end.Publicos.Services.PublicosService;
import mx.utng.finer_back_end.Publicos.Services.VerCategoriaService;
import mx.utng.finer_back_end.Publicos.Services.VerCursoService;
import mx.utng.finer_back_end.Publicos.Util.JwtUtil;
import mx.utng.finer_back_end.Publicos.Services.verInscripcionIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class PublicosController {

    @Autowired
    private VerCategoriaService categoriaService;
    @Autowired
    private verInscripcionIdService verInscripcionIdService;

    private static final SecureRandom random = new SecureRandom();
    public static String tokenGenerado = null;
    private final PublicosService usuarioService;
    private final EmailService emailService;
    private final VerCursoService cursoService;

    public PublicosController(PublicosService usuarioService, EmailService emailService, VerCursoService cursoService) {
        this.usuarioService = usuarioService;
        this.emailService = emailService;
        this.cursoService = cursoService;
    }

    /**
     * - Genera el token de 6 dígitos numéricos
     * 
     * @return regresa el código de 6 dígitos
     */
    private String generarTokenNumerico() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    /**
     * Obtiene el correo del usuario según su ID y envía el token.
     * 
     * @param correoUsuario correo del usuario a donde se enviará el token.
     * @return Mensaje de confirmación o error.
     */
    public ResponseEntity<String> enviarToken(@PathVariable String correoUsuario) {
        tokenGenerado = generarTokenNumerico();

        boolean enviado = emailService.mandarTokenNumerico(correoUsuario, tokenGenerado);

        if (enviado) {
            return ResponseEntity.ok("Token enviado a " + correoUsuario);
        } else {
            return ResponseEntity.status(500).body("Error al enviar el token.");
        }
    }

    public ResponseEntity<String> enviarTokenSesion(@PathVariable String correoUsuario) {
        tokenGenerado = generarTokenNumerico();

        boolean enviado = emailService.mandarTokenNumericoSesion(correoUsuario, tokenGenerado);

        if (enviado) {
            return ResponseEntity.ok("Token enviado a " + correoUsuario);
        } else {
            return ResponseEntity.status(500).body("Error al enviar el token.");
        }
    }

    /**
     * Comparar los tokens que coincidan y mandar true o false en caso de que sean
     * iguales,
     * este token sera enviado desde front-end.
     * 
     * @param token Token que se envia en parámetros de la ruta
     * @return true si son iguales, si no false
     */
    @GetMapping("/comparar")
    private Map<String, Boolean> compararToken(@RequestParam String token) {
        boolean resultado = tokenGenerado != null && token.equals(tokenGenerado);
        return Map.of("resultado", resultado);
    }
    

    /**
     * Método para iniciar sesión en la aplicación
     * después de verfiicar la identidad en la base de datos
     * y a su correo electrónico
     * 
     * @param obj - Obj que se recibe desde el frontend
     * @return - Regresa la respuesta al frontend que el usuario se autentico y se
     *         envio su token
     * @apiNote - El token que se genera solo funciona mientras la aplicación esta
     *          en su linea de vida (no se reinicio)
     */
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/iniciar-sesion")
    private ResponseEntity<Map<String, Object>> iniciarSesion(@RequestParam String nombreUsuario,
            @RequestParam String contrasenia) {
        try {
            // Llamamos a la función 'autenticarUsuario' del servicio
            Object[] usuarioDatos = usuarioService.autenticarUsuario(nombreUsuario, contrasenia);

            // Verificamos si el usuario fue autenticado correctamente en la base de datos
            if (usuarioDatos != null && (Boolean) usuarioDatos[3]) {
                Integer idUsuario = (Integer) usuarioDatos[0];
                Integer idRol = (Integer) usuarioDatos[1];
                String correo = (String) usuarioDatos[2];

                try {
                    this.enviarTokenSesion(correo);
                } catch (Exception e) {
                    return ResponseEntity.status(500)
                            .body(Map.of("error", "Ocurrió un error al enviar el correo: " + e.getMessage()));
                }
                String token = jwtUtil.generarToken(nombreUsuario);
    
                // Retornamos los datos requeridos
                Map<String, Object> response = new HashMap<>();
                response.put("idUsuario", idUsuario);
                response.put("idRol", idRol);
                response.put("token", token);
    
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Usuario o contraseña incorrectos."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al autenticar: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para obtener todos los cursos aprobados existentes.
     * Se asume que los cursos en la tabla Curso son los aprobados.
     * 
     * @return ResponseEntity con la lista de cursos o mensaje de error.
     */
    @GetMapping("/ver-cursos")
    public ResponseEntity<?> verCursos() {
        try {
            List<CursoDetalleDTO> cursos = cursoService.verCursos();

            if (cursos.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("mensaje", "No se encontraron cursos aprobados");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            return ResponseEntity.ok(cursos);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Error al obtener los cursos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/ver-categoria-aprobada")
    public ResponseEntity<?> obtenerCategorias() {
        try {
            List<VerCategoriasDTO> verCategoriasDTOs = categoriaService.obtenerCategorias();
            if (verCategoriasDTOs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encuentran categorías");
            }
            return ResponseEntity.ok(verCategoriasDTOs);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno al procesar la solicitud: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Solicitud incorrecta: " + e.getMessage());
        }
    }

    /**
     * Endpoint para enviar el token al correo del usuario.
     * Se espera un JSON con la propiedad "correoUsuario".
     * Ejemplo de petición:
     * {
     * "correoUsuario": "usuario@ejemplo.com"
     * }
     * 
     * @param request Mapa que contiene el correo del usuario.
     * @return ResponseEntity con mensaje de confirmación o error.
     */
    @PostMapping("/enviar-token")
    public ResponseEntity<String> enviarToken(@RequestBody Map<String, String> request) {
        String correoUsuario = request.get("correoUsuario");
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            return ResponseEntity.badRequest().body("El correoUsuario es obligatorio");
        }

        tokenGenerado = generarTokenNumerico();
        boolean enviado = emailService.mandarTokenNumerico(correoUsuario, tokenGenerado);
        if (enviado) {
            return ResponseEntity.ok("Token enviado a " + correoUsuario);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al enviar el token.");
        }
    }

    @PostMapping("/enviar-token/inicioSesion")
    public ResponseEntity<String> enviarTokenInicioSesion(@RequestBody Map<String, String> request) {
        String correoUsuario = request.get("correoUsuario");
        if (correoUsuario == null || correoUsuario.isEmpty()) {
            return ResponseEntity.badRequest().body("El correoUsuario es obligatorio");
        }

        tokenGenerado = generarTokenNumerico();
        boolean enviado = emailService.mandarTokenNumericoSesion(correoUsuario, tokenGenerado);
        if (enviado) {
            return ResponseEntity.ok("Token enviado a " + correoUsuario);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al enviar el token.");
        }
    }

    /**
     * Endpoint para recuperar (actualizar) la contraseña.
     * Se espera un JSON con "correo", "token" y "nuevaContrasenia".
     * Ejemplo de petición:
     * {
     * "correo": "usuario@ejemplo.com",
     * "token": "123456",
     * "nuevaContrasenia": "NuevaClaveSegura123!"
     * }
     * 
     * @param dto Objeto con correo, token y nueva contraseña.
     * @return ResponseEntity con true si la actualización fue exitosa, o false en
     *         caso contrario.
     */
    @PostMapping("/recuperar-contrasenia")
    public ResponseEntity<Boolean> recuperarContrasenia(@RequestBody RecuperarContraseniaDTO dto) {

        if (tokenGenerado == null || !tokenGenerado.equals(dto.getToken())) {
            System.out.println("Tokens no coinciden.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        try {
            String resultado = usuarioService.actualizarContrasenia(dto.getCorreo(), dto.getNuevaContrasenia());
            System.out.println("Resultado de actualizar_contrasenia: " + resultado);

            boolean exito = resultado.contains("exitosamente");
            if (exito) {
                tokenGenerado = null; // 🔹 Invalida el token después de la actualización exitosa
                return ResponseEntity.ok(true);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    @GetMapping("/verIdInscripcion/{idUsuario}/{idCurso}")
    public ResponseEntity<?> obtenerIdInscripcion(@PathVariable Integer idUsuario, @PathVariable Integer idCurso) {
        try {
            List<verIdInscripcionDTO> progreso = verInscripcionIdService.obtenerIdInscripcion(idUsuario, idCurso);

            if (progreso.isEmpty()) {
                // Respuesta de error en formato JSON
                Map<String, Object> response = new HashMap<>();
                response.put("mensaje", "No se encontró el id");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            return ResponseEntity.ok(progreso); // Esto sí será JSON si DTO está bien definido
        } catch (Exception e) {
            e.printStackTrace();

            // Error interno en formato JSON
            Map<String, Object> error = new HashMap<>();
            error.put("mensaje", "Error interno del servidor");
            error.put("detalle", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

}
