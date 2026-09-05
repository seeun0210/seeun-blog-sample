package site.seeun.blogsample.threads;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TxService {

    private static final Logger log = LoggerFactory.getLogger(TxService.class);

    private final JdbcTemplate jdbc;
    private final DataSource ds;
    private final ChildService child;

    TxService(JdbcTemplate jdbc, DataSource ds, ChildService child) {
        this.jdbc = jdbc;
        this.ds = ds;
        this.child = child;
    }

    /** ThreadLocal 상태를 그대로 찍는다. DataSourceUtils를 거쳐야 트랜잭션 커넥션이 재사용된다. */
    void trace(String tag) {
        boolean bound = TransactionSynchronizationManager.getResource(ds) != null;
        Connection c = DataSourceUtils.getConnection(ds);
        log.info("{} | thread={} | txActive={} | resource(ThreadLocal)={} | {}",
                tag,
                Thread.currentThread().getName(),
                TransactionSynchronizationManager.isActualTransactionActive(),
                bound,
                c);
        DataSourceUtils.releaseConnection(c, ds);
    }

    /** E1: 한 트랜잭션 안에서 두 번 조회 → 같은 커넥션이어야 한다. */
    @Transactional
    public void inTx() {
        trace("트랜잭션 1번째");
        jdbc.queryForObject("SELECT 1", Integer.class);
        trace("트랜잭션 2번째");
    }

    /** E1 대조군: 트랜잭션 없이 두 번 → 매번 새 커넥션. */
    public void noTx() {
        trace("무-트랜잭션 1번째");
        trace("무-트랜잭션 2번째");
    }

    /** E2: 트랜잭션 안에서 다른 스레드로 나가면 ThreadLocal이 안 따라간다. 부모는 롤백. */
    @Transactional
    public void parentRollsBack() throws InterruptedException {
        jdbc.update("INSERT INTO member VALUES (1, 'parent')");
        trace("부모");
        Thread t = new Thread(() -> {
            trace("자식(새 스레드)");
            child.insertChild();
        }, "child-thread");
        t.start();
        t.join();
        throw new IllegalStateException("부모 일부러 롤백");
    }

    /** E3/E4: 트랜잭션을 열어둔 채 대기. 쿼리는 한 줄도 안 친다. */
    @Transactional
    public void holdTx(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    /** E3 대조군: 트랜잭션 없이 같은 시간 대기. */
    public void freeWait(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}

@Service
class ChildService {

    private final JdbcTemplate jdbc;

    ChildService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void insertChild() {
        jdbc.update("INSERT INTO member VALUES (2, 'child')");
    }
}
