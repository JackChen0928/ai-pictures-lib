package com.web.aipictureslib.manager.websocket.interceptor;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.web.aipictureslib.manager.auth.SpaceUserAuthManager;
import com.web.aipictureslib.manager.auth.model.SpaceUserPermissionConstant;
import com.web.aipictureslib.manager.websocket.model.PictureEditActionEnum;
import com.web.aipictureslib.manager.websocket.model.PictureEditMessageTypeEnum;
import com.web.aipictureslib.manager.websocket.model.PictureEditRequestMessage;
import com.web.aipictureslib.model.entity.Picture;
import com.web.aipictureslib.model.entity.Space;
import com.web.aipictureslib.model.entity.User;
import com.web.aipictureslib.model.enums.SpaceTypeEnum;
import com.web.aipictureslib.service.PictureService;
import com.web.aipictureslib.service.SpaceService;
import com.web.aipictureslib.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest){
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            //获取请求参数
            String pictureId = servletRequest.getParameter("pictureId");
            if(ObjUtil.isEmpty(pictureId))
            {
                log.error("图片id为空，拒绝握手");
                return false;
            }
            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser))
            {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            //检验用户是否有该图片的权限
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture))
            {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null){
                space = spaceService.getById(spaceId);
                if (space == null)
                {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue())
                {
                    log.error("非团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT))
            {
                log.error("用户没有该图片的编辑权限，拒绝握手");
                return false;
            }
            //设置属性attrition
            attributes.put("userId",loginUser.getId());
            attributes.put("user", loginUser);
            attributes.put("pictureId",Long.valueOf(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
