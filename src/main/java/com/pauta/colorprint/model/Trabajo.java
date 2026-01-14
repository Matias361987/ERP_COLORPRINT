package com.pauta.colorprint.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "trabajos")
public class Trabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ot;
    private String cliente;
    private String vendedora;
    private String tema;

    private String maquina;
    private String resolucion;
    private String sustrato;

    @Column(name = "pass")
    private String pass;

    private Integer cantidad;
    private Double ancho;
    private Double alto;
    private Double m2Totales;

    @Column(length = 1000)
    private String terminaciones;

    private String tipoDespacho;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaEntrega;

    // --- NUEVOS CAMPOS ---
    private String turnoEntrega; // AM o PM

    @Column(length = 500)
    private String notaVb;       // Nota para Visto Bueno
    // ---------------------

    private String despacharA;

    // --- INSTALACIONES ---
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInstalacion;
    private boolean instalacionRealizada;
    private String notaInstalacion;

    @Enumerated(EnumType.STRING)
    private EstadoTrabajo estadoActual;

    private LocalDateTime fechaIngreso;
    private Long orden;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (fechaIngreso == null) fechaIngreso = LocalDateTime.now();
        if (estadoActual == null) estadoActual = EstadoTrabajo.REVISAR_ARCHIVO;
        if (resolucion == null || resolucion.isEmpty()) resolucion = "Alta";

        // Valor por defecto para el turno si viene nulo
        if (turnoEntrega == null || turnoEntrega.isEmpty()) turnoEntrega = "PM";

        if (ancho != null && alto != null && cantidad != null) {
            this.m2Totales = (ancho * alto * cantidad) / 10000.0;
        } else {
            this.m2Totales = 0.0;
        }

        if (orden == null) {
            this.orden = System.currentTimeMillis();
        }
    }
}