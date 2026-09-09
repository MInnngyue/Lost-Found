package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfComment;

/**
 * 评论服务
 */
public interface ILfCommentService
{
    List<LfComment> selectVisibleComments(Long postId);

    LfComment addComment(LfComment comment, Long operatorId);
}
