# 큐리즘 TV 시스템 기술 아키텍처

## 개요
큐리즘 TV 시스템은 웹 기반 컨트롤러와 Android TV 네이티브 앱을 통해 다중 TV 제어를 제공하는 분산 시스템입니다. 이 문서는 시스템의 기술적 구조와 구현 세부사항을 설명합니다.

## 시스템 아키텍처

### 전체 시스템 구성도
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   모바일 기기    │    │   웹 컨트롤러    │    │   Android TV    │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ QR 스캐너   │ │    │ │ Controller  │ │    │ │ QR Setup    │ │
│ │ /connect    │ │    │ │ /controller │ │    │ │ Activity    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
└─────────────────┘    └─────────────────┘    │ ┌─────────────┐ │
         │                       │            │ │ Video       │ │
         │                       │            │ │ Receiver    │ │
         │                       │            │ │ Activity    │ │
         └───────────────────────┼────────────┼─└─────────────┘ │
                                 │            └─────────────────┘
                                 │                       │
                    ┌─────────────┴───────────────────────┴─────────────┐
                    │              WebSocket Server                     │
                    │              (api.curizm.io)                     │
                    └─────────────────────┬─────────────────────────────┘
                                          │
                    ┌─────────────────────┴─────────────────────────────┐
                    │              REST API Server                     │
                    │              (api.curizm.io)                     │
                    └───────────────────────────────────────────────────┘
```

## 웹 시스템 아키텍처

### 프론트엔드 구성
- **모바일 설정 페이지**: `/connect` - QR 코드 스캔 및 TV 연결
- **QR 코드 페이지**: `/qr` - TV에 표시되는 QR 코드
- **웹 컨트롤러**: `/tv/controller.html` - TV 제어 인터페이스
- **테스트 수신기**: `/tv/receiver.html` - 개발/테스트용

### 기술 스택
- **프레임워크**: React/Next.js
- **스타일링**: CSS-in-JS 또는 Tailwind CSS
- **WebSocket**: Socket.IO 클라이언트
- **HTTP 클라이언트**: Fetch API 또는 Axios

### 주요 컴포넌트

#### 모바일 설정 페이지 (`/connect`)
```typescript
// 주요 기능
- QR 코드 스캔 (ZXing 또는 QuaggaJS)
- 6자리 코드 수동 입력
- 회사명/비밀번호 입력
- WebSocket 연결 및 설정 전송
- 자동 리다이렉트 처리
```

#### 웹 컨트롤러 (`/tv/controller.html`)
```typescript
// 주요 기능
- 방 연결 및 상태 모니터링
- 플레이리스트 로드 및 관리
- 재생 제어 (재생/일시정지/이전/다음)
- 볼륨 및 자막 제어
- TV 상태 모니터링
```

## Android TV 앱 아키텍처

### 앱 구조
```
curizm-tv/
├── app/
│   ├── src/main/java/io/curizm/tv/
│   │   ├── MainActivity.kt           # 앱 진입점
│   │   ├── QRSetupActivity.kt        # QR 설정 화면
│   │   └── VideoReceiverActivity.kt  # 비디오 재생 화면
│   ├── src/main/res/
│   │   ├── layout/                   # UI 레이아웃
│   │   ├── drawable/                 # 이미지 리소스
│   │   └── values/                   # 색상, 문자열, 테마
│   └── src/main/AndroidManifest.xml  # 앱 매니페스트
```

### 기술 스택
- **언어**: Kotlin
- **UI**: ViewBinding + XML 레이아웃
- **비디오**: ExoPlayer (HLS 스트리밍)
- **네트워크**: Socket.IO + OkHttp
- **QR 코드**: ZXing
- **JSON**: Gson

### 주요 클래스

#### MainActivity
```kotlin
class MainActivity : AppCompatActivity() {
    // 앱 진입점
    // - 저장된 설정 확인
    // - QR 설정 또는 비디오 수신기 선택
    // - Konami 코드 리셋 처리
}
```

#### QRSetupActivity
```kotlin
class QRSetupActivity : AppCompatActivity() {
    // QR 코드 생성 및 표시
    // - API에서 세션 생성
    // - QR 코드 자동 생성
    // - 6자리 코드 표시
    // - WebSocket 연결 대기
    // - 설정 완료 시 비디오 수신기로 전환
}
```

#### VideoReceiverActivity
```kotlin
class VideoReceiverActivity : AppCompatActivity() {
    // 비디오 재생 및 제어
    // - ExoPlayer 설정 및 HLS 스트리밍
    // - WebSocket 명령 수신 및 처리
    // - 자막 및 배경음악 처리
    // - 작품 정보 오버레이 표시
    // - 상태 모니터링 및 하트비트 전송
}
```

## WebSocket 통신 프로토콜

### 연결 설정
```javascript
// 클라이언트 연결
const socket = io('wss://api.curizm.io');

