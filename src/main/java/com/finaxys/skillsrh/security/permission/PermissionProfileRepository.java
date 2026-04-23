package com.finaxys.skillsrh.security.permission;

import java.util.Map;

public interface PermissionProfileRepository {

    Map<String, PermissionProfile> findAllByKey();
}

