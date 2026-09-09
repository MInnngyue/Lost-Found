package com.ruoyi.lostfound.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.lostfound.domain.LfAppeal;
import com.ruoyi.lostfound.domain.LfReport;
import com.ruoyi.lostfound.domain.LfViolationCase;
import com.ruoyi.lostfound.domain.LfViolationAction;
import com.ruoyi.lostfound.service.ILfModerationService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 举报、处罚和申诉接口
 */
@RestController
@RequestMapping("/lostfound/moderation")
public class LfModerationController extends BaseController
{
    @Autowired
    private ILfModerationService moderationService;

    @PreAuthorize("@ss.hasPermi('lostfound:report:add')")
    @Log(title = "提交举报", businessType = BusinessType.INSERT)
    @RateLimiter(key = "lostfound:report:add", time = 300, count = 5,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PostMapping("/report")
    public AjaxResult report(@RequestBody LfReport report)
    {
        return success(moderationService.createReport(report, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:report:list')")
    @GetMapping("/report/manage/list")
    public TableDataInfo reportList(LfReport query)
    {
        startPage();
        return getDataTable(moderationService.selectReportList(query));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:case:list')")
    @GetMapping("/case/manage/list")
    public TableDataInfo caseList(LfViolationCase query)
    {
        startPage();
        return getDataTable(moderationService.selectCaseList(query));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:action:list')")
    @GetMapping("/action/manage/list")
    public TableDataInfo actionList(LfViolationAction query)
    {
        startPage();
        return getDataTable(moderationService.selectActionList(query));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:appeal:list')")
    @GetMapping("/appeal/manage/list")
    public TableDataInfo appealList(LfAppeal query)
    {
        startPage();
        return getDataTable(moderationService.selectAppealList(query));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:action:query')")
    @GetMapping("/action/my")
    public AjaxResult myActions()
    {
        return success(moderationService.selectMyActions(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:appeal:query')")
    @GetMapping("/appeal/my")
    public AjaxResult myAppeals()
    {
        return success(moderationService.selectMyAppeals(getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:report:handle')")
    @Log(title = "处理举报", businessType = BusinessType.UPDATE)
    @PutMapping("/report/{reportId}/handle")
    public AjaxResult handleReport(@PathVariable Long reportId, @RequestBody LfViolationCase decision)
    {
        return toAjax(moderationService.handleReport(reportId, decision, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:case:handle')")
    @Log(title = "处理违规案件", businessType = BusinessType.UPDATE)
    @PutMapping("/case/{caseId}/resolve")
    public AjaxResult resolveCase(@PathVariable Long caseId, @RequestBody LfViolationCase decision)
    {
        return toAjax(moderationService.resolveCase(caseId, decision.getStatus(),
                decision.getDescription(), getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:appeal:add')")
    @Log(title = "提交申诉", businessType = BusinessType.INSERT)
    @PostMapping("/appeal")
    public AjaxResult appeal(@RequestBody LfAppeal appeal)
    {
        return toAjax(moderationService.submitAppeal(appeal, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:appeal:handle')")
    @Log(title = "处理申诉", businessType = BusinessType.UPDATE)
    @PutMapping("/appeal/{appealId}/review")
    public AjaxResult reviewAppeal(@PathVariable Long appealId, @RequestBody LfAppeal appeal)
    {
        return toAjax(moderationService.reviewAppeal(appealId, appeal.getStatus(),
                appeal.getReviewResult(), getUserId()));
    }
}
