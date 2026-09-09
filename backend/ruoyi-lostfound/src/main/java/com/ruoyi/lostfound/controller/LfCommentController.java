package com.ruoyi.lostfound.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.lostfound.domain.LfComment;
import com.ruoyi.lostfound.service.ILfCommentService;

/**
 * 帖子评论接口
 */
@RestController
@RequestMapping("/lostfound/comment")
public class LfCommentController extends BaseController
{
    @Autowired
    private ILfCommentService commentService;

    @Anonymous
    @GetMapping("/post/{postId}")
    public AjaxResult list(@org.springframework.web.bind.annotation.PathVariable Long postId)
    {
        List<LfComment> comments = commentService.selectVisibleComments(postId);
        return success(comments);
    }

    @PreAuthorize("@ss.hasPermi('lostfound:comment:add')")
    @Log(title = "帖子评论", businessType = BusinessType.INSERT)
    @RateLimiter(key = "lostfound:comment:add", time = 60, count = 20,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody LfComment comment)
    {
        return success(commentService.addComment(comment, getUserId()));
    }
}
