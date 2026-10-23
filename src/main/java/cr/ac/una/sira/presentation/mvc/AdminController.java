package cr.ac.una.sira.presentation.mvc;

import cr.ac.una.sira.business.*;
import cr.ac.una.sira.business.dto.ContratoApi.*;
import cr.ac.una.sira.business.exception.*;
import cr.ac.una.sira.data.RolUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AdminController {
    private final ParticipanteApiService participantes;
    private final UsuarioApiService usuarios;
    private final RutinaApiService rutinas;
    private final String portalUrl;

    public AdminController(ParticipanteApiService participantes, UsuarioApiService usuarios, RutinaApiService rutinas,
            @Value("${sira.portal-url}") String portalUrl) {
        this.participantes = participantes;
        this.usuarios = usuarios;
        this.rutinas = rutinas;
        this.portalUrl = portalUrl;
    }

    @ModelAttribute("portalUrl")
    public String portalUrl() { return portalUrl; }

    @GetMapping("/")
    public String inicio() { return "redirect:/admin/login"; }
    @GetMapping("/admin")
    public String admin() { return "redirect:/admin/participantes"; }
    @GetMapping("/admin/login")
    public String login(org.springframework.security.core.Authentication usuario) {
        if (usuario != null && usuario.getAuthorities().stream().anyMatch(rol -> rol.getAuthority().equals("ROLE_PROFESIONAL"))) {
            return "redirect:/admin/participantes";
        }
        return "admin/login";
    }

    @RequestMapping("/admin/acceso-denegado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accesoDenegado(HttpServletRequest request, Model model) {
        boolean formularioVencido = request.getAttribute(WebAttributes.ACCESS_DENIED_403) instanceof CsrfException;
        model.addAttribute("formularioVencido", formularioVencido);
        return "admin/acceso-denegado";
    }

    private Pageable pagina(int page) {
        return PageRequest.of(Math.max(0, page), 10, Sort.by("id"));
    }

    @GetMapping("/admin/participantes")
    public String participantes(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("pagina", participantes.listar(null, null, pagina(page)));
        return "admin/participantes";
    }

    @GetMapping("/admin/participantes/nuevo")
    public String nuevoParticipante(Model model) {
        model.addAttribute("form", new ParticipanteEntrada("", null, null, true));
        return formularioParticipante(model, null);
    }

    @GetMapping("/admin/participantes/{id}/editar")
    public String editarParticipante(@PathVariable Long id, Model model) {
        var p = participantes.obtener(id);
        model.addAttribute("form", new ParticipanteEntrada(p.nombre(), p.fechaNacimiento(), p.encargadoId(), p.activo()));
        return formularioParticipante(model, id);
    }

    private String formularioParticipante(Model model, Long id) {
        model.addAttribute("id", id);
        return "admin/participante-form";
    }

    @PostMapping("/admin/participantes/guardar")
    public String guardarParticipante(@RequestParam(required = false) Long id,
            @Valid @ModelAttribute("form") ParticipanteEntrada form, BindingResult result,
            Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                if (id == null) participantes.crear(form); else participantes.actualizar(id, form);
                flash.addFlashAttribute("mensaje", "Participante guardado");
                return "redirect:/admin/participantes";
            } catch (EntradaNegocioInvalidaException | ConflictoRecursoException | RecursoNoEncontradoException ex) {
                result.reject("negocio", ex.getMessage());
            }
        }
        return formularioParticipante(model, id);
    }

    @GetMapping("/admin/encargados")
    public String encargados(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("pagina", usuarios.listar(RolUsuario.ENCARGADO, null, pagina(page)));
        return "admin/encargados";
    }

    @GetMapping("/admin/encargados/nuevo")
    public String nuevoEncargado(Model model) {
        model.addAttribute("form", new UsuarioEntrada("", "", ""));
        return "admin/encargado-form";
    }

    @PostMapping("/admin/encargados/guardar")
    public String guardarEncargado(@Valid @ModelAttribute("form") UsuarioEntrada form, BindingResult result,
            RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                usuarios.crearEncargado(form);
                flash.addFlashAttribute("mensaje", "Encargado creado; ya puede asignarlo a un participante");
                return "redirect:/admin/encargados";
            } catch (ConflictoRecursoException ex) { result.reject("negocio", ex.getMessage()); }
        }
        return "admin/encargado-form";
    }

    @GetMapping("/admin/rutinas")
    public String rutinas(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("pagina", rutinas.listar(null, null, null, pagina(page)));
        return "admin/rutinas";
    }

    @GetMapping("/admin/rutinas/nueva")
    public String nuevaRutina(Model model) {
        model.addAttribute("form", new RutinaFormulario(null, "", LocalTime.of(7, 0), LocalDate.now(),
                null, Set.of((short)1, (short)2, (short)3, (short)4, (short)5), ""));
        model.addAttribute("id", null);
        return "admin/rutina-form";
    }

    @GetMapping("/admin/rutinas/{id}/editar")
    public String editarRutina(@PathVariable Long id, Model model) {
        var r = rutinas.obtener(id);
        model.addAttribute("form", new RutinaFormulario(r.participanteId(), r.nombre(), r.horaInicio(),
                r.vigenciaDesde(), r.vigenciaHasta(), r.diasSemana(), r.pasos().stream()
                    .map(p -> p.descripcion() + " | " + p.duracionEstimadaMin()).collect(Collectors.joining("\n"))));
        model.addAttribute("id", id);
        return "admin/rutina-form";
    }

    @PostMapping("/admin/rutinas/guardar")
    public String guardarRutina(@RequestParam(required = false) Long id,
            @Valid @ModelAttribute("form") RutinaFormulario form, BindingResult result,
            Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                if (id == null) rutinas.crear(form.entrada()); else rutinas.actualizar(id, form.entrada());
                flash.addFlashAttribute("mensaje", "Borrador guardado. Publiquelo cuando este listo.");
                return "redirect:/admin/rutinas";
            } catch (IllegalArgumentException | EntradaNegocioInvalidaException | ConflictoRecursoException | RecursoNoEncontradoException ex) {
                result.reject("negocio", ex.getMessage());
            }
        }
        model.addAttribute("id", id);
        return "admin/rutina-form";
    }

    @PostMapping("/admin/rutinas/{id}/publicar")
    public String publicar(@PathVariable Long id, RedirectAttributes flash) {
        try {
            rutinas.publicar(id);
            flash.addFlashAttribute("mensaje", "Rutina publicada y disponible para el encargado");
        } catch (ReglaNegocioException | ConflictoRecursoException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/rutinas";
    }
}
