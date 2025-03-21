package com.cutplan.cutplan.dto;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlanoCorteDTO {
    private int totalBarras;
    private double comprimentoBarra;
    private double espacoEntreCortes;
    private double sobraUltimaBarra;
    private List<BarraCorteDTO> barras;
} 