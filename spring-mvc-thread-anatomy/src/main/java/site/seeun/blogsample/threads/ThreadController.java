package site.seeun.blogsample.threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThreadController {

    private static final Logger log = LoggerFactory.getLogger(ThreadController.class);

    // ponytail: 로그 패턴의 [nio-8080-exec-N] 칸이 곧 스레드 이름이다. 따로 찍을 필요 없다.
    @GetMapping("/thread")
    public String thread() throws InterruptedException {
        log.info("요청 시작");
        Thread.sleep(1000); // 스레드를 붙잡아 둬야 풀 소진이 눈에 보인다
        log.info("요청 종료");
        return "ok\n";
    }
}
