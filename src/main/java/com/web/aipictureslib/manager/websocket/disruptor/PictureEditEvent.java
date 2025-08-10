package com.web.aipictureslib.manager.websocket.disruptor;

import com.web.aipictureslib.manager.websocket.model.PictureEditRequestMessage;
import com.web.aipictureslib.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {


    /**
     * 图片编辑请求消息,接收前端传来的信息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的Session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片ID
     */
    private Long pictureId;

}
