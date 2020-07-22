package com.jason.chatbot.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author jasonsong
 * 2020/7/16
 */

@Slf4j
@Configuration
@EnableConfigurationProperties(DingdingProperties.class)
public class DingdingAutoConfiguration {

    @Resource
    private DingdingProperties dingdingProperties;

    @Bean
    @ConditionalOnMissingBean(DingChatBot.class)
    public DingChatBot createDingdingBean() {
        return new DingChatBot(
                dingdingProperties.getSecret(),
                dingdingProperties.getAccessToken(),
                dingdingProperties.getWebHook() + dingdingProperties.getAccessToken(),
                dingdingProperties.getRate());
    }


}
