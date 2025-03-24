package com.cutplan.cutplan.controller; 

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cutplan.cutplan.dto.BarraDTO;
import com.cutplan.cutplan.dto.PlanoCorteDTO;
import com.cutplan.cutplan.dto.PecaDTO;
import com.cutplan.cutplan.entity.Barra;
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

    @PostMapping("/barras")
    public ResponseEntity<Barra> cadastrarBarra(@RequestBody BarraDTO barraDTO) {
        Barra barra = materialService.cadastrarBarra(barraDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(barra);
    }

    @GetMapping("/barras")
    public ResponseEntity<List<Barra>> listarBarras() {
        List<Barra> barras = materialService.listarBarras();
        return ResponseEntity.ok(barras);
    }

    @PostMapping("/pecas")
    public ResponseEntity<ResultadoMaterial> cadastrarPeca(@RequestBody PecaDTO pecaDTO) {
        ResultadoMaterial peca = materialService.cadastrarPeca(pecaDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(peca);
    }

    @DeleteMapping("/resultados")
    public ResponseEntity<String> limparResultados() {
        materialService.limparResultados();
        return ResponseEntity.ok("Tabela de resultados limpa com sucesso");
    }

    @PostMapping("/otimizar")
    public ResponseEntity<?> otimizarCorte(
            @RequestParam(required = false) Long barraId,
            @RequestParam double espacoEntreCortes) {
        
        List<Barra> barras = materialService.listarBarras();
        if (barras.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: Não existem barras cadastradas. Por favor, cadastre pelo menos uma barra primeiro.");
        }

        List<ResultadoMaterial> pecas = materialService.obterResultados();
        if (pecas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: Não existem peças cadastradas. Você tem duas opções:\n\n" +
                         "1. Cadastrar peças manualmente usando o endpoint POST /api/material/pecas com o seguinte formato:\n" +
                         "   {\n" +
                         "       \"codigoPeca\": \"string\",\n" +
                         "       \"descricao\": \"string\",\n" +
                         "       \"quantidade\": number,\n" +
                         "       \"comprimento\": number\n" +
                         "   }\n\n" +
                         "2. Enviar um arquivo Excel usando o endpoint POST /api/material/upload\n" +
                         "   O arquivo Excel deve conter as colunas: Código da Peça, Descrição, Quantidade e Comprimento.");
        }

        if (barraId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erro: Por favor, selecione uma barra para otimização. Barras disponíveis: " + barras);
        }

        PlanoCorteDTO plano = materialService.otimizarCorte(pecas, barraId, espacoEntreCortes);
        return ResponseEntity.ok(plano);
    }
}
