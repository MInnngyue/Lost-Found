package com.ruoyi.lostfound.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.lostfound.domain.LfClaim;
import com.ruoyi.lostfound.domain.LfClaimEvent;
import com.ruoyi.lostfound.domain.LfMatch;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostEvent;
import com.ruoyi.lostfound.mapper.LfClaimMapper;
import com.ruoyi.lostfound.mapper.LfMatchMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.service.ILfMessageService;
import com.ruoyi.lostfound.service.ILfAuditService;
import com.ruoyi.lostfound.service.ILfClaimService;
import com.ruoyi.lostfound.service.IRestrictionService;

/**
 * 认领状态机服务。
 */
@Service
public class LfClaimServiceImpl implements ILfClaimService
{
    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";
    private static final String HANDOFF = "HANDOFF";
    private static final String COMPLETED = "COMPLETED";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String RESOLVED = "RESOLVED";
    private static final String CANCELLED = "CANCELLED";
    private static final String REJECTED = "REJECTED";

    @Autowired
    private LfClaimMapper claimMapper;

    @Autowired
    private LfPostMapper postMapper;

    @Autowired
    private LfMatchMapper matchMapper;

    @Autowired
    private ILfMessageService messageService;

    @Autowired
    private IRestrictionService restrictionService;

    @Autowired(required = false)
    private ILfAuditService auditService;

    @Override
    @Transactional
    public LfClaim createClaim(LfClaim request, Long operatorId, String operatorName)
    {
        if (restrictionService.isRestricted(operatorId, "CLAIM_APPLY"))
        {
            throw new ServiceException("当前账号暂时不能提交认领申请");
        }
        if (request == null || request.getPostId() == null)
        {
            throw new ServiceException("帖子不能为空");
        }
        if (StringUtils.isBlank(request.getEvidenceText()))
        {
            throw new ServiceException("请填写认领依据");
        }
        if (request.getEvidenceText().length() > 2000)
        {
            throw new ServiceException("认领依据不能超过2000个字符");
        }
        LfPost post = postMapper.selectPostById(request.getPostId());
        if (post == null || !"0".equals(post.getDelFlag())
                || !"PUBLISHED".equals(post.getLifecycleStatus())
                || !"APPROVED".equals(post.getReviewStatus()))
        {
            throw new ServiceException("帖子不存在或当前不接受认领");
        }
        if (operatorId == null || operatorId.equals(post.getPublisherId()))
        {
            throw new ServiceException("不能认领自己发布的帖子");
        }
        if (claimMapper.selectActiveClaim(request.getPostId(), operatorId) != null)
        {
            throw new ServiceException("你已经提交过该帖子的有效认领申请");
        }
        if (request.getMatchId() != null)
        {
            LfMatch match = matchMapper.selectMatchById(request.getMatchId());
            if (match == null || !request.getPostId().equals(match.getLostPostId())
                    && !request.getPostId().equals(match.getFoundPostId())
                    || !"SUGGESTED".equals(match.getStatus())
                    && !"VIEWED".equals(match.getStatus())
                    && !"CONFIRMED".equals(match.getStatus()))
            {
                throw new ServiceException("匹配记录不存在或已失效");
            }
        }

        LfClaim claim = new LfClaim();
        claim.setPostId(post.getPostId());
        claim.setApplicantId(operatorId);
        claim.setPostOwnerId(post.getPublisherId());
        claim.setMatchId(request.getMatchId());
        claim.setEvidenceText(request.getEvidenceText().trim());
        claim.setCreateBy(operatorName);
        claim.setUpdateBy(operatorName);
        claimMapper.insertClaim(claim);
        recordEvent(claim.getClaimId(), null, PENDING, operatorId, "CREATE", null);
        audit(operatorId, "CLAIM_CREATE", claim.getClaimId(), null, "{\"status\":\"PENDING\"}");
        notify(post.getPublisherId(), "认领申请", "有人提交了你的帖子认领申请，请查看认领依据。",
                claim.getClaimId());
        return claim;
    }

    @Override
    public List<LfClaim> selectMyClaims(Long operatorId)
    {
        return claimMapper.selectClaimListForUser(operatorId);
    }