// 초기 인증
socket.emit('HELLO', {
    role: 'controller' | 'receiver',
    companyName: 'company_name',
    secretCode: 'secret_code'
});
```

### 이벤트 타입

#### 컨트롤러 → 서버
- `HELLO`: 클라이언트 등록
- `SET_PLAYLIST`: 플레이리스트 설정
- `COMMAND`: 재생 제어 명령

#### 서버 → 수신기
- `SET_PLAYLIST`: 플레이리스트 데이터 전송
- `COMMAND`: 재생 제어 명령 전달

#### 수신기 → 서버
- `HELLO`: 수신기 등록
- `HEARTBEAT`: 상태 주기적 전송

### 명령 타입
```typescript
interface Command {
    action: 'PLAY' | 'PAUSE' | 'NEXT' | 'PREV' | 
            'JUMP_TO_INDEX' | 'SEEK' | 
            'SET_VIDEO_VOLUME' | 'SET_BGM_VOLUME' | 
            'SET_CAPTIONS';
    value?: number | boolean;
    startAt?: number; // 동기화 시작 시간
}
```

## REST API 엔드포인트

### TV 설정 API
```http
POST /api/v1/tv/setup
Content-Type: application/json

{
    "companyName": "company_name",
    "secretCode": "secret_code",
    "wsUrl": "wss://api.curizm.io",
    "apiUrl": "https://api.curizm.io"
}
```

### 플레이리스트 조회 API
```http
GET /api/v1/exhibition/tv?companyName={name}&secretCode={code}
```

### 세션 생성 API (Android 앱용)
```http
POST /api/v1/tv/generate-session
```

## 데이터 모델

### 플레이리스트 아이템
```typescript
interface PlaylistItem {
    video: string;        // HLS 스트림 URL
    audio?: string;       // 배경음악 URL
    subtitle?: string;    // 자막 파일 URL
    poster?: string;      // 포스터 이미지 URL
    title: string;        // 작품 제목
    artist: string;       // 작가명
    size: string;         // 크기 정보
    material: string;     // 재료 정보
    order: number;        // 정렬 순서
}
```

### 설정 데이터 (Android 앱)
```kotlin
data class TVConfig(
    val wsUrl: String,
    val companyName: String,
    val secretCode: String,
    val apiUrl: String
)
```

## 색상 시스템

### 브랜드 색상
```css
/* 주요 색상 */
--curizm-charcoal: #352B2B;    /* 배경색 */
--curizm-coral: #FF5935;       /* 액센트 색상 */
--curizm-cream: #FFFBE9;       /* 텍스트 색상 */
--curizm-forest: #3F5743;      /* 보조 색상 */

/* 변형 색상 */
--curizm-cream-dark: #F5F0D9;
--curizm-coral-light: #FF7A5C;
--curizm-forest-light: #4F6753;
--curizm-charcoal-light: #453A3A;
```

## 성능 최적화

### 웹 시스템
- **코드 스플리팅**: 페이지별 번들 분리
- **이미지 최적화**: WebP 형식 사용
- **캐싱**: Service Worker를 통한 오프라인 지원
- **WebSocket 연결 관리**: 자동 재연결 및 백오프

### Android TV 앱
- **ExoPlayer 최적화**: HLS 버퍼링 설정 조정
- **메모리 관리**: 비디오 전환 시 메모리 정리
- **UI 최적화**: ViewBinding 사용으로 성능 향상
- **네트워크 최적화**: OkHttp 연결 풀링

## 보안 고려사항

### 인증 및 권한
- **회사명/비밀번호**: 기본 인증 방식
- **WebSocket 인증**: 연결 시 자격 증명 검증
- **API 보안**: HTTPS/WSS 사용

### 데이터 보호
- **설정 저장**: Android SharedPreferences 암호화
- **네트워크 통신**: TLS 1.2+ 사용
- **로깅**: 민감한 정보 제외

## 모니터링 및 로깅

### 웹 시스템
- **브라우저 콘솔**: JavaScript 오류 및 디버그 정보
- **네트워크 탭**: API 요청/응답 모니터링
- **WebSocket 연결**: 실시간 연결 상태 확인

### Android TV 앱
- **Logcat**: Android Studio를 통한 로그 확인
- **크래시 리포팅**: Google Play Console 통합
- **성능 모니터링**: ExoPlayer 성능 메트릭

## 배포 및 유지보수

### 웹 시스템 배포
- **CDN**: 정적 자산 전역 배포
- **서버 배포**: API 서버 업데이트
- **도메인 관리**: SSL 인증서 갱신

### Android TV 앱 배포
- **Google Play Store**: 자동 업데이트 지원
- **버전 관리**: Semantic Versioning
- **A/B 테스트**: Google Play Console 기능 활용

## 확장성 고려사항

### 수평적 확장
- **로드 밸런싱**: 다중 서버 인스턴스
- **WebSocket 클러스터링**: Redis Adapter 사용
- **CDN 확장**: 글로벌 엣지 서버

### 수직적 확장
- **서버 리소스**: CPU/메모리 증설
- **데이터베이스**: 읽기 전용 복제본
- **캐싱**: Redis/Memcached 도입

---

*이 문서는 큐리즘 TV 시스템 v1.5 기준으로 작성되었으며, 시스템 업데이트에 따라 내용이 변경될 수 있습니다.*
