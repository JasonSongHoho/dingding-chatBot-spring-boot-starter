package com.jason.chatbot.exception;

/**
 * @author jasonsong
 * 2020/7/21
 */


public class RateLimiterException extends Exception {
    public RateLimiterException(String token) {
        super("钉钉消息发送太频繁，超过阈值（每3秒1条）被拒绝！token:" + token);
    }
}
