package com.ruoyi.lostfound.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfCategory;
import com.ruoyi.lostfound.mapper.LfCategoryMapper;
import com.ruoyi.lostfound.service.ILfCategoryService;

/**
 * 失物招领分类服务实现
 */
@Service
public class LfCategoryServiceImpl implements ILfCategoryService
{
    private static final Set<String> ROOT_TYPES = Set.of("ITEM", "COLOR", "CAMPUS");
    private static final Set<String> CATEGORY_TYPES = Set.of("ITEM", "COLOR", "CAMPUS", "AREA", "LOCATION");

    @Autowired
    private LfCategoryMapper categoryMapper;

    @Override
    public List<LfCategory> selectCategoryList(LfCategory category)
    {
        return categoryMapper.selectCategoryList(category);
    }

    @Override
    public List<LfCategory> selectCategoryTree(String categoryType)
    {
        List<LfCategory> categories = categoryMapper.selectEnabledCategoryList(categoryType);
        Map<Long, LfCategory> categoryMap = new LinkedHashMap<>();
        List<LfCategory> roots = new ArrayList<>();
        for (LfCategory category : categories)
        {
            category.setChildren(new ArrayList<>());
            categoryMap.put(category.getCategoryId(), category);
        }
        for (LfCategory category : categories)
        {
            LfCategory parent = categoryMap.get(category.getParentId());
            if (parent == null)
            {
                roots.add(category);
            }
            else
            {
                parent.getChildren().add(category);
            }
        }
        return roots;
    }

    @Override
    public LfCategory selectCategoryById(Long categoryId)
    {
        return categoryMapper.selectCategoryById(categoryId);
    }

    @Override
    public boolean checkCategoryCodeUnique(LfCategory category)
    {
        LfCategory info = categoryMapper.checkCategoryCodeUnique(category);
        return info == null || info.getCategoryId().equals(category.getCategoryId());
    }

    @Override
    @Transactional
    public int insertCategory(LfCategory category)
    {
        validateCategoryStructure(category, null);
        if (!checkCategoryCodeUnique(category))
        {
            throw new ServiceException("同分类类型下分类编码已存在");
        }
        category.setDelFlag(UserConstants.NORMAL);
        return categoryMapper.insertCategory(category);
    }

    @Override
    @Transactional
    public int updateCategory(LfCategory category)
    {
        if (category == null || category.getCategoryId() == null)
        {
            throw new ServiceException("分类编号不能为空");
        }
        LfCategory current = selectCategoryById(category.getCategoryId());
        if (current == null || !UserConstants.NORMAL.equals(current.getDelFlag()))
        {
            throw new ServiceException("分类不存在或已删除");
        }
        if (!checkCategoryCodeUnique(category))
        {
            throw new ServiceException("同分类类型下分类编码已存在");
        }
        if (category.getCategoryId().equals(category.getParentId()))
        {
            throw new ServiceException("父级分类不能设置为自身");
        }
        if (category.getParentId() == null || category.getCategoryType() == null
                || category.getStatus() == null || category.getSortOrder() == null)
        {
            throw new ServiceException("分类信息不完整");
        }
        int childCount = categoryMapper.countChildrenByParentId(current);
        if (childCount > 0 && (!current.getCategoryType().equals(category.getCategoryType())
                || !current.getParentId().equals(category.getParentId())
                || !UserConstants.NORMAL.equals(category.getStatus())))
        {
            throw new ServiceException("分类存在子分类，不能修改类型、父级或停用");
        }
        validateCategoryStructure(category, category.getCategoryId());
        return categoryMapper.updateCategory(category);
    }

    @Override
    @Transactional
    public int deleteCategoryByIds(Long[] categoryIds)
    {
        for (Long categoryId : categoryIds)
        {
            LfCategory category = selectCategoryById(categoryId);
            if (category == null || !UserConstants.NORMAL.equals(category.getDelFlag()))
            {
                throw new ServiceException("分类不存在或已删除");
            }
            if (categoryMapper.countChildrenByParentId(category) > 0)
            {
                throw new ServiceException("分类下存在子分类，不能删除");
            }
        }
        return categoryMapper.deleteCategoryByIds(categoryIds);
    }

    private void validateCategoryStructure(LfCategory category, Long currentId)
    {
        if (category == null || !CATEGORY_TYPES.contains(category.getCategoryType()))
        {
            throw new ServiceException("分类类型不合法");
        }
        if (category.getParentId() == null || category.getParentId() < 0)
        {
            throw new ServiceException("父级分类不合法");
        }
        if (category.getSortOrder() == null || category.getSortOrder() < 0)
        {
            throw new ServiceException("显示顺序不能为负数");
        }
        if (!"0".equals(category.getStatus()) && !"1".equals(category.getStatus()))
        {
            throw new ServiceException("分类状态不合法");
        }

        Long parentId = category.getParentId();
        if (parentId == 0)
        {
            if (!ROOT_TYPES.contains(category.getCategoryType()))
            {
                throw new ServiceException("区域和地点必须选择父级分类");
            }
            return;
        }
        if (currentId != null && currentId.equals(parentId))
        {
            throw new ServiceException("父级分类不能设置为自身");
        }

        LfCategory parent = categoryMapper.selectCategoryById(parentId);
        if (parent == null || !UserConstants.NORMAL.equals(parent.getDelFlag())
                || !UserConstants.NORMAL.equals(parent.getStatus()))
        {
            throw new ServiceException("父级分类不存在、已停用或已删除");
        }
        String expectedParentType = "AREA".equals(category.getCategoryType()) ? "CAMPUS"
                : "LOCATION".equals(category.getCategoryType()) ? "AREA" : null;
        if (expectedParentType == null || !expectedParentType.equals(parent.getCategoryType()))
        {
            throw new ServiceException("分类父级类型不匹配");
        }

        Set<Long> visited = new HashSet<>();
        Long ancestorId = parent.getCategoryId();
        while (ancestorId != null && ancestorId != 0 && visited.add(ancestorId))
        {
            if (currentId != null && currentId.equals(ancestorId))
            {
                throw new ServiceException("分类层级不能形成循环引用");
            }
            LfCategory ancestor = categoryMapper.selectCategoryById(ancestorId);
            if (ancestor == null)
            {
                break;
            }
            ancestorId = ancestor.getParentId();
        }
    }
}
