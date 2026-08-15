## 🐙 GitHub Rules 상세

### 1. GitHub Rules를 정하는 이유

GitHub는 팀 프로젝트의 코드 저장소이자 협업 기록 공간입니다.

팀원마다 각자 코드를 수정하면 충돌이 발생하거나, 어떤 기능이 언제 수정되었는지 추적하기 어려울 수 있습니다.

따라서 브랜치 생성 방식, 커밋 메시지 작성 방식, Pull Request 리뷰 방식을 미리 정해두고 동일한 기준으로 작업합니다.

---

## 2. 브랜치 전략

### 기본 브랜치 구조

우리 팀은 아래 브랜치 구조를 사용합니다.

```
main
develop
feature branch
```

| 브랜치 | 용도 |
| --- | --- |
| `main` | 최종 제출 또는 배포 가능한 안정 버전 |
| `develop` | 기능 개발이 모이는 통합 브랜치 |
| `feature branch` | 개인별 기능 개발 브랜치 |

### 브랜치 사용 흐름

기능 개발은 `main`에서 직접 하지 않습니다.

각자 맡은 기능은 별도 브랜치를 생성해서 작업한 뒤, PR을 통해 `develop` 브랜치에 병합합니다.

```
main
  └── develop
        ├── feature/login
        ├── feature/job-list
        ├── feature/ai-summary
        └── feature/mypage
```

### 작업 시작 전 브랜치 생성 방법

```
# develop 브랜치로 이동
git checkout develop

# 최신 코드 가져오기
git pull origin develop

# 새 기능 브랜치 생성
git checkout -b feature/login
```

### 브랜치명 작성 규칙

브랜치명은 아래 형식으로 작성합니다.

```
작업유형/기능명
```

예시:

```
feature/login
feature/job-list
feature/ai-summary
fix/login-error
docs/readme
refactor/api-client
style/main-page
```

| 작업유형 | 의미 | 예시 |
| --- | --- | --- |
| `feature` | 새로운 기능 개발 | `feature/login` |
| `fix` | 오류 수정 | `fix/login-error` |
| `docs` | 문서 수정 | `docs/readme` |
| `style` | UI/CSS 수정 | `style/main-page` |
| `refactor` | 코드 구조 개선 | `refactor/api-client` |
| `setting` | 환경설정 | `setting/env` |

### 브랜치 작업 시 주의사항

- `main` 브랜치에는 직접 push하지 않습니다.
- `develop` 브랜치에도 직접 push하지 않고 PR을 통해 병합합니다.
- 기능 하나당 브랜치 하나를 생성합니다.
- 작업 전에는 항상 `develop` 브랜치를 최신 상태로 가져옵니다.
- 브랜치명은 작업 내용을 알 수 있게 작성합니다.

---

## 3. 커밋 규칙

### 커밋 메시지 기본 형식

우리 팀은 아래 형식으로 커밋 메시지를 작성합니다.

```
[작업유형]_날짜/파일명 또는 기능명

- 작업한 내용
- 변경한 이유
- 확인한 내용
```

예시:

```
[Add]_07/LoginPage.jsx, authApi.js

- 로그인 페이지 UI 구현
- 이메일/비밀번호 입력값을 로그인 API로 전달
- 로그인 성공 시 accessToken 저장 확인
```

### 작업유형 규칙

| 작업유형 | 의미 | 사용 상황 |
| --- | --- | --- |
| `[Add]` | 새로운 기능, 파일, 화면 추가 | 로그인 페이지 추가, API 함수 추가 |
| `[Fix]` | 오류 수정 | 로그인 실패 메시지 오류 수정 |
| `[Update]` | 기존 기능 개선 또는 내용 변경 | 카드 UI 개선, 필터 조건 변경 |
| `[Remove]` | 불필요한 코드나 파일 삭제 | 테스트 데이터 삭제 |
| `[Refactor]` | 기능 변화 없이 코드 구조 개선 | API 요청 로직 분리 |
| `[Docs]` | 문서 수정 | README, Notion, 주석 문서 수정 |
| `[Style]` | 디자인, CSS, 레이아웃 수정 | 버튼 색상 변경, 반응형 수정 |
| `[Setting]` | 환경설정 변경 | env 예시 파일, 패키지 설치 |

