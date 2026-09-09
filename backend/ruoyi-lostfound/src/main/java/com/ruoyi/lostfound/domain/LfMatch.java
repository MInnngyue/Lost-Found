package com.ruoyi.lostfound.domain;

import java.util.Date;

/**
 * 寻物招领匹配关系 lf_match
 */
public class LfMatch
{
    private Long matchId;
    private Long lostPostId;
    private Long foundPostId;
    private String status;
    private Integer totalScore;
    private Integer itemScore;
    private Integer colorScore;
    private Integer locationScore;
    private Integer timeScore;
    private String source;
    private Date expireTime;
    private Date createTime;
    private Date updateTime;
    private LfPost lostPost;
    private LfPost foundPost;

    public Long getMatchId()
    {
        return matchId;
    }

    public void setMatchId(Long matchId)
    {
        this.matchId = matchId;
    }

    public Long getLostPostId()
    {
        return lostPostId;
    }

    public void setLostPostId(Long lostPostId)
    {
        this.lostPostId = lostPostId;
    }

    public Long getFoundPostId()
    {
        return foundPostId;
    }

    public void setFoundPostId(Long foundPostId)
    {
        this.foundPostId = foundPostId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Integer getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore)
    {
        this.totalScore = totalScore;
    }

    public Integer getItemScore()
    {
        return itemScore;
    }

    public void setItemScore(Integer itemScore)
    {
        this.itemScore = itemScore;
    }

    public Integer getColorScore()
    {
        return colorScore;
    }

    public void setColorScore(Integer colorScore)
    {
        this.colorScore = colorScore;
    }

    public Integer getLocationScore()
    {
        return locationScore;
    }

    public void setLocationScore(Integer locationScore)
    {
        this.locationScore = locationScore;
    }

    public Integer getTimeScore()
    {
        return timeScore;
    }

    public void setTimeScore(Integer timeScore)
    {
        this.timeScore = timeScore;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public Date getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Date expireTime)
    {
        this.expireTime = expireTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public LfPost getLostPost()
    {
        return lostPost;
    }

    public void setLostPost(LfPost lostPost)
    {
        this.lostPost = lostPost;
    }

    public LfPost getFoundPost()
    {
        return foundPost;
    }

    public void setFoundPost(LfPost foundPost)
    {
        this.foundPost = foundPost;
    }
}
