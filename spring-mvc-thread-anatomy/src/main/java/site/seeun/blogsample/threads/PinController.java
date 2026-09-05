package site.seeun.blogsample.threads;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PinController {

    // ponytail: 락 객체가 스택에 갇히면 JIT이 lock을 지워버린다(escape analysis). 필드에 흘려서 방지.
    private static volatile Object sink;

    /** 경합 없는 락 + 블로킹. 락이 달라 논리적으로는 전부 동시 실행 가능 → 느려지면 원인은 pinning뿐. */
    @GetMapping("/pin")
    public String pin(@RequestParam(defaultValue = "1000") long ms) throws InterruptedException {
        Object lock = new Object();
        sink = lock;
        synchronized (lock) {
            Thread.sleep(ms);
        }
        return Thread.currentThread() + "\n";
    }

    /** 대조군: 같은 구조를 ReentrantLock으로. 가상 스레드가 언마운트된다. */
    @GetMapping("/nopin")
    public String nopin(@RequestParam(defaultValue = "1000") long ms) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        sink = lock;
        lock.lock();
        try {
            Thread.sleep(ms);
        } finally {
            lock.unlock();
        }
        return Thread.currentThread() + "\n";
    }
}
