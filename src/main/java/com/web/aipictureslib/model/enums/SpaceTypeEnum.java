package com.web.aipictureslib.model.enums;

import lombok.Getter;

@Getter
public enum SpaceTypeEnum {
    PRIVATE("私有空间", 0),
    TEAM("团队空间", 1);


    private final String text;
    private final int value;

    SpaceTypeEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (value == null) return null;
        for (SpaceTypeEnum valueEnum : values()) {
            if (valueEnum.value == value) {
                return valueEnum;
            }
        }
        return null;
    }

}
