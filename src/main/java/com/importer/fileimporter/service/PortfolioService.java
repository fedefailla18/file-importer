package com.importer.fileimporter.service;

import com.importer.fileimporter.entity.Portfolio;
import com.importer.fileimporter.entity.User;
import com.importer.fileimporter.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public Portfolio findOrSave(String name) {
        Optional<Portfolio> byName = getByName(name);
        return byName
                .orElseGet(() -> saveBasicEntity(name));
    }

    public Optional<Portfolio> getByName(String name) {
        return portfolioRepository.findByName(name);
    }

    public List<Portfolio> getAll() {
        return portfolioRepository.findAll();
    }

    public List<Portfolio> getAllForUser(User user) {
        return portfolioRepository.findByUser(user);
    }

    public Optional<Portfolio> getByNameForUser(String name, User user) {
        return portfolioRepository.findByNameAndUser(name, user);
    }

    private Portfolio saveBasicEntity(String name) {
        log.info("New portfolio detected: " + name);
        return portfolioRepository.save(Portfolio.builder()
                .name(name)
                .creationDate(LocalDateTime.now())
                .build());
    }

    public List<Portfolio> findAll() {
        return portfolioRepository.findAll();
    }
}
