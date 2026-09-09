package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfClaim;

/**
 * 认领申请服务
 */
public interface ILfClaimService
{
    LfClaim createClaim(LfClaim request, Long operatorId, String operatorName);

    List<LfClaim> selectMyClaims(Long operatorId);

    List<LfClaim> selectClaimsByPost(Long postId, Long operatorId);

    List<LfClaim> selectManageClaims(LfClaim query);

    int confirmClaim(Long claimId, Long operatorId, String operatorName);

    int cancelClaim(Long claimId, Long operatorId, String operatorName);

    int rejectClaim(Long claimId, Long operatorId, String operatorName, String reason);
}
