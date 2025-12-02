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

    @PostMapping("/ajuste")
    public String ajustarStock(@RequestParam Long id, @RequestParam Double cantidad, @RequestParam String accion) {
        Insumo i = insumoRepository.findById(id).orElse(null);
        String cat = (i != null) ? i.getCategoria() : "";
        if (i != null) {
            if ("sumar".equals(accion)) i.setStockActual(i.getStockActual() + cantidad);
            else i.setStockActual(Math.max(0, i.getStockActual() - cantidad));
            insumoRepository.save(i);
        }
        return "redirect:/inventario/detalle?categoria=" + cat;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarInsumo(@PathVariable Long id) {
        Insumo i = insumoRepository.findById(id).orElse(null);
        String cat = (i != null) ? i.getCategoria() : "";
        if (i != null) insumoRepository.deleteById(id);
        if (cat.isEmpty()) return "redirect:/inventario";
        return "redirect:/inventario/detalle?categoria=" + cat;
    }
}