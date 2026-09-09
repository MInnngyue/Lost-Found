package com.ruoyi.lostfound.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.lostfound.domain.LfCategory;
import com.ruoyi.lostfound.service.ILfCategoryService;

/**
 * 失物招领分类管理
 */
@RestController
@RequestMapping("/lostfound/category")
public class LfCategoryController extends BaseController
{
    @Autowired
    private ILfCategoryService categoryService;

    @PreAuthorize("@ss.hasPermi('lostfound:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(LfCategory category)
    {
        startPage();
        return getDataTable(categoryService.selectCategoryList(category));
    }

    @Anonymous
    @GetMapping("/tree")
    public AjaxResult tree(@RequestParam(required = false) String categoryType)
    {
        return success(categoryService.selectCategoryTree(categoryType));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:category:query')")
    @GetMapping("/{categoryId}")
    public AjaxResult getInfo(@PathVariable Long categoryId)
    {
        return success(categoryService.selectCategoryById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:category:add')")
    @Log(title = "失物招领分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody LfCategory category)
    {
        category.setCreateBy(getUsername());
        category.setUpdateBy(getUsername());
        return toAjax(categoryService.insertCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:category:edit')")
    @Log(title = "失物招领分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody LfCategory category)
    {
        category.setUpdateBy(getUsername());
        return toAjax(categoryService.updateCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:category:remove')")
    @Log(title = "失物招领分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(categoryService.deleteCategoryByIds(categoryIds));
    }
}
