package com.web.aipictureslib.manager.websocket.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.web.aipictureslib.manager.websocket.disruptor.PictureEditEventProducer;
import com.web.aipictureslib.manager.websocket.disruptor.PictureEditEventWorkHandler;
import com.web.aipictureslib.manager.websocket.model.PictureEditActionEnum;
import com.web.aipictureslib.manager.websocket.model.PictureEditMessageTypeEnum;
import com.web.aipictureslib.manager.websocket.model.PictureEditRequestMessage;
import com.web.aipictureslib.manager.websocket.model.PictureEditResponseMessage;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PictureEditHandler extends TextWebSocketHandler {
    @Resource
    private UserService userService;

    @Resource
    PictureEditEventProducer pictureEditEventProducer ;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    //广播消息（有排除session）
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,WebSocketSession excludeSession)throws Exception
    {
        Set<WebSocketSession> sessionset = pictureSessions.get(pictureId);
        if(CollUtil.isNotEmpty( sessionset)){
            //创建ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            //配置序列化：将Long类型转为String类型，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);// 支持 long 基本类型
            objectMapper.registerModule(module);

            //序列化为JSON字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for(WebSocketSession Session:sessionset){
                if (excludeSession != null && excludeSession.equals(Session))
                    continue;
                if(Session.isOpen())
                    Session.sendMessage(textMessage);
            }
        }
    }
    // 全部广播（不排除任何session）
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }
    //
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //保存会话到集合中
        User user  = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("picture");
        pictureSessions.putIfAbsent(pictureId,ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        //构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑",user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        //广播给同一张照片的用户
        broadcastToPicture(pictureId,pictureEditResponseMessage);
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("picture");
        User user = (User) attributes.get("user");
        //移除当前用户的编辑状态
        handleExitEditMessage(null,session,user,pictureId);

        //删除对话
        Set<WebSocketSession> Sessionset = pictureSessions.get(pictureId);
        if (Sessionset != null)
        {
            Sessionset.remove(session);
            if (Sessionset.isEmpty())
                pictureSessions.remove(pictureId);
        }

        //响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s退出编辑",user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId,pictureEditResponseMessage);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    //将消息message解析成PictureEditRequestMessage对象
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);

        //从session属性中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("picture");
        //生产消息

        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);

    }

    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())){
            //移除当前用户的编辑状态
            pictureEditingUsers.remove(pictureId);
            //构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            String message = String.format("%s退出编辑",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            broadcastToPicture(pictureId,pictureEditResponseMessage);
        }

    }

    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum ActionEnum = PictureEditActionEnum.getByValue(editAction);
        if (ActionEnum != null)
            return;
        //确定是当前编辑者
        if (editingUserId != null && editingUserId.equals(user.getId()))
        {
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            String message = String.format("%s执行了%s操作",user.getUserName(),ActionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            broadcastToPicture(pictureId,pictureEditResponseMessage,session);
        }
    }

    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
    //没有用户正在编辑该图片，才能进入编辑
        if (!pictureEditingUsers.containsKey(pictureId)){
            //把当前用户添加到正在编辑的用户集合中
            pictureEditingUsers.put(pictureId,user.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s进入编辑",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId,pictureEditResponseMessage);
        }

    }
}
