package com.pauta.colorprint.controller;

import com.pauta.colorprint.model.Insumo;
import com.pauta.colorprint.repository.InsumoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    @Autowired
    private InsumoRepository insumoRepository;

    @GetMapping
    public String verCategorias(Model model) {
        List<String> categorias = insumoRepository.findDistinctCategorias();
        model.addAttribute("categorias", categorias);
        model.addAttribute("nuevoInsumo", new Insumo());
        return "inventario-categorias";
    }

    @GetMapping("/detalle")
    public String verDetalle(@RequestParam String categoria, Model model) {
        List<Insumo> insumos = insumoRepository.findByCategoriaOrderByNombreAsc(categoria);
        long alertas = insumos.stream().filter(i -> i.getStockActual() <= i.getStockCritico()).count();

        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("insumos", insumos);
        model.addAttribute("alertas", alertas);
        return "inventario-detalle";
    }

    @PostMapping("/guardar")
    public String guardarInsumo(@ModelAttribute Insumo insumo) {
        insumoRepository.save(insumo);
        return "redirect:/inventario/detalle?categoria=" + insumo.getCategoria();
    }

    // --- CORRECCIÓN AQUÍ PARA QUE SUME Y RESTE BIEN ---
    @PostMapping("/ajuste")
    public String ajustarStock(@RequestParam Long id, @RequestParam Double cantidad, @RequestParam String accion) {
        Insumo i = insumoRepository.findById(id).orElse(null);
        String categoriaReturn = "";

        if (i != null) {
            categoriaReturn = i.getCategoria(); // Guardamos la categoría para volver

            if ("sumar".equals(accion)) {
                i.setStockActual(i.getStockActual() + cantidad);
            } else if ("restar".equals(accion)) {
                double nuevoStock = i.getStockActual() - cantidad;
                i.setStockActual(Math.max(0, nuevoStock)); // Evitar negativos
            }
            insumoRepository.save(i);
        }

        // Redirigir a la misma categoría para ver el cambio
        return "redirect:/inventario/detalle?categoria=" + categoriaReturn;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarInsumo(@PathVariable Long id) {
        // Necesitamos saber la categoría antes de borrar para redirigir bien
        Insumo i = insumoRepository.findById(id).orElse(null);
        String categoriaReturn = (i != null) ? i.getCategoria() : "";

        if (i != null) {
            insumoRepository.deleteById(id);
        }

        // Si la categoría queda vacía o hay error, volvemos al inicio, si no, a la tabla
        if (categoriaReturn.isEmpty()) return "redirect:/inventario";
        return "redirect:/inventario/detalle?categoria=" + categoriaReturn;
    }
}