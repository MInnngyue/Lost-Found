package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfMatch;
import com.ruoyi.lostfound.domain.LfPost;

/**
 * 帖子匹配数据层
 */
public interface LfMatchMapper
{
    List<LfPost> selectOppositePublishedPosts(LfPost post);

    LfMatch selectMatchByPair(@Param("lostPostId") Long lostPostId, @Param("foundPostId") Long foundPostId);

    LfMatch selectMatchById(Long matchId);

    int insertMatch(LfMatch match);

    int updateMatch(LfMatch match);

    List<LfMatch> selectMatchesByPost(@Param("postId") Long postId);

    List<LfMatch> selectManageMatchList(LfMatch query);
}
