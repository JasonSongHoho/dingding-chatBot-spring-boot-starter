package com.jason.chatbot.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jasonsong
 * 2020/7/16
 */


@ConfigurationProperties(prefix = "dingding.chatbot")
@Data
public class DingdingProperties {
    private String secret;
    private String accessToken;
    private String webHook = "https://oapi.dingtalk.com/robot/send?access_token=";
    private Double rate = 1D/6D;
}
