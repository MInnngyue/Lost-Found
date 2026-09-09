package com.ruoyi.lostfound.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 失物招领定时任务配置。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "lostfound.expiry.enabled", havingValue = "true")
public class LfSchedulingConfig
{
}
