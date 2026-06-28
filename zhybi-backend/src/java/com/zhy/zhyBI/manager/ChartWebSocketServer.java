package com.zhy.zhyBI.manager;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务端：推送图表生成状态
 * 端点相对路径：/api/websocket/chart/{userId}（如果你的Spring Boot配置了server.servlet.context-path=/api）
 * 完整连接地址：ws://localhost:8101/api/websocket/chart/{userId}
 */
// 只保留相对路径，去掉协议、域名、端口
@ServerEndpoint("/websocket/chart/{userId}")
@Component
@Slf4j
public class ChartWebSocketServer {
    // 存储每个用户的连接（userId -> Session）
    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        if (StrUtil.isBlank(userId)) {
            log.error("WebSocket连接失败：userId为空");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.valueOf(String.valueOf(1007)), "userId不能为空"));
            } catch (IOException e) {
                log.error("关闭无效连接失败", e);
            }
            return;
        }
        SESSION_MAP.put(userId, session);
        log.info("用户{}建立WebSocket连接，当前在线数：{}", userId, SESSION_MAP.size());
    }


    /**
     * 连接关闭调用
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        if (StrUtil.isNotBlank(userId)) {
            SESSION_MAP.remove(userId);
            log.info("用户{}关闭WebSocket连接，当前在线数：{}", userId, SESSION_MAP.size());
        }
    }

    /**
     * 收到客户端消息调用
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到客户端消息：{}", message);
    }

    /**
     * 发生错误调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket连接发生错误", error);
        // 错误时移除无效Session
        SESSION_MAP.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }

    /**
     * 主动推送消息给指定用户（线程安全优化）
     * @param userId 用户ID
     * @param message 消息（JSON格式）
     */
    public static void sendMessage(String userId, String message) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(message)) {
            log.warn("推送消息失败：userId或message为空");
            return;
        }
        Session session = SESSION_MAP.get(userId);
        if (session == null) {
            log.warn("用户{}未连接WebSocket", userId);
            return;
        }
        // 双重检查Session是否打开
        if (!session.isOpen()) {
            SESSION_MAP.remove(userId);
            log.warn("用户{}的WebSocket连接已关闭，移除连接", userId);
            return;
        }
        // 同步发送，避免多线程同时操作一个Session
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
                log.debug("成功给用户{}推送消息：{}", userId, message);
            } catch (IOException e) {
                log.error("给用户{}推送消息失败", userId, e);
                // 推送失败时移除无效连接
                SESSION_MAP.remove(userId);
            }
        }
    }

    /**
     * 推送图表状态给用户
     * @param userId 用户ID
     * @param chartId 图表ID
     * @param status 状态（wait/running/succeed/failed）
     * @param execMessage 执行信息（失败时用）
     */
    public static void sendChartStatus(String userId, Long chartId, String status, String execMessage) {
        String message = String.format(
                "{\"chartId\":\"%s\",\"status\":\"%s\",\"execMessage\":\"%s\"}",
                chartId, status, StrUtil.emptyToDefault(execMessage, "")
        );
        sendMessage(userId, message);
    }
}