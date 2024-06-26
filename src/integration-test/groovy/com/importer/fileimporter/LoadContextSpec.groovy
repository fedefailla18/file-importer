package com.importer.fileimporter

import com.importer.fileimporter.controller.WebController
import org.springframework.beans.factory.annotation.Autowired

class LoadContextSpec extends BaseIntegrationSpec {

    @Autowired (required = false)
    private WebController webController

    def "when context is loaded then all expected beans are created"() {
        expect: "the WebController is created"
        webController
    }
}

