package com.pauta.colorprint.repository;

import com.pauta.colorprint.model.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    // 1. Obtener lista de categorías únicas (Para las Cards)
    @Query("SELECT DISTINCT i.categoria FROM Insumo i ORDER BY i.categoria ASC")
    List<String> findDistinctCategorias();

    // 2. Obtener productos de una categoría específica (Para la Tabla)
    List<Insumo> findByCategoriaOrderByNombreAsc(String categoria);
}