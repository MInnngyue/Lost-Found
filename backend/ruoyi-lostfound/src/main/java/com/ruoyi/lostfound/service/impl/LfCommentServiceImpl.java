package com.ruoyi.lostfound.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.lostfound.domain.LfComment;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.mapper.LfCommentMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.service.ILfCommentService;
import com.ruoyi.lostfound.service.IRestrictionService;

/**
 * 评论服务实现
 */
@Service
public class LfCommentServiceImpl implements ILfCommentService
{
    @Autowired
    private LfCommentMapper commentMapper;

    @Autowired
    private LfPostMapper postMapper;

    @Autowired
    private IRestrictionService restrictionService;

    @Override
    public List<LfComment> selectVisibleComments(Long postId)
    {
        return commentMapper.selectVisibleComments(postId);
    }

    @Override
    @Transactional
    public LfComment addComment(LfComment comment, Long operatorId)
    {
        if (restrictionService.isRestricted(operatorId, "COMMENT"))
        {
            throw new ServiceException("当前账号暂时不能发表评论");
        }
        if (comment == null || comment.getPostId() == null || StringUtils.isBlank(comment.getContent()))
        {
            throw new ServiceException("评论内容不能为空");
        }
        String content = comment.getContent().trim();
        if (content.length() > 1000)
        {
            throw new ServiceException("评论不能超过1000个字符");
        }
        LfPost post = postMapper.selectPostById(comment.getPostId());
        if (post == null || !"0".equals(post.getDelFlag())
                || !"PUBLISHED".equals(post.getLifecycleStatus())
                || !"APPROVED".equals(post.getReviewStatus()))
        {
            throw new ServiceException("帖子不存在或暂不允许评论");
        }
        if (comment.getParentId() == null)
        {
            comment.setParentId(0L);
        }
        if (comment.getParentId() > 0)
        {
            throw new ServiceException("暂不支持回复已删除或未公开评论");
        }
        if (commentMapper.countRecentDuplicate(comment.getPostId(), operatorId, content) > 0)
        {
            throw new ServiceException("请勿重复发送相同评论");
        }
        comment.setUserId(operatorId);
        comment.setContent(content);
        comment.setContentStatus("VISIBLE");
        commentMapper.insertComment(comment);
        return comment;
    }
}
