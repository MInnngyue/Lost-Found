package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.mockito.Spy;
import org.springframework.mock.web.MockMultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostImage;
import com.ruoyi.lostfound.domain.LfPostReview;
import com.ruoyi.lostfound.domain.LfCategory;
import com.ruoyi.lostfound.mapper.LfCategoryMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.service.IMatchingService;
import com.ruoyi.lostfound.service.ILfMessageService;
import com.ruoyi.lostfound.service.IRestrictionService;

@ExtendWith(MockitoExtension.class)
class LfPostServiceImplTest
{
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @Spy
    private Validator validator = VALIDATOR_FACTORY.getValidator();

    @Mock
    private LfPostMapper postMapper;

    @Mock
    private LfCategoryMapper categoryMapper;

    @Mock
    private IMatchingService matchingService;

    @Mock
    private ILfMessageService messageService;

    @Mock
    private IRestrictionService restrictionService;

    @InjectMocks
    private LfPostServiceImpl postService;

    @AfterAll
    static void closeValidator()
    {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void insertPostShouldIgnoreClientOwnedState()
    {
        LfPost post = validPost();
        enabledCategories();
        post.setPublisherId(999L);
        post.setLifecycleStatus("PUBLISHED");
        post.setReviewStatus("APPROVED");
        when(postMapper.insertPost(post)).thenReturn(1);

        assertEquals(1, postService.insertPost(post, 10L));

        assertEquals(10L, post.getPublisherId());
        assertEquals("DRAFT", post.getLifecycleStatus());
        assertEquals("PENDING", post.getReviewStatus());
        verify(postMapper).insertPost(post);
    }

    @Test
    void insertPostShouldRejectMissingCategoryBeforeWriting()
    {
        LfPost post = validPost();
        assertThrows(ServiceException.class, () -> postService.insertPost(post, 10L));
        verify(postMapper, never()).insertPost(any());
        verify(postMapper, never()).insertPostEvent(any());
    }

    @Test
    void insertPostShouldRejectWrongCategoryType()
    {
        LfPost post = validPost();
        when(categoryMapper.selectCategoryById(1L)).thenReturn(category(1L, "COLOR", 0L));
        assertThrows(ServiceException.class, () -> postService.insertPost(post, 10L));
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    void insertPostShouldRejectDisabledCategory()
    {
        LfPost post = validPost();
        LfCategory category = category(1L, "ITEM", 0L);
        category.setStatus("1");
        when(categoryMapper.selectCategoryById(1L)).thenReturn(category);
        assertThrows(ServiceException.class, () -> postService.insertPost(post, 10L));
    }

    @Test
    void insertPostShouldRejectAreaFromAnotherCampus()
    {
        LfPost post = validPost();
        post.setAreaCategoryId(4L);
        enabledCategories();
        when(categoryMapper.selectCategoryById(4L)).thenReturn(category(4L, "AREA", 99L));
        assertThrows(ServiceException.class, () -> postService.insertPost(post, 10L));
        verify(postMapper, never()).insertPost(any());
    }

    @Test
    void submissionRevalidatesCategoriesDisabledAfterDraftWasSaved()
    {
        LfPost post = validPost();
        when(postMapper.selectPostById(1L)).thenReturn(post);
        assertThrows(ServiceException.class, () -> postService.submitPost(1L, 10L));
        verify(postMapper, never()).submitPost(any());
    }

    @Test
    void submissionRejectsLegacyDraftWithMissingDate()
    {
        LfPost post = validPost();
        post.setEventDate(null);
        when(postMapper.selectPostById(1L)).thenReturn(post);
        assertThrows(ServiceException.class, () -> postService.submitPost(1L, 10L));
        verify(postMapper, never()).submitPost(any());
        verify(postMapper, never()).insertPostEvent(any());
    }

    @Test
    void rejectionRequiresReasonAndDoesNotWriteOnInvalidInput()
    {
        LfPostReview review = new LfPostReview();
        review.setPostId(1L);
        review.setAction("REJECT");
        review.setReason("   ");
        assertThrows(ServiceException.class, () -> postService.reviewPost(review, 2L));
        verify(postMapper, never()).reviewPost(any());
    }

    private LfPost validPost()
    {
        LfPost post = post(1L, 10L, "DRAFT", 0);
        post.setPostType("LOST");
        post.setTitle("Test post");
        post.setEventDate(java.sql.Date.valueOf("2026-09-01"));
        post.setItemCategoryId(1L);
        post.setColorCategoryId(2L);
        post.setCampusCategoryId(3L);
        return post;
    }

    private void enabledCategories()
    {
        when(categoryMapper.selectCategoryById(1L)).thenReturn(category(1L, "ITEM", 0L));
        when(categoryMapper.selectCategoryById(2L)).thenReturn(category(2L, "COLOR", 0L));
        when(categoryMapper.selectCategoryById(3L)).thenReturn(category(3L, "CAMPUS", 0L));
    }

    private LfCategory category(Long id, String type, Long parentId)
    {
        LfCategory category = new LfCategory();
        category.setCategoryId(id);
        category.setCategoryType(type);
        category.setParentId(parentId);
        category.setStatus("0");
        category.setDelFlag("0");
        return category;
    }

    @Test
    void selectMyPostListShouldOverrideClientPublisherId()
    {
        LfPost query = new LfPost();
        query.setPublisherId(999L);
        when(postMapper.selectManagePostList(query)).thenReturn(java.util.List.of());

        assertEquals(0, postService.selectMyPostList(query, 10L).size());

        assertEquals(10L, query.getPublisherId());
        verify(postMapper).selectManagePostList(query);
    }

    @Test
    void expirePostsShouldUpdateOnlyPublishedPostsAndRecordEvent()
    {
        LfPost post = post(1L, 10L, "PUBLISHED", 3);
        post.setReviewStatus("APPROVED");
        when(postMapper.selectPostsToExpire()).thenReturn(java.util.List.of(post));
        when(postMapper.expirePost(any(LfPost.class))).thenReturn(1);

        assertEquals(1, postService.expirePosts("system", null));

        verify(postMapper).expirePost(any(LfPost.class));
        verify(postMapper).insertPostEvent(any());
    }

    @Test
    void updatePostShouldRejectAnotherUsersPost()
    {
        LfPost current = post(1L, 10L, "DRAFT", 0);
        LfPost update = new LfPost();
        update.setPostId(1L);
        when(postMapper.selectPostById(1L)).thenReturn(current);

        assertThrows(ServiceException.class, () -> postService.updatePost(update, 11L));
    }

    @Test
    void reviewShouldUseVersionAndRecordAuditHistory()
    {
        LfPost current = post(1L, 10L, "PENDING_REVIEW", 3);
        LfPostReview review = new LfPostReview();
        review.setPostId(1L);
        review.setAction("APPROVE");
        when(postMapper.selectPostById(1L)).thenReturn(current);
        when(postMapper.reviewPost(any(LfPost.class))).thenReturn(1);

        assertEquals(1, postService.reviewPost(review, 2L));

        verify(postMapper).reviewPost(any(LfPost.class));
        verify(postMapper).insertPostReview(review);
        verify(postMapper).insertPostEvent(any());
        assertEquals(2L, review.getReviewerId());
        assertEquals("PUBLISHED", review.getAfterStatus());
    }

    @Test
    void uploadShouldRejectNonImageContent()
    {
        LfPost post = post(1L, 10L, "DRAFT", 0);
        when(postMapper.selectPostById(1L)).thenReturn(post);
        when(postMapper.countPostImages(1L)).thenReturn(0);
        MockMultipartFile file = new MockMultipartFile("file", "payload.png", "image/png",
                "not an image".getBytes());

        assertThrows(ServiceException.class, () -> postService.uploadPostImage(1L, file, 10L, "user"));
        verify(postMapper, never()).insertPostImage(any());
    }

    @Test
    void uploadShouldRejectFilesLargerThanFiveMegabytes()
    {
        LfPost post = post(1L, 10L, "DRAFT", 0);
        when(postMapper.selectPostById(1L)).thenReturn(post);
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png",
                new byte[5 * 1024 * 1024 + 1]);

        assertThrows(ServiceException.class, () -> postService.uploadPostImage(1L, file, 10L, "user"));
        verify(postMapper, never()).countPostImages(1L);
    }

    @Test
    void uploadShouldRejectTheTenthImage()
    {
        LfPost post = post(1L, 10L, "DRAFT", 0);
        when(postMapper.selectPostById(1L)).thenReturn(post);
        when(postMapper.countPostImages(1L)).thenReturn(9);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png",
                "not read".getBytes());

        assertThrows(ServiceException.class, () -> postService.uploadPostImage(1L, file, 10L, "user"));
        verify(postMapper, never()).selectPostImageByHash(any(), any());
    }

