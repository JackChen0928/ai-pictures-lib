package com.web.aipictureslib.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserAddRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 用户角色：view/editor/admin
     */
    private String spaceRole;

}
