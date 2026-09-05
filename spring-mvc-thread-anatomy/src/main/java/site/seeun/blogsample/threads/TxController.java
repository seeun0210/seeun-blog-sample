package site.seeun.blogsample.threads;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TxController {

    private static final Logger log = LoggerFactory.getLogger(TxController.class);

    private final TxService tx;
    private final JdbcTemplate jdbc;
    private final HikariDataSource ds;

    TxController(TxService tx, JdbcTemplate jdbc, HikariDataSource ds) {
        this.tx = tx;
        this.jdbc = jdbc;
        this.ds = ds;
    }

    @GetMapping("/tx/in")   public String in()   { tx.inTx(); return "ok\n"; }
    @GetMapping("/tx/none") public String none() { tx.noTx(); return "ok\n"; }

    @GetMapping("/tx/async")
    public String async() throws InterruptedException {
        jdbc.update("DELETE FROM member");
        try {
            tx.parentRollsBack();
        } catch (IllegalStateException expected) {
            log.info("부모 롤백됨: {}", expected.getMessage());
        }
        return "남은 행: " + jdbc.queryForList("SELECT id, name FROM member ORDER BY id") + "\n";
    }

    /** 트랜잭션 열고 대기 → 커넥션 점유. 대기 중 풀 상태를 같이 찍는다. */
    @GetMapping("/tx/hold")
    public String hold(@RequestParam(defaultValue = "200") long ms) throws InterruptedException {
        tx.holdTx(ms);
        return pool();
    }

    /** 트랜잭션 없이 대기 → 커넥션 안 잡음. */
    @GetMapping("/tx/free")
    public String free(@RequestParam(defaultValue = "200") long ms) throws InterruptedException {
        tx.freeWait(ms);
        return pool();
    }

    @GetMapping("/tx/pool")
    public String pool() {
        var p = ds.getHikariPoolMXBean();
        return "active=%d idle=%d 대기=%d%n".formatted(
                p.getActiveConnections(), p.getIdleConnections(), p.getThreadsAwaitingConnection());
    }
}
