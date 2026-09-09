package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.mapper.LfMessageMapper;

@ExtendWith(MockitoExtension.class)
class LfMessageServiceImplTest
{
    @Mock
    private LfMessageMapper messageMapper;

    @InjectMocks
    private LfMessageServiceImpl messageService;

    @Test
    void notificationShouldRejectIncompletePayload()
    {
        assertThrows(ServiceException.class, () -> messageService.send(new LfMessage()));
        verify(messageMapper, never()).insertMessage(any());
    }

    @Test
    void notificationShouldRejectOverlongContent()
    {
        LfMessage message = new LfMessage();
        message.setToUserId(20L);
        message.setMessageType("CLAIM");
        message.setContent("x".repeat(2001));

        assertThrows(ServiceException.class, () -> messageService.send(message));
        verify(messageMapper, never()).insertMessage(any());
    }
}
