package com.ruoyi.lostfound.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.lostfound.mapper.LfViolationMapper;
import com.ruoyi.lostfound.service.IRestrictionService;

/**
 * 处罚限制查询实现
 */
@Service
public class RestrictionServiceImpl implements IRestrictionService
{
    @Autowired
    private LfViolationMapper violationMapper;

    @Override
    public boolean isRestricted(Long userId, String actionType)
    {
        return userId != null && violationMapper.selectActiveAction(userId, actionType) != null;
    }
}
