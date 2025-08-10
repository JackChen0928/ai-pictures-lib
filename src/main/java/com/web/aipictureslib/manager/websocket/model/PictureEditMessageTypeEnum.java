package com.web.aipictureslib.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public enum PictureEditMessageTypeEnum {

    INFO("发送通知","INFO"),
    ERROR("发送错误","ERROR"),
    ENTER_EDIT("进入编辑状态","ENTER_EDIT"),
    EXIT_EDIT("退出编辑状态","EXIT_EDIT"),
    EDIT_ACTION("执行编辑操作","EDIT_ACTION");

    private final String text;
    private final String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }


    public static PictureEditMessageTypeEnum getByValue(String value) {
        if (value == null|| value.isEmpty())
            return null;
        for (PictureEditMessageTypeEnum type : PictureEditMessageTypeEnum.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
