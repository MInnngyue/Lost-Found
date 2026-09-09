package com.ruoyi.lostfound.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.lostfound.domain.LfAppeal;
import com.ruoyi.lostfound.domain.LfComment;
import com.ruoyi.lostfound.domain.LfClaim;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPrivateMessage;
import com.ruoyi.lostfound.domain.LfReport;
import com.ruoyi.lostfound.domain.LfViolationAction;
import com.ruoyi.lostfound.domain.LfViolationCase;
import com.ruoyi.lostfound.mapper.LfAppealMapper;
import com.ruoyi.lostfound.mapper.LfClaimMapper;
import com.ruoyi.lostfound.mapper.LfCommentMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.mapper.LfPrivateMessageMapper;
import com.ruoyi.lostfound.mapper.LfReportMapper;
import com.ruoyi.lostfound.mapper.LfViolationMapper;
import com.ruoyi.lostfound.service.ILfMessageService;
import com.ruoyi.lostfound.service.ILfAuditService;
import com.ruoyi.lostfound.service.ILfModerationService;

/**
 * 举报、案件和申诉实现。
 */
@Service
public class LfModerationServiceImpl implements ILfModerationService
{
    private static final String[] TARGET_TYPES = { "POST", "COMMENT", "USER", "CLAIM", "PRIVATE_MESSAGE" };
    private static final String[] REASONS = { "SPAM", "FRAUD", "ABUSE", "ILLEGAL_CONTENT", "PRIVACY", "OTHER" };
    private static final String[] ACTION_TYPES = { "POST_PUBLISH", "COMMENT", "CLAIM_APPLY", "PRIVATE_MESSAGE" };
    private static final String[] SEVERITIES = { "LOW", "MEDIUM", "HIGH", "CRITICAL" };
    private static final String[] CASE_RESOLUTION_STATUSES = { "RESOLVED", "DISMISSED" };

    @Autowired
    private LfReportMapper reportMapper;

    @Autowired
    private LfViolationMapper violationMapper;

    @Autowired
    private LfAppealMapper appealMapper;

    @Autowired
    private LfClaimMapper claimMapper;

    @Autowired
    private LfPostMapper postMapper;

    @Autowired
    private LfCommentMapper commentMapper;

    @Autowired
    private LfPrivateMessageMapper privateMessageMapper;

    @Autowired
    private ILfMessageService messageService;

    @Autowired(required = false)
    private ILfAuditService auditService;

    @Override
    @Transactional
    public LfReport createReport(LfReport report, Long reporterId)
    {
        if (report == null || report.getTargetId() == null
                || !contains(TARGET_TYPES, report.getTargetType())
                || !contains(REASONS, report.getReasonCode()))
        {
            throw new ServiceException("举报对象或举报原因不合法");
        }
        if (StringUtils.isBlank(report.getDescription()) || report.getDescription().length() > 2000)
        {
            throw new ServiceException("请填写举报说明，且不能超过2000个字符");
        }
        Long subjectUserId = resolveSubjectUser(report.getTargetType(), report.getTargetId(), reporterId);
        if (subjectUserId == null || subjectUserId.equals(reporterId))
        {
            throw new ServiceException("不能举报自己或不存在的对象");
        }
        report.setSubjectUserId(subjectUserId);
        if (reportMapper.countDuplicate(reporterId, report.getTargetType(), report.getTargetId()) > 0)
        {
            throw new ServiceException("你已经提交过该对象的待处理举报");
        }
        report.setReporterId(reporterId);
        report.setDescription(report.getDescription().trim());
        reportMapper.insertReport(report);
        audit(reporterId, "REPORT_CREATE", report.getReportId(), null,
                "{\"targetType\":\"" + report.getTargetType() + "\",\"targetId\":" + report.getTargetId() + "}");
        return report;
    }

    @Override
    public List<LfReport> selectReportList(LfReport query)
    {
        return reportMapper.selectReportList(query);
    }

    @Override
    public List<LfViolationCase> selectCaseList(LfViolationCase query)
    {
        return violationMapper.selectCaseList(query);
    }

    @Override
    public List<LfViolationAction> selectActionList(LfViolationAction query)
    {
        return violationMapper.selectActionList(query);
    }

    @Override
    public List<LfViolationAction> selectMyActions(Long userId)
    {
        return violationMapper.selectActionListByUser(userId);
    }

    @Override
    public List<LfAppeal> selectAppealList(LfAppeal query)
    {
        return appealMapper.selectAppealList(query);
    }

    @Override
    public List<LfAppeal> selectMyAppeals(Long userId)
    {
        return appealMapper.selectAppealListByUser(userId);
    }

