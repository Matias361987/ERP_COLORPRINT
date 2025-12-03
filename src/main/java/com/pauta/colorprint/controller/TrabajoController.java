package com.pauta.colorprint.controller;

import com.pauta.colorprint.dto.KpiResult;
import com.pauta.colorprint.model.EstadoTrabajo;
import com.pauta.colorprint.model.Trabajo;
import com.pauta.colorprint.service.TrabajoService;
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

    // --- NAVEGACIÓN BÁSICA ---
    @GetMapping("/") public String inicio() { return "redirect:/login"; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/ingreso") public String verIngreso(Model model) { return "ingreso"; }

    // --- CALENDARIO DE INSTALACIONES ---
    @GetMapping("/calendario")
    public String verCalendario(@RequestParam(required = false) String fechaBase, Model model) {
        LocalDate hoy = (fechaBase != null && !fechaBase.isEmpty()) ? LocalDate.parse(fechaBase) : LocalDate.now();
        LocalDate lunes = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
        LocalDate domingo = lunes.plusDays(6);

        List<Trabajo> instalaciones = trabajoService.getInstalacionesSemana(lunes, domingo);
        List<Trabajo> pendientes = trabajoService.getInstalacionesSinFecha();

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
        } catch (Exception e) { e.printStackTrace(); }
        return "redirect:/instalaciones/por-agendar";
    }

    @PostMapping("/instalacion/completar/{id}")
    public String completarInstalacion(@PathVariable Long id) {
        trabajoService.completarInstalacion(id);
        return "redirect:/calendario";
    }

    @GetMapping("/instalaciones/por-agendar")
    public String verPorAgendar(Model model) {
        List<Trabajo> pendientes = trabajoService.getInstalacionesSinFecha();
        model.addAttribute("pendientes", pendientes);
        return "agendar";
    }

    // --- PAUTA ---
    @GetMapping("/pauta")
    public String verPauta(@RequestParam(required = false) String estado,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String fecha,
                           Model model) {
        List<Trabajo> listaTrabajos;
        EstadoTrabajo estadoSeleccionado = null;
        LocalDate fechaFiltro = (fecha != null && !fecha.isEmpty()) ? LocalDate.parse(fecha) : null;

        if (estado != null && !estado.isEmpty()) {
            try {
                estadoSeleccionado = EstadoTrabajo.valueOf(estado);
                if (estadoSeleccionado == EstadoTrabajo.HISTORICOS) {
                    listaTrabajos = (keyword != null && !keyword.isEmpty()) ?
                            trabajoService.buscarHistoricos(keyword) : trabajoService.buscarHistoricos(null);
                } else {
                    listaTrabajos = trabajoService.obtenerPorEstado(estadoSeleccionado);
                }
            } catch (IllegalArgumentException e) { listaTrabajos = trabajoService.obtenerPendientes(fechaFiltro); }
        } else {
            listaTrabajos = trabajoService.obtenerPendientes(fechaFiltro);
        }

        model.addAttribute("trabajos", listaTrabajos);
        model.addAttribute("estadoActual", estadoSeleccionado);
        model.addAttribute("keyword", keyword);
        model.addAttribute("fechaFiltro", fechaFiltro);
        return "pauta";
    }

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

        model.addAttribute("kpiTotalM2", trabajoService.getTotalM2(periodo));
        model.addAttribute("kpiTotalOrdenes", trabajoService.getTotalOrdenes(periodo));
        Long ordenes = trabajoService.getTotalOrdenes(periodo);
        model.addAttribute("kpiPromedio", (ordenes > 0) ? (trabajoService.getTotalM2(periodo) / ordenes) : 0);
        model.addAttribute("periodoActual", periodo);
        return "estadisticas";
    }

    // --- GUARDADO MASIVO COMPLETO ---
    @PostMapping("/guardar")
    public String guardarMasivo(
            @RequestParam String ot, @RequestParam String cliente,
            @RequestParam String vendedora, // RECIBE VENDEDORA
            @RequestParam String fechaEntrega, @RequestParam EstadoTrabajo estadoActual,

            @RequestParam(required = false) List<String> tema,
            @RequestParam(required = false) List<String> maquina,
            @RequestParam(required = false) List<String> resolucion,
            @RequestParam(required = false) List<String> sustrato,
            @RequestParam(required = false) List<Double> ancho,
            @RequestParam(required = false) List<Double> alto,
            @RequestParam(required = false) List<Integer> cantidad,
            @RequestParam(required = false) List<String> terminaciones,

            // LISTAS DE LOGÍSTICA POR FILA
            @RequestParam(required = false) List<String> tipoDespacho,
            @RequestParam(required = false) List<String> despacharA
    ) {
        if (maquina == null || maquina.isEmpty()) return "redirect:/ingreso";

        for (int i = 0; i < maquina.size(); i++) {
            if (ancho.get(i) == null || cantidad.get(i) == null) continue;

            Trabajo t = new Trabajo();

            // Cabecera
            t.setOt(ot);
            t.setCliente(cliente);
            t.setVendedora(vendedora); // GUARDA VENDEDORA
            t.setFechaEntrega(LocalDate.parse(fechaEntrega));
            t.setEstadoActual(estadoActual);

            // Filas
            t.setTema(tema.get(i));
            t.setMaquina(maquina.get(i));
            t.setResolucion(resolucion.get(i));
            t.setSustrato(sustrato.get(i));
            t.setAncho(ancho.get(i));
            t.setAlto(alto.get(i));
            t.setCantidad(cantidad.get(i));
            t.setTerminaciones(terminaciones.get(i));

            // Logística (Validando listas)
            if (tipoDespacho != null && i < tipoDespacho.size()) t.setTipoDespacho(tipoDespacho.get(i));
            if (despacharA != null && i < despacharA.size()) t.setDespacharA(despacharA.get(i));

            t.setOrden(System.currentTimeMillis() + i);
            trabajoService.guardarTrabajo(t);
        }
        return "redirect:/pauta";
    }

    // --- ACCIONES VARIAS ---
    @PostMapping("/avanzar/{id}") public String avanzarEstado(@PathVariable Long id) { trabajoService.avanzarEstado(id); return "redirect:/pauta"; }
    @PostMapping("/standby/{id}") public String moverStandBy(@PathVariable Long id) { Trabajo t = trabajoService.obtenerPorId(id); if(t!=null){t.setEstadoActual(EstadoTrabajo.STAND_BY); trabajoService.guardarTrabajo(t);} return "redirect:/pauta"; }
    @PostMapping("/mover-manual") public String moverManual(@RequestParam Long id, @RequestParam String destino) { trabajoService.moverAEstadoEspecifico(id, destino); return "redirect:/pauta"; }

    @PostMapping("/actualizar")
    public String actualizarTrabajo(@RequestParam Long id, @RequestParam String maquina, @RequestParam String resolucion, @RequestParam String fechaEntrega) {
        Trabajo t = trabajoService.obtenerPorId(id);
        if(t!=null){
            t.setMaquina(maquina); t.setResolucion(resolucion);
            if (fechaEntrega != null && !fechaEntrega.isEmpty()) t.setFechaEntrega(LocalDate.parse(fechaEntrega));
            trabajoService.guardarTrabajo(t);
        }
        return "redirect:/pauta";
    }

    @PostMapping("/subir/{id}") public String subirTrabajo(@PathVariable Long id) { trabajoService.subirOrden(id); return "redirect:/pauta?estado=COLA_DE_IMPRESION"; }
    @PostMapping("/bajar/{id}") public String bajarTrabajo(@PathVariable Long id) { trabajoService.bajarOrden(id); return "redirect:/pauta?estado=COLA_DE_IMPRESION"; }
    @PostMapping("/reordenar") @ResponseBody public String reordenar(@RequestBody List<Long> ids) { trabajoService.guardarNuevoOrden(ids); return "OK"; }

    // ELIMINAR (SOLO HISTÓRICOS)
    @PostMapping("/eliminar-trabajo/{id}")
    public String eliminarTrabajo(@PathVariable Long id) {
        Trabajo t = trabajoService.obtenerPorId(id);
        if (t != null) { trabajoService.eliminarTrabajo(id); }
        return "redirect:/pauta?estado=HISTORICOS";
    }
}