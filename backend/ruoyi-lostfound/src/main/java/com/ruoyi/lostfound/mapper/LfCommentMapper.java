package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfComment;

/**
 * 评论数据层
 */
public interface LfCommentMapper
{
    List<LfComment> selectVisibleComments(Long postId);

    LfComment selectCommentById(Long commentId);

    int countRecentDuplicate(@Param("postId") Long postId, @Param("userId") Long userId,
            @Param("content") String content);

    int insertComment(LfComment comment);
}
