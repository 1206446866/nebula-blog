package com.nebula.article;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.nebula.article.entity.Article;
//import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.mapper.ArticleMapper;
import com.nebula.article.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static com.nebula.article.entity
        .table.ArticleTableDef.ARTICLE;

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
