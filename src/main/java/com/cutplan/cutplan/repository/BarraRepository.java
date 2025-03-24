package com.cutplan.cutplan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cutplan.cutplan.entity.Barra;

@Repository
public interface BarraRepository extends JpaRepository<Barra, Long> {
} 