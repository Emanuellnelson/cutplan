package com.cutplan.cutplan.controller; 

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cutplan.cutplan.dto.PlanoCorteDTO;
import com.cutplan.cutplan.entity.ResultadoMaterial;
import com.cutplan.cutplan.service.MaterialService;

import lombok.AllArgsConstructor;

@RestController 
@RequestMapping("/api/material")
@AllArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: Nenhum arquivo foi enviado");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".xlsx")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: O arquivo deve ser uma planilha Excel (.xlsx)");
        }

        try {
            materialService.processarPlanilha(file);
            return ResponseEntity.ok("Planilha processada com sucesso");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro ao processar planilha: " + e.getMessage());
        }
    }

    @GetMapping("/resultados")
    public ResponseEntity<List<ResultadoMaterial>> obterResultados() {
        List<ResultadoMaterial> resultados = materialService.obterResultados();
        return ResponseEntity.ok(resultados);
    }

    @PostMapping("/otimizar")
    public ResponseEntity<PlanoCorteDTO> otimizarCorte(
            @RequestParam("comprimentoBarra") double comprimentoBarra,
            @RequestParam("espacoEntreCortes") double espacoEntreCortes) {
        List<ResultadoMaterial> pecas = materialService.obterResultados();
        PlanoCorteDTO plano = materialService.otimizarCorte(pecas, comprimentoBarra, espacoEntreCortes);
        return ResponseEntity.ok(plano);
    }
}
