#!/bin/zsh
# 이 모듈의 모든 측정을 재현한다. JDK 21과 25 경로를 환경변수로 넘긴다.
#
#   JDK21=/path/to/21/bin/java JDK25=/path/to/25/bin/java ./benchmark/run.sh all
#
# 서브커맨드: platform | pinning | sweep | cpu | pool | tx | all
set -e
zmodload zsh/datetime

HERE=${0:a:h}
ROOT=$HERE/../..
JAR=$HERE/../build/libs/spring-mvc-thread-anatomy-0.1.0.jar
: ${JDK21:=$(/usr/libexec/java_home -v 21)/bin/java}
: ${JDK25:=$(/usr/libexec/java_home -v 25)/bin/java}
PORT=8080

build() { (cd $ROOT && ./gradlew :spring-mvc-thread-anatomy:bootJar -q); }

up() {  # up <java> <extra jvm args...>
  local java=$1; shift
  $java "$@" -jar $JAR > /tmp/anatomy.log 2>&1 &
  JPID=$!
  local tries=0
  until curl -s -o /dev/null --max-time 5 "localhost:$PORT/tx/pool"; do
    sleep 0.3
    (( ++tries > 100 )) && { echo "기동 실패 — /tmp/anatomy.log 확인"; exit 1; }
  done
}
down() { kill -9 $JPID 2>/dev/null || true; sleep 0.7; }

burst() {  # burst <path> <count> → 초 단위 소요시간
  # zsh에서 `path`는 PATH에 묶인 특수 변수다. 절대 지역변수로 쓰지 말 것.
  local ep=$1 n=$2
  local args=()
  for i in $(seq 1 $n); do args+=(-o /dev/null "localhost:$PORT$ep"); done
  local S=$EPOCHREALTIME
  curl -s --parallel --parallel-immediate --parallel-max $n --max-time 120 $args
  printf "%.2f" $(($EPOCHREALTIME - $S))
}

platform() {  # 플랫폼 스레드 풀 vs 가상 스레드
  up $JDK25 -Dspring.threads.virtual.enabled=false
  echo "platform,5,8,1000,$(burst '/thread' 8)"
  down
  up $JDK25 -Dspring.threads.virtual.enabled=true
  echo "virtual,5,8,1000,$(burst '/thread' 8)"
  down
}

pinning() {  # 캐리어 1개로 고정한 2x2
  for jdk in 21 25; do
    local java; [[ $jdk == 21 ]] && java=$JDK21 || java=$JDK25
    up $java -Djdk.virtualThreadScheduler.parallelism=1 -Djdk.virtualThreadScheduler.maxPoolSize=1
    burst '/nopin?ms=1000' 2 > /dev/null   # warmup
    echo "$jdk,/nopin,$(burst '/nopin?ms=1000' 4)"
    echo "$jdk,/pin,$(burst '/pin?ms=1000' 4)"
    down
  done
}

sweep() {  # 캐리어 수를 바꿔가며
  echo "jdk,parallelism,endpoint,requests,block_ms,elapsed_s,theoretical_s"
  for jdk in 21 25; do
    local java; [[ $jdk == 21 ]] && java=$JDK21 || java=$JDK25
    for p in 1 2 3 4 6 8 12 24; do
      up $java -Djdk.virtualThreadScheduler.parallelism=$p -Djdk.virtualThreadScheduler.maxPoolSize=$p
      burst '/nopin?ms=200' 24 > /dev/null
      local theo=$(python3 -c "import math;print(f'{math.ceil(24/$p)*0.2:.2f}')")
      echo "$jdk,$p,/pin,24,200,$(burst '/pin?ms=200' 24),$theo"
      echo "$jdk,$p,/nopin,24,200,$(burst '/nopin?ms=200' 24),$theo"
      down
    done
  done
}

cpu() {  # CPU 개수 자체를 속인다
  echo "jdk,active_processor_count,endpoint,requests,block_ms,elapsed_s,theoretical_s"
  for c in 1 2 4 12; do
    up $JDK21 -XX:ActiveProcessorCount=$c
    burst '/nopin?ms=200' 24 > /dev/null
    local theo=$(python3 -c "import math;print(f'{math.ceil(24/$c)*0.2:.2f}')")
    echo "21,$c,/pin,24,200,$(burst '/pin?ms=200' 24),$theo"
    echo "21,$c,/nopin,24,200,$(burst '/nopin?ms=200' 24),$theo"
    down
  done
}

pool() {  # 동시 쿼리 수가 풀 크기까지만 늘어나는지
  echo "jdk,pool_size,requests,query_ms,elapsed_s,theoretical_s"
  for p in 1 2 4 8; do
    up $JDK25 -Dspring.datasource.hikari.maximum-pool-size=$p
    burst '/db?ms=200' 2 > /dev/null
    local theo=$(python3 -c "import math;print(f'{math.ceil(8/$p)*0.2:.2f}')")
    echo "25,$p,8,200,$(burst '/db?ms=200' 8),$theo"
    down
  done
}

tx() {  # 트랜잭션이 커넥션을 잡는 시간
  up $JDK25 -Dspring.datasource.hikari.maximum-pool-size=2
  curl -s localhost:$PORT/tx/in    > /dev/null   # 로그: 같은 커넥션 프록시 2회
  curl -s localhost:$PORT/tx/none  > /dev/null   # 로그: 다른 프록시 2회
  echo "--- 새 스레드로 나갈 때: $(curl -s localhost:$PORT/tx/async)"
  burst '/tx/free?ms=200' 2 > /dev/null
  echo "/tx/hold,true,2,8,200,$(burst '/tx/hold?ms=200' 8),0.80"
  echo "/tx/free,false,2,8,200,$(burst '/tx/free?ms=200' 8),0.20"
  burst '/tx/hold?ms=600' 8 > /dev/null &
  sleep 0.3; echo -n "hold 중 풀 상태: "; curl -s localhost:$PORT/tx/pool
  wait %1 2>/dev/null || true
  down
  echo "(스레드/커넥션 로그는 /tmp/anatomy.log 참고)"
}

build
case ${1:-all} in
  platform) platform ;;
  pinning)  pinning ;;
  sweep)    sweep ;;
  cpu)      cpu ;;
  pool)     pool ;;
  tx)       tx ;;
  all)      platform; pinning; sweep; cpu; pool; tx ;;
  *) echo "usage: $0 [platform|pinning|sweep|cpu|pool|tx|all]"; exit 1 ;;
esac
