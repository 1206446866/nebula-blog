import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.row.Db;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import java.nio.file.*;
import java.util.*;

/** 显式创建空的ES测试库，只复制结构与权限定义，不复制业务账号、文章或评论。 */
public class PrepareSearchTestDatabase {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("nebula_es_[0-9_]+"))
            throw new IllegalArgumentException("须明确指定专用ES测试库");
        String database = args[0];
        Path root = Path.of("").toAbsolutePath();
        ((ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger("ROOT")).setLevel(ch.qos.logback.classic.Level.OFF);
        try (var pool = new HikariDataSource()) {
            pool.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/nebula_blog?serverTimezone=Asia/Shanghai");
            pool.setUsername("root");
            pool.setPassword(Files.readString(root.resolve("nebula-start/.admin-secrets/ROOT_PASSWORD")).strip());
            pool.setMaximumPoolSize(1);
            new MybatisFlexBootstrap().setDataSource("search-test-prepare",pool).setLogImpl(NoLoggingImpl.class).start();
            if (Db.selectOneBySql("SELECT COUNT(*) n FROM information_schema.tables WHERE table_schema=?",database).getLong("n") != 0)
                throw new IllegalStateException("测试库已有表，拒绝覆盖");
            Db.updateBySql("CREATE DATABASE IF NOT EXISTS " + database + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            for (var row : Db.selectListBySql("SELECT table_name AS name FROM information_schema.tables WHERE table_schema='nebula_blog' AND table_type='BASE TABLE' ORDER BY table_name")) {
                String table = row.getString("name");
                if (!table.matches("[a-z_]+") || table.startsWith("search_")) continue;
                Db.updateBySql("CREATE TABLE " + database + ".`" + table + "` LIKE nebula_blog.`" + table + "`");
                if (Set.of("role","permission","role_permission").contains(table))
                    Db.insertBySql("INSERT INTO " + database + ".`" + table + "` SELECT * FROM nebula_blog.`" + table + "`");
            }
            Db.updateBySql("GRANT SELECT,INSERT,UPDATE,DELETE ON " + database + ".* TO 'nebula_blog_app'@'localhost'");
            System.out.println("EMPTY_SEARCH_TEST_DATABASE_READY " + database);
        }
    }
}
