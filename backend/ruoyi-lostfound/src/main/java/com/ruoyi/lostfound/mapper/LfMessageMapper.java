package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfMessage;

/**
 * 系统通知数据层
 */
public interface LfMessageMapper
{
    List<LfMessage> selectMessages(Long userId);

    int countUnread(Long userId);

    int insertMessage(LfMessage message);

    int markRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    int markAllRead(Long userId);
}
