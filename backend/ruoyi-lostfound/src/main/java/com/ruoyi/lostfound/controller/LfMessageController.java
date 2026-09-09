package com.ruoyi.lostfound.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.service.ILfMessageService;

/**
 * 系统通知接口
 */
@RestController
@RequestMapping("/lostfound/message")
public class LfMessageController extends BaseController
{
    @Autowired
    private ILfMessageService messageService;

    @PreAuthorize("@ss.hasPermi('lostfound:message:query')")
    @GetMapping
    public AjaxResult list()
    {
        List<LfMessage> messages = messageService.selectMyMessages(getUserId());
        return success(messages);
    }

    @PreAuthorize("@ss.hasPermi('lostfound:message:query')")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount()
    {
        return success(messageService.countUnread(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:message:edit')")
    @PutMapping("/{messageId}/read")
    public AjaxResult read(@PathVariable Long messageId)
    {
        return toAjax(messageService.markRead(messageId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:message:edit')")
    @PutMapping("/read-all")
    public AjaxResult readAll()
    {
        return toAjax(messageService.markAllRead(getUserId()));
    }
}
