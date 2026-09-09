package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfMessage;

/**
 * 系统通知服务
 */
public interface ILfMessageService
{
    List<LfMessage> selectMyMessages(Long userId);

    int countUnread(Long userId);

    int markRead(Long messageId, Long userId);

    int markAllRead(Long userId);

    int send(LfMessage message);
}
