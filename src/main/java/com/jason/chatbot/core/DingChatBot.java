package com.jason.chatbot.core;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import com.jason.chatbot.exception.RateLimiterException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.time.Duration;

/**
 * 钉钉机器人，每个机器人每分钟最多发送20条。如果超过20条，会限流10分钟。
 *
 * @author jasonsong
 * 2020/7/16
 */

@Slf4j
@Getter
public class DingChatBot {


    private String secret;
    private String accessToken;
    private String webHookToken;
    private Double rate;
    /**
     * 限流器，默认每6S允许一次
     */
    private RateLimiter rateLimiter;

    public DingChatBot(String secret, String accessToken, String webHookToken, Double rate) {
        this.secret = secret;
        this.accessToken = accessToken;
        this.webHookToken = webHookToken;
        this.rate = rate;
        rateLimiter = RateLimiter.create(rate);
    }

    private void blockRequest() {
        double acquire = rateLimiter.acquire(1);
        int value = new Double(acquire).intValue();
        rateLimiter.acquire(++value);
        log.warn("blockRequest:{}", value);
    }

    public String sendMsg(String msg) throws RateLimiterException {
        return sendMsg(msg, Duration.ofSeconds(1));
    }

    public String sendMsg(String msg, Duration timeout) throws RateLimiterException {
        boolean acquire = rateLimiter.tryAcquire(timeout);
        if (!acquire) {
            log.error("DingChatbot.sendMsg():{} 限流被拒绝", msg);
            throw new RateLimiterException(accessToken);
        }

        String contact = " ***** ";
        Long timestamp = System.currentTimeMillis();
        String webHookToken = "";
        if (secret != null && !secret.isEmpty()) {
            String sign = generateSign(timestamp);
            webHookToken = this.webHookToken + "&timestamp=" + timestamp + "&sign=" + sign;
        }
        Header header = new BasicHeader("Content-Type", "application/json; charset=utf-8");
        JSONObject content = new JSONObject();
        JSONObject parameters = new JSONObject();
        parameters.put("msgtype", "text");
        content.put("content", contact + msg + contact);
        parameters.put("text", content);

        String response = "";
        try {
            response = HttpClient.post(webHookToken, parameters, header);
            JSONObject resObj = JSON.parseObject(response);
            if (resObj != null) {
                Integer errcode = resObj.getInteger("errcode");
                if (errcode == null || errcode != 0) {
                    log.warn("sendMsg abnormal,blocking. response:{}", response);
                    blockRequest();
                }
            }
        } catch (Exception e) {
            log.error("sendMsg occur error:", e);
        }
        return response;
    }


    private String generateSign(Long timestamp) {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            return URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        } catch (Exception e) {
            log.error("generateSign occur error:", e);
            return "";
        }
    }

}
