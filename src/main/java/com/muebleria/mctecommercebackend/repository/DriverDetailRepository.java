package com.muebleria.mctecommercebackend.repository;

import com.muebleria.mctecommercebackend.model.DriverDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverDetailRepository extends JpaRepository<DriverDetail, Long> {
}