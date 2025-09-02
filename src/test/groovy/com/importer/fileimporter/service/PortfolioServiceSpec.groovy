package com.importer.fileimporter.service

import com.importer.fileimporter.config.security.services.CurrentUserProvider
import com.importer.fileimporter.entity.Portfolio
import com.importer.fileimporter.entity.User
import com.importer.fileimporter.repository.PortfolioRepository
import spock.lang.Specification

class PortfolioServiceSpec extends Specification {

    def portfolioRepository = Mock(PortfolioRepository)
    def currentUserProvider = Mock(CurrentUserProvider)
    def portfolioService = new PortfolioService(portfolioRepository, currentUserProvider)

    def "findOrSave should return existing portfolio for current user"() {
        given:
        def portfolioName = "TestPortfolio"
        def user = new User(username: "testuser")
        def portfolio = new Portfolio(name: portfolioName, user: user)

        and:
        currentUserProvider.getCurrentUser() >> user
        portfolioRepository.findByNameAndUser(portfolioName, user) >> Optional.of(portfolio)

        when:
        def result = portfolioService.findOrSave(portfolioName)

        then:
        result == portfolio
        0 * portfolioRepository.save(_ as Portfolio)
    }

    def "findOrSave should create new portfolio if not found for current user"() {
        given:
        def portfolioName = "TestPortfolio"
        def user = new User(username: "testuser")

        and:
        currentUserProvider.getCurrentUser() >> user
        portfolioRepository.findByNameAndUser(portfolioName, user) >> Optional.empty()

        when:
        def result = portfolioService.findOrSave(portfolioName)

        then:
        1 * portfolioRepository.save({ Portfolio p ->
            p.getName() == portfolioName && p.getUser() == user
        }) >> { Portfolio p -> p }
    }

    def "findOrSave should find portfolio by name when no current user"() {
        given:
        def portfolioName = "TestPortfolio"
        def portfolio = new Portfolio(name: portfolioName)

        and:
        currentUserProvider.getCurrentUser() >> null
        portfolioRepository.findByName(portfolioName) >> Optional.of(portfolio)

        when:
        def result = portfolioService.findOrSave(portfolioName)

        then:
        result == portfolio
        0 * portfolioRepository.save(_ as Portfolio)
    }

    def "findOrSave should create new portfolio when no current user and portfolio not found"() {
        given:
        def portfolioName = "TestPortfolio"

        and:
        currentUserProvider.getCurrentUser() >> null
        portfolioRepository.findByName(portfolioName) >> Optional.empty()

        when:
        def result = portfolioService.findOrSave(portfolioName)

        then:
        1 * portfolioRepository.save({ Portfolio p ->
            p.getName() == portfolioName && p.getUser() == null
        }) >> { Portfolio p -> p }
    }
}
