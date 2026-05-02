# 🖥️ Medical RAG — Frontend

의료 AI 질의응답 시스템의 Next.js 프론트엔드입니다.

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript |
| Styling | Tailwind CSS |
| 상태 관리 | Zustand (인증 상태) |
| HTTP 클라이언트 | Axios (JWT 자동 갱신 인터셉터) |
| UI 컴포넌트 | Lucide React (아이콘) |
| Markdown | react-markdown + remark-gfm |

---

## 📁 프로젝트 구조

```
frontend/
├── app/
│   ├── (auth)/
│   │   ├── login/page.tsx       # 로그인 페이지
│   │   └── register/page.tsx    # 회원가입 페이지
│   └── (main)/
│       ├── layout.tsx           # 사이드바 + AuthGuard
│       ├── chat/page.tsx        # 새 채팅 (RAG 질의응답)
│       ├── chat/[id]/page.tsx   # 기존 대화 계속
│       ├── bookmarks/page.tsx   # 북마크 목록
│       └── admin/documents/     # 문서 관리 (Admin 전용)
│           └── page.tsx
├── components/
│   ├── chat/
│   │   ├── MessageBubble.tsx    # 메시지 렌더링 (마크다운 + 소스 칩)
│   │   └── SourceChips.tsx      # RAG 참고 문서 출처 표시
│   ├── layout/
│   │   ├── Sidebar.tsx          # 사이드바 (대화 목록 + 네비게이션)
│   │   └── AuthGuard.tsx        # 비로그인 시 /login 리다이렉트
│   └── ui/
│       ├── Button.tsx           # 공통 버튼 컴포넌트
│       └── Input.tsx            # 공통 인풋 컴포넌트
├── lib/
│   ├── api.ts                   # Axios 인스턴스 (JWT 자동 갱신)
│   └── utils.ts                 # cn(), formatDate() 유틸
├── store/
│   └── auth.ts                  # Zustand 인증 상태 (persist)
├── types/
│   └── index.ts                 # 백엔드 DTO 타입 정의
└── next.config.mjs              # API 프록시 설정
```

---

## 🚀 시작하기

### 사전 요구사항

- Node.js 20+
- 백엔드 서버 실행 중 (`http://localhost:8080`)

### 로컬 개발 서버 실행

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 http://localhost:3000 접속

### 프로덕션 빌드

```bash
npm run build
npm start
```

---

## 🔗 API 프록시

`next.config.mjs`에 설정된 rewrite 규칙으로 CORS 없이 백엔드와 통신합니다.

```
[브라우저] → /api/* → [Next.js] → http://localhost:8080/api/*
```

| 환경 | 백엔드 주소 |
|------|------------|
| 로컬 개발 | `http://localhost:8080` (기본값) |
| Docker Compose | `BACKEND_URL=http://app:8080` (환경변수) |

---

## 📱 페이지 구성

### 로그인 / 회원가입
- JWT 기반 로그인
- 회원가입 시 유효성 검사 (비밀번호: 영문+숫자+특수문자 8~16자)

### 채팅 (RAG 질의응답)
- 새 대화 시작 및 기존 대화 이어서 하기
- AI 답변 아래 참고 문서 출처 칩 표시 (문서명 + 유사도 %)
- 마크다운 렌더링 지원
- 메시지 복사 / 북마크 저장 기능
- 대화 목록 사이드바 (최근 순 정렬, 삭제 가능)

### 북마크
- AI 답변을 북마크로 저장
- 저장한 답변 목록 조회 및 삭제

### 문서 관리 (Admin 전용)
- 의료 문서 업로드 (PDF, TXT, DOCX)
- 업로드 진행률 실시간 표시 (SSE)
- 문서 청크 상세 보기
- 문서 삭제 (벡터 임베딩 포함)

---

## 🔐 인증 흐름

```
로그인 성공
  → accessToken → localStorage 저장
  → refresh_token → HttpOnly Cookie (자동)
  → Zustand store에 user 정보 저장 (페이지 새로고침 후에도 유지)

API 요청
  → Axios 인터셉터가 Authorization 헤더 자동 주입

401 응답 시
  → refresh-token 쿠키로 자동 재발급
  → 실패 시 /login 리다이렉트
```

---

## 🌐 환경변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `BACKEND_URL` | 백엔드 API 주소 | `http://localhost:8080` |

`.env.local` 파일로 설정:

```env
BACKEND_URL=http://localhost:8080
```

---

## 🐳 Docker로 실행

루트 디렉토리의 `docker-compose.yml`을 사용합니다:

```bash
# 루트에서 실행
docker-compose up -d --build frontend
```

프론트엔드 컨테이너는 `app` 서비스(백엔드)가 시작된 후 실행됩니다.
