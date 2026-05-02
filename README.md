# 🏥 Medical RAG — 의료 AI 질의응답 시스템

의료 문서를 기반으로 AI가 답변하는 RAG(Retrieval-Augmented Generation) 시스템입니다.  
Spring Boot 백엔드 + Next.js 프론트엔드 + PostgreSQL(pgvector) 벡터스토어로 구성됩니다.

---

## 📐 시스템 아키텍처

```
사용자
  │
  ▼
[Next.js :3000]  ──/api/*──▶  [Spring Boot :8080]
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              [PostgreSQL]       [Redis]        [OpenAI API]
           (JPA + pgvector)  (토큰 블랙리스트)  (GPT-4o-mini
            벡터 유사도 검색                  + Embedding)
```

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.5 |
| AI | Spring AI 1.0.0 (OpenAI GPT-4o-mini) |
| Embedding | text-embedding-3-small (1536차원) |
| VectorStore | pgvector (HNSW 인덱스, Cosine Distance) |
| Database | PostgreSQL 16 |
| Cache | Redis 7.2 |
| Auth | JWT (Access Token + Refresh Token) |
| Docs | Swagger UI (SpringDoc 2.8.5) |
| Build | Gradle 9.4.1 |
| Container | Docker / Docker Compose |

---

## 📁 프로젝트 구조

```
banking-system/
├── src/main/java/com/jaeuk/job_ai/
│   ├── config/          # Security, CORS, Redis, Swagger 설정
│   ├── controller/      # REST API 컨트롤러
│   ├── dto/             # 요청/응답 DTO
│   ├── entity/          # JPA 엔티티
│   ├── enums/           # UserRole, DocumentStatus, MessageRole 등
│   ├── exception/       # 커스텀 예외
│   ├── repository/      # JPA Repository
│   ├── security/        # JWT 필터, UserDetails
│   ├── service/         # 비즈니스 로직
│   └── util/            # JwtUtil
├── src/main/resources/
│   └── application.yml  # 환경 설정
├── frontend/            # Next.js 프론트엔드
├── Dockerfile           # 백엔드 Docker 이미지
├── docker-compose.yml   # 전체 서비스 오케스트레이션
└── .env.example         # 환경변수 예시
```

---

## 🚀 시작하기

### 사전 요구사항

- Java 21
- Docker & Docker Compose
- OpenAI API Key

### 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열어 값을 채워주세요:

```env
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret_minimum_32_characters
OPENAI_API_KEY=sk-...
```

> JWT_SECRET 생성: `openssl rand -base64 32`

### 2. 백엔드 JAR 빌드

```bash
./gradlew bootJar
```

### 3. 전체 서비스 실행

```bash
docker-compose up --build
```

| 서비스 | 주소 |
|--------|------|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

---

## 📡 API 목록

### 인증 (`/api/users`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/users/register` | 회원가입 | ❌ |
| POST | `/api/users/login` | 로그인 (JWT 발급) | ❌ |
| POST | `/api/users/logout` | 로그아웃 | ✅ |
| GET | `/api/users/me` | 내 정보 조회 | ✅ |
| PUT | `/api/users/me` | 내 정보 수정 | ✅ |
| DELETE | `/api/users/me` | 회원 탈퇴 | ✅ |
| POST | `/api/users/refresh-token` | 액세스 토큰 재발급 | ❌ |

### 채팅 (`/api/chat`, `/api/conversations`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/chat` | RAG 질의응답 | ✅ |
| GET | `/api/conversations` | 대화 목록 | ✅ |
| GET | `/api/conversations/{id}` | 대화 상세 | ✅ |
| PATCH | `/api/conversations/{id}` | 대화 이름 변경 | ✅ |
| DELETE | `/api/conversations/{id}` | 대화 삭제 | ✅ |

### 북마크 (`/api/bookmarks`)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/bookmarks` | 북마크 목록 | ✅ |
| POST | `/api/bookmarks` | 북마크 추가 | ✅ |
| DELETE | `/api/bookmarks/{id}` | 북마크 삭제 | ✅ |

