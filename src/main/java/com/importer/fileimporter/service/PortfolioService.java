package com.importer.fileimporter.service;

import com.importer.fileimporter.config.security.services.CurrentUserProvider;
import com.importer.fileimporter.entity.ExchangeName;
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
    private final CurrentUserProvider currentUserProvider;

    public Portfolio findOrSave(String name) {
        return findOrSave(name, null);
    }

    public Portfolio findOrSave(String name, ExchangeName exchangeName) {
        log.info("Finding or saving portfolio: " + name);
        User currentUser = currentUserProvider.getCurrentUser();
        Optional<Portfolio> byName;
        
        if (currentUser != null) {
            byName = getByNameForUser(name, currentUser);
        } else {
            byName = getByName(name);
        }
        
        Portfolio result = byName.orElseGet(() -> saveBasicEntity(name, exchangeName));
        if (exchangeName != null && result.getExchangeName() == null) {
            result.setExchangeName(exchangeName);
            result = portfolioRepository.save(result);
        }
        log.info("findOrSave result for {}: {}", name, result.getId());
        return result;
    }

    public Optional<Portfolio> getByName(String name) {
        List<Portfolio> allByName = portfolioRepository.findAllByName(name);
        if (allByName.size() > 1) {
            log.warn("Multiple portfolios found with name: {}. IDs: {}", name, 
                    allByName.stream().map(p -> p.getId().toString()).collect(java.util.stream.Collectors.joining(", ")));
        }
        return allByName.isEmpty() ? Optional.empty() : Optional.of(allByName.get(0));
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

    private Portfolio saveBasicEntity(String name, ExchangeName exchangeName) {
        log.info("New portfolio detected: " + name);
        User currentUser = currentUserProvider.getCurrentUser();

        Portfolio portfolio = Portfolio.builder()
                .name(name)
                .exchangeName(exchangeName)
                .creationDate(LocalDateTime.now())
                .user(currentUser)
                .build();

        if (currentUser != null) {
            log.info("Associating portfolio with user: " + currentUser.getUsername());
        } else {
            log.warn("No authenticated user found when creating portfolio: " + name);
        }

        return portfolioRepository.save(portfolio);
    }

    public List<Portfolio> findAll() {
        return portfolioRepository.findAll();
    }
}
