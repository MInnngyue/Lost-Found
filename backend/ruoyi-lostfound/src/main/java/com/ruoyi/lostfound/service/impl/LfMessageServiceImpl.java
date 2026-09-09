package com.ruoyi.lostfound.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.mapper.LfMessageMapper;
import com.ruoyi.lostfound.service.ILfMessageService;

/**
 * 系统通知服务实现
 */
@Service
public class LfMessageServiceImpl implements ILfMessageService
{
    @Autowired
    private LfMessageMapper messageMapper;

    @Override
    public List<LfMessage> selectMyMessages(Long userId)
    {
        return messageMapper.selectMessages(userId);
    }

    @Override
    public int countUnread(Long userId)
    {
        return messageMapper.countUnread(userId);
    }

    @Override
    public int markRead(Long messageId, Long userId)
    {
        return messageMapper.markRead(messageId, userId);
    }

    @Override
    public int markAllRead(Long userId)
    {
        return messageMapper.markAllRead(userId);
    }

    @Override
    public int send(LfMessage message)
    {
        if (message == null || message.getToUserId() == null
                || StringUtils.isBlank(message.getMessageType())
                || StringUtils.isBlank(message.getContent()))
        {
            throw new ServiceException("通知参数不完整");
        }
        if (message.getTitle() != null && message.getTitle().length() > 255
                || message.getContent().length() > 2000)
        {
            throw new ServiceException("通知内容超出长度限制");
        }
        if (message.getFromUserId() == null)
        {
            message.setFromUserId(0L);
        }
        return messageMapper.insertMessage(message);
    }
}
