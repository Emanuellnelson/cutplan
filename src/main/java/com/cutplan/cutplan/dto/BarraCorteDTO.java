package com.cutplan.cutplan.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BarraCorteDTO {
    private int numeroBarra;
    private List<PecaCorteDTO> pecas;
    private double comprimentoRestante;
} 