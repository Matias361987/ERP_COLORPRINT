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

    public List<Trabajo> obtenerTodos() {
        return trabajoRepository.findAll();
    }

    public List<Trabajo> obtenerPendientes(LocalDate fechaFiltro) {
        if (fechaFiltro != null)
            return trabajoRepository.findByEstadoActualNotAndFechaEntregaOrderByOrdenAsc(EstadoTrabajo.HISTORICOS, fechaFiltro);
        return trabajoRepository.findByEstadoActualNotOrderByFechaEntregaAsc(EstadoTrabajo.HISTORICOS);
    }

    public List<Trabajo> obtenerPorEstado(EstadoTrabajo estado) {
        return trabajoRepository.findByEstadoActualOrderByOrdenAsc(estado);
    }

    public List<Trabajo> buscarHistoricos(String palabraClave) {
        if (palabraClave != null) return trabajoRepository.buscarEnHistoricos(palabraClave);
        return trabajoRepository.findByEstadoActualOrderByOrdenAsc(EstadoTrabajo.HISTORICOS);
    }

    public Trabajo obtenerPorId(Long id) {
        return trabajoRepository.findById(id).orElse(null);
    }

    public void guardarTrabajo(Trabajo trabajo) {
        trabajoRepository.save(trabajo);
    }

    public void avanzarEstado(Long id) {
        Trabajo trabajo = trabajoRepository.findById(id).orElse(null);
        if (trabajo != null) {
            EstadoTrabajo actual = trabajo.getEstadoActual();
            EstadoTrabajo[] estados = EstadoTrabajo.values();
            int indiceActual = actual.ordinal();

            if (indiceActual < estados.length - 1) {
                trabajo.setEstadoActual(estados[indiceActual + 1]);

                // TRUCO: Usamos la hora actual. Es un número tan grande que SIEMPRE será el último.
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

                // TRUCO: Hora actual = Final de la fila garantizado
                t.setOrden(System.currentTimeMillis());

                trabajoRepository.save(t);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void subirOrden(Long id) {
        Trabajo actual = trabajoRepository.findById(id).orElse(null);
        if (actual != null) {
            Trabajo vecino = trabajoRepository.buscarVecinoArriba(actual.getEstadoActual().name(), actual.getOrden());
            if (vecino != null) {
                Long temp = actual.getOrden();
                actual.setOrden(vecino.getOrden());
                vecino.setOrden(temp);
                trabajoRepository.save(actual);
                trabajoRepository.save(vecino);
            }
        }
    }

    public void bajarOrden(Long id) {
        Trabajo actual = trabajoRepository.findById(id).orElse(null);
        if (actual != null) {
            Trabajo vecino = trabajoRepository.buscarVecinoAbajo(actual.getEstadoActual().name(), actual.getOrden());
            if (vecino != null) {
                Long temp = actual.getOrden();
                actual.setOrden(vecino.getOrden());
                vecino.setOrden(temp);
                trabajoRepository.save(actual);
                trabajoRepository.save(vecino);
            }
        }
    }

    // --- OPTIMIZACIÓN 1: DRAG & DROP MASIVO ---
    // Antes: N consultas + N updates. Ahora: 1 consulta + 1 update masivo.
    public void guardarNuevoOrden(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        // Traemos todos los trabajos implicados de una sola vez
        List<Trabajo> trabajos = trabajoRepository.findAllById(ids);

        // Actualizamos el orden en memoria
        for (Trabajo t : trabajos) {
            // Buscamos la posición (índice) que tiene este ID en la lista que mandó el frontend
            int nuevoOrden = ids.indexOf(t.getId());
            if (nuevoOrden != -1) {
                t.setOrden((long) nuevoOrden);
            }
        }

        // Guardamos todos los cambios juntos
        trabajoRepository.saveAll(trabajos);
    }

    // KPIs
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
        LocalDate[] r = calcularRango(periodo);
        return trabajoRepository.getKpiSustratos(r[0], r[1]);
    }

    public List<KpiResult> getStatsMaquinas(String periodo) {
        LocalDate[] r = calcularRango(periodo);
        return trabajoRepository.getKpiMaquinas(r[0], r[1]);
    }

    public List<KpiResult> getStatsResolucion(String periodo) {
        LocalDate[] r = calcularRango(periodo);
        return trabajoRepository.getKpiResolucion(r[0], r[1]);
    }

    public List<KpiResult> getTopClientes(String periodo) {
        LocalDate[] r = calcularRango(periodo);
        return trabajoRepository.getTopClientes(r[0], r[1]);
    }

    public Double getTotalM2(String periodo) {
        LocalDate[] r = calcularRango(periodo);
        Double t = trabajoRepository.getTotalM2Filtrado(r[0], r[1]);
        return (t != null) ? t : 0.0;
    }

    public Long getTotalOrdenes(String periodo) {
        LocalDate[] r = calcularRango(periodo);
        return trabajoRepository.getTotalOrdenesFiltrado(r[0], r[1]);
    }

    // RESUMEN COLA
    public List<KpiResult> getResumenCola() {
        return trabajoRepository.getResumenColaImpresion();
    }

    // INSTALACIONES
    public List<Trabajo> getInstalacionesSemana(LocalDate inicio, LocalDate fin) {
        return trabajoRepository.findInstalacionesSemana(inicio, fin);
    }

    public List<Trabajo> getInstalacionesSinFecha() {
        return trabajoRepository.findInstalacionesSinFecha(EstadoTrabajo.HISTORICOS);
    }

    public List<Trabajo> buscarGlobalmente(String keyword) {
        if (keyword == null || keyword.isEmpty()) return List.of();
        return trabajoRepository.buscarGlobalmente(keyword);
    }

    public void completarInstalacion(Long id) {
        Trabajo t = trabajoRepository.findById(id).orElse(null);
        if (t != null) {
            t.setInstalacionRealizada(true);
            trabajoRepository.save(t);
        }
    }

    public void eliminarTrabajo(Long id) {
        trabajoRepository.deleteById(id);
    }

    // --- OPTIMIZACIÓN 2: MOVER MASIVO ---
    // Antes: Loop con updates individuales. Ahora: saveAll optimizado.
    public void moverMasivo(List<Long> ids, String nombreEstado) {
        if (ids == null || ids.isEmpty()) return;
        try {
            EstadoTrabajo nuevoEstado = EstadoTrabajo.valueOf(nombreEstado);
            long time = System.currentTimeMillis(); // Hora base

            List<Trabajo> trabajos = trabajoRepository.findAllById(ids);

            for (int i = 0; i < trabajos.size(); i++) {
                Trabajo t = trabajos.get(i);
                t.setEstadoActual(nuevoEstado);
                // Asignamos hora + 1, hora + 2... Todos quedarán al final.
                t.setOrden(time + i);
            }

            trabajoRepository.saveAll(trabajos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}