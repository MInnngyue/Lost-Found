package com.ruoyi.lostfound.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.lostfound.domain.LfPost;
import com.ruoyi.lostfound.domain.LfPostEvent;
import com.ruoyi.lostfound.domain.LfPostReview;
import com.ruoyi.lostfound.domain.LfPostImage;

class LfPostMapperTest
{
    @Test
    void joinedQueriesShouldQualifySharedColumnNames() throws Exception
    {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("LfPost", LfPost.class);
        config.getTypeAliasRegistry().registerAlias("LfPostReview", LfPostReview.class);
        config.getTypeAliasRegistry().registerAlias("LfPostEvent", LfPostEvent.class);
        config.getTypeAliasRegistry().registerAlias("LfPostImage", LfPostImage.class);
        String resource = "mapper/lostfound/LfPostMapper.xml";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource))
        {
            new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
        for (String statement : new String[] { "selectPublicPostList", "selectManagePostList",
                "selectPublicPostById", "selectPostById" })
        {
            Object params = statement.endsWith("List") ? new LfPost() : 1L;
            String sql = config.getMappedStatement(LfPostMapper.class.getName() + "." + statement)
                    .getBoundSql(params).getSql();
            assertFalse(sql.matches("(?is).*\\b(?:and|where|by)\\s+(?:del_flag|create_time)\\b.*"), sql);
            assertTrue(sql.contains("item.category_name as item_category_name"), sql);
        }
        for (String statement : new String[] { "updatePost", "submitPost", "withdrawPost", "reviewPost",
                "resolvePost", "expirePost", "increaseViewCount" })
        {
            Object params = statement.equals("increaseViewCount") ? 1L : new LfPost();
            String sql = config.getMappedStatement(LfPostMapper.class.getName() + "." + statement)
                    .getBoundSql(params).getSql();
            assertTrue(sql.contains("update lf_post"), sql);
            assertFalse(sql.contains("p."), sql);
            assertTrue(sql.contains("del_flag = '0'"), sql);
        }
        for (String statement : new String[] { "selectPostImages", "selectPostImage",
                "selectPostImageByHash", "countPostImages", "insertPostImage",
                "updateImageAccessUrl", "updateImageReviewStatus" })
        {
            assertTrue(config.getMappedStatement(LfPostMapper.class.getName() + "." + statement) != null,
                    statement);
        }
        assertTrue(config.getMappedStatement(LfPostMapper.class.getName() + ".selectPostsToExpire") != null);
    }
}
