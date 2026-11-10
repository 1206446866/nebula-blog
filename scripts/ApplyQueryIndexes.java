import com.mybatisflex.core.MybatisFlexBootstrap;
import com.mybatisflex.core.row.Db;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import java.nio.file.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 使用项目运行类路径执行查询索引迁移；只添加索引，不修改业务记录，同名定义不一致时停止。 */
public class ApplyQueryIndexes {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("nebula_blog|nebula_deep_[0-9_]+"))
            throw new IllegalArgumentException("须明确指定项目库或专用测试库");
        Path root = Path.of("").toAbsolutePath().normalize();
        String sql = Files.readString(root.resolve("database/migrations/20261107_query_indexes.sql"))
                .replaceAll("(?m)^--[^\\n]*", "");
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("ROOT"))
                .setLevel(ch.qos.logback.classic.Level.OFF);
        try (var pool = new HikariDataSource()) {
            pool.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/" + args[0] + "?serverTimezone=Asia/Shanghai");
            pool.setUsername("root");
            pool.setPassword(Files.readString(root.resolve("nebula-start/.admin-secrets/ROOT_PASSWORD")).strip());
            pool.setMaximumPoolSize(1);
            new MybatisFlexBootstrap().setDataSource("query-index-migration", pool).setLogImpl(NoLoggingImpl.class).start();
            var pattern = Pattern.compile("CREATE INDEX (\\w+) ON (\\w+)\\(([^)]+)\\)");
            for (String statement : sql.split(";")) {
                if (statement.isBlank()) continue;
                var match = pattern.matcher(statement.strip());
                if (!match.matches()) throw new IllegalArgumentException("不支持的迁移语句");
                String name = match.group(1), table = match.group(2), columns = match.group(3).replace(" ", "");
                var existing = Db.selectListBySql("SELECT column_name AS index_column,non_unique AS index_non_unique,"
                        + "collation AS index_direction,sub_part AS index_prefix FROM information_schema.statistics "
                        + "WHERE table_schema=DATABASE() AND table_name=? AND index_name=? ORDER BY seq_in_index", table, name);
                if (existing.isEmpty()) Db.updateBySql(statement.strip());
                else if (!columns.equals(existing.stream().map(r -> r.getString("index_column")).collect(Collectors.joining(",")))
                        || existing.stream().anyMatch(r -> r.getLong("index_non_unique") != 1L
                            || !"A".equals(r.getString("index_direction")) || r.get("index_prefix") != null))
                    throw new IllegalStateException("同名索引定义不一致：" + name);
                System.out.println("INDEX_READY " + table + "." + name);
            }
        }
    }
}
