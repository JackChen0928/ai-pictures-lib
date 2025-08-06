package com.web.aipictureslib.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserRoles implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限名称
     */
    private List<String> permissions;

    /**
     * 权限描述
     */
    private String description;



}
