package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.ExternalApiRawResponse;
import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExternalApiRawResponseRepository extends JpaRepository<ExternalApiRawResponse, UUID> {
}
