package com.finaxys.skillsrh.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static-resource requests to the Angular SPA index.html.
 * This enables Angular's client-side routing to work when the app is served by Spring Boot.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/hello",
        "/good-night",
        "/forbidden"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}

