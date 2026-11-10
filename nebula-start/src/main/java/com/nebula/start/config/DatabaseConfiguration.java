package com.nebula.start.config;

import com.mybatisflex.spring.boot.MybatisFlexProperties;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.MyBatisExceptionTranslator;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

/** 项目固定使用MySQL；错误分类不能在连接池耗尽时再次借连接查询数据库类型。 */
@Configuration(proxyBeanMethods = false)
public class DatabaseConfiguration {
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory factory, MybatisFlexProperties properties) {
        var executor = properties.getExecutorType();
        if (executor == null) executor = factory.getConfiguration().getDefaultExecutorType();
        var translator = new MyBatisExceptionTranslator(() -> new SQLErrorCodeSQLExceptionTranslator("MySQL"), true);
        return new SqlSessionTemplate(factory, executor, translator);
    }
}
