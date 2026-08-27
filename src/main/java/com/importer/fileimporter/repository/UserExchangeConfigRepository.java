package com.importer.fileimporter.repository;

import com.importer.fileimporter.entity.ExchangeName;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.entity.UserExchangeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserExchangeConfigRepository extends JpaRepository<UserExchangeConfig, UUID> {
    List<UserExchangeConfig> findByUser(User user);
    Optional<UserExchangeConfig> findByUserAndExchangeName(User user, ExchangeName exchangeName);
}
