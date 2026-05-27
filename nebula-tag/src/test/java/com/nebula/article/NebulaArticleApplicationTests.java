package com.nebula.article;

import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.nebula.article.entity
        .table.ArticleTableDef.ARTICLE;

//import com.nebula.article.mapper.ArticleMapper;

@SpringBootTest
class NebulaArticleApplicationTests {

    @Autowired
    ArticleService articleService;

    @Test
    void contextLoads() {        // 清空表，避免重复
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(ARTICLE.ID.eq(1));
        System.out.println(queryWrapper.toSQL());
        articleService.remove(queryWrapper);



    }

}
