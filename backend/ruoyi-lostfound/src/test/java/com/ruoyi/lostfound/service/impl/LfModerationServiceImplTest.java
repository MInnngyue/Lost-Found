package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfAppeal;
import com.ruoyi.lostfound.domain.LfClaim;
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

@ExtendWith(MockitoExtension.class)
class LfModerationServiceImplTest
{
    @Mock
    private LfReportMapper reportMapper;

    @Mock
    private LfViolationMapper violationMapper;

    @Mock
    private LfAppealMapper appealMapper;

    @Mock
    private LfClaimMapper claimMapper;

    @Mock
    private LfPostMapper postMapper;

    @Mock
    private LfCommentMapper commentMapper;

    @Mock
    private LfPrivateMessageMapper privateMessageMapper;

    @Mock
    private ILfMessageService messageService;

    @InjectMocks
    private LfModerationServiceImpl moderationService;

    @Test
    void reportShouldRejectUnsupportedReason()
    {
        LfReport report = report("POST", 7L, "NOT_ALLOWED", "spam");

        assertThrows(ServiceException.class, () -> moderationService.createReport(report, 20L));
        verify(reportMapper, never()).insertReport(any());
    }

    @Test
    void acceptedReportShouldRejectExpiredAction()
    {
        when(reportMapper.selectReportById(11L)).thenReturn(pendingReport(11L));
        when(postMapper.selectPostById(7L)).thenReturn(post(7L, 10L));

        LfViolationCase decision = new LfViolationCase();
        decision.setStatus("ACCEPTED");
        decision.setViolationType("SPAM");
        decision.setSeverity("MEDIUM");
        decision.setDescription("确认存在重复发布");
        decision.setActionType("COMMENT");
        decision.setActionReason("暂时禁止评论");
        decision.setActionEndTime(new Date(System.currentTimeMillis() - 1000));

        assertThrows(ServiceException.class, () -> moderationService.handleReport(11L, decision, 1L));
        verify(violationMapper, never()).insertCase(any());
    }

    @Test
    void appealShouldRequireActionOwnedByAppellant()
    {
        LfAppeal appeal = new LfAppeal();
        appeal.setActionId(5L);
        appeal.setReason("处罚事实有误");
        LfViolationAction action = new LfViolationAction();
        action.setUserId(99L);
        when(violationMapper.selectActionById(5L)).thenReturn(action);

        assertThrows(ServiceException.class, () -> moderationService.submitAppeal(appeal, 20L));
        verify(appealMapper, never()).insertAppeal(any());
    }

    @Test
    void caseResolutionShouldRejectUnknownStatus()
    {
        assertThrows(ServiceException.class,
                () -> moderationService.resolveCase(3L, "INVALID", "处理结果", 1L));
        verify(violationMapper, never()).selectCaseById(3L);
    }

    @Test
    void caseResolutionShouldNotReopenAnOpenCase()
    {
        assertThrows(ServiceException.class,
                () -> moderationService.resolveCase(3L, "OPEN", "处理结果", 1L));
        verify(violationMapper, never()).selectCaseById(3L);
    }

    @Test
    void claimReportShouldResolveOtherParticipant()
    {
        LfReport report = report("CLAIM", 8L, "ABUSE", "认领过程存在骚扰");
        LfClaim claim = new LfClaim();
        claim.setClaimId(8L);
        claim.setApplicantId(20L);
        claim.setPostOwnerId(30L);
        when(claimMapper.selectClaimById(8L)).thenReturn(claim);

        moderationService.createReport(report, 20L);

        assertEquals(30L, report.getSubjectUserId());
        verify(reportMapper).insertReport(report);
    }

    @Test
    void privateMessageReportShouldOnlyBeAllowedForRecipient()
    {
        LfReport report = report("PRIVATE_MESSAGE", 9L, "ABUSE", "私信内容存在骚扰");
        LfPrivateMessage message = new LfPrivateMessage();
        message.setMessageId(9L);
        message.setFromUserId(30L);
        message.setToUserId(20L);
        when(privateMessageMapper.selectMessageById(9L)).thenReturn(message);

        moderationService.createReport(report, 20L);

        assertEquals(30L, report.getSubjectUserId());
        verify(reportMapper).insertReport(report);
    }

    @Test
    void privateMessageReportShouldRejectSender()
    {
        LfReport report = report("PRIVATE_MESSAGE", 9L, "ABUSE", "不应允许发送者举报自己的消息");
        LfPrivateMessage message = new LfPrivateMessage();
        message.setMessageId(9L);
        message.setFromUserId(30L);
        message.setToUserId(20L);
        when(privateMessageMapper.selectMessageById(9L)).thenReturn(message);

        assertThrows(ServiceException.class, () -> moderationService.createReport(report, 30L));
        verify(reportMapper, never()).insertReport(any());
    }

    private LfReport report(String targetType, Long targetId, String reason, String description)
    {
        LfReport report = new LfReport();
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReasonCode(reason);
        report.setDescription(description);
        return report;
    }

    private LfReport pendingReport(Long reportId)
    {
        LfReport report = report("POST", 7L, "SPAM", "重复内容");
        report.setReportId(reportId);
        report.setReporterId(20L);
        report.setStatus("PENDING");
        return report;
    }

    private LfPost post(Long postId, Long publisherId)
    {
        LfPost post = new LfPost();
        post.setPostId(postId);
        post.setPublisherId(publisherId);
        return post;
    }
}
