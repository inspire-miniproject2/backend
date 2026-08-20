# notification-service

Kafka 이벤트 구독과 알림 저장 담당 디렉터리입니다. 이 서비스는 `notification-db`만 소유합니다.

## 이메일 알림

민원 상태 변경 또는 답변 등록 이벤트의 `notifyChannels`에 `EMAIL`이 포함된 경우,
User Service에서 사용자의 이메일 수신 동의와 활성 상태를 확인한 뒤 SMTP로 메일을 발송합니다.
로컬 Compose 환경에서는 이메일 발송이 기본 활성화되며 Mailpit이 SMTP 메일을 수신합니다.
브라우저에서 `http://localhost:8025`를 열어 실제 외부 주소로 전송하지 않고 발송 결과를 확인할 수 있습니다.
배포 환경에서는 실제 SMTP 설정을 주입하고 `EMAIL_NOTIFICATIONS_ENABLED`를 명시적으로 관리해야 합니다.

필수 환경변수는 `EMAIL_NOTIFICATIONS_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`입니다.
인증이 필요한 SMTP 서버는 `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`,
`MAIL_STARTTLS_ENABLED`도 설정합니다. SMTP 비밀번호는 저장소에 커밋하지 않습니다.

메일이 생성되려면 민원 접수 시 `notifyChannels=EMAIL`을 선택하고 사용자 계정의
`emailNotifyAgreed`가 활성화되어 있어야 합니다. 설정을 변경한 뒤에는
`notification-service` 컨테이너를 다시 생성해야 합니다.

