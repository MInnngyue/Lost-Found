package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfClaim;
import com.ruoyi.lostfound.domain.LfClaimEvent;

/**
 * 认领申请数据层
 */
public interface LfClaimMapper
{
    LfClaim selectActiveClaim(@Param("postId") Long postId, @Param("applicantId") Long applicantId);

    LfClaim selectClaimById(Long claimId);

    List<LfClaim> selectClaimListForUser(Long userId);

    List<LfClaim> selectClaimListByPost(Long postId);

    List<LfClaim> selectManageClaimList(LfClaim query);

    int insertClaim(LfClaim claim);

    int updateClaim(LfClaim claim);

    int insertClaimEvent(LfClaimEvent event);
}
