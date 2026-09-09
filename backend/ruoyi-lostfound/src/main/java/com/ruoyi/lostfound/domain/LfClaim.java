package com.ruoyi.lostfound.domain;

import java.util.Date;

/**
 * 认领申请 lf_claim
 */
public class LfClaim
{
    private Long claimId;
    private Long postId;
    private Long applicantId;
    private Long postOwnerId;
    private Long matchId;
    private String status;
    private String evidenceText;
    private String ownerRemark;
    private String applicantConfirmed;
    private String ownerConfirmed;
    private Date acceptedTime;
    private Date completedTime;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private Integer version;
    private String delFlag;
    private String postTitle;
    private String postType;
    private String otherPartyName;
    private String myRole;

    public Long getClaimId()
    {
        return claimId;
    }

    public void setClaimId(Long claimId)
    {
        this.claimId = claimId;
    }

    public Long getPostId()
    {
        return postId;
    }

    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    public Long getApplicantId()
    {
        return applicantId;
    }

    public void setApplicantId(Long applicantId)
    {
        this.applicantId = applicantId;
    }

    public Long getPostOwnerId()
    {
        return postOwnerId;
    }

    public void setPostOwnerId(Long postOwnerId)
    {
        this.postOwnerId = postOwnerId;
    }

    public Long getMatchId()
    {
        return matchId;
    }

    public void setMatchId(Long matchId)
    {
        this.matchId = matchId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getEvidenceText()
    {
        return evidenceText;
    }

    public void setEvidenceText(String evidenceText)
    {
        this.evidenceText = evidenceText;
    }

    public String getOwnerRemark()
    {
        return ownerRemark;
    }

    public void setOwnerRemark(String ownerRemark)
    {
        this.ownerRemark = ownerRemark;
    }

    public String getApplicantConfirmed()
    {
        return applicantConfirmed;
    }

    public void setApplicantConfirmed(String applicantConfirmed)
    {
        this.applicantConfirmed = applicantConfirmed;
    }

    public String getOwnerConfirmed()
    {
        return ownerConfirmed;
    }

    public void setOwnerConfirmed(String ownerConfirmed)
    {
        this.ownerConfirmed = ownerConfirmed;
    }

    public Date getAcceptedTime()
    {
        return acceptedTime;
    }

    public void setAcceptedTime(Date acceptedTime)
    {
        this.acceptedTime = acceptedTime;
    }

    public Date getCompletedTime()
    {
        return completedTime;
    }

    public void setCompletedTime(Date completedTime)
    {
        this.completedTime = completedTime;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getPostTitle()
    {
        return postTitle;
    }

    public void setPostTitle(String postTitle)
    {
        this.postTitle = postTitle;
    }

    public String getPostType()
    {
        return postType;
    }

    public void setPostType(String postType)
    {
        this.postType = postType;
    }

    public String getOtherPartyName()
    {
        return otherPartyName;
    }

    public void setOtherPartyName(String otherPartyName)
    {
        this.otherPartyName = otherPartyName;
    }

    public String getMyRole()
    {
        return myRole;
    }

    public void setMyRole(String myRole)
    {
        this.myRole = myRole;
    }
}
