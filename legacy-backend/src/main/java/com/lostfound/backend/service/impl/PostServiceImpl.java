package com.lostfound.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lostfound.backend.common.exception.BusinessException;
import com.lostfound.backend.dto.PostPublishDTO;
import com.lostfound.backend.dto.PostQueryDTO;
import com.lostfound.backend.entity.Post;
import com.lostfound.backend.entity.PostImage;
import com.lostfound.backend.entity.User;
import com.lostfound.backend.mapper.PostImageMapper;
import com.lostfound.backend.mapper.PostMapper;
import com.lostfound.backend.mapper.UserMapper;
import com.lostfound.backend.service.MatchingService;
import com.lostfound.backend.service.PostService;
import com.lostfound.backend.vo.PostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostImageMapper postImageMapper;
    private final UserMapper userMapper;
    private final MatchingService matchingService;

    @Override
    public Page<PostVO> page(PostQueryDTO query) {
        // 构建查询条件，支持类型/分类/状态/关键词筛选
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .notIn(Post::getStatus, 4, 5)
                .eq(query.getType() != null, Post::getType, query.getType())
                .eq(query.getItemCategory() != null && !query.getItemCategory().isEmpty(),
                        Post::getItemCategory, query.getItemCategory())
                .eq(query.getStatus() != null, Post::getStatus, query.getStatus())
                .and(query.getKeyword() != null && !query.getKeyword().isEmpty(),
                        w -> w.like(Post::getTitle, query.getKeyword())
                             .or()
                             .like(Post::getDescription, query.getKeyword()))
                .orderByDesc(Post::getCreateTime);

        long total = postMapper.selectCount(wrapper);
        int offset = (query.getPage() - 1) * query.getPageSize();
        // FIXME: 这种拼接 SQL 的方式不太好，后面改用 Page 对象
        List<Post> records = postMapper.selectList(
                wrapper.last(String.format("LIMIT %d,%d", offset, query.getPageSize())));

        Page<PostVO> result = new Page<>(query.getPage(), query.getPageSize(), total);
        result.setRecords(records.stream().map(this::toVO).toList());
        return result;
    }

    @Override
    public PostVO detail(Long id) {
        // 查帖子详情，顺便累加浏览量
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);

        PostVO vo = toVO(post);
        vo.setCoverImage(null); // 详情页不返回 coverImage，用 images 列表
        return vo;
    }

    @Override
    @Transactional
    public PostVO publish(Long userId, PostPublishDTO dto) {
        // 新建帖子，状态默认 0（待审核）
        Post post = new Post();
        post.setUserId(userId);
        post.setType(dto.getType());
        post.setItemCategory(dto.getItemCategory());
        post.setColor(dto.getColor());
        post.setLocationCampus(dto.getLocationCampus());
        post.setLocationArea(dto.getLocationArea());
        post.setLocationDetail(dto.getLocationDetail());
        post.setLostTime(dto.getLostTime());
        post.setTitle(dto.getTitle());
        post.setDescription(dto.getDescription());
        post.setStatus(0);
        post.setViewCount(0);
        post.setReviewStatus(0);
        postMapper.insert(post);

        // 处理图片列表
        if (dto.getImages() != null) {
            for (int i = 0; i < dto.getImages().length; i++) {
                PostImage img = new PostImage();
                img.setPostId(post.getId());
                img.setImageUrl(dto.getImages()[i]);
                img.setSortOrder(i);
                postImageMapper.insert(img);
            }
        }

        // 触发匹配，失败了不阻塞发布
        try {
            matchingService.match(post);
        } catch (Exception e) {
            log.warn("匹配计算异常: {}", e.getMessage());
        }

        return toVO(post);
    }

    @Override
    public void complete(Long id, Long userId) {
        // 完结帖子，只有发帖人自己能操作
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        if (!post.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");
        if (post.getStatus() != 0 && post.getStatus() != 1) throw new BusinessException(400, "当前状态不可完结");
        post.setStatus(3);
        postMapper.updateById(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO update(Long id, Long userId, PostPublishDTO dto) {
        // 更新帖子信息，需要校验权限
        Post post = postMapper.selectById(id);
        if (post == null) throw new BusinessException(404, "帖子不存在");
        if (!post.getUserId().equals(userId)) throw new BusinessException(403, "无权操作");

        post.setType(dto.getType());
        post.setItemCategory(dto.getItemCategory());
        post.setColor(dto.getColor());
        post.setLocationCampus(dto.getLocationCampus());
        post.setLocationArea(dto.getLocationArea());
        post.setLocationDetail(dto.getLocationDetail());
        post.setLostTime(dto.getLostTime());
        post.setTitle(dto.getTitle());
        post.setDescription(dto.getDescription());
        postMapper.updateById(post);

        // 图片列表如果传了就重新设置，先删后插
        if (dto.getImages() != null && dto.getImages().length > 0) {
            postImageMapper.delete(new LambdaQueryWrapper<PostImage>()
                    .eq(PostImage::getPostId, post.getId()));
            for (int i = 0; i < dto.getImages().length; i++) {
                PostImage img = new PostImage();
                img.setPostId(post.getId());
                img.setImageUrl(dto.getImages()[i]);
                img.setSortOrder(i);
                postImageMapper.insert(img);
            }
        }

        return toVO(post);
    }

    @Override
    public void remove(Long id, Long userId) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作");
        }
        postMapper.deleteById(id);
    }

    // 把 Post entity 转成 VO，附带用户信息和图片列表
    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setType(post.getType());
        vo.setItemCategory(post.getItemCategory());
        vo.setColor(post.getColor());
        vo.setLocationCampus(post.getLocationCampus());
        vo.setLocationArea(post.getLocationArea());
        vo.setLocationDetail(post.getLocationDetail());
        vo.setLostTime(post.getLostTime());
        vo.setTitle(post.getTitle());
        vo.setDescription(post.getDescription());
        vo.setStatus(post.getStatus());
        vo.setViewCount(post.getViewCount());
        vo.setReviewStatus(post.getReviewStatus());
        vo.setUserId(post.getUserId());
        vo.setCreateTime(post.getCreateTime());

        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setCreditScore(user.getCreditScore());
        }

        List<PostImage> images = postImageMapper.selectList(new LambdaQueryWrapper<PostImage>()
                .eq(PostImage::getPostId, post.getId())
                .orderByAsc(PostImage::getSortOrder));
        if (!images.isEmpty()) {
            vo.setCoverImage(images.get(0).getImageUrl());
            vo.setImages(images.stream().map(PostImage::getImageUrl).toList());
        }

        return vo;
    }
}
