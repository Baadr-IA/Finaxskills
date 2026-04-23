package com.finaxys.skillsrh.security.permission;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.permissions")
public class PermissionProfilesProperties {

    private String file = "classpath:security/permission-profiles.json";

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}

