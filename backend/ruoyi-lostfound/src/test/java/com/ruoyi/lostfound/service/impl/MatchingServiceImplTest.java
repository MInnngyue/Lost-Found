package com.ruoyi.lostfound.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.sql.Date;
import org.junit.jupiter.api.Test;
import com.ruoyi.lostfound.domain.LfPost;

class MatchingServiceImplTest
{
    private final MatchingServiceImpl service = new MatchingServiceImpl();

    @Test
    void matchingScoreShouldRewardSameItemLocationAndNearbyDate()
    {
        LfPost lost = post("LOST", 1L, 2L, 3L, 4L, "2026-09-01");
        LfPost found = post("FOUND", 1L, 2L, 3L, 4L, "2026-09-02");

        MatchingServiceImpl.MatchScore score = service.score(lost, found);

        assertEquals(45, score.item());
        assertEquals(15, score.color());
        assertEquals(20, score.location());
        assertEquals(20, score.time());
        assertEquals(100, score.total());
    }

    @Test
    void matchingScoreShouldNotAwardLocationWhenCampusDiffers()
    {
        LfPost lost = post("LOST", 1L, 2L, 3L, 4L, "2026-09-01");
        LfPost found = post("FOUND", 1L, 2L, 9L, 4L, "2026-09-20");

        MatchingServiceImpl.MatchScore score = service.score(lost, found);

        assertEquals(0, score.location());
        assertEquals(0, score.time());
        assertEquals(60, score.total());
    }

    private LfPost post(String type, Long item, Long color, Long campus, Long area, String date)
    {
        LfPost post = new LfPost();
        post.setPostType(type);
        post.setItemCategoryId(item);
        post.setColorCategoryId(color);
        post.setCampusCategoryId(campus);
        post.setAreaCategoryId(area);
        post.setEventDate(Date.valueOf(date));
        return post;
    }
}
