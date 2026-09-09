package com.ruoyi.lostfound.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.file.InvalidExtensionException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.lostfound.domain.LfCategory;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostEvent;
import com.ruoyi.lostfound.domain.LfPostImage;
import com.ruoyi.lostfound.domain.LfPostReview;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.mapper.LfCategoryMapper;
import com.ruoyi.lostfound.service.IMatchingService;
import com.ruoyi.lostfound.service.ILfAuditService;
import com.ruoyi.lostfound.service.ILfMessageService;
import com.ruoyi.lostfound.service.ILfPostService;
import com.ruoyi.lostfound.service.IRestrictionService;

/**
 * 失物招领帖子服务实现
 */
@Service
public class LfPostServiceImpl implements ILfPostService
{
    private static final String DRAFT = "DRAFT";
    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String RESOLVED = "RESOLVED";
    private static final String REJECTED = "REJECTED";
    private static final String WITHDRAWN = "WITHDRAWN";
    private static final String REMOVED = "REMOVED";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String IMAGE_PENDING = "PENDING";
    private static final String IMAGE_APPROVED = "APPROVED";
    private static final String IMAGE_REJECTED = "REJECTED";
    private static final int MAX_POST_IMAGES = 9;
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;

    @Autowired
    private LfPostMapper postMapper;

    @Autowired
    private LfCategoryMapper categoryMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private IMatchingService matchingService;

    @Autowired
    private ILfMessageService messageService;

    @Autowired
    private IRestrictionService restrictionService;

    @Autowired(required = false)
    private ILfAuditService auditService;

    @Override
    public List<LfPost> selectPublicPostList(LfPost post)
    {
        return postMapper.selectPublicPostList(post);
    }

    @Override
    public LfPost selectPublicPostById(Long postId)
    {
        LfPost post = postMapper.selectPublicPostById(postId);
        if (post != null)
        {
            postMapper.increaseViewCount(postId);
        }
        return post;
    }

    @Override
    public List<LfPost> selectManagePostList(LfPost post)
    {
        return postMapper.selectManagePostList(post);
    }

    @Override
    public List<LfPost> selectMyPostList(LfPost post, Long operatorId)
    {
        post.setPublisherId(operatorId);
        return postMapper.selectManagePostList(post);
    }

    @Override
    public LfPost selectPostById(Long postId)
    {
        return postMapper.selectPostById(postId);
    }

    @Override
    @Transactional
    public int expirePosts(String operatorName, Long operatorId)
    {
        int expired = 0;
        for (LfPost post : postMapper.selectPostsToExpire())
        {
            LfPost update = new LfPost();
            update.setPostId(post.getPostId());
            update.setVersion(post.getVersion());
            update.setUpdateBy(operatorName);
            if (postMapper.expirePost(update) == 0)
            {
                continue;
            }
            recordEvent(post.getPostId(), PUBLISHED, "EXPIRED", operatorId,
                    "SYSTEM", "EXPIRE", "帖子已超过有效期");
            audit(operatorId, "POST_EXPIRE", post.getPostId(),
                    "{\"lifecycle\":\"PUBLISHED\"}", "{\"lifecycle\":\"EXPIRED\"}");
            expired++;
        }
        return expired;
    }

    @Override
    @Transactional
    public int insertPost(LfPost post, Long operatorId)
    {
        validatePostData(post);
        post.setPublisherId(operatorId);
        post.setLifecycleStatus(DRAFT);
        post.setReviewStatus(PENDING);
        post.setViewCount(0);
        post.setVersion(0);
        post.setDelFlag("0");
        int rows = postMapper.insertPost(post);
        recordEvent(post.getPostId(), null, DRAFT, operatorId, "USER", "CREATE", null);
        audit(operatorId, "POST_CREATE", post.getPostId(), null, "{\"lifecycle\":\"DRAFT\"}");
        return rows;
    }

    @Override
    @Transactional
    public int updatePost(LfPost post, Long operatorId)
    {
        LfPost current = requireOwnerPost(post.getPostId(), operatorId);
        if (!DRAFT.equals(current.getLifecycleStatus()) && !REJECTED.equals(current.getLifecycleStatus()))
        {
            throw new ServiceException("只有草稿或被拒绝的帖子可以编辑");
        }
        validatePostData(post);
        post.setPublisherId(operatorId);
        post.setVersion(current.getVersion());
        int rows = postMapper.updatePost(post);
        if (rows == 0)
        {
            throw new ServiceException("帖子已被修改，请刷新后重试");
        }
        audit(operatorId, "POST_UPDATE", post.getPostId(),
                "{\"lifecycle\":\"" + current.getLifecycleStatus() + "\"}", "{\"lifecycle\":\""
                        + current.getLifecycleStatus() + "\"}");
        return rows;
    }

