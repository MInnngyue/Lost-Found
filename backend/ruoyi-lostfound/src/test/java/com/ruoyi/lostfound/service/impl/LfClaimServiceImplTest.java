package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.ruoyi.lostfound.domain.LfClaim;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.mapper.LfClaimMapper;
import com.ruoyi.lostfound.mapper.LfMatchMapper;
import com.ruoyi.lostfound.mapper.LfPostMapper;
import com.ruoyi.lostfound.service.ILfMessageService;
import com.ruoyi.lostfound.service.IRestrictionService;

@ExtendWith(MockitoExtension.class)
class LfClaimServiceImplTest
{
    @Mock
    private LfClaimMapper claimMapper;

    @Mock
    private LfPostMapper postMapper;

    @Mock
    private LfMatchMapper matchMapper;

    @Mock
    private ILfMessageService messageService;

    @Mock
    private IRestrictionService restrictionService;

    @InjectMocks
    private LfClaimServiceImpl claimService;

    @Test
    void createClaimShouldWritePendingClaimAndEvent()
    {
        LfPost post = publicPost();
        when(postMapper.selectPostById(7L)).thenReturn(post);
        when(claimMapper.selectActiveClaim(7L, 20L)).thenReturn(null);
        when(claimMapper.insertClaim(any(LfClaim.class))).thenAnswer(invocation -> {
            invocation.<LfClaim>getArgument(0).setClaimId(11L);
            return 1;
        });

        LfClaim request = new LfClaim();
        request.setPostId(7L);
        request.setEvidenceText("能描述物品内侧标签");

        LfClaim result = claimService.createClaim(request, 20L, "student");

        assertEquals(11L, result.getClaimId());
        verify(claimMapper).insertClaim(any(LfClaim.class));
        verify(claimMapper).insertClaimEvent(any());
    }

    @Test
    void createClaimShouldRejectPostOwner()
    {
        LfPost post = publicPost();
        when(postMapper.selectPostById(7L)).thenReturn(post);
        LfClaim request = new LfClaim();
        request.setPostId(7L);
        request.setEvidenceText("evidence");

        assertThrows(ServiceException.class, () -> claimService.createClaim(request, 10L, "owner"));
        verify(claimMapper, never()).insertClaim(any());
    }

    @Test
    void confirmClaimShouldFollowAcceptedHandoffAndCompletedStates()
    {
        LfClaim claim = claim(11L, 20L, 10L, "PENDING", "0", "0", 3);
        when(claimMapper.selectClaimById(11L)).thenReturn(claim);
        when(claimMapper.updateClaim(any(LfClaim.class))).thenReturn(1);

        assertEquals(1, claimService.confirmClaim(11L, 10L, "owner"));
        assertEquals("ACCEPTED", claim.getStatus());
        assertEquals("0", claim.getApplicantConfirmed());

        claim.setVersion(4);
        assertEquals(1, claimService.confirmClaim(11L, 20L, "student"));
        assertEquals("HANDOFF", claim.getStatus());
        assertEquals("1", claim.getApplicantConfirmed());

        when(postMapper.selectPostById(7L)).thenReturn(publicPost());
        when(postMapper.resolvePost(any(LfPost.class))).thenReturn(1);
        claim.setVersion(5);
        assertEquals(1, claimService.confirmClaim(11L, 10L, "owner"));
        assertEquals("COMPLETED", claim.getStatus());
        assertEquals("1", claim.getOwnerConfirmed());
        verify(postMapper).resolvePost(any(LfPost.class));
        verify(postMapper).insertPostEvent(any());
        verify(claimMapper, org.mockito.Mockito.times(3)).insertClaimEvent(any());
    }

    @Test
    void applicantCannotConfirmBeforeOwnerAccepts()
    {
        LfClaim claim = claim(11L, 20L, 10L, "PENDING", "0", "0", 0);
        when(claimMapper.selectClaimById(11L)).thenReturn(claim);

        assertThrows(ServiceException.class, () -> claimService.confirmClaim(11L, 20L, "student"));
        verify(claimMapper, never()).updateClaim(any());
    }

    @Test
    void confirmClaimShouldRejectUnrelatedUser()
    {
        LfClaim claim = claim(11L, 20L, 10L, "PENDING", "0", "0", 0);
        when(claimMapper.selectClaimById(11L)).thenReturn(claim);

        assertThrows(ServiceException.class, () -> claimService.confirmClaim(11L, 99L, "other"));
        verify(claimMapper, never()).updateClaim(any());
    }

    private LfPost publicPost()
    {
        LfPost post = new LfPost();
        post.setPostId(7L);
        post.setPublisherId(10L);
        post.setLifecycleStatus("PUBLISHED");
        post.setReviewStatus("APPROVED");
        post.setDelFlag("0");
        return post;
    }

    private LfClaim claim(Long id, Long applicant, Long owner, String status,
            String applicantConfirmed, String ownerConfirmed, int version)
    {
        LfClaim claim = new LfClaim();
        claim.setClaimId(id);
        claim.setApplicantId(applicant);
        claim.setPostOwnerId(owner);
        claim.setStatus(status);
        claim.setPostId(7L);
        claim.setApplicantConfirmed(applicantConfirmed);
        claim.setOwnerConfirmed(ownerConfirmed);
        claim.setVersion(version);
        claim.setDelFlag("0");
        return claim;
    }
}
