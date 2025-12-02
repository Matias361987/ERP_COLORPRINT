package com.pauta.colorprint.service;

import com.pauta.colorprint.dto.KpiResult;
import com.pauta.colorprint.model.EstadoTrabajo;
import com.pauta.colorprint.model.Trabajo;
import com.pauta.colorprint.repository.TrabajoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class TrabajoService {

    @Autowired
    private TrabajoRepository trabajoRepository;

    // --- MÉTODOS OPERATIVOS ---
    public List<Trabajo> obtenerTodos() { return trabajoRepository.findAll(); }

    public List<Trabajo> obtenerPendientes(LocalDate fechaFiltro) {
        if (fechaFiltro != null) return trabajoRepository.findByEstadoActualNotAndFechaEntregaOrderByOrdenAsc(EstadoTrabajo.HISTORICOS, fechaFiltro);
        return trabajoRepository.findByEstadoActualNotOrderByFechaEntregaAsc(EstadoTrabajo.HISTORICOS);
    }

    public List<Trabajo> obtenerPorEstado(EstadoTrabajo estado) { return trabajoRepository.findByEstadoActualOrderByOrdenAsc(estado); }

    public List<Trabajo> buscarHistoricos(String palabraClave) {
        if (palabraClave != null) return trabajoRepository.buscarEnHistoricos(palabraClave);
        return trabajoRepository.findByEstadoActualOrderByOrdenAsc(EstadoTrabajo.HISTORICOS);
    }

    public Trabajo obtenerPorId(Long id) { return trabajoRepository.findById(id).orElse(null); }
    public void guardarTrabajo(Trabajo trabajo) { trabajoRepository.save(trabajo); }

    public void avanzarEstado(Long id) {
        Trabajo trabajo = trabajoRepository.findById(id).orElse(null);
        if (trabajo != null) {
            EstadoTrabajo actual = trabajo.getEstadoActual();
            EstadoTrabajo[] estados = EstadoTrabajo.values();
            int indiceActual = actual.ordinal();
            if (indiceActual < estados.length - 1) {
                trabajo.setEstadoActual(estados[indiceActual + 1]);
                trabajo.setOrden(System.currentTimeMillis());
                trabajoRepository.save(trabajo);
            }
        }
    }

    public void moverAEstadoEspecifico(Long id, String nombreEstado) {
        Trabajo t = trabajoRepository.findById(id).orElse(null);
        if (t != null) {
            try {
                t.setEstadoActual(EstadoTrabajo.valueOf(nombreEstado));
                t.setOrden(System.currentTimeMillis());
                trabajoRepository.save(t);
            } catch (Exception e) {}
        }
    }

    public void subirOrden(Long id) {
        Trabajo actual = trabajoRepository.findById(id).orElse(null);
        if (actual != null) {
            Trabajo vecino = trabajoRepository.buscarVecinoArriba(actual.getEstadoActual().name(), actual.getOrden());
            if (vecino != null) {
                Long temp = actual.getOrden(); actual.setOrden(vecino.getOrden()); vecino.setOrden(temp);
                trabajoRepository.save(actual); trabajoRepository.save(vecino);
            }
        }
    }

    public void bajarOrden(Long id) {
        Trabajo actual = trabajoRepository.findById(id).orElse(null);
        if (actual != null) {
            Trabajo vecino = trabajoRepository.buscarVecinoAbajo(actual.getEstadoActual().name(), actual.getOrden());
            if (vecino != null) {
                Long temp = actual.getOrden(); actual.setOrden(vecino.getOrden()); vecino.setOrden(temp);
                trabajoRepository.save(actual); trabajoRepository.save(vecino);
            }
        }
    }

    public void guardarNuevoOrden(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            Trabajo t = trabajoRepository.findById(ids.get(i)).orElse(null);
            if (t != null) { t.setOrden((long) i); trabajoRepository.save(t); }
        }
    }

    // --- INSTALACIONES ---
    public List<Trabajo> getInstalacionesSemana(LocalDate inicio, LocalDate fin) {
        return trabajoRepository.findInstalacionesSemana(inicio, fin);
    }

    // CORRECCIÓN AQUÍ: Pasamos el estado HISTORICOS para excluirlo
    public List<Trabajo> getInstalacionesSinFecha() {
        return trabajoRepository.findInstalacionesSinFecha(EstadoTrabajo.HISTORICOS);
    }

    public void completarInstalacion(Long id) {
        Trabajo t = trabajoRepository.findById(id).orElse(null);
        if (t != null) {
            t.setInstalacionRealizada(true);
            trabajoRepository.save(t);
        }
    }

    // --- KPIs ---
    private LocalDate[] calcularRango(String periodo) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = LocalDate.of(2000, 1, 1);
        LocalDate fin = LocalDate.of(2100, 12, 31);

        if ("mes_actual".equals(periodo)) {
            inicio = hoy.with(TemporalAdjusters.firstDayOfMonth());
            fin = hoy.with(TemporalAdjusters.lastDayOfMonth());
        } else if ("mes_anterior".equals(periodo)) {
            inicio = hoy.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());
            fin = hoy.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        } else if ("anio_actual".equals(periodo)) {
            inicio = hoy.with(TemporalAdjusters.firstDayOfYear());
            fin = hoy.with(TemporalAdjusters.lastDayOfYear());
        }
        return new LocalDate[]{inicio, fin};
    }

    public List<KpiResult> getStatsSustratos(String periodo) {
        LocalDate[] rango = calcularRango(periodo);
        return trabajoRepository.getKpiSustratos(rango[0], rango[1]);
    }

    public List<KpiResult> getStatsMaquinas(String periodo) {
        LocalDate[] rango = calcularRango(periodo);
        return trabajoRepository.getKpiMaquinas(rango[0], rango[1]);
    }

    public List<KpiResult> getStatsResolucion(String periodo) {
        LocalDate[] rango = calcularRango(periodo);
        return trabajoRepository.getKpiResolucion(rango[0], rango[1]);
    }

    public List<KpiResult> getTopClientes(String periodo) {
        LocalDate[] rango = calcularRango(periodo);
        return trabajoRepository.getTopClientes(rango[0], rango[1]);
    }

    public Double getTotalM2(String periodo) {
        LocalDate[] rango = calcularRango(periodo);
        Double total = trabajoRepository.getTotalM2Filtrado(rango[0], rango[1]);
        return (total != null) ? total : 0.0;
    }

    public Long getTotalOrdenes(String periodo) {
        LocalDate[] rango = calcularRango(periodo);
        return trabajoRepository.getTotalOrdenesFiltrado(rango[0], rango[1]);
    }
}