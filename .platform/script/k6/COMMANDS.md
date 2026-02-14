# k6 실행 커맨드

## 1) AUTH_TOKEN 없이 실행
```bash
BASE_URL="https://example.com" ./.platform/script/k6/run-get-post.sh no-auth
```

## 2) AUTH_TOKEN 포함 실행
```bash
BASE_URL="https://example.com" AUTH_TOKEN="your_token_here" ./.platform/script/k6/run-get-post.sh auth
```

## 3) p95 임계값 변경 실행
```bash
BASE_URL="https://example.com" P95_THRESHOLD_MS=250 ./.platform/script/k6/run-get-post.sh custom-p95
```

## 4) 결과 파일 확인
```bash
ls -l ./summary.json
```