    @Override
    @Transactional
    public int submitPost(Long postId, Long operatorId)
    {
        if (restrictionService.isRestricted(operatorId, "POST_PUBLISH"))
        {
            throw new ServiceException("当前账号暂时不能发布帖子");
        }
        LfPost current = requireOwnerPost(postId, operatorId);
        if (!DRAFT.equals(current.getLifecycleStatus()) && !REJECTED.equals(current.getLifecycleStatus()))
        {
            throw new ServiceException("当前帖子状态不允许提交审核");
        }
        validatePostData(current);
        LfPost update = new LfPost();
        update.setPostId(postId);
        update.setPublisherId(operatorId);
        update.setVersion(current.getVersion());
        int rows = postMapper.submitPost(update);
        if (rows == 0)
        {
            throw new ServiceException("帖子已被修改，请刷新后重试");
        }
        recordEvent(postId, current.getLifecycleStatus(), PENDING_REVIEW, operatorId, "USER", "SUBMIT_REVIEW", null);
        audit(operatorId, "POST_SUBMIT_REVIEW", postId,
                "{\"lifecycle\":\"" + current.getLifecycleStatus() + "\"}", "{\"lifecycle\":\"PENDING_REVIEW\"}");
        return rows;
    }

    @Override
    @Transactional
    public int withdrawPost(Long postId, Long operatorId)
    {
        LfPost current = requireOwnerPost(postId, operatorId);
        if (!DRAFT.equals(current.getLifecycleStatus())
                && !PENDING_REVIEW.equals(current.getLifecycleStatus())
                && !PUBLISHED.equals(current.getLifecycleStatus()))
        {
            throw new ServiceException("当前帖子状态不允许撤回");
        }
        LfPost update = new LfPost();
        update.setPostId(postId);
        update.setPublisherId(operatorId);
        update.setVersion(current.getVersion());
        int rows = postMapper.withdrawPost(update);
        if (rows == 0)
        {
            throw new ServiceException("帖子已被修改，请刷新后重试");
        }
        recordEvent(postId, current.getLifecycleStatus(), WITHDRAWN, operatorId, "USER", "WITHDRAW", null);
        audit(operatorId, "POST_WITHDRAW", postId,
                "{\"lifecycle\":\"" + current.getLifecycleStatus() + "\"}", "{\"lifecycle\":\"WITHDRAWN\"}");
        return rows;
    }

