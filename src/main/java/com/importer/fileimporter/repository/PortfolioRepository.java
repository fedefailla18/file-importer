package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    @Override
    List<Portfolio> findAll();

    @Query(value = "select p from Portfolio p " +
            "where lower(p.name) like lower(concat('%', :name,'%'))")
    Optional<Portfolio> findByName(@Param("name") String name);

}
