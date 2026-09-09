package com.ruoyi.lostfound.mapper;

import java.util.List;
import com.ruoyi.lostfound.domain.LfCategory;

/**
 * 失物招领分类数据层
 */
public interface LfCategoryMapper
{
    List<LfCategory> selectCategoryList(LfCategory category);

    List<LfCategory> selectEnabledCategoryList(String categoryType);

    LfCategory selectCategoryById(Long categoryId);

    LfCategory checkCategoryCodeUnique(LfCategory category);

    int countChildrenByParentId(LfCategory category);

    int insertCategory(LfCategory category);

    int updateCategory(LfCategory category);

    int deleteCategoryByIds(Long[] categoryIds);
}