    @Override
    @Transactional
    public int reviewPost(LfPostReview review, Long operatorId)
    {
        if (!"APPROVE".equals(review.getAction()) && !"REJECT".equals(review.getAction())
                && !"REMOVE".equals(review.getAction()))
        {
            throw new ServiceException("审核动作不合法");
        }
        if (("REJECT".equals(review.getAction()) || "REMOVE".equals(review.getAction()))
                && StringUtils.isBlank(review.getReason()))
        {
            throw new ServiceException("拒绝或下架帖子必须填写理由");
        }
        LfPost current = selectPostById(review.getPostId());
        if (current == null || !"0".equals(current.getDelFlag()))
        {
            throw new ServiceException("帖子不存在或已删除");
        }
        String afterLifecycle;
        String afterReview;
        if ("APPROVE".equals(review.getAction()))
        {
            if (!PENDING_REVIEW.equals(current.getLifecycleStatus()))
            {
                throw new ServiceException("只有待审核帖子可以通过审核");
            }
            afterLifecycle = PUBLISHED;
            afterReview = APPROVED;
        }
        else if ("REJECT".equals(review.getAction()))
        {
            if (!PENDING_REVIEW.equals(current.getLifecycleStatus()))
            {
                throw new ServiceException("只有待审核帖子可以拒绝");
            }
            afterLifecycle = REJECTED;
            afterReview = REJECTED;
        }
        else
        {
            if (!PUBLISHED.equals(current.getLifecycleStatus()))
            {
                throw new ServiceException("只有已发布帖子可以下架");
            }
            afterLifecycle = REMOVED;
            afterReview = REJECTED;
        }

        LfPost update = new LfPost();
        update.setPostId(current.getPostId());
        update.setLifecycleStatus(afterLifecycle);
        update.setReviewStatus(afterReview);
        update.setVersion(current.getVersion());
        int rows = postMapper.reviewPost(update);
        if (rows == 0)
        {
            throw new ServiceException("帖子已被其他管理员处理，请刷新后重试");
        }

        review.setReviewerId(operatorId);
        review.setBeforeStatus(current.getLifecycleStatus());
        review.setAfterStatus(afterLifecycle);
        postMapper.insertPostReview(review);
        postMapper.updateImageReviewStatus(current.getPostId(),
                "APPROVE".equals(review.getAction()) ? IMAGE_APPROVED : IMAGE_REJECTED);
        recordEvent(current.getPostId(), current.getLifecycleStatus(), afterLifecycle,
                operatorId, "ADMIN", "REVIEW_" + review.getAction(), review.getReason());
        audit(operatorId, "POST_REVIEW_" + review.getAction(), current.getPostId(),
                "{\"lifecycle\":\"" + current.getLifecycleStatus() + "\"}",
                "{\"lifecycle\":\"" + afterLifecycle + "\",\"review\":\"" + afterReview + "\"}");
        if ("APPROVE".equals(review.getAction()))
        {
            current.setLifecycleStatus(afterLifecycle);
            current.setReviewStatus(afterReview);
            matchingService.generateForPost(current);
        }
        if (current.getPublisherId() != null)
        {
            String title = "APPROVE".equals(review.getAction()) ? "帖子审核通过"
                    : "REJECT".equals(review.getAction()) ? "帖子审核未通过" : "帖子已下架";
            String content = "帖子《" + current.getTitle() + "》" + title + "。"
                    + (StringUtils.isBlank(review.getReason()) ? "" : "原因：" + review.getReason());
            LfMessage message = new LfMessage();
            message.setToUserId(current.getPublisherId());
            message.setMessageType("POST_REVIEW");
            message.setTitle(title);
            message.setContent(content);
            message.setRelatedType("POST");
            message.setRelatedId(current.getPostId());
            messageService.send(message);
        }
        return rows;
    }

    @Override
    @Transactional
    public LfPostImage uploadPostImage(Long postId, MultipartFile file, Long operatorId, String operatorName)
    {
        LfPost post = requireOwnerPost(postId, operatorId);
        if (!DRAFT.equals(post.getLifecycleStatus()) && !REJECTED.equals(post.getLifecycleStatus()))
        {
            throw new ServiceException("只有草稿或被拒绝的帖子可以上传图片");
        }
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE)
        {
            throw new ServiceException("单张图片不能超过5MB");
        }
        if (postMapper.countPostImages(postId) >= MAX_POST_IMAGES)
        {
            throw new ServiceException("每个帖子最多上传9张图片");
        }

        ImageMetadata metadata = inspectImage(file);
        LfPostImage existing = postMapper.selectPostImageByHash(postId, metadata.sha256());
        if (existing != null)
        {
            return existing;
        }