    @Test
    void uploadShouldRejectPublishedPosts()
    {
        LfPost post = post(1L, 10L, "PUBLISHED", 0);
        when(postMapper.selectPostById(1L)).thenReturn(post);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png",
                "not read".getBytes());

        assertThrows(ServiceException.class, () -> postService.uploadPostImage(1L, file, 10L, "user"));
        verify(postMapper, never()).countPostImages(1L);
    }

    @Test
    void uploadShouldReturnExistingImageForDuplicateContent()
    {
        LfPost post = post(1L, 10L, "DRAFT", 0);
        LfPostImage existing = new LfPostImage();
        existing.setImageId(8L);
        existing.setPostId(1L);
        when(postMapper.selectPostById(1L)).thenReturn(post);
        when(postMapper.countPostImages(1L)).thenReturn(1);
        when(postMapper.selectPostImageByHash(any(), any())).thenReturn(existing);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", validPng());

        assertEquals(existing, postService.uploadPostImage(1L, file, 10L, "user"));
        verify(postMapper, never()).insertPostImage(any());
    }

    private byte[] validPng()
    {
        return Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    }

    private LfPost post(Long postId, Long publisherId, String lifecycleStatus, int version)
    {
        LfPost post = new LfPost();
        post.setPostId(postId);
        post.setPublisherId(publisherId);
        post.setLifecycleStatus(lifecycleStatus);
        post.setReviewStatus("PENDING");
        post.setVersion(version);
        post.setDelFlag("0");
        return post;
    }
}
