package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfViolationAction;
import com.ruoyi.lostfound.domain.LfViolationCase;

/**
 * 违规案件和处罚数据层
 */
public interface LfViolationMapper
{
    LfViolationAction selectActiveAction(@Param("userId") Long userId, @Param("actionType") String actionType);

    LfViolationCase selectCaseById(Long caseId);

    List<LfViolationCase> selectCaseList(LfViolationCase violationCase);

    LfViolationAction selectActionById(Long actionId);

    List<LfViolationAction> selectActionList(LfViolationAction action);

    List<LfViolationAction> selectActionListByUser(Long userId);

    int insertCase(LfViolationCase violationCase);

    int insertAction(LfViolationAction action);

    int expireAction(LfViolationAction action);

    int updateCase(LfViolationCase violationCase);
}