    @Override
    public List<LfClaim> selectClaimsByPost(Long postId, Long operatorId)
    {
        LfPost post = postMapper.selectPostById(postId);
        if (post == null || !operatorId.equals(post.getPublisherId()))
        {
            throw new ServiceException("只有帖子发布者可以查看认领申请");
        }
        return claimMapper.selectClaimListByPost(postId);
    }

    @Override
    public List<LfClaim> selectManageClaims(LfClaim query)
    {
        return claimMapper.selectManageClaimList(query);
    }

    @Override
    @Transactional
    public int confirmClaim(Long claimId, Long operatorId, String operatorName)
    {
        LfClaim current = requireParticipant(claimId, operatorId);
        ensureConfirmable(current);
        String before = current.getStatus();
        boolean isApplicant = operatorId.equals(current.getApplicantId());
        if (isApplicant && "1".equals(current.getApplicantConfirmed())
                || !isApplicant && "1".equals(current.getOwnerConfirmed()))
        {
            throw new ServiceException("你已经确认过该认领");
        }
        if (PENDING.equals(current.getStatus()))
        {
            if (isApplicant)
            {
                throw new ServiceException("请等待帖子发布者接受认领申请");
            }
            current.setStatus(ACCEPTED);
            current.setAcceptedTime(new Date());
        }
        else if (ACCEPTED.equals(current.getStatus()))
        {
            if (!isApplicant)
            {
                throw new ServiceException("请等待认领人确认交接");
            }
            current.setApplicantConfirmed("1");
            current.setStatus(HANDOFF);
        }
        else if (HANDOFF.equals(current.getStatus()))
        {
            if (isApplicant)
            {
                throw new ServiceException("认领人已经确认交接，请等待帖子发布者完成确认");
            }
            current.setOwnerConfirmed("1");
            current.setStatus(COMPLETED);
            current.setCompletedTime(new Date());
        }
        else
        {
            throw new ServiceException("当前认领状态不允许确认");
        }
        current.setUpdateBy(operatorName);
        int rows = claimMapper.updateClaim(current);
        if (rows == 0)
        {
            throw new ServiceException("认领状态已被其他操作修改，请刷新后重试");
        }
        if (COMPLETED.equals(current.getStatus()))
        {
            resolvePost(current, operatorName, operatorId);
        }
        recordEvent(claimId, before, current.getStatus(), operatorId, "CONFIRM", null);
        audit(operatorId, "CLAIM_" + current.getStatus(), claimId,
                "{\"status\":\"" + before + "\"}", "{\"status\":\"" + current.getStatus() + "\"}");
        notify(otherParty(current, operatorId), "认领状态更新",
                COMPLETED.equals(current.getStatus()) ? "双方已确认，认领流程已完成。" : "对方已更新认领状态，请登录查看下一步操作。",
                claimId);
        return rows;
    }

    private void resolvePost(LfClaim claim, String operatorName, Long operatorId)
    {
        LfPost post = postMapper.selectPostById(claim.getPostId());
        if (post == null || !"0".equals(post.getDelFlag()))
        {
            throw new ServiceException("关联帖子不存在或已删除，认领无法完成");
        }
        if (RESOLVED.equals(post.getLifecycleStatus()))
        {
            return;
        }
        if (!PUBLISHED.equals(post.getLifecycleStatus())
                || !"APPROVED".equals(post.getReviewStatus()))
        {
            throw new ServiceException("关联帖子当前状态不允许完成认领");
        }
        LfPost update = new LfPost();
        update.setPostId(post.getPostId());
        update.setVersion(post.getVersion());
        update.setUpdateBy(operatorName);
        if (postMapper.resolvePost(update) == 0)
        {
            throw new ServiceException("帖子状态已被其他操作修改，请刷新后重试");
        }
        LfPostEvent event = new LfPostEvent();
        event.setPostId(post.getPostId());
        event.setEventType("RESOLVE");
        event.setFromStatus(PUBLISHED);
        event.setToStatus(RESOLVED);
        event.setOperatorId(operatorId);
        event.setSource("CLAIM");
        event.setReason("认领双方确认完成");
        postMapper.insertPostEvent(event);
        audit(operatorId, "POST_RESOLVE", post.getPostId(),
                "{\"lifecycle\":\"PUBLISHED\"}", "{\"lifecycle\":\"RESOLVED\"}");
    }

