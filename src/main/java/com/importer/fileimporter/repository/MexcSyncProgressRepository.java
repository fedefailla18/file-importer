package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.MexcSyncProgress;
import com.importer.fileimporter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MexcSyncProgressRepository extends JpaRepository<MexcSyncProgress, UUID> {
    Optional<MexcSyncProgress> findByUserAndSymbol(User user, String symbol);
    List<MexcSyncProgress> findByUser(User user);
}