    @Override
    @Transactional
    public int handleReport(Long reportId, LfViolationCase decision, Long handlerId)
    {
        LfReport report = reportMapper.selectReportById(reportId);
        if (report == null || !"PENDING".equals(report.getStatus()))
        {
            throw new ServiceException("举报不存在或已处理");
        }
        if (decision == null || !"REJECTED".equals(decision.getStatus())
                && !"ACCEPTED".equals(decision.getStatus()))
        {
            throw new ServiceException("举报处理结果不合法");
        }
        report.setHandlerId(handlerId);
        String result = StringUtils.trim(decision.getDescription());
        if (StringUtils.isBlank(result) || result.length() > 2000)
        {
            throw new ServiceException("处理举报必须填写结果，且不能超过2000个字符");
        }
        report.setHandleResult(result);
        if ("REJECTED".equals(decision.getStatus()))
        {
            report.setStatus("REJECTED");
            int rows = updateReport(report);
            audit(handlerId, "REPORT_REJECT", reportId, "{\"status\":\"PENDING\"}",
                    "{\"status\":\"REJECTED\"}");
            return rows;
        }
        Long subjectUserId = resolveSubjectUser(report.getTargetType(), report.getTargetId(),
                report.getReporterId());
        if (subjectUserId == null)
        {
            throw new ServiceException("举报对象已不存在");
        }
        if (!contains(REASONS, decision.getViolationType())
                || !contains(SEVERITIES, decision.getSeverity()))
        {
            throw new ServiceException("受理举报必须填写违规类型和严重程度");
        }
        if (StringUtils.isBlank(decision.getDescription()) || decision.getDescription().length() > 2000)
        {
            throw new ServiceException("案件说明不能为空且不能超过2000个字符");
        }
        decision.setDescription(decision.getDescription().trim());
        decision.setSubjectUserId(subjectUserId);
        decision.setSourceReportId(reportId);
        decision.setHandlerId(handlerId);
        decision.setStatus("OPEN");
        if (StringUtils.isNotBlank(decision.getActionType()))
        {
            if (!contains(ACTION_TYPES, decision.getActionType())
                    || StringUtils.isBlank(decision.getActionReason()))
            {
                throw new ServiceException("处罚动作或处罚理由不合法");
            }
            if (decision.getActionReason().trim().length() > 2000)
            {
                throw new ServiceException("处罚理由不能超过2000个字符");
            }
            if (decision.getActionEndTime() != null
                    && decision.getActionEndTime().before(new java.util.Date()))
            {
                throw new ServiceException("处罚结束时间不能早于当前时间");
            }
        }
        violationMapper.insertCase(decision);
        if (StringUtils.isNotBlank(decision.getActionType()))
        {
            LfViolationAction action = new LfViolationAction();
            action.setCaseId(decision.getCaseId());
            action.setUserId(subjectUserId);
            action.setActionType(decision.getActionType());
            action.setScope("ALL");
            action.setEndTime(decision.getActionEndTime());
            action.setReason(decision.getActionReason());
            action.setOperatorId(handlerId);
            violationMapper.insertAction(action);
        }
        report.setStatus("ACCEPTED");
        report.setCaseId(decision.getCaseId());
        int rows = updateReport(report);
        audit(handlerId, "REPORT_ACCEPT", reportId, "{\"status\":\"PENDING\"}",
                "{\"status\":\"ACCEPTED\",\"caseId\":" + decision.getCaseId() + "}");
        notify(report.getReporterId(), "举报已受理", "你的举报已受理，平台已建立违规案件。", reportId);
        return rows;
    }

    @Override
    @Transactional
    public int submitAppeal(LfAppeal appeal, Long appellantId)
    {
        if (appeal == null || appeal.getActionId() == null || StringUtils.isBlank(appeal.getReason())
                || appeal.getReason().length() > 3000)
        {
            throw new ServiceException("申诉参数不完整或超出长度限制");
        }
        LfViolationAction action = violationMapper.selectActionById(appeal.getActionId());
        if (action == null || !appellantId.equals(action.getUserId()))
        {
            throw new ServiceException("处罚动作不存在或无权申诉");
        }
        if (appealMapper.countOpenAppeal(appeal.getActionId(), appellantId) > 0)
        {
            throw new ServiceException("该处罚已有待处理申诉");
        }
        appeal.setCaseId(action.getCaseId());
        appeal.setAppellantId(appellantId);
        appeal.setReason(appeal.getReason().trim());
        appeal.setStatus("SUBMITTED");
        int rows = appealMapper.insertAppeal(appeal);
        audit(appellantId, "APPEAL_CREATE", appeal.getAppealId(), null, "{\"status\":\"SUBMITTED\"}");
        return rows;
    }

