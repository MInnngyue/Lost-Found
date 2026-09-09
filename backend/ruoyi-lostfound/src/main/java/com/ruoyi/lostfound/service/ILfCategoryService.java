package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfCategory;

/**
 * 失物招领分类服务
 */
public interface ILfCategoryService
{
    List<LfCategory> selectCategoryList(LfCategory category);

    List<LfCategory> selectCategoryTree(String categoryType);

    LfCategory selectCategoryById(Long categoryId);

    boolean checkCategoryCodeUnique(LfCategory category);

    int insertCategory(LfCategory category);

    int updateCategory(LfCategory category);

    int deleteCategoryByIds(Long[] categoryIds);
}
