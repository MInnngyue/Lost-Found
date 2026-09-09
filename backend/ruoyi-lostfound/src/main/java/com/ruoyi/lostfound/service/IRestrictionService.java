package com.ruoyi.lostfound.service;

/**
 * 用户处罚限制查询
 */
public interface IRestrictionService
{
    boolean isRestricted(Long userId, String actionType);
}
