package com.finaxys.skillsrh.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static-resource requests to the Angular SPA index.html.
 * This enables Angular's client-side routing to work when the app is served by Spring Boot.
 *
 * The regex [^\\.] excludes paths with a dot (e.g. .js, .css, .ico) so static assets
 * are still served normally. The /** variant covers nested routes like /collaborateurs/123.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/{path:[^\\.]*}",
        "/{path:[^\\.]*}/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}

