package com.ruoyi.lostfound.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.lostfound.domain.LfReport;

/**
 * 举报数据层
 */
public interface LfReportMapper
{
    LfReport selectReportById(Long reportId);

    int countDuplicate(@Param("reporterId") Long reporterId, @Param("targetType") String targetType,
            @Param("targetId") Long targetId);

    int insertReport(LfReport report);

    List<LfReport> selectReportList(LfReport report);

    int updateReport(LfReport report);
}
