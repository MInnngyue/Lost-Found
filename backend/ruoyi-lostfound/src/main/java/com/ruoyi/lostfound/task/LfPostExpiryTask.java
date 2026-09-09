package com.ruoyi.lostfound.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.lostfound.service.ILfPostService;

/**
 * 失物招领帖子过期任务。
 */
@Component
@ConditionalOnProperty(name = "lostfound.expiry.enabled", havingValue = "true")
public class LfPostExpiryTask
{
    private static final Logger log = LoggerFactory.getLogger(LfPostExpiryTask.class);

    @Autowired
    private ILfPostService postService;

    @Scheduled(cron = "${lostfound.expiry.cron:0 0/10 * * * ?}")
    public void expirePosts()
    {
        int count = postService.expirePosts("system", null);
        if (count > 0)
        {
            log.info("失物招领帖子自动过期完成，处理数量：{}", count);
        }
    }
}
