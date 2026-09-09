package com.ruoyi.lostfound.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.lostfound.domain.LfAuditEvent;
import com.ruoyi.lostfound.mapper.LfAuditMapper;
import com.ruoyi.lostfound.service.ILfAuditService;

/**
 * 业务审计服务实现。
 */
@Service
public class LfAuditServiceImpl implements ILfAuditService
{
    @Autowired
    private LfAuditMapper auditMapper;

    @Override
    public void record(Long actorId, String actorType, String eventType, String targetType,
            Long targetId, String beforeData, String afterData)
    {
        LfAuditEvent event = new LfAuditEvent();
        event.setActorId(actorId);
        event.setActorType(actorType);
        event.setEventType(eventType);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setBeforeData(beforeData);
        event.setAfterData(afterData);
        HttpServletRequest request = ServletUtils.getRequest();
        if (request != null)
        {
            event.setClientIp(IpUtils.getIpAddr(request));
            event.setUserAgent(request.getHeader("User-Agent"));
        }
        auditMapper.insertAuditEvent(event);
    }
}
