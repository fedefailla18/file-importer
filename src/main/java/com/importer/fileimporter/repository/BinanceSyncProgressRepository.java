package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.BinanceSyncProgress;
import com.importer.fileimporter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BinanceSyncProgressRepository extends JpaRepository<BinanceSyncProgress, UUID> {
    Optional<BinanceSyncProgress> findByUserAndSymbol(User user, String symbol);
    List<BinanceSyncProgress> findByUser(User user);
}
