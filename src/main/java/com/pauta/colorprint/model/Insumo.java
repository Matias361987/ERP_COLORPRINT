package com.pauta.colorprint.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "insumos")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String categoria;
    private String unidad;

    private String descripcion;
    // PRECIO ELIMINADO

    private Double stockActual;
    private Double stockCritico;

    @PrePersist
    protected void onCreate() {
        if (stockActual == null) stockActual = 0.0;
        if (stockCritico == null) stockCritico = 5.0;
    }
}