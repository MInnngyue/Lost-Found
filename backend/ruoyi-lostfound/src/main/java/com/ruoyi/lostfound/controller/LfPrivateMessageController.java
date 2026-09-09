package com.ruoyi.lostfound.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.lostfound.domain.LfPrivateMessage;
import com.ruoyi.lostfound.service.ILfPrivateMessageService;

/**
 * 私信接口
 */
@RestController
@RequestMapping("/lostfound/private-message")
public class LfPrivateMessageController extends BaseController
{
    @Autowired
    private ILfPrivateMessageService privateMessageService;

    @PreAuthorize("@ss.hasPermi('lostfound:message:query')")
    @GetMapping("/conversations")
    public AjaxResult conversations()
    {
        List<LfPrivateMessage> conversations = privateMessageService.selectConversations(getUserId());
        return success(conversations);
    }

    @PreAuthorize("@ss.hasPermi('lostfound:message:query')")
    @GetMapping("/with/{userId}")
    public AjaxResult with(@PathVariable Long userId)
    {
        return success(privateMessageService.selectConversation(getUserId(), userId));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:message:add')")
    @Log(title = "发送私信", businessType = BusinessType.INSERT)
    @RateLimiter(key = "lostfound:message:add", time = 60, count = 30,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PostMapping
    public AjaxResult send(@RequestBody LfPrivateMessage message)
    {
        return success(privateMessageService.sendMessage(message, getUserId()));
    }
}
