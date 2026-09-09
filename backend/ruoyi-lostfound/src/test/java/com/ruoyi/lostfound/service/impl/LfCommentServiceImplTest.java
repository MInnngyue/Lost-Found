package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfComment;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.mapper.LfCommentMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.service.IRestrictionService;

@ExtendWith(MockitoExtension.class)
class LfCommentServiceImplTest
{
    @Mock
    private LfCommentMapper commentMapper;

    @Mock
    private LfPostMapper postMapper;

    @Mock
    private IRestrictionService restrictionService;

    @InjectMocks
    private LfCommentServiceImpl commentService;

    @Test
    void commentShouldRejectUnpublishedPost()
    {
        LfPost post = new LfPost();
        post.setLifecycleStatus("PENDING_REVIEW");
        post.setReviewStatus("PENDING");
        post.setDelFlag("0");
        when(postMapper.selectPostById(7L)).thenReturn(post);
        LfComment comment = comment(7L, "hello");

        assertThrows(ServiceException.class, () -> commentService.addComment(comment, 20L));
        verify(commentMapper, never()).insertComment(any());
    }

    @Test
    void commentShouldRejectRecentDuplicate()
    {
        when(postMapper.selectPostById(7L)).thenReturn(publicPost());
        when(commentMapper.countRecentDuplicate(7L, 20L, "hello")).thenReturn(1);

        assertThrows(ServiceException.class, () -> commentService.addComment(comment(7L, "hello"), 20L));
        verify(commentMapper, never()).insertComment(any());
    }

    private LfComment comment(Long postId, String content)
    {
        LfComment comment = new LfComment();
        comment.setPostId(postId);
        comment.setContent(content);
        return comment;
    }

    private LfPost publicPost()
    {
        LfPost post = new LfPost();
        post.setLifecycleStatus("PUBLISHED");
        post.setReviewStatus("APPROVED");
        post.setDelFlag("0");
        return post;
    }
}
