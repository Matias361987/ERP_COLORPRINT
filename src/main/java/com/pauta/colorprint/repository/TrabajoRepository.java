package com.pauta.colorprint.repository;

import com.pauta.colorprint.dto.KpiResult;
import com.pauta.colorprint.model.Trabajo;
import com.pauta.colorprint.model.EstadoTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface TrabajoRepository extends JpaRepository<Trabajo, Long> {

    List<Trabajo> findByEstadoActualOrderByOrdenAsc(EstadoTrabajo estado);

    @Query("SELECT t FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND (LOWER(t.ot) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.cliente) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Trabajo> buscarEnHistoricos(@Param("keyword") String keyword);

    @Query(value = "SELECT * FROM trabajos WHERE estado_actual = :estado AND orden < :ordenActual ORDER BY orden DESC LIMIT 1", nativeQuery = true)
    Trabajo buscarVecinoArriba(@Param("estado") String estado, @Param("ordenActual") Long ordenActual);

    @Query(value = "SELECT * FROM trabajos WHERE estado_actual = :estado AND orden > :ordenActual ORDER BY orden ASC LIMIT 1", nativeQuery = true)
    Trabajo buscarVecinoAbajo(@Param("estado") String estado, @Param("ordenActual") Long ordenActual);

    List<Trabajo> findByEstadoActualNotOrderByFechaEntregaAsc(EstadoTrabajo estadoExcluido);
    List<Trabajo> findByEstadoActualNotAndFechaEntregaOrderByOrdenAsc(EstadoTrabajo estadoExcluido, LocalDate fecha);

    // KPIs
    @Query("SELECT COALESCE(UPPER(t.sustrato), 'SIN DEFINIR') AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' GROUP BY UPPER(t.sustrato) ORDER BY valor DESC")
    List<KpiResult> getKpiSustratos();

    @Query("SELECT COALESCE(UPPER(t.maquina), 'SIN ASIGNAR') AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' GROUP BY UPPER(t.maquina) ORDER BY valor DESC")
    List<KpiResult> getKpiMaquinas();

    @Query("SELECT t.resolucion AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' GROUP BY t.resolucion")
    List<KpiResult> getKpiResolucion();

    @Query("SELECT UPPER(t.cliente) AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' GROUP BY UPPER(t.cliente) ORDER BY valor DESC LIMIT 5")
    List<KpiResult> getTopClientes();

    @Query("SELECT SUM(t.m2Totales) FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS'")
    Double getTotalM2Historico();

    @Query("SELECT COUNT(t) FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS'")
    Long getTotalOrdenesHistorico();

    // KPIs FILTRADOS
    @Query("SELECT COALESCE(UPPER(t.sustrato), 'SIN DEFINIR') AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND t.fechaEntrega BETWEEN :inicio AND :fin GROUP BY UPPER(t.sustrato) ORDER BY valor DESC")
    List<KpiResult> getKpiSustratos(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(UPPER(t.maquina), 'SIN ASIGNAR') AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND t.fechaEntrega BETWEEN :inicio AND :fin GROUP BY UPPER(t.maquina) ORDER BY valor DESC")
    List<KpiResult> getKpiMaquinas(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT t.resolucion AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND t.fechaEntrega BETWEEN :inicio AND :fin GROUP BY t.resolucion")
    List<KpiResult> getKpiResolucion(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT UPPER(t.cliente) AS nombre, SUM(t.m2Totales) AS valor FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND t.fechaEntrega BETWEEN :inicio AND :fin GROUP BY UPPER(t.cliente) ORDER BY valor DESC LIMIT 5")
    List<KpiResult> getTopClientes(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT SUM(t.m2Totales) FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND t.fechaEntrega BETWEEN :inicio AND :fin")
    Double getTotalM2Filtrado(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(t) FROM Trabajo t WHERE t.estadoActual = 'HISTORICOS' AND t.fechaEntrega BETWEEN :inicio AND :fin")
    Long getTotalOrdenesFiltrado(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);


    // --- INSTALACIONES ---
    @Query("SELECT t FROM Trabajo t WHERE t.fechaInstalacion BETWEEN :inicio AND :fin AND t.instalacionRealizada = false ORDER BY t.fechaInstalacion ASC")
    List<Trabajo> findInstalacionesSemana(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    // CORRECCIÓN DE SEGURIDAD: Usamos EstadoTrabajo.HISTORICOS como parámetro, no String
    @Query("SELECT t FROM Trabajo t WHERE t.tipoDespacho = 'Instalacion' AND t.fechaInstalacion IS NULL AND t.estadoActual <> :estadoExcluido")
    List<Trabajo> findInstalacionesSinFecha(@Param("estadoExcluido") EstadoTrabajo estadoExcluido);
}