package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfPrivateMessage;
import com.ruoyi.lostfound.mapper.LfPrivateMessageMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.lostfound.service.IRestrictionService;

@ExtendWith(MockitoExtension.class)
class LfPrivateMessageServiceImplTest
{
    @Mock
    private LfPrivateMessageMapper privateMessageMapper;

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private IRestrictionService restrictionService;

    @InjectMocks
    private LfPrivateMessageServiceImpl privateMessageService;

    @Test
    void privateMessageShouldRejectSelf()
    {
        LfPrivateMessage message = new LfPrivateMessage();
        message.setToUserId(20L);
        message.setContent("hello");

        assertThrows(ServiceException.class, () -> privateMessageService.sendMessage(message, 20L));
        verify(privateMessageMapper, never()).insertMessage(any());
    }

    @Test
    void privateMessageShouldRejectUnknownRecipient()
    {
        when(userMapper.selectUserById(20L)).thenReturn(null);
        LfPrivateMessage message = new LfPrivateMessage();
        message.setToUserId(20L);
        message.setContent("hello");

        assertThrows(ServiceException.class, () -> privateMessageService.sendMessage(message, 10L));
        verify(privateMessageMapper, never()).insertMessage(any());
    }

    @Test
    void privateMessageShouldRejectOverlongContent()
    {
        SysUser user = new SysUser();
        when(userMapper.selectUserById(20L)).thenReturn(user);
        LfPrivateMessage message = new LfPrivateMessage();
        message.setToUserId(20L);
        message.setContent("x".repeat(2001));

        assertThrows(ServiceException.class, () -> privateMessageService.sendMessage(message, 10L));
        verify(privateMessageMapper, never()).insertMessage(any());
    }
}
