package com.ruoyi.lostfound.mapper;

import java.util.List;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostEvent;
import com.ruoyi.lostfound.domain.LfPostReview;
import com.ruoyi.lostfound.domain.LfPostImage;
import org.apache.ibatis.annotations.Param;

/**
 * 失物招领帖子数据层
 */
public interface LfPostMapper
{
    List<LfPost> selectPublicPostList(LfPost post);

    LfPost selectPublicPostById(Long postId);

    List<LfPost> selectManagePostList(LfPost post);

    LfPost selectPostById(Long postId);

    List<LfPost> selectPostsToExpire();

    int insertPost(LfPost post);

    int updatePost(LfPost post);

    int submitPost(LfPost post);

    int withdrawPost(LfPost post);

    int reviewPost(LfPost post);

    int resolvePost(LfPost post);

    int expirePost(LfPost post);

    int increaseViewCount(Long postId);

    int insertPostReview(LfPostReview review);

    int insertPostEvent(LfPostEvent event);

    List<LfPostImage> selectPostImages(Long postId);

    LfPostImage selectPostImage(@Param("postId") Long postId, @Param("imageId") Long imageId);

    LfPostImage selectPostImageByHash(@Param("postId") Long postId, @Param("sha256") String sha256);

    int countPostImages(Long postId);

    int insertPostImage(LfPostImage image);

    int updateImageAccessUrl(LfPostImage image);

    int updateImageReviewStatus(@Param("postId") Long postId, @Param("reviewStatus") String reviewStatus);
}
