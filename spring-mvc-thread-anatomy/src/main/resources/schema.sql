-- ponytail: H2에 SLEEP이 없어서 Thread.sleep을 함수로 노출. 느린 쿼리 흉내용.
CREATE ALIAS IF NOT EXISTS SLEEP FOR 'java.lang.Thread.sleep(long)';
DROP TABLE IF EXISTS member;
CREATE TABLE member (id INT PRIMARY KEY, name VARCHAR(50));
