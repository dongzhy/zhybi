package com.zhy.zhyBI.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.Map;

public class OpenAiApi {
    public static void main(String[] args) {
        String url = "https:xxxxx";
        Map<String,Object> hashMap = new HashMap<>();
        hashMap.put("message","用户的消息帮我分析");
        String json = JSONUtil.toJsonStr(hashMap);
        String result = HttpRequest.post(url)
                .header("Authorization", "替换自己的key")
                .body(json)
                .execute()
                .body();
        //最终结果
    }
}
