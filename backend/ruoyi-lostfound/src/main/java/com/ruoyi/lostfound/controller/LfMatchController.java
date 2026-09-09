package com.ruoyi.lostfound.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.lostfound.domain.LfMatch;
import com.ruoyi.lostfound.service.IMatchingService;

/**
 * 帖子匹配接口
 */
@RestController
@RequestMapping("/lostfound/match")
public class LfMatchController extends BaseController
{
    @Autowired
    private IMatchingService matchingService;

    @PreAuthorize("@ss.hasPermi('lostfound:match:query')")
    @GetMapping("/post/{postId}")
    public AjaxResult listByPost(@PathVariable Long postId)
    {
        return success(matchingService.selectMatchesByPost(postId));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:match:list')")
    @GetMapping("/manage/list")
    public TableDataInfo manageList(LfMatch query)
    {
        startPage();
        return getDataTable(matchingService.selectManageMatches(query));
    }
}
