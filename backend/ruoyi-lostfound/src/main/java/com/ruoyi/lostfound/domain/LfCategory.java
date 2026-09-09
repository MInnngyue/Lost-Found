package com.ruoyi.lostfound.domain;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 失物招领分类和地点 lf_category
 */
public class LfCategory extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String categoryType;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer sortOrder;
    private String status;
    private String delFlag;
    private List<LfCategory> children = new ArrayList<>();

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    @NotBlank(message = "分类类型不能为空")
    @Size(max = 32, message = "分类类型长度不能超过32个字符")
    @Pattern(regexp = "ITEM|COLOR|CAMPUS|AREA|LOCATION", message = "分类类型不合法")
    public String getCategoryType()
    {
        return categoryType;
    }

    public void setCategoryType(String categoryType)
    {
        this.categoryType = categoryType;
    }

    @NotNull(message = "父级分类不能为空")
    @PositiveOrZero(message = "父级分类不能为负数")
    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 64, message = "分类编码长度不能超过64个字符")
    public String getCategoryCode()
    {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode)
    {
        this.categoryCode = categoryCode;
    }

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 128, message = "分类名称长度不能超过128个字符")
    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    @NotNull(message = "显示顺序不能为空")
    @Min(value = 0, message = "显示顺序不能为负数")
    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "0|1", message = "分类状态不合法")
    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public List<LfCategory> getChildren()
    {
        return children;
    }

    public void setChildren(List<LfCategory> children)
    {
        this.children = children;
    }
}
