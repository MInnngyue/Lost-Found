package com.ruoyi.lostfound.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostImage;
import com.ruoyi.lostfound.domain.LfPostReview;
import com.ruoyi.lostfound.service.ILfPostService;

/**
 * 失物招领帖子管理
 */
@RestController
@RequestMapping("/lostfound/post")
public class LfPostController extends BaseController
{
    @Autowired
    private ILfPostService postService;

    @Anonymous
    @GetMapping("/list")
    public TableDataInfo list(LfPost post)
    {
        startPage();
        return getDataTable(postService.selectPublicPostList(post));
    }

    @Anonymous
    @GetMapping("/{postId}")
    public AjaxResult getPublicInfo(@PathVariable String postId)
    {
        if (!postId.matches("[0-9]+"))
        {
            return error("帖子ID格式不正确");
        }
        return success(postService.selectPublicPostById(Long.valueOf(postId)));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:list')")
    @GetMapping("/manage/list")
    public TableDataInfo manageList(LfPost post)
    {
        startPage();
        return getDataTable(postService.selectManagePostList(post));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:mine')")
    @GetMapping("/mine/list")
    public TableDataInfo mine(LfPost post)
    {
        startPage();
        return getDataTable(postService.selectMyPostList(post, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:query')")
    @GetMapping("/manage/{postId}")
    public AjaxResult getManageInfo(@PathVariable Long postId)
    {
        return success(postService.selectPostById(postId));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:add')")
    @Log(title = "失物招领帖子", businessType = BusinessType.INSERT)
    @RateLimiter(key = "lostfound:post:add", time = 60, count = 5,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody LfPost post)
    {
        post.setCreateBy(getUsername());
        post.setUpdateBy(getUsername());
        if (postService.insertPost(post, getUserId()) == 0)
        {
            return error("帖子保存失败");
        }
        return success(post);
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:edit')")
    @Log(title = "失物招领帖子", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody LfPost post)
    {
        post.setUpdateBy(getUsername());
        return toAjax(postService.updatePost(post, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:publish')")
    @Log(title = "提交帖子审核", businessType = BusinessType.UPDATE)
    @RateLimiter(key = "lostfound:post:submit", time = 60, count = 10,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PutMapping("/submit/{postId}")
    public AjaxResult submit(@PathVariable Long postId)
    {
        return toAjax(postService.submitPost(postId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:edit')")
    @Log(title = "撤回失物招领帖子", businessType = BusinessType.UPDATE)
    @PutMapping("/withdraw/{postId}")
    public AjaxResult withdraw(@PathVariable Long postId)
    {
        return toAjax(postService.withdrawPost(postId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:review')")
    @Log(title = "审核失物招领帖子", businessType = BusinessType.UPDATE)
    @PutMapping("/review")
    public AjaxResult review(@Valid @RequestBody LfPostReview review)
    {
        return toAjax(postService.reviewPost(review, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:post:edit')")
    @Log(title = "失物招领帖子图片", businessType = BusinessType.INSERT)
    @RateLimiter(key = "lostfound:post:image", time = 60, count = 30,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PostMapping("/{postId}/images")
    public AjaxResult uploadImage(@PathVariable Long postId, MultipartFile file)
    {
        return success(postService.uploadPostImage(postId, file, getUserId(), getUsername()));
    }

    @Anonymous
    @GetMapping("/{postId}/images/{imageId}")
    public void image(@PathVariable Long postId, @PathVariable Long imageId,
            HttpServletResponse response) throws IOException
    {
        LoginUser loginUser = currentLoginUser();
        Long operatorId = loginUser == null ? null : loginUser.getUserId();
        boolean reviewer = operatorId != null && SecurityUtils.hasPermi("lostfound:post:review");
        LfPostImage image = postService.selectPostImage(postId, imageId, operatorId, reviewer);
        if (image == null || !FileUtils.checkAllowDownload(image.getObjectKey()))
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File profile = new File(RuoYiConfig.getProfile()).getCanonicalFile();
        File file = new File(RuoYiConfig.getProfile() + FileUtils.stripPrefix(image.getObjectKey()))
                .getCanonicalFile();
        if (!file.toPath().startsWith(profile.toPath()) || !file.isFile())
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(image.getMimeType());
        response.setHeader("Cache-Control", "no-store");
        Files.copy(file.toPath(), response.getOutputStream());
    }

    private LoginUser currentLoginUser()
    {
        if (SecurityUtils.getAuthentication() == null
                || !(SecurityUtils.getAuthentication().getPrincipal() instanceof LoginUser))
        {
            return null;
        }
        return (LoginUser) SecurityUtils.getAuthentication().getPrincipal();
    }
}
