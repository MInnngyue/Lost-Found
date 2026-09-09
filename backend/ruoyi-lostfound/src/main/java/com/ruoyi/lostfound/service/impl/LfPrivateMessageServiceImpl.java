package com.ruoyi.lostfound.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.lostfound.domain.LfPrivateMessage;
import com.ruoyi.lostfound.mapper.LfPrivateMessageMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.service.ILfPrivateMessageService;
import com.ruoyi.lostfound.service.IRestrictionService;

/**
 * 私信服务实现
 */
@Service
public class LfPrivateMessageServiceImpl implements ILfPrivateMessageService
{
    @Autowired
    private LfPrivateMessageMapper privateMessageMapper;

    @Autowired
    private com.ruoyi.system.mapper.SysUserMapper userMapper;

    @Autowired
    private IRestrictionService restrictionService;

    @Override
    public List<LfPrivateMessage> selectConversations(Long userId)
    {
        return privateMessageMapper.selectConversations(userId);
    }

    @Override
    @Transactional
    public List<LfPrivateMessage> selectConversation(Long userId, Long otherUserId)
    {
        ensureOtherUser(userId, otherUserId);
        List<LfPrivateMessage> messages = privateMessageMapper.selectConversation(userId, otherUserId);
        privateMessageMapper.markConversationRead(userId, otherUserId);
        return messages;
    }

    @Override
    @Transactional
    public LfPrivateMessage sendMessage(LfPrivateMessage message, Long userId)
    {
        if (restrictionService.isRestricted(userId, "PRIVATE_MESSAGE"))
        {
            throw new ServiceException("当前账号暂时不能发送私信");
        }
        if (message == null || message.getToUserId() == null
                || StringUtils.isBlank(message.getContent()))
        {
            throw new ServiceException("私信内容不能为空");
        }
        ensureOtherUser(userId, message.getToUserId());
        String content = message.getContent().trim();
        if (content.length() > 2000)
        {
            throw new ServiceException("私信不能超过2000个字符");
        }
        message.setFromUserId(userId);
        message.setContent(content);
        message.setConversationKey(conversationKey(userId, message.getToUserId()));
        privateMessageMapper.insertMessage(message);
        return message;
    }

    private void ensureOtherUser(Long userId, Long otherUserId)
    {
        if (userId == null || otherUserId == null || userId.equals(otherUserId)
                || userMapper.selectUserById(otherUserId) == null)
        {
            throw new ServiceException("私信对象不存在或不合法");
        }
    }

    private String conversationKey(Long first, Long second)
    {
        return Math.min(first, second) + ":" + Math.max(first, second);
    }
}
