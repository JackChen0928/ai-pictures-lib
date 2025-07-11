package com.web.aipictureslib.model.VO;

import lombok.Data;

import java.io.Serializable;
import java.sql.Date;

@Data
/**
 * 用户视图（脱敏）展示用户列表时使用
 */
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

}