    @Override
    @Transactional
    public int reviewAppeal(Long appealId, String status, String result, Long reviewerId)
    {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)
                || StringUtils.isBlank(result) || result.trim().length() > 2000)
        {
            throw new ServiceException("申诉处理参数不合法");
        }
        LfAppeal appeal = appealMapper.selectAppealById(appealId);
        if (appeal == null || !"SUBMITTED".equals(appeal.getStatus()))
        {
            throw new ServiceException("申诉不存在或已处理");
        }
        appeal.setStatus(status);
        appeal.setReviewResult(result.trim());
        appeal.setReviewerId(reviewerId);
        int rows = appealMapper.updateAppeal(appeal);
        if (rows != 1)
        {
            throw new ServiceException("申诉状态已被其他管理员处理，请刷新后重试");
        }
        if ("APPROVED".equals(status))
        {
            LfViolationAction action = violationMapper.selectActionById(appeal.getActionId());
            if (action != null)
            {
                action.setEndTime(new java.util.Date());
                violationMapper.expireAction(action);
            }
            notify(appeal.getAppellantId(), "申诉已通过", "平台已通过你的申诉，相关处罚已终止。", appealId);
        }
        audit(reviewerId, "APPEAL_" + status, appealId,
                "{\"status\":\"SUBMITTED\"}", "{\"status\":\"" + status + "\"}");
        return rows;
    }

    @Override
    @Transactional
    public int resolveCase(Long caseId, String status, String result, Long handlerId)
    {
        if (!contains(CASE_RESOLUTION_STATUSES, status)
                || StringUtils.isBlank(result) || result.trim().length() > 2000)
        {
            throw new ServiceException("案件处理参数不合法");
        }
        LfViolationCase current = violationMapper.selectCaseById(caseId);
        if (current == null || !"OPEN".equals(current.getStatus()))
        {
            throw new ServiceException("案件不存在或已处理");
        }
        current.setStatus(status);
        current.setDescription(result.trim());
        current.setHandlerId(handlerId);
        int rows = violationMapper.updateCase(current);
        if (rows != 1)
        {
            throw new ServiceException("案件状态已被其他管理员处理，请刷新后重试");
        }
        audit(handlerId, "CASE_" + status, caseId,
                "{\"status\":\"OPEN\"}", "{\"status\":\"" + status + "\"}");
        return rows;
    }

    private int updateReport(LfReport report)
    {
        int rows = reportMapper.updateReport(report);
        if (rows != 1)
        {
            throw new ServiceException("举报状态已被其他管理员处理，请刷新后重试");
        }
        return rows;
    }

    private Long resolveSubjectUser(String targetType, Long targetId, Long reporterId)
    {
        if ("USER".equals(targetType))
        {
            return targetId;
        }
        if ("POST".equals(targetType))
        {
            LfPost post = postMapper.selectPostById(targetId);
            return post == null ? null : post.getPublisherId();
        }
        if ("COMMENT".equals(targetType))
        {
            LfComment comment = commentMapper.selectCommentById(targetId);
            return comment == null ? null : comment.getUserId();
        }
        if ("CLAIM".equals(targetType))
        {
            LfClaim claim = claimMapper.selectClaimById(targetId);
            if (claim == null || reporterId == null)
            {
                return null;
            }
            if (reporterId.equals(claim.getApplicantId()))
            {
                return claim.getPostOwnerId();
            }
            if (reporterId.equals(claim.getPostOwnerId()))
            {
                return claim.getApplicantId();
            }
            return null;
        }
        if ("PRIVATE_MESSAGE".equals(targetType))
        {
            LfPrivateMessage message = privateMessageMapper.selectMessageById(targetId);
            if (message == null || reporterId == null || !reporterId.equals(message.getToUserId()))
            {
                return null;
            }
            return message.getFromUserId();
        }
        return null;
    }

    private boolean contains(String[] values, String value)
    {
        for (String item : values)
        {
            if (item.equals(value))
            {
                return true;
            }
        }
        return false;
    }

    private void notify(Long userId, String title, String content, Long relatedId)
    {
        LfMessage message = new LfMessage();
        message.setToUserId(userId);
        message.setMessageType("MODERATION");
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedType("REPORT");
        message.setRelatedId(relatedId);
        messageService.send(message);
    }

    private void audit(Long actorId, String eventType, Long targetId, String beforeData, String afterData)
    {
        if (auditService != null)
        {
            auditService.record(actorId, "USER", eventType, "MODERATION", targetId, beforeData, afterData);
        }
    }
}
