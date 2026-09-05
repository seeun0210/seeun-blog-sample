package site.seeun.blogsample.threads;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbController {

    private static final Logger log = LoggerFactory.getLogger(DbController.class);

    private final JdbcTemplate jdbc;
    private final HikariDataSource ds;

    DbController(JdbcTemplate jdbc, HikariDataSource ds) {
        this.jdbc = jdbc;
        this.ds = ds;
    }

    /** 느린 쿼리 1방. 커넥션을 ms 동안 붙잡는다. */
    @GetMapping("/db")
    public String db(@RequestParam(defaultValue = "200") long ms) {
        var pool = ds.getHikariPoolMXBean();
        log.info("커넥션 대여 시도 (active={}, idle={}, 대기={})",
                pool.getActiveConnections(), pool.getIdleConnections(), pool.getThreadsAwaitingConnection());
        jdbc.queryForObject("SELECT SLEEP(?)", Object.class, ms);
        return "ok\n";
    }
}
