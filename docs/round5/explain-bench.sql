-- =====================================================================
-- Round 5 · 상품 목록 조회 인덱스 성능 비교 (AS-IS vs TO-BE)
-- =====================================================================
-- 실행:
--   docker compose -f ./docker/infra-compose.yml exec -T mysql \
--     mysql -uapplication -papplication loopers < docs/round5/explain-bench.sql
--
-- 주의: commerce-api 는 local/test 프로필에서 ddl-auto=create 이므로
--       앱을 띄우면 이 bench_products 테이블과 무관하게 동작합니다.
--       (벤치 전용 테이블이라 앱 스키마와 분리해 두었습니다)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. 클린 스타트
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS bench_products;

CREATE TABLE bench_products (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  brand_id       BIGINT       NOT NULL,
  name           VARCHAR(255) NOT NULL,
  price          BIGINT       NOT NULL,
  stock_quantity INT          NOT NULL,
  status         VARCHAR(50)  NOT NULL,
  like_count     BIGINT       NOT NULL DEFAULT 0,
  created_at     DATETIME(6)  NOT NULL,
  updated_at     DATETIME(6)  NOT NULL,
  deleted_at     DATETIME(6)  NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 10만 건 더미 (분포 다양하게 → 카디널리티 확보)
--    10개 숫자 테이블 5개를 CROSS JOIN → 10^5 = 100,000 행
-- ---------------------------------------------------------------------
INSERT INTO bench_products
  (brand_id, name, price, stock_quantity, status, like_count, created_at, updated_at)
SELECT
  FLOOR(1 + RAND() * 500),                                   -- brand_id  1~500   (카디널리티 높음)
  CONCAT('product-', seq),
  FLOOR(1000 + RAND() * 1000000),                            -- price     다양
  FLOOR(RAND() * 1000),                                      -- stock
  CASE WHEN RAND() < 0.9 THEN 'ON_SALE' ELSE 'SOLD_OUT' END, -- status    90/10   (카디널리티 낮음)
  FLOOR(RAND() * 10000),                                     -- like_count 다양
  NOW(6), NOW(6)
FROM (
  SELECT d1.n + d2.n*10 + d3.n*100 + d4.n*1000 + d5.n*10000 AS seq
  FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d1
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d2
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d3
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d4
  CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d5
) numbers;

ANALYZE TABLE bench_products;

SELECT COUNT(*)                  AS total_rows   FROM bench_products;
SELECT COUNT(DISTINCT brand_id)  AS brand_card   FROM bench_products;
SELECT COUNT(DISTINCT status)    AS status_card  FROM bench_products;
SELECT COUNT(DISTINCT like_count) AS like_card    FROM bench_products;

-- ---------------------------------------------------------------------
-- 2. AS-IS : 인덱스 없음 (브랜드 필터 + 좋아요순)
-- ---------------------------------------------------------------------
SELECT '=== AS-IS (no index) : EXPLAIN ===' AS phase;
EXPLAIN
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE' AND brand_id = 1
ORDER BY like_count DESC
LIMIT 20;

SELECT '=== AS-IS (no index) : EXPLAIN ANALYZE ===' AS phase;
EXPLAIN ANALYZE
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE' AND brand_id = 1
ORDER BY like_count DESC
LIMIT 20;

-- ---------------------------------------------------------------------
-- 3. 인덱스 생성 : (brand_id, like_count)
-- ---------------------------------------------------------------------
CREATE INDEX idx_bench_brand_like ON bench_products (brand_id, like_count);
ANALYZE TABLE bench_products;

-- ---------------------------------------------------------------------
-- 4. TO-BE : 인덱스 적용 후 (같은 쿼리)
-- ---------------------------------------------------------------------
SELECT '=== TO-BE (brand_id, like_count) : EXPLAIN ===' AS phase;
EXPLAIN
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE' AND brand_id = 1
ORDER BY like_count DESC
LIMIT 20;

SELECT '=== TO-BE (brand_id, like_count) : EXPLAIN ANALYZE ===' AS phase;
EXPLAIN ANALYZE
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE' AND brand_id = 1
ORDER BY like_count DESC
LIMIT 20;

-- ---------------------------------------------------------------------
-- 5. (보너스) 커버링 인덱스 확인 : 필요한 컬럼이 인덱스에 다 있으면 Using index
-- ---------------------------------------------------------------------
SELECT '=== BONUS : covering index (SELECT id, brand_id, like_count) ===' AS phase;
EXPLAIN
SELECT id, brand_id, like_count FROM bench_products
WHERE brand_id = 1
ORDER BY like_count DESC
LIMIT 20;

-- ---------------------------------------------------------------------
-- 6. (보너스) Leftmost 위반 확인 : 선두 컬럼(brand_id) 없이 like_count 단독
-- ---------------------------------------------------------------------
SELECT '=== BONUS : leftmost violation (WHERE like_count only) ===' AS phase;
EXPLAIN
SELECT * FROM bench_products
WHERE like_count = 5000
LIMIT 20;

-- ---------------------------------------------------------------------
-- 7. 전체 인기상품 정렬 (브랜드 필터 없음) + 좋아요순
--    복합 인덱스 (brand_id, like_count) 는 선두 컬럼 brand_id 가
--    조건에 없으므로 ORDER BY like_count 에 쓰이지 못함 → filesort
-- ---------------------------------------------------------------------
SELECT '=== FULL-LIST AS-IS (composite only, no brand filter) : EXPLAIN ===' AS phase;
EXPLAIN
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE'
ORDER BY like_count DESC
LIMIT 20;

SELECT '=== FULL-LIST AS-IS : EXPLAIN ANALYZE ===' AS phase;
EXPLAIN ANALYZE
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE'
ORDER BY like_count DESC
LIMIT 20;

-- ---------------------------------------------------------------------
-- 8. 단일 인덱스 (like_count) 생성 : 전체 좋아요순 정렬 커버
-- ---------------------------------------------------------------------
CREATE INDEX idx_bench_like ON bench_products (like_count);
ANALYZE TABLE bench_products;

-- ---------------------------------------------------------------------
-- 9. FULL-LIST TO-BE : 단일 (like_count) 인덱스 적용 후
--    Backward index scan 으로 정렬 자체를 인덱스가 대체 → filesort 제거
-- ---------------------------------------------------------------------
SELECT '=== FULL-LIST TO-BE (single like_count) : EXPLAIN ===' AS phase;
EXPLAIN
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE'
ORDER BY like_count DESC
LIMIT 20;

SELECT '=== FULL-LIST TO-BE : EXPLAIN ANALYZE ===' AS phase;
EXPLAIN ANALYZE
SELECT * FROM bench_products
WHERE deleted_at IS NULL AND status = 'ON_SALE'
ORDER BY like_count DESC
LIMIT 20;
