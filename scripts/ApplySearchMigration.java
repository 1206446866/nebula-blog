import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.row.Db;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import java.nio.file.*;
import java.util.regex.*;

/** 使用Flex显式安装搜索任务表及触发器；保留业务记录，同名触发器不同定义时停止。 */
public class ApplySearchMigration {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("nebula_blog|nebula_es_[0-9_]+"))
            throw new IllegalArgumentException("须指定项目库或专用ES测试库");
        Path root = Path.of("").toAbsolutePath();
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("ROOT")).setLevel(ch.qos.logback.classic.Level.OFF);
        try (var pool = new HikariDataSource()) {
            pool.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/" + args[0] + "?serverTimezone=Asia/Shanghai");
            pool.setUsername("root");pool.setPassword(Files.readString(root.resolve("nebula-start/.admin-secrets/ROOT_PASSWORD")).strip());pool.setMaximumPoolSize(1);
            new MybatisFlexBootstrap().setDataSource("search-migration", pool).setLogImpl(NoLoggingImpl.class).start();
            // 先扩容正文，保留原字符集和排序规则；已为MEDIUMTEXT/LONGTEXT时不重复改表或降级。
            var content = Db.selectOneBySql("SELECT data_type kind,character_set_name charset_name,collation_name collation_value,is_nullable nullable_value FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='article' AND column_name='content'");
            if (content == null) throw new IllegalStateException("文章正文列不存在");
            if ("text".equals(content.getString("kind"))) {
                String charset = content.getString("charset_name"),collation = content.getString("collation_value");
                if (charset == null || collation == null || !charset.matches("[a-zA-Z0-9_]+") || !collation.matches("[a-zA-Z0-9_]+"))
                    throw new IllegalStateException("正文字符集定义异常，停止迁移");
                String capacity = Files.readString(root.resolve("database/migrations/20261108_article_content_capacity.sql")).replaceAll("(?m)^--[^\\n]*", "").strip();
                capacity = capacity.replace("MEDIUMTEXT NOT NULL", "MEDIUMTEXT CHARACTER SET " + charset + " COLLATE " + collation
                        + ("YES".equals(content.getString("nullable_value")) ? " NULL" : " NOT NULL"));
                Db.updateBySql(capacity.replaceFirst(";\\s*$", ""));
            } else if (!java.util.Set.of("mediumtext", "longtext").contains(content.getString("kind"))) {
                throw new IllegalStateException("正文列类型与预期不符，停止迁移");
            }
            String sql = Files.readString(root.resolve("database/migrations/20261107_search_index.sql")).replaceAll("(?m)^--[^\\n]*", "");
            for (String statement : sql.split(";")) {
                statement = statement.strip();if (statement.isEmpty()) continue;
                var trigger = Pattern.compile("CREATE TRIGGER (\\w+).*?FOR EACH ROW\\s+(.*)",Pattern.DOTALL).matcher(statement);
                if (trigger.matches()) {
                    var existing = Db.selectOneBySql("SELECT action_statement definition FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND trigger_name=?", trigger.group(1));
                    if (existing != null) {
                        if (!normalize(existing.getString("definition")).equals(normalize(trigger.group(2))))
                            throw new IllegalStateException("同名搜索触发器定义不同：" + trigger.group(1));
                        continue;
                    }
                }
                Db.updateBySql(statement);
            }
            System.out.println("SEARCH_MIGRATION_READY " + args[0]);
        }
    }
    /** 只忽略格式与大小写，不能把SQL定义不一致当作已应用。 */
    private static String normalize(String sql) { return sql.replaceAll("\\s+", " ").strip().toLowerCase(java.util.Locale.ROOT); }
}
