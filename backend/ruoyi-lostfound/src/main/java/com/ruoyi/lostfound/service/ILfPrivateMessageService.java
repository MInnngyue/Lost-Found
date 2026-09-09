package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfPrivateMessage;

/**
 * 私信服务
 */
public interface ILfPrivateMessageService
{
    List<LfPrivateMessage> selectConversations(Long userId);

    List<LfPrivateMessage> selectConversation(Long userId, Long otherUserId);

    LfPrivateMessage sendMessage(LfPrivateMessage message, Long userId);
}
