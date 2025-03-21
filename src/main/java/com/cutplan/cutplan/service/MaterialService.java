package com.cutplan.cutplan.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cutplan.cutplan.dto.BarraCorteDTO;
import com.cutplan.cutplan.dto.PecaCorteDTO;
import com.cutplan.cutplan.dto.PlanoCorteDTO;
import com.cutplan.cutplan.entity.ResultadoMaterial;
import com.cutplan.cutplan.repository.ResultadoMaterialRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class MaterialService {

    private final ResultadoMaterialRepository resultadoMaterialRepository;
    private final DataFormatter dataFormatter = new DataFormatter();

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            String value;
            if (cell.getCellType() == CellType.NUMERIC) {
                double numericValue = cell.getNumericCellValue();
                if (numericValue == Math.floor(numericValue)) {
                    value = String.format("%.0f", numericValue);
                } else {
                    value = String.valueOf(numericValue);
                }
            } else {
                value = dataFormatter.formatCellValue(cell);
            }
            return value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            throw new RuntimeException("Campo DIM não pode estar vazio");
        }

        try {
            double value;
            String strValue;
            
            if (cell.getCellType() == CellType.NUMERIC) {
                value = cell.getNumericCellValue();
            } else {
                strValue = getCellValueAsString(cell).replace(",", ".");
                
                if (strValue.isEmpty()) {
                    throw new RuntimeException("Campo DIM não pode estar vazio");
                }
                
                try {
                    value = Double.parseDouble(strValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("O valor '" + strValue + "' não é um número válido para o campo DIM");
                }
            }
            
            if (value <= 0) {
                throw new RuntimeException("O valor do campo DIM deve ser maior que zero");
            }
            
            return value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler o campo DIM");
        }
    }

    private int getCellValueAsInt(Cell cell) {
        if (cell == null) {
            throw new RuntimeException("Campo QTE não pode estar vazio");
        }

        try {
            int value;
            String strValue;
            
            if (cell.getCellType() == CellType.NUMERIC) {
                value = (int) cell.getNumericCellValue();
            } else {
                strValue = getCellValueAsString(cell);
                
                if (strValue.isEmpty()) {
                    throw new RuntimeException("Campo QTD não pode estar vazio");
                }
                
                try {
                    value = Integer.parseInt(strValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("O valor '" + strValue + "não é um número válido para o campo QTD");
                }
            }
            
            if (value <= 0) {
                throw new RuntimeException("O valor do campo QTE deve ser maior que zero");
            }
            
            return value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler o campo QTD");
        }
    }

    public void processarPlanilha(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Arquivo não fornecido ou vazio");
        }

        log.info("Processando planilha: {}", file.getOriginalFilename());

        try {
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet sheet = workbook.getSheetAt(0);

            resultadoMaterialRepository.deleteAll();

            int totalRows = sheet.getPhysicalNumberOfRows();
            if (totalRows <= 1) {
                workbook.close();
                throw new RuntimeException("A planilha está vazia ou contém apenas o cabeçalho");
            }

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                try {
                    // Cell idCell = row.getCell(0);
                    Cell dimCell = row.getCell(0);
                    Cell qtdCell = row.getCell(1);

                    // String identificador = getCellValueAsString(idCell);
                    // if (identificador.isEmpty()) {
                    //     continue;
                    // }

                    ResultadoMaterial resultado = new ResultadoMaterial();
                    
                    try {
                        double comprimento = getCellValueAsDouble(dimCell);
                        resultado.setComprimentoPeca(comprimento);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Linha " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }

                    try {
                        int quantidade = getCellValueAsInt(qtdCell);
                        resultado.setQuantidade(quantidade);
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Linha " + (row.getRowNum() + 1) + ": " + e.getMessage());
                    }

                    resultadoMaterialRepository.save(resultado);
                    log.info("Linha {} processada: ID={}, DIM={}, QTE={}", 
                        row.getRowNum() + 1, resultado.getComprimentoPeca(), resultado.getQuantidade());
                } catch (Exception e) {
                    workbook.close();
                    throw new RuntimeException(e.getMessage());
                }
            }

            workbook.close();
            log.info("Planilha processada com sucesso");
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler o arquivo");
        }
    }

    public PlanoCorteDTO otimizarCorte(List<ResultadoMaterial> pecas, double comprimentoBarra, double espacoEntreCortes) {
        // Ordena as peças em ordem decrescente de comprimento
        pecas.sort((a, b) -> b.getComprimentoPeca().compareTo(a.getComprimentoPeca()));
        
        PlanoCorteDTO plano = new PlanoCorteDTO();
        plano.setComprimentoBarra(comprimentoBarra);
        plano.setEspacoEntreCortes(espacoEntreCortes);
        plano.setBarras(new ArrayList<>());
        
        double comprimentoRestante = comprimentoBarra;
        int barrasUtilizadas = 1;
        boolean primeiraPecaNaBarra = true;
        BarraCorteDTO barraAtual = new BarraCorteDTO();
        barraAtual.setNumeroBarra(barrasUtilizadas);
        barraAtual.setPecas(new ArrayList<>());
        plano.getBarras().add(barraAtual);
        
        // Algoritmo First Fit Decreasing (FFD)
        for (ResultadoMaterial peca : pecas) {
            int quantidadeRestante = peca.getQuantidade();
            
            while (quantidadeRestante > 0) {
                // Calcula o espaço necessário considerando a folga de corte
                double espacoNecessario = peca.getComprimentoPeca();
                if (!primeiraPecaNaBarra) {
                    espacoNecessario += espacoEntreCortes;
                }
                
                if (espacoNecessario <= comprimentoRestante) {
                    // A peça cabe na barra atual
                    PecaCorteDTO pecaCorte = new PecaCorteDTO();
                    pecaCorte.setComprimento(peca.getComprimentoPeca());
                    pecaCorte.setPosicaoInicial(comprimentoBarra - comprimentoRestante);
                    
                    barraAtual.getPecas().add(pecaCorte);
                    comprimentoRestante -= espacoNecessario;
                    quantidadeRestante--;
                    primeiraPecaNaBarra = false;
                } else {
                    // Precisa de uma nova barra
                    barraAtual.setComprimentoRestante(comprimentoRestante);
                    
                    barrasUtilizadas++;
                    comprimentoRestante = comprimentoBarra;
                    primeiraPecaNaBarra = true;
                    
                    barraAtual = new BarraCorteDTO();
                    barraAtual.setNumeroBarra(barrasUtilizadas);
                    barraAtual.setPecas(new ArrayList<>());
                    plano.getBarras().add(barraAtual);
                }
            }
        }
        
        // Atualiza os dados finais do plano
        barraAtual.setComprimentoRestante(comprimentoRestante);
        plano.setTotalBarras(barrasUtilizadas);
        plano.setSobraUltimaBarra(comprimentoRestante);
        
        return plano;
    }

    public List<ResultadoMaterial> obterResultados() {
        return resultadoMaterialRepository.findAll();
    }
}
