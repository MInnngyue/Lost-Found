package com.ruoyi.lostfound.service.impl;

import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.lostfound.domain.LfMatch;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.mapper.LfMatchMapper;
import com.ruoyi.lostfound.service.IMatchingService;

/**
 * 基于结构化字段的匹配服务。
 */
@Service
public class MatchingServiceImpl implements IMatchingService
{
    private static final int MATCH_THRESHOLD = 50;
    private static final String SUGGESTED = "SUGGESTED";

    @Autowired
    private LfMatchMapper matchMapper;

    @Override
    @Transactional
    public List<LfMatch> generateForPost(LfPost post)
    {
        List<LfMatch> result = new ArrayList<>();
        if (post == null || post.getPostId() == null
                || !"PUBLISHED".equals(post.getLifecycleStatus())
                || !"APPROVED".equals(post.getReviewStatus()))
        {
            return result;
        }

        for (LfPost candidate : matchMapper.selectOppositePublishedPosts(post))
        {
            MatchScore score = score(post, candidate);
            if (score.total() < MATCH_THRESHOLD)
            {
                continue;
            }
            Long lostPostId = "LOST".equals(post.getPostType()) ? post.getPostId() : candidate.getPostId();
            Long foundPostId = "FOUND".equals(post.getPostType()) ? post.getPostId() : candidate.getPostId();
            LfMatch match = matchMapper.selectMatchByPair(lostPostId, foundPostId);
            if (match == null)
            {
                match = new LfMatch();
                match.setLostPostId(lostPostId);
                match.setFoundPostId(foundPostId);
                match.setStatus(SUGGESTED);
                match.setSource("AUTO");
                applyScore(match, score);
                matchMapper.insertMatch(match);
            }
            else if (SUGGESTED.equals(match.getStatus()) || "VIEWED".equals(match.getStatus()))
            {
                applyScore(match, score);
                matchMapper.updateMatch(match);
            }
            result.add(match);
        }
        return result;
    }

    @Override
    public List<LfMatch> selectMatchesByPost(Long postId)
    {
        return matchMapper.selectMatchesByPost(postId);
    }

    @Override
    public List<LfMatch> selectManageMatches(LfMatch query)
    {
        return matchMapper.selectManageMatchList(query);
    }

    MatchScore score(LfPost left, LfPost right)
    {
        int item = same(left.getItemCategoryId(), right.getItemCategoryId()) ? 45 : 0;
        int color = same(left.getColorCategoryId(), right.getColorCategoryId()) ? 15 : 0;
        int location = 0;
        if (same(left.getCampusCategoryId(), right.getCampusCategoryId()))
        {
            location += 15;
            if (same(left.getAreaCategoryId(), right.getAreaCategoryId()))
            {
                location += 5;
            }
        }
        int time = timeScore(left.getEventDate(), right.getEventDate());
        return new MatchScore(item, color, location, time);
    }

    private int timeScore(Date left, Date right)
    {
        if (left == null || right == null)
        {
            return 0;
        }
        long days = Math.abs(ChronoUnit.DAYS.between(
                toLocalDate(left), toLocalDate(right)));
        if (days <= 1)
        {
            return 20;
        }
        if (days <= 3)
        {
            return 12;
        }
        if (days <= 7)
        {
            return 6;
        }
        return 0;
    }

    private LocalDate toLocalDate(Date date)
    {
        if (date instanceof java.sql.Date sqlDate)
        {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private boolean same(Object left, Object right)
    {
        return left != null && left.equals(right);
    }

    private void applyScore(LfMatch match, MatchScore score)
    {
        match.setItemScore(score.item());
        match.setColorScore(score.color());
        match.setLocationScore(score.location());
        match.setTimeScore(score.time());
        match.setTotalScore(score.total());
    }

    record MatchScore(int item, int color, int location, int time)
    {
        int total()
        {
            return item + color + location + time;
        }
    }
}
