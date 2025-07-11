package com.web.aipictureslib.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    User("用户", "user"),
    Admin("管理员", "admin");

    private final String text;

    private final String value;
    UserRoleEnum(String text ,String  value) {
        this.text = text;
        this.value = value;
    }
    /**
     * 根据 value 获取枚举 例如根据“user“获取枚举 User
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value){
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
