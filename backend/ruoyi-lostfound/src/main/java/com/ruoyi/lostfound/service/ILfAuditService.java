package com.ruoyi.lostfound.service;

/**
 * 业务审计服务。
 */
public interface ILfAuditService
{
    void record(Long actorId, String actorType, String eventType, String targetType,
            Long targetId, String beforeData, String afterData);
}
