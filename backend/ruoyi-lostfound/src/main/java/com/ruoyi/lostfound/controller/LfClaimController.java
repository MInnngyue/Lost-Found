package com.ruoyi.lostfound.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.lostfound.domain.LfClaim;
import com.ruoyi.lostfound.service.ILfClaimService;

/**
 * 认领申请接口
 */
@RestController
@RequestMapping("/lostfound/claim")
public class LfClaimController extends BaseController
{
    @Autowired
    private ILfClaimService claimService;

    @PreAuthorize("@ss.hasPermi('lostfound:claim:add')")
    @Log(title = "认领申请", businessType = BusinessType.INSERT)
    @RateLimiter(key = "lostfound:claim:add", time = 60, count = 5,
            limitType = com.ruoyi.common.enums.LimitType.IP)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody LfClaim claim)
    {
        return success(claimService.createClaim(claim, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:claim:query')")
    @GetMapping("/my")
    public AjaxResult my()
    {
        List<LfClaim> claims = claimService.selectMyClaims(getUserId());
        return success(claims);
    }

    @PreAuthorize("@ss.hasPermi('lostfound:claim:query')")
    @GetMapping("/post/{postId}")
    public AjaxResult byPost(@PathVariable Long postId)
    {
        return success(claimService.selectClaimsByPost(postId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:claim:list')")
    @GetMapping("/manage/list")
    public TableDataInfo manageList(LfClaim query)
    {
        startPage();
        return getDataTable(claimService.selectManageClaims(query));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:claim:edit')")
    @Log(title = "确认认领", businessType = BusinessType.UPDATE)
    @PutMapping("/{claimId}/confirm")
    public AjaxResult confirm(@PathVariable Long claimId)
    {
        return toAjax(claimService.confirmClaim(claimId, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:claim:edit')")
    @Log(title = "取消认领", businessType = BusinessType.UPDATE)
    @PutMapping("/{claimId}/cancel")
    public AjaxResult cancel(@PathVariable Long claimId)
    {
        return toAjax(claimService.cancelClaim(claimId, getUserId(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('lostfound:claim:edit')")
    @Log(title = "拒绝认领", businessType = BusinessType.UPDATE)
    @PutMapping("/{claimId}/reject")
    public AjaxResult reject(@PathVariable Long claimId, @RequestBody LfClaim claim)
    {
        return toAjax(claimService.rejectClaim(claimId, getUserId(), getUsername(), claim.getOwnerRemark()));
    }
}
