package com.ruoyi.lostfound.service;

import java.util.List;
import com.ruoyi.lostfound.domain.LfMatch;
import com.ruoyi.lostfound.domain.LfPost;

/**
 * 寻物招领匹配服务
 */
public interface IMatchingService
{
    List<LfMatch> generateForPost(LfPost post);

    List<LfMatch> selectMatchesByPost(Long postId);

    List<LfMatch> selectManageMatches(LfMatch query);
}
