package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.lostfound.domain.LfCategory;
import com.ruoyi.lostfound.mapper.LfCategoryMapper;

@ExtendWith(MockitoExtension.class)
class LfCategoryServiceImplTest
{
    @Mock
    private LfCategoryMapper categoryMapper;

    @InjectMocks
    private LfCategoryServiceImpl categoryService;

    @Test
    void insertShouldRejectAreaWithoutCampusParent()
    {
        LfCategory category = category(1L, "AREA", 0L, "0");

        assertThrows(ServiceException.class, () -> categoryService.insertCategory(category));
        verify(categoryMapper, never()).insertCategory(any());
    }

    @Test
    void insertShouldRejectLocationWithCampusParent()
    {
        LfCategory category = category(1L, "LOCATION", 2L, "0");
        when(categoryMapper.selectCategoryById(2L)).thenReturn(category(2L, "CAMPUS", 0L, "0"));

        assertThrows(ServiceException.class, () -> categoryService.insertCategory(category));
        verify(categoryMapper, never()).insertCategory(any());
    }

    @Test
    void insertShouldRejectDisabledParent()
    {
        LfCategory category = category(1L, "AREA", 2L, "0");
        when(categoryMapper.selectCategoryById(2L)).thenReturn(category(2L, "CAMPUS", 0L, "1"));

        assertThrows(ServiceException.class, () -> categoryService.insertCategory(category));
        verify(categoryMapper, never()).insertCategory(any());
    }

    @Test
    void updateShouldRejectHierarchyCycle()
    {
        LfCategory current = category(1L, "CAMPUS", 0L, "0");
        LfCategory update = category(1L, "CAMPUS", 3L, "0");
        when(categoryMapper.selectCategoryById(1L)).thenReturn(current);
        when(categoryMapper.checkCategoryCodeUnique(update)).thenReturn(null);
        when(categoryMapper.countChildrenByParentId(current)).thenReturn(0);
        when(categoryMapper.selectCategoryById(3L)).thenReturn(category(3L, "AREA", 1L, "0"));

        assertThrows(ServiceException.class, () -> categoryService.updateCategory(update));
        verify(categoryMapper, never()).updateCategory(any());
    }

    @Test
    void updateShouldNotDisableCategoryWithChildren()
    {
        LfCategory current = category(1L, "CAMPUS", 0L, "0");
        LfCategory update = category(1L, "CAMPUS", 0L, "1");
        when(categoryMapper.selectCategoryById(1L)).thenReturn(current);
        when(categoryMapper.checkCategoryCodeUnique(update)).thenReturn(null);
        when(categoryMapper.countChildrenByParentId(current)).thenReturn(1);

        assertThrows(ServiceException.class, () -> categoryService.updateCategory(update));
        verify(categoryMapper, never()).updateCategory(any());
    }

    private LfCategory category(Long id, String type, Long parentId, String status)
    {
        LfCategory category = new LfCategory();
        category.setCategoryId(id);
        category.setCategoryType(type);
        category.setParentId(parentId);
        category.setCategoryCode("CODE_" + id);
        category.setCategoryName("分类" + id);
        category.setSortOrder(0);
        category.setStatus(status);
        category.setDelFlag("0");
        return category;
    }
}