    @Override
    @Transactional
    public int cancelClaim(Long claimId, Long operatorId, String operatorName)
    {
        LfClaim current = requireParticipant(claimId, operatorId);
        ensureConfirmable(current);
        String before = current.getStatus();
        current.setStatus(CANCELLED);
        current.setUpdateBy(operatorName);
        int rows = claimMapper.updateClaim(current);
        if (rows == 0)
        {
            throw new ServiceException("认领状态已被其他操作修改，请刷新后重试");
        }
        recordEvent(claimId, before, CANCELLED, operatorId, "CANCEL", null);
        audit(operatorId, "CLAIM_CANCEL", claimId,
                "{\"status\":\"" + before + "\"}", "{\"status\":\"CANCELLED\"}");
        notify(otherParty(current, operatorId), "认领已取消", "对方取消了认领申请。", claimId);
        return rows;
    }

    @Override
    @Transactional
    public int rejectClaim(Long claimId, Long operatorId, String operatorName, String reason)
    {
        LfClaim current = requireParticipant(claimId, operatorId);
        if (!operatorId.equals(current.getPostOwnerId()))
        {
            throw new ServiceException("只有帖子发布者可以拒绝认领");
        }
        ensureConfirmable(current);
        if (StringUtils.isBlank(reason))
        {
            throw new ServiceException("拒绝认领必须填写理由");
        }
        String before = current.getStatus();
        current.setStatus(REJECTED);
        current.setOwnerRemark(reason.trim());
        current.setUpdateBy(operatorName);
        int rows = claimMapper.updateClaim(current);
        if (rows == 0)
        {
            throw new ServiceException("认领状态已被其他操作修改，请刷新后重试");
        }
        recordEvent(claimId, before, REJECTED, operatorId, "REJECT", reason.trim());
        audit(operatorId, "CLAIM_REJECT", claimId,
                "{\"status\":\"" + before + "\"}", "{\"status\":\"REJECTED\"}");
        notify(current.getApplicantId(), "认领申请未通过", "帖子发布者拒绝了你的认领申请：" + reason.trim(), claimId);
        return rows;
    }

    private LfClaim requireParticipant(Long claimId, Long operatorId)
    {
        LfClaim claim = claimMapper.selectClaimById(claimId);
        if (claim == null || !operatorId.equals(claim.getApplicantId())
                && !operatorId.equals(claim.getPostOwnerId()))
        {
            throw new ServiceException("认领记录不存在或无权操作");
        }
        return claim;
    }

    private void ensureConfirmable(LfClaim claim)
    {
        if (!PENDING.equals(claim.getStatus()) && !ACCEPTED.equals(claim.getStatus())
                && !HANDOFF.equals(claim.getStatus()))
        {
            throw new ServiceException("当前认领状态不允许此操作");
        }
    }

    private void recordEvent(Long claimId, String from, String to, Long operatorId,
            String eventType, String reason)
    {
        LfClaimEvent event = new LfClaimEvent();
        event.setClaimId(claimId);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setOperatorId(operatorId);
        event.setEventType(eventType);
        event.setReason(reason);
        claimMapper.insertClaimEvent(event);
    }

    private Long otherParty(LfClaim claim, Long operatorId)
    {
        return operatorId.equals(claim.getApplicantId()) ? claim.getPostOwnerId() : claim.getApplicantId();
    }

    private void notify(Long userId, String title, String content, Long claimId)
    {
        LfMessage message = new LfMessage();
        message.setToUserId(userId);
        message.setMessageType("CLAIM");
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedType("CLAIM");
        message.setRelatedId(claimId);
        messageService.send(message);
    }

    private void audit(Long actorId, String eventType, Long targetId, String beforeData, String afterData)
    {
        if (auditService != null)
        {
            auditService.record(actorId, "USER", eventType, "CLAIM", targetId, beforeData, afterData);
        }
    }
}
