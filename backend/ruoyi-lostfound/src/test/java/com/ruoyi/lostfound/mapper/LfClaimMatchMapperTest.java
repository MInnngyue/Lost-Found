package com.ruoyi.lostfound.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.lostfound.domain.LfClaim;
import com.ruoyi.lostfound.domain.LfClaimEvent;
import com.ruoyi.lostfound.domain.LfMatch;
import com.ruoyi.lostfound.domain.LfPost;

class LfClaimMatchMapperTest
{
    @Test
    void claimAndMatchMappersShouldParseAllStatements() throws Exception
    {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("LfClaim", LfClaim.class);
        config.getTypeAliasRegistry().registerAlias("LfClaimEvent", LfClaimEvent.class);
        config.getTypeAliasRegistry().registerAlias("LfMatch", LfMatch.class);
        config.getTypeAliasRegistry().registerAlias("LfPost", LfPost.class);

        parse(config, "mapper/lostfound/LfClaimMapper.xml");
        parse(config, "mapper/lostfound/LfMatchMapper.xml");

        for (String statement : new String[] {
                "selectActiveClaim", "selectClaimById", "selectClaimListForUser",
                "selectClaimListByPost", "insertClaim", "updateClaim", "insertClaimEvent" })
        {
            assertNotNull(config.getMappedStatement(LfClaimMapper.class.getName() + "." + statement), statement);
        }
        for (String statement : new String[] {
                "selectOppositePublishedPosts", "selectMatchByPair", "selectMatchById",
                "insertMatch", "updateMatch", "selectMatchesByPost" })
        {
            assertNotNull(config.getMappedStatement(LfMatchMapper.class.getName() + "." + statement), statement);
        }

        String sql = config.getMappedStatement(LfMatchMapper.class.getName() + ".selectOppositePublishedPosts")
                .getBoundSql(new LfPost()).getSql();
        assertTrue(sql.contains("p.post_id <>"), sql);
    }

    private void parse(Configuration config, String resource) throws Exception
    {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource))
        {
            new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
    }
}
