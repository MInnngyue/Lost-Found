package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfPrivateMessage;

/**
 * 私信数据层
 */
public interface LfPrivateMessageMapper
{
    List<LfPrivateMessage> selectConversations(Long userId);

    List<LfPrivateMessage> selectConversation(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    LfPrivateMessage selectMessageById(Long messageId);

    int insertMessage(LfPrivateMessage message);

    int markConversationRead(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
