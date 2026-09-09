package com.ruoyi.lostfound.domain;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 失物招领帖子 lf_post
 */
public class LfPost extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long postId;
    private Long publisherId;
    private String postType;
    private String lifecycleStatus;
    private String reviewStatus;
    private String title;
    private String description;
    private Long itemCategoryId;
    private Long colorCategoryId;
    private Long campusCategoryId;
    private Long areaCategoryId;
    private String locationDetail;
    private Date eventDate;
    private Date expireTime;
    private Integer viewCount;
    private Integer version;
    private String delFlag;
    private String keyword;
    private String itemCategoryName;
    private String colorCategoryName;
    private String campusCategoryName;
    private String areaCategoryName;
    private List<LfPostImage> images = new ArrayList<>();

    public Long getPostId()
    {
        return postId;
    }

    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    public Long getPublisherId()
    {
        return publisherId;
    }

    public void setPublisherId(Long publisherId)
    {
        this.publisherId = publisherId;
    }

    @NotBlank(message = "帖子类型不能为空")
    @Pattern(regexp = "LOST|FOUND", message = "帖子类型只能是LOST或FOUND")
    public String getPostType()
    {
        return postType;
    }

    public void setPostType(String postType)
    {
        this.postType = postType;
    }

    public String getLifecycleStatus()
    {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus)
    {
        this.lifecycleStatus = lifecycleStatus;
    }

    public String getReviewStatus()
    {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus)
    {
        this.reviewStatus = reviewStatus;
    }

    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 128, message = "帖子标题长度不能超过128个字符")
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Size(max = 10000, message = "帖子描述长度不能超过10000个字符")
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @NotNull(message = "物品分类不能为空")
    @Positive(message = "物品分类ID不合法")
    public Long getItemCategoryId()
    {
        return itemCategoryId;
    }

    public void setItemCategoryId(Long itemCategoryId)
    {
        this.itemCategoryId = itemCategoryId;
    }

    @NotNull(message = "颜色不能为空")
    @Positive(message = "颜色分类ID不合法")
    public Long getColorCategoryId()
    {
        return colorCategoryId;
    }

    public void setColorCategoryId(Long colorCategoryId)
    {
        this.colorCategoryId = colorCategoryId;
    }

    @NotNull(message = "校区不能为空")
    @Positive(message = "校区分类ID不合法")
    public Long getCampusCategoryId()
    {
        return campusCategoryId;
    }

    public void setCampusCategoryId(Long campusCategoryId)
    {
        this.campusCategoryId = campusCategoryId;
    }

    @Positive(message = "区域分类ID不合法")
    public Long getAreaCategoryId()
    {
        return areaCategoryId;
    }

    public void setAreaCategoryId(Long areaCategoryId)
    {
        this.areaCategoryId = areaCategoryId;
    }

    @Size(max = 255, message = "地点详情长度不能超过255个字符")
    public String getLocationDetail()
    {
        return locationDetail;
    }

    public void setLocationDetail(String locationDetail)
    {
        this.locationDetail = locationDetail;
    }

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "事件日期不能为空")
    @PastOrPresent(message = "事件日期不能晚于今天")
    public Date getEventDate()
    {
        return eventDate;
    }

    public void setEventDate(Date eventDate)
    {
        this.eventDate = eventDate;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Integer getViewCount()
    {
        return viewCount;
    }

    public void setViewCount(Integer viewCount)
    {
        this.viewCount = viewCount;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public String getItemCategoryName()
    {
        return itemCategoryName;
    }

    public void setItemCategoryName(String itemCategoryName)
    {
        this.itemCategoryName = itemCategoryName;
    }

    public String getColorCategoryName()
    {
        return colorCategoryName;
    }

    public void setColorCategoryName(String colorCategoryName)
    {
        this.colorCategoryName = colorCategoryName;
    }

    public String getCampusCategoryName()
    {
        return campusCategoryName;
    }

    public void setCampusCategoryName(String campusCategoryName)
    {
        this.campusCategoryName = campusCategoryName;
    }

    public String getAreaCategoryName()
    {
        return areaCategoryName;
    }

    public void setAreaCategoryName(String areaCategoryName)
    {
        this.areaCategoryName = areaCategoryName;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public List<LfPostImage> getImages()
    {
        return images;
    }

    public void setImages(List<LfPostImage> images)
    {
        this.images = images;
    }
}
