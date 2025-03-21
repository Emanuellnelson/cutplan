package com.cutplan.cutplan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cutplan.cutplan.entity.ResultadoMaterial;

@Repository
public interface ResultadoMaterialRepository extends JpaRepository<ResultadoMaterial, Long> {
} 