package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfAppeal;

/**
 * 申诉数据层
 */
public interface LfAppealMapper
{
    LfAppeal selectAppealById(Long appealId);

    List<LfAppeal> selectAppealList(LfAppeal appeal);

    List<LfAppeal> selectAppealListByUser(Long appellantId);

    int insertAppeal(LfAppeal appeal);

    int updateAppeal(LfAppeal appeal);

    int countOpenAppeal(@Param("actionId") Long actionId, @Param("appellantId") Long appellantId);
}
