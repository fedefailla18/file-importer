package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.ExternalApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExternalApiLogRepository extends JpaRepository<ExternalApiLog, UUID> {
}
