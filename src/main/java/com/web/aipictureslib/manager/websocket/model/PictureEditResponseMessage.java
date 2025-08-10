package com.web.aipictureslib.manager.websocket.model;

import com.web.aipictureslib.model.VO.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
* 图片编辑响应消息,返回给前端的信息
*/
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如：“ENTER_EDIT”,"EXIT_EDIT","EDIT_ACTION"
     */
    private String type;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 用户信息
     */
    private UserVO user;
}
