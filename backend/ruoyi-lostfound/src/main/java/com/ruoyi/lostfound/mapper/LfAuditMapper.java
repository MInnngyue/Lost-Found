package com.ruoyi.lostfound.mapper;

import com.ruoyi.lostfound.domain.LfAuditEvent;

/**
 * 业务审计数据层。
 */
public interface LfAuditMapper
{
    int insertAuditEvent(LfAuditEvent event);
}
