package com.pauta.colorprint.controller;

import com.pauta.colorprint.dto.KpiResult;
import com.pauta.colorprint.model.EstadoTrabajo;
import com.pauta.colorprint.model.Trabajo;
import com.pauta.colorprint.service.TrabajoService;
import com.pauta.colorprint.repository.TrabajoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class TrabajoController {

    @Autowired
    private TrabajoService trabajoService;

    @Autowired
    private TrabajoRepository trabajoRepository;

    // --- NAVEGACIÓN ---
    @GetMapping("/") public String inicio() { return "redirect:/login"; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/ingreso") public String verIngreso(Model model) { return "ingreso"; }

    // --- PAUTA ---
    @GetMapping("/pauta")
    public String verPauta(@RequestParam(required = false) String estado,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String fecha,
                           Model model) {

        List<Trabajo> listaTrabajos = null;
        EstadoTrabajo estadoSeleccionado = null;
        LocalDate fechaFiltro = null;

        if (fecha != null && !fecha.isEmpty()) {
            try { fechaFiltro = LocalDate.parse(fecha); } catch (Exception e) { fechaFiltro = null; }
        }

        try {
            if (keyword != null && !keyword.isEmpty()) {
                listaTrabajos = trabajoService.buscarGlobalmente(keyword);
                model.addAttribute("busquedaActiva", true);
            } else if (estado != null && !estado.isEmpty()) {
                try {
                    estadoSeleccionado = EstadoTrabajo.valueOf(estado);
                    if (estadoSeleccionado == EstadoTrabajo.HISTORICOS) {
                        listaTrabajos = trabajoService.buscarHistoricos(null);
                    } else {
                        listaTrabajos = trabajoService.obtenerPorEstado(estadoSeleccionado);
                        if (estadoSeleccionado == EstadoTrabajo.COLA_DE_IMPRESION) {
                            try {
                                model.addAttribute("resumenMateriales", trabajoService.getResumenCola());
                            } catch (Exception e) {
                                System.out.println("Advertencia: No se pudo cargar el resumen de materiales.");
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    estadoSeleccionado = null;
                    listaTrabajos = trabajoService.obtenerPendientes(fechaFiltro);
                }
            } else {
                listaTrabajos = trabajoService.obtenerPendientes(fechaFiltro);
            }
        } catch (Exception e) {
            System.out.println("Error recuperado en Pauta: " + e.getMessage());
            listaTrabajos = trabajoService.obtenerPendientes(null);
        }

        model.addAttribute("trabajos", listaTrabajos);
        model.addAttribute("estadoActual", estadoSeleccionado);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fechaFiltro", fechaFiltro);

        return "pauta";
    }

    // --- CALENDARIO ---
    @GetMapping("/calendario")
    public String verCalendario(@RequestParam(required = false) String fechaBase, Model model) {
        LocalDate hoy = (fechaBase != null && !fechaBase.isEmpty()) ? LocalDate.parse(fechaBase) : LocalDate.now();
        LocalDate lunes = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
        LocalDate domingo = lunes.plusDays(6);
        List<Trabajo> instalaciones = trabajoService.getInstalacionesSemana(lunes, domingo);
        List<Trabajo> pendientes = trabajoRepository.findInstalacionesSinFecha(EstadoTrabajo.HISTORICOS);
        model.addAttribute("instalaciones", instalaciones);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("lunes", lunes);
        model.addAttribute("semanaActual", lunes);
        return "calendario";
    }

    @PostMapping("/instalaciones/asignar")
    public String asignarFechaInstalacion(@RequestParam Long id, @RequestParam String fecha, @RequestParam(required = false) String nota) {
        try {
            if (id == null) return "redirect:/calendario";
            Trabajo t = trabajoService.obtenerPorId(id);
            if (t != null) {
                t.setFechaInstalacion(LocalDate.parse(fecha));
                t.setNotaInstalacion(nota);
                trabajoService.guardarTrabajo(t);
            }
        } catch (Exception e) {}
        return "redirect:/instalaciones/por-agendar";
    }

    @PostMapping("/instalacion/completar/{id}") public String completarInstalacion(@PathVariable Long id) { trabajoService.completarInstalacion(id); return "redirect:/calendario"; }
    @GetMapping("/instalaciones/por-agendar") public String verPorAgendar(Model model) { List<Trabajo> pendientes = trabajoRepository.findInstalacionesSinFecha(EstadoTrabajo.HISTORICOS); model.addAttribute("pendientes", pendientes); return "agendar"; }

    // --- ESTADÍSTICAS ---
    @GetMapping("/estadisticas")
    public String verEstadisticas(@RequestParam(required = false, defaultValue = "todo") String periodo, Model model) {
        List<KpiResult> sustratos = trabajoService.getStatsSustratos(periodo);
        List<KpiResult> maquinas = trabajoService.getStatsMaquinas(periodo);
        List<KpiResult> resolucion = trabajoService.getStatsResolucion(periodo);
        List<KpiResult> clientes = trabajoService.getTopClientes(periodo);

        model.addAttribute("labelSustratos", sustratos.stream().map(KpiResult::getNombre).collect(Collectors.toList()));
        model.addAttribute("dataSustratos", sustratos.stream().map(KpiResult::getValor).collect(Collectors.toList()));
        model.addAttribute("labelMaquinas", maquinas.stream().map(KpiResult::getNombre).collect(Collectors.toList()));
        model.addAttribute("dataMaquinas", maquinas.stream().map(KpiResult::getValor).collect(Collectors.toList()));
        model.addAttribute("labelCalidad", resolucion.stream().map(KpiResult::getNombre).collect(Collectors.toList()));
        model.addAttribute("dataCalidad", resolucion.stream().map(KpiResult::getValor).collect(Collectors.toList()));
        model.addAttribute("labelClientes", clientes.stream().map(KpiResult::getNombre).collect(Collectors.toList()));
        model.addAttribute("dataClientes", clientes.stream().map(KpiResult::getValor).collect(Collectors.toList()));

        Double totalM2 = trabajoService.getTotalM2(periodo);
        if (totalM2 == null) totalM2 = 0.0;
        Long totalOrdenes = trabajoService.getTotalOrdenes(periodo);
        if (totalOrdenes == null) totalOrdenes = 0L;

        model.addAttribute("kpiTotalM2", totalM2);
        model.addAttribute("kpiTotalOrdenes", totalOrdenes);
        model.addAttribute("kpiPromedio", (totalOrdenes > 0) ? (totalM2 / totalOrdenes) : 0);
        model.addAttribute("periodoActual", periodo);
        return "estadisticas";
    }

    // --- GUARDADO ---
    @PostMapping("/guardar")
    public String guardarMasivo(
            @RequestParam String ot, @RequestParam String cliente, @RequestParam String vendedora,
            @RequestParam String fechaEntrega, @RequestParam EstadoTrabajo estadoActual,
            @RequestParam(required = false) List<String> tema, @RequestParam(required = false) List<String> maquina,
            @RequestParam(required = false) List<String> resolucion, @RequestParam(required = false) List<String> sustrato,
            @RequestParam(required = false) List<Double> ancho, @RequestParam(required = false) List<Double> alto,
            @RequestParam(required = false) List<Integer> cantidad, @RequestParam(required = false) List<String> terminaciones,
            @RequestParam(required = false) List<String> tipoDespacho, @RequestParam(required = false) List<String> despacharA
    ) {
        if (maquina == null || maquina.isEmpty()) return "redirect:/ingreso";
        for (int i = 0; i < maquina.size(); i++) {
            if (ancho.get(i) == null || cantidad.get(i) == null) continue;
            Trabajo t = new Trabajo();
            t.setOt(ot); t.setCliente(cliente); t.setVendedora(vendedora); t.setFechaEntrega(LocalDate.parse(fechaEntrega)); t.setEstadoActual(estadoActual);
            t.setTema(tema.get(i)); t.setMaquina(maquina.get(i)); t.setResolucion(resolucion.get(i)); t.setSustrato(sustrato.get(i));
            t.setAncho(ancho.get(i)); t.setAlto(alto.get(i)); t.setCantidad(cantidad.get(i));
            t.setTerminaciones(terminaciones.get(i));
            if (tipoDespacho != null && i < tipoDespacho.size()) t.setTipoDespacho(tipoDespacho.get(i));
            if (despacharA != null && i < despacharA.size()) t.setDespacharA(despacharA.get(i));
            t.setOrden(System.currentTimeMillis() + i);
            trabajoService.guardarTrabajo(t);
        }
        return "redirect:/pauta";
    }

    // --- ACCIONES Y MODIFICACIÓN ---

    @PostMapping("/actualizar")
    public String actualizarTrabajo(@RequestParam Long id,
                                    @RequestParam String maquina,
                                    @RequestParam String resolucion,
                                    @RequestParam(required = false) String pass, // NUEVO CAMPO PASS
                                    @RequestParam String fechaEntrega,
                                    @RequestParam(required = false) String urlActual, // PARA REDIRECCIÓN
                                    @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            Trabajo t = trabajoService.obtenerPorId(id);
            if(t != null){
                t.setMaquina(maquina);
                t.setResolucion(resolucion);
                t.setPass(pass); // GUARDAMOS EL PASS
                if (fechaEntrega != null && !fechaEntrega.isEmpty()) t.setFechaEntrega(LocalDate.parse(fechaEntrega));
                trabajoService.guardarTrabajo(t);
            }
        } catch (Exception e) {}

        // LÓGICA DE REDIRECCIÓN PRIORITARIA
        if (urlActual != null && !urlActual.isEmpty()) return "redirect:" + urlActual;
        if (referer != null && !referer.isEmpty()) return "redirect:" + referer;
        return "redirect:/pauta";
    }

    @PostMapping("/avanzar/{id}")
    public String avanzarEstado(@PathVariable Long id, @RequestHeader(value = "Referer", required = false) String referer) {
        trabajoService.avanzarEstado(id);
        return "redirect:" + (referer != null ? referer : "/pauta");
    }

    @PostMapping("/standby/{id}")
    public String moverStandBy(@PathVariable Long id, @RequestHeader(value = "Referer", required = false) String referer) {
        Trabajo t = trabajoService.obtenerPorId(id);
        if(t!=null){
            t.setEstadoActual(EstadoTrabajo.STAND_BY);
            trabajoService.guardarTrabajo(t);
        }
        return "redirect:" + (referer != null ? referer : "/pauta");
    }

    @PostMapping("/subir/{id}")
    public String subirTrabajo(@PathVariable Long id, @RequestHeader(value = "Referer", required = false) String referer) {
        trabajoService.subirOrden(id);
        return "redirect:" + (referer != null ? referer : "/pauta");
    }

    @PostMapping("/bajar/{id}")
    public String bajarTrabajo(@PathVariable Long id, @RequestHeader(value = "Referer", required = false) String referer) {
        trabajoService.bajarOrden(id);
        return "redirect:" + (referer != null ? referer : "/pauta");
    }

    @PostMapping("/eliminar-trabajo/{id}")
    public String eliminarTrabajo(@PathVariable Long id, @RequestHeader(value = "Referer", required = false) String referer) {
        Trabajo t = trabajoService.obtenerPorId(id);
        if (t != null) {
            trabajoService.eliminarTrabajo(id);
        }
        return "redirect:" + (referer != null ? referer : "/pauta?estado=HISTORICOS");
    }

    @PostMapping("/acciones/masivas")
    public String accionesMasivas(@RequestParam(required = false) List<Long> ids, @RequestParam String destino, @RequestHeader(value = "Referer", required = false) String referer) {
        if (ids != null && !ids.isEmpty()) {
            trabajoService.moverMasivo(ids, destino);
        }
        return "redirect:" + (referer != null ? referer : "/pauta");
    }

    @PostMapping("/reordenar")
    @ResponseBody
    public String reordenar(@RequestBody List<Long> ids) {
        trabajoService.guardarNuevoOrden(ids);
        return "OK";
    }
}