        String objectKey = null;
        try
        {
            objectKey = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file,
                    MimeTypeUtils.IMAGE_EXTENSION, true);
            LfPostImage image = new LfPostImage();
            image.setPostId(postId);
            image.setObjectKey(objectKey);
            image.setAccessUrl("");
            image.setSha256(metadata.sha256());
            image.setSortOrder(postMapper.countPostImages(postId));
            image.setReviewStatus(IMAGE_PENDING);
            image.setFileSize(file.getSize());
            image.setMimeType(metadata.mimeType());
            image.setCreateBy(operatorName);
            postMapper.insertPostImage(image);
            image.setAccessUrl("/lostfound/post/" + postId + "/images/" + image.getImageId());
            postMapper.updateImageAccessUrl(image);
            return image;
        }
        catch (IOException e)
        {
            if (objectKey != null)
            {
                deleteStoredFile(objectKey);
            }
            throw new ServiceException("图片保存失败，请稍后重试");
        }
        catch (InvalidExtensionException e)
        {
            if (objectKey != null)
            {
                deleteStoredFile(objectKey);
            }
            throw new ServiceException("仅支持 JPG、PNG、GIF 或 BMP 图片");
        }
        catch (RuntimeException e)
        {
            if (objectKey != null)
            {
                deleteStoredFile(objectKey);
            }
            throw e;
        }
    }

    @Override
    public LfPostImage selectPostImage(Long postId, Long imageId, Long operatorId, boolean reviewer)
    {
        LfPost post = postMapper.selectPostById(postId);
        LfPostImage image = postMapper.selectPostImage(postId, imageId);
        if (post == null || image == null)
        {
            return null;
        }
        boolean isPublic = (PUBLISHED.equals(post.getLifecycleStatus())
                || RESOLVED.equals(post.getLifecycleStatus()))
                && APPROVED.equals(post.getReviewStatus())
                && IMAGE_APPROVED.equals(image.getReviewStatus());
        boolean isOwner = operatorId != null && operatorId.equals(post.getPublisherId());
        if (isPublic || isOwner || reviewer)
        {
            return image;
        }
        return null;
    }

    private LfPost requireOwnerPost(Long postId, Long operatorId)
    {
        LfPost post = selectPostById(postId);
        if (post == null || !"0".equals(post.getDelFlag()))
        {
            throw new ServiceException("帖子不存在或已删除");
        }
        if (!operatorId.equals(post.getPublisherId()))
        {
            throw new ServiceException("无权操作其他用户的帖子");
        }
        return post;
    }

    private void validatePostData(LfPost post)
    {
        Set<ConstraintViolation<LfPost>> violations = validator.validate(post);
        if (!violations.isEmpty())
        {
            throw new ServiceException(violations.iterator().next().getMessage());
        }
        requireCategory(post.getItemCategoryId(), "ITEM", "物品分类");
        requireCategory(post.getColorCategoryId(), "COLOR", "颜色");
        requireCategory(post.getCampusCategoryId(), "CAMPUS", "校区");
        if (post.getAreaCategoryId() != null)
        {
            LfCategory area = requireCategory(post.getAreaCategoryId(), "AREA", "区域");
            if (!post.getCampusCategoryId().equals(area.getParentId()))
            {
                throw new ServiceException("区域不属于所选校区");
            }
        }
    }

    private LfCategory requireCategory(Long id, String type, String label)
    {
        LfCategory category = id == null ? null : categoryMapper.selectCategoryById(id);
        if (category == null || !type.equals(category.getCategoryType())
                || !"0".equals(category.getStatus()) || !"0".equals(category.getDelFlag()))
        {
            throw new ServiceException(label + "不存在、已停用或类型不正确，请重新选择");
        }
        return category;
    }

    private ImageMetadata inspectImage(MultipartFile file)
    {
        String extension = FileUploadUtils.getExtension(file).toLowerCase();
        try (InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input))
        {
            if (imageInput == null)
            {
                throw new ServiceException("无法识别图片内容");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext())
            {
                throw new ServiceException("文件内容不是有效图片");
            }
            ImageReader reader = readers.next();
            try
            {
                String format = reader.getFormatName().toLowerCase();
                if (!Set.of("jpg", "jpeg", "png", "gif", "bmp").contains(format)
                        || !Set.of("jpg", "jpeg", "png", "gif", "bmp").contains(extension))
                {
                    throw new ServiceException("仅支持 JPG、PNG、GIF 或 BMP 图片");
                }
                reader.setInput(imageInput, true, true);
                BufferedImage image = reader.read(0);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0)
                {
                    throw new ServiceException("图片内容无效");
                }
                return new ImageMetadata(sha256(file), mimeType(format));
            }
            finally
            {
                reader.dispose();
            }
        }
        catch (IOException e)
        {
            throw new ServiceException("图片读取失败，请重新选择");
        }
    }

    private String sha256(MultipartFile file)
    {
        try (InputStream input = file.getInputStream())
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1)
            {
                digest.update(buffer, 0, length);
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest())
            {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            throw new ServiceException("图片校验失败，请重试");
        }
    }

    private String mimeType(String format)
    {
        return switch (format)
        {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    private void deleteStoredFile(String objectKey)
    {
        File file = new File(RuoYiConfig.getProfile() + FileUtils.stripPrefix(objectKey));
        FileUtils.deleteFile(file.getAbsolutePath());
    }

    private record ImageMetadata(String sha256, String mimeType)
    {
    }

    private void recordEvent(Long postId, String fromStatus, String toStatus, Long operatorId,
            String source, String eventType, String reason)
    {
        LfPostEvent event = new LfPostEvent();
        event.setPostId(postId);
        event.setEventType(eventType);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setOperatorId(operatorId);
        event.setSource(source);
        event.setReason(reason);
        postMapper.insertPostEvent(event);
    }

    private void audit(Long actorId, String eventType, Long targetId, String beforeData, String afterData)
    {
        if (auditService != null)
        {
            auditService.record(actorId, actorId == null ? "SYSTEM" : "USER", eventType,
                    "POST", targetId, beforeData, afterData);
        }
    }
}