### 커밋 제목 작성 예시

```
[Add]_07/LoginPage.jsx
[Add]_07/PublicApiService.js
[Fix]_07/LoginPage.jsx, authApi.js
[Update]_07/JobCard.jsx
[Refactor]_07/apiClient.js
[Docs]_07/README
[Setting]_07/env
```

파일이 많거나 기능 단위로 작업한 경우에는 기능명으로 작성합니다.

```
[Add]_07/Login
[Fix]_07/JobSearch
[Update]_07/AI_Summary
[Refactor]_07/UserService
```

### 실제 커밋 메시지 예시

```
[Add]_07/JobListPage.jsx, jobApi.js

- 채용공고 목록 페이지 구현
- 공공데이터 API 응답값을 카드 형태로 출력
- 지역별 필터 선택 기능 추가
- 서울, 부산, 대전 검색 결과 출력 확인
```

```
[Fix]_07/LoginPage.jsx, authApi.js

- 로그인 실패 시 에러 메시지가 표시되지 않던 문제 수정
- 서버 응답 코드에 따라 안내 문구 분기 처리
- 잘못된 비밀번호 입력 시 에러 메시지 출력 확인
```

```
[Refactor]_07/apiClient.js

- fetch 중복 로직을 공통 API 클라이언트로 분리
- API 요청 실패 처리 로직 통합
- 기존 채용공고 조회 기능 정상 동작 확인
```

### 커밋을 나누는 기준

한 커밋에는 하나의 작업 목적만 담습니다.

좋은 예:

```
[Add]_07/LoginPage.jsx

- 로그인 페이지 UI 구현
- 이메일/비밀번호 입력 폼 추가
```

```
[Fix]_07/LoginPage.jsx

- 로그인 실패 시 에러 메시지 미출력 문제 수정
```

피해야 할 예:

```
[Add]_07/All

- 로그인 구현
- 메인 페이지 수정
- README 수정
- 버그 수정
- 디자인 변경
```

여러 기능을 한 커밋에 넣으면 나중에 오류가 발생했을 때 원인을 찾기 어렵습니다.

### 커밋 전 확인사항

```
[ ] 실행했을 때 에러가 없는가?
[ ] 불필요한 console.log가 남아 있지 않은가?
[ ] API Key 또는 개인정보가 코드에 포함되어 있지 않은가?
[ ] 사용하지 않는 파일, 변수, 함수가 없는가?
[ ] 커밋 메시지가 작업 내용을 명확히 설명하는가?
```

---

## 4. Pull Request 규칙

### PR을 사용하는 이유

Pull Request는 작업한 코드를 바로 합치기 전에 팀원이 함께 확인하는 과정입니다.

PR을 통해 아래 내용을 확인합니다.

- 기능이 정상적으로 동작하는지
- 코드가 이해하기 쉬운지
- 다른 기능에 영향을 주지 않는지
- API Key나 개인정보가 노출되지 않았는지
- 문서나 실행 방법이 필요한 경우 함께 정리되었는지

### PR 생성 전 작업 순서

```
# 현재 브랜치 확인
git branch

# 변경 파일 확인
git status

# 커밋 생성
git add .
git commit

# GitHub에 브랜치 업로드
git push origin feature/login
```

이후 GitHub에서 Pull Request를 생성합니다.

PR 대상 브랜치는 기본적으로 `develop`으로 설정합니다.

```
feature/login → develop
```

### PR 제목 작성 규칙

PR 제목은 작업 내용을 한눈에 알 수 있도록 작성합니다.

```
[Add] 로그인 기능 구현
[Fix] 채용공고 검색 오류 수정
[Update] 메인 페이지 UI 개선
[Docs] README 실행 방법 추가
```

### PR 본문 양식

```
## 작업 내용
-

## 변경 이유
-

## 확인한 내용
-

## 참고 사항
-
```

### PR 작성 예시

