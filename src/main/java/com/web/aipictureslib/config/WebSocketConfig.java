package com.web.aipictureslib.config;

import com.web.aipictureslib.manager.websocket.handler.PictureEditHandler;
import com.web.aipictureslib.manager.websocket.interceptor.WsHandShakeInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Component
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Resource
    private PictureEditHandler pictureEditHandler;
    @Resource
    private WsHandShakeInterceptor wsHandShakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pictureEditHandler, "/ws/picture/edit")
                .addInterceptors(wsHandShakeInterceptor)
                .setAllowedOrigins("*");
    }
}
