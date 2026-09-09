// MyBatis Plus 自动填充 handler，处理 createTime 和 updateTime

package com.lostfound.backend.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Slf4j
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    // insert 的时候自动填充 createTime 和 updateTime
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("==> 自动填充 createTime & updateTime");
        this.strictInsertFill(
                metaObject,
                "createTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
        this.strictInsertFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
    }

    // update 的时候只更新 updateTime
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("==> 自动更新 updateTime");
        this.strictUpdateFill(
                metaObject,
                "updateTime",
                LocalDateTime.class,
                LocalDateTime.now()
        );
    }
}