package com.zhy.zhyBI;

import com.zhy.zhyBI.manager.ChartWebSocketServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootTest
public class WebSocketTest {
    @Resource
    private ChartWebSocketServer chartWebSocketServer;

    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    @Test
    void test() {
        ChartWebSocketServer.sendChartStatus(
                "12344555112",
                Long.valueOf("newChartId"),
                "test",
                "测试推送：能收到就是通的！"
        );


    }
}