### 문서 관리 — Admin 전용 (`/api/admin/documents`)

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/admin/documents` | 문서 업로드 (비동기) | ADMIN |
| GET | `/api/admin/documents/{id}/progress` | 업로드 진행률 (SSE) | ADMIN |
| GET | `/api/admin/documents` | 문서 목록 | ADMIN |
| GET | `/api/admin/documents/{id}` | 문서 상세 (청크 포함) | ADMIN |
| DELETE | `/api/admin/documents/{id}` | 문서 삭제 | ADMIN |

---

## 🔐 인증 방식

- **로그인** → `Authorization: Bearer <accessToken>` 헤더 반환
- **Refresh Token** → HttpOnly Cookie (`refresh_token`)로 발급
- **로그아웃** → Access Token을 Redis 블랙리스트에 등록

---

## 🤖 RAG 동작 방식

```
1. 사용자 질문 입력
2. text-embedding-3-small 로 질문 벡터화
3. pgvector에서 코사인 유사도 검색
   - topK: 4 (상위 4개 청크)
   - similarityThreshold: 0.5 (유사도 50% 이상)
4. 검색된 문서 청크를 Context로 구성
5. GPT-4o-mini 에 [Context + 질문] 전달
6. AI 답변 + 참고 출처(SourceChip) 반환
```

### 지원 문서 형식

| 형식 | 확장자 | 최대 크기 |
|------|--------|----------|
| PDF | `.pdf` | 50MB |
| 텍스트 | `.txt` | 50MB |
| Word | `.docx`, `.doc` | 50MB |

---

## 👤 사용자 역할

| 역할 | 권한 |
|------|------|
| `USER` | 채팅, 북마크, 대화 관리 |
| `ADMIN` | USER 권한 + 문서 업로드/삭제 |

### Admin 계정 설정

회원가입 후 DB에서 직접 변경:

```bash
docker exec -it medical-rag-postgres psql -U postgres -d medical_rag \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';"
```

변경 후 재로그인 필요 (새 토큰에 ADMIN 권한 반영)

---

## 🌐 배포 (AWS EC2)

### 인프라 구성

| 항목 | 내용                 |
|------|--------------------|
| Cloud | AWS EC2            |
| 리전 | ap-northeast-2 (서울) |
| OS | Ubuntu 22.04 LTS   |
| 인스턴스 | t2.small           |
| 스토리지 | 30GB gp3           |

### 접속 주소

| 서비스 | 주소 |
|--------|------|
| 프론트엔드 | http://{EC2_PUBLIC_DNS}:3000 |
| 백엔드 API | http://{EC2_PUBLIC_DNS}:8080 |
| Swagger UI | http://{EC2_PUBLIC_DNS}:8080/swagger-ui.html |

> `{EC2_PUBLIC_DNS}` 는 AWS 콘솔 → EC2 → 인스턴스 → 퍼블릭 DNS 값으로 교체하세요.

### EC2 배포 순서

```bash
# 1. EC2 접속
ssh -i your-key.pem ubuntu@{EC2_PUBLIC_DNS}

# 2. Docker 설치
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo usermod -aG docker ubuntu
newgrp docker

# 3. 코드 클론
git clone https://github.com/leejaeuk0718/banking-system.git
cd banking-system

# 4. 환경변수 설정
cp .env.example .env
nano .env

# 5. Java 설치 및 빌드
sudo apt install -y openjdk-21-jdk
./gradlew bootJar

# 6. 전체 서비스 실행
docker-compose up -d --build
```

---

## 🐳 Docker 명령어

```bash
# 전체 실행
docker-compose up -d --build

# 백엔드만 재빌드
./gradlew bootJar && docker-compose up -d --build app

# 로그 확인
docker-compose logs -f app

# 전체 종료
docker-compose down

# 데이터 포함 완전 초기화
docker-compose down -v
```

---

## ⚙️ 환경변수 목록

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `DB_PASSWORD` | PostgreSQL 비밀번호 | `mypassword` |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | `openssl rand -base64 32` |
| `OPENAI_API_KEY` | OpenAI API 키 | `sk-...` |
| `SPRING_DATASOURCE_URL` | DB 접속 URL | `jdbc:postgresql://postgres:5432/medical_rag` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `redis` |
