package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfAppeal;
import com.ruoyi.lostfound.domain.LfReport;
import com.ruoyi.lostfound.domain.LfViolationCase;

/**
 * 举报、违规案件和申诉服务
 */
public interface ILfModerationService
{
    LfReport createReport(LfReport report, Long reporterId);

    List<LfReport> selectReportList(LfReport query);

    List<LfViolationCase> selectCaseList(LfViolationCase query);

    List<com.ruoyi.lostfound.domain.LfViolationAction> selectActionList(
            com.ruoyi.lostfound.domain.LfViolationAction query);

    List<com.ruoyi.lostfound.domain.LfViolationAction> selectMyActions(Long userId);

    List<LfAppeal> selectAppealList(LfAppeal query);

    List<LfAppeal> selectMyAppeals(Long userId);

    int handleReport(Long reportId, LfViolationCase decision, Long handlerId);

    int resolveCase(Long caseId, String status, String result, Long handlerId);

    int submitAppeal(LfAppeal appeal, Long appellantId);

    int reviewAppeal(Long appealId, String status, String result, Long reviewerId);
}
