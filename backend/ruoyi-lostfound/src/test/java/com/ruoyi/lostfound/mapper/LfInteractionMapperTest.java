package com.ruoyi.lostfound.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.ruoyi.lostfound.domain.LfComment;
import com.ruoyi.lostfound.domain.LfMessage;
import com.ruoyi.lostfound.domain.LfPrivateMessage;
import com.ruoyi.lostfound.domain.LfAppeal;
import com.ruoyi.lostfound.domain.LfReport;
import com.ruoyi.lostfound.domain.LfViolationAction;
import com.ruoyi.lostfound.domain.LfViolationCase;
import com.ruoyi.lostfound.mapper.LfAppealMapper;
import com.ruoyi.lostfound.mapper.LfReportMapper;
import com.ruoyi.lostfound.mapper.LfViolationMapper;

class LfInteractionMapperTest
{
    @Test
    void interactionMappersShouldParseAllStatements() throws Exception
    {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("LfComment", LfComment.class);
        config.getTypeAliasRegistry().registerAlias("LfMessage", LfMessage.class);
        config.getTypeAliasRegistry().registerAlias("LfPrivateMessage", LfPrivateMessage.class);
        config.getTypeAliasRegistry().registerAlias("LfAppeal", LfAppeal.class);
        config.getTypeAliasRegistry().registerAlias("LfReport", LfReport.class);
        config.getTypeAliasRegistry().registerAlias("LfViolationAction", LfViolationAction.class);
        config.getTypeAliasRegistry().registerAlias("LfViolationCase", LfViolationCase.class);
        parse(config, "mapper/lostfound/LfCommentMapper.xml");
        parse(config, "mapper/lostfound/LfMessageMapper.xml");
        parse(config, "mapper/lostfound/LfPrivateMessageMapper.xml");
        parse(config, "mapper/lostfound/LfReportMapper.xml");
        parse(config, "mapper/lostfound/LfViolationMapper.xml");
        parse(config, "mapper/lostfound/LfAppealMapper.xml");

        for (String statement : new String[] {
                "selectVisibleComments", "countRecentDuplicate", "insertComment" })
        {
            assertNotNull(config.getMappedStatement(LfCommentMapper.class.getName() + "." + statement), statement);
        }
        for (String statement : new String[] {
                "selectMessages", "countUnread", "insertMessage", "markRead", "markAllRead" })
        {
            assertNotNull(config.getMappedStatement(LfMessageMapper.class.getName() + "." + statement), statement);
        }
        for (String statement : new String[] {
                "selectConversations", "selectConversation", "insertMessage", "markConversationRead" })
        {
            assertNotNull(config.getMappedStatement(
                    LfPrivateMessageMapper.class.getName() + "." + statement), statement);
        }
        for (String statement : new String[] {
                "selectReportById", "selectReportList", "insertReport", "updateReport", "countDuplicate" })
        {
            assertNotNull(config.getMappedStatement(LfReportMapper.class.getName() + "." + statement), statement);
        }
        for (String statement : new String[] {
                "selectCaseById", "selectCaseList", "selectActionById", "selectActionList",
                "selectActionListByUser", "insertCase", "insertAction", "expireAction", "updateCase" })
        {
            assertNotNull(config.getMappedStatement(LfViolationMapper.class.getName() + "." + statement), statement);
        }
        for (String statement : new String[] {
                "selectAppealById", "selectAppealList", "selectAppealListByUser",
                "insertAppeal", "updateAppeal", "countOpenAppeal" })
        {
            assertNotNull(config.getMappedStatement(LfAppealMapper.class.getName() + "." + statement), statement);
        }
    }

    private void parse(Configuration config, String resource) throws Exception
    {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource))
        {
            new XMLMapperBuilder(stream, config, resource, config.getSqlFragments()).parse();
        }
    }
}
