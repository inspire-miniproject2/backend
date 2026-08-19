# notification-service

Kafka 이벤트 구독과 알림 저장 담당 디렉터리입니다. 이 서비스는 `notification-db`만 소유합니다.

## 이메일 알림

민원 상태 변경 또는 답변 등록 이벤트의 `notifyChannels`에 `EMAIL`이 포함된 경우,
User Service에서 사용자의 이메일 수신 동의와 활성 상태를 확인한 뒤 SMTP로 메일을 발송합니다.
`EMAIL_NOTIFICATIONS_ENABLED`의 기본값은 `false`이며 SMTP 설정을 완료한 환경에서만 활성화해야 합니다.

필수 환경변수는 `EMAIL_NOTIFICATIONS_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_FROM`입니다.
인증이 필요한 SMTP 서버는 `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`,
`MAIL_STARTTLS_ENABLED`도 설정합니다. SMTP 비밀번호는 저장소에 커밋하지 않습니다.

