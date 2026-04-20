package com.finaxys.templateappname.security.permission;

import java.util.Map;

public interface PermissionProfileRepository {

    Map<String, PermissionProfile> findAllByKey();
}