```
## 작업 내용
- 로그인 페이지 UI를 구현했습니다.
- 이메일, 비밀번호 입력 폼을 추가했습니다.
- 로그인 API 요청 함수를 연결했습니다.

## 변경 이유
- 사용자가 서비스에 로그인한 뒤 개인화 기능을 사용할 수 있도록 하기 위해 구현했습니다.

## 확인한 내용
- 이메일과 비밀번호 입력 후 로그인 요청이 정상적으로 전송되는지 확인했습니다.
- 로그인 실패 시 에러 메시지가 표시되는지 확인했습니다.
- API Key가 코드에 직접 포함되어 있지 않은지 확인했습니다.

## 참고 사항
- 현재 회원가입 기능은 아직 연결되지 않았습니다.
- 추후 accessToken 만료 처리 로직이 필요합니다.
```

---

## 5. PR 리뷰 규칙

### 리뷰 기준

리뷰어는 아래 기준으로 코드를 확인합니다.

```
[ ] 기능이 PR 설명대로 동작하는가?
[ ] 코드가 이해하기 쉬운 구조인가?
[ ] 중복 코드가 지나치게 많지 않은가?
[ ] 파일명, 함수명, 변수명이 역할에 맞게 작성되었는가?
[ ] API Key, 비밀번호, 개인정보가 포함되어 있지 않은가?
[ ] 다른 기능에 영향을 주는 변경은 없는가?
[ ] 실행 방법이나 환경변수 변경이 있다면 문서에 반영되었는가?
```

### 리뷰 코멘트 작성 방식

리뷰 의견은 구체적으로 작성합니다.

좋은 예:

```
로그인 실패 처리 로직이 LoginPage 안에 길게 들어가 있어서,
authApi.js의 함수로 분리하면 재사용하기 좋을 것 같습니다.
```

```
API 요청 실패 시 사용자에게 아무 안내가 없어 보입니다.
catch 문에서 기본 에러 메시지를 보여주면 좋겠습니다.
```

피해야 할 예:

```
별로예요.
```

```
다시 해주세요.
```

### 리뷰 반영 방식

리뷰를 받은 사람은 수정 후 다시 커밋하고 push합니다.

```
git add .
git commit -m "[Fix]_07/LoginPage.jsx"
git push origin feature/login
```

PR은 자동으로 업데이트됩니다.

### PR 승인 및 병합 기준

아래 조건을 만족하면 `develop` 브랜치에 병합합니다.

```
[ ] 기능이 정상 동작함
[ ] 리뷰어가 확인 완료함
[ ] 충돌이 없음
[ ] 민감 정보가 포함되어 있지 않음
[ ] 필요한 문서가 업데이트됨
```

병합 후에는 팀원들이 `develop` 브랜치를 최신 상태로 가져옵니다.

```
git checkout develop
git pull origin develop
```

---

## 6. 충돌 발생 시 처리 방법

다른 팀원이 같은 파일을 수정하면 merge conflict가 발생할 수 있습니다.

충돌이 발생하면 혼자 임의로 삭제하지 않고, 해당 파일을 수정한 팀원과 함께 확인합니다.

기본 처리 흐름:

```
git checkout develop
git pull origin develop

git checkout feature/login
git merge develop
```

충돌 파일을 확인한 뒤 필요한 코드를 정리하고 다시 커밋합니다.

```
git add .
git commit -m "[Fix]_07/MergeConflict"
git push origin feature/login
```

---

## 7. 최종 GitHub 운영 규칙 요약

```
1. main에는 직접 push하지 않는다.
2. develop을 기준으로 기능 브랜치를 생성한다.
3. 기능 하나당 브랜치 하나를 사용한다.
4. 커밋 메시지는 [작업유형]_날짜/파일명 형식으로 작성한다.
5. PR은 feature branch에서 develop으로 보낸다.
6. PR에는 작업 내용, 변경 이유, 확인 내용을 작성한다.
7. 리뷰 후 승인되면 develop에 병합한다.
8. API Key, 비밀번호, 개인정보는 절대 GitHub에 올리지 않는다.
```