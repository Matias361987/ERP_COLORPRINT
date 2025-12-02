package com.pauta.colorprint.model;

public enum EstadoTrabajo {
    REVISAR_ARCHIVO,
    IMPRIMIR_VB,
    VB_IMPRESO,
    VB_CLIENTE,
    VB_EN_RUTA,
    VB_APROBADO,
    COLA_DE_IMPRESION,
    TERMINACIONES,
    SELLADO,
    DESPACHOS,
    STAND_BY, // Estado de Pausa
    HISTORICOS
}