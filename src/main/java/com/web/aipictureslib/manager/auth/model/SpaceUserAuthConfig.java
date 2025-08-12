package com.web.aipictureslib.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserAuthConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    private List<SpaceUserRole> roles;

}
