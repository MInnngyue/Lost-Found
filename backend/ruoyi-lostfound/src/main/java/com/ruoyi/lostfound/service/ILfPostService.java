package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostReview;
import com.ruoyi.lostfound.domain.LfPostImage;
import org.springframework.web.multipart.MultipartFile;

/**
 * 失物招领帖子服务
 */
public interface ILfPostService
{
    List<LfPost> selectPublicPostList(LfPost post);

    LfPost selectPublicPostById(Long postId);

    List<LfPost> selectManagePostList(LfPost post);

    List<LfPost> selectMyPostList(LfPost post, Long operatorId);

    LfPost selectPostById(Long postId);

    int expirePosts(String operatorName, Long operatorId);

    int insertPost(LfPost post, Long operatorId);

    int updatePost(LfPost post, Long operatorId);

    int submitPost(Long postId, Long operatorId);

    int withdrawPost(Long postId, Long operatorId);

    int reviewPost(LfPostReview review, Long operatorId);

    LfPostImage uploadPostImage(Long postId, MultipartFile file, Long operatorId, String operatorName);

    LfPostImage selectPostImage(Long postId, Long imageId, Long operatorId, boolean reviewer);
}
