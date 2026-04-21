# Phase 2 설계 메모 — AccessibilityService 기반 카카오톡 대화 수신

> 작성: 2026-04-21 / 상태: 설계 초안

---

## 개요

Phase 1은 `PROCESS_TEXT` Intent로 사용자가 수동 선택한 텍스트를 받는다.
Phase 2에서는 `AccessibilityService`로 카카오톡 알림/화면을 자동 파싱해 대화 컨텍스트를 수집한다.

---

## MessengerAccessibilityService 구조

```kotlin
class MessengerAccessibilityService : AccessibilityService() {

    private val throttleMs = 500L
    private var lastEventTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 배터리 throttle: 500ms 이내 이벤트 무시
        val now = System.currentTimeMillis()
        if (now - lastEventTime < throttleMs) return
        lastEventTime = now

        if (event.packageName != "com.kakao.talk") return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val root = rootInActiveWindow ?: return
        KakaoTalkParser.parse(root)?.let { turn ->
            ConversationBuffer.add(turn)
        }
    }

    override fun onInterrupt() {}
}
```

---

## KakaoTalkParser 설계

```kotlin
object KakaoTalkParser {
    // assets/parsers/kakaotalk_selectors.json 에서 뷰 ID 로드
    fun parse(root: AccessibilityNodeInfo): ConversationTurn? {
        val bubbles = root.findAccessibilityNodeInfosByViewId(
            "com.kakao.talk:id/message_body"
        )
        if (bubbles.isEmpty()) return null

        val lastBubble = bubbles.last()
        val sender = root.findAccessibilityNodeInfosByViewId(
            "com.kakao.talk:id/sender_name"
        ).lastOrNull()?.text?.toString() ?: "상대방"

        return ConversationTurn(
            sender = sender,
            message = lastBubble.text?.toString() ?: return null,
            timestamp = System.currentTimeMillis()
        )
    }
}
```

---

## ConversationTurn 데이터 모델

```kotlin
data class ConversationTurn(
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isMe: Boolean = false
)
```

---

## 메모리 전용 저장 전략

- DB/파일 저장 없음 — 개인정보 최소 수집 원칙
- `ConversationBuffer`: in-memory `ArrayDeque<ConversationTurn>(maxSize = 20)`
- 앱 종료 시 자동 소멸
- 최대 20턴만 유지 (오래된 것 자동 제거)

```kotlin
object ConversationBuffer {
    private const val MAX_SIZE = 20
    private val buffer = ArrayDeque<ConversationTurn>(MAX_SIZE)

    fun add(turn: ConversationTurn) {
        if (buffer.size >= MAX_SIZE) buffer.removeFirst()
        buffer.addLast(turn)
    }

    fun getContext(last: Int = 5): String =
        buffer.takeLast(last).joinToString("\n") { "${it.sender}: ${it.message}" }

    fun clear() = buffer.clear()
}
```

---

## 배터리 throttle 500ms 구현 방법

AccessibilityService는 이벤트를 매우 빈번하게 수신한다.
배터리 최적화를 위해 두 가지 레벨로 throttle을 적용한다:

1. **이벤트 레벨** (위 코드 참조): `System.currentTimeMillis()` 차이 < 500ms → skip
2. **파싱 레벨**: 동일 메시지 텍스트가 연속으로 오면 중복 skip

```kotlin
private var lastParsedMessage = ""

fun parse(root: AccessibilityNodeInfo): ConversationTurn? {
    // ... 파싱 로직 ...
    if (turn.message == lastParsedMessage) return null  // 중복 skip
    lastParsedMessage = turn.message
    return turn
}
```

---

## AndroidManifest.xml 추가 항목 (Phase 2)

```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />

<service
    android:name=".accessibility.MessengerAccessibilityService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

---

## res/xml/accessibility_service_config.xml (Phase 2)

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowContentChanged|typeNotificationStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="500"
    android:packageNames="com.kakao.talk" />
```

---

## 검증 방법

1. **단위 테스트**: `MockAccessibilityNodeInfo` 목업으로 KakaoTalkParser 파싱 검증
2. **UI 테스트**: 에뮬레이터에서 카카오톡 앱 실행 → Espresso AccessibilityChecks
3. **통합 테스트**: 실기기에서 채팅방 진입 → ConversationBuffer 내용 Logcat 확인
4. **배터리 테스트**: `adb shell dumpsys batterystats` 로 서비스 wake lock 측정

---

## 주의 사항

- Google Play 정책상 AccessibilityService는 "장애인 보조 목적 또는 사용자의 직접적 요청"으로만 허용
- 스토어 등록 시 사용 목적을 명확히 기재해야 함
- 카카오톡 버전 업데이트 시 뷰 ID 변경 가능 → `kakaotalk_selectors.json` 원격 업데이트 전략 검토 필요
