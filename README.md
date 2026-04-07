# TexasHoldemWithAI

Java로 구현한 텍사스 홀덤 포커 게임 엔진입니다.  
몬테카를로 시뮬레이션 기반 승률 추정과 성향별 AI 플레이어를 포함합니다.

---

## 프로젝트 구조

```
TexasHoldemWithAI/
└── Main/
    └── src/main/java/com/texasholdem/
        ├── MainApplication.java # Swing UI 메인 클래스 (다국어 지원)
        ├── GameController.java  # 게임 흐름 제어, 자동 진행 타이머
        ├── LanguageManager.java # 국제화(i18n) 관리 (한국어/영어)
        ├── Game.java            # 게임 상태 및 전이 로직 (순수 함수형)
        ├── AI.java              # 승률 추정 (몬테카를로) + 액션 결정
        ├── HandEval.java        # 핸드 평가 (족보 판정, 7장→최고 5장)
        ├── Card.java            # 카드 단일 객체 (Rank, Suit enum 포함)
        └── Deck.java            # 52장 덱 생성 및 셔플
```

---

## 주요 기능

### 카드 / 덱
- `Card`: Rank(2~A)와 Suit(s/h/d/c)로 구성된 불변 객체
  - `equals()` / `hashCode()` 구현 → `Set<Card>` 사용 가능
  - `toString()` → `"As"` (파싱/동일성 기준), `format()` → `"A♠"` (UI 표시용)
- `Deck.createDeck()`: 매번 새로 섞인 52장 리스트 반환

### 핸드 평가 (`HandEval`)
- `eval5Cards(List<Card>)` : 5장 평가 → `int[]` 점수 배열 반환
- `eval7Cards(List<Card>)` : 7장 중 C(7,5)=21가지 조합을 전부 평가해 최고 점수 반환
- `compareHands(int[], int[])` : 두 점수 배열 비교 (양수/0/음수)
- `handName(int[])` : 족보 이름 문자열 반환

점수 배열 형식: `[category, kicker1, kicker2, ...]`  
category 값이 클수록 강한 족보:

| category | 족보 | 예시 |
|---|---|---|
| 10 | Royal Straight Flush | ♠️T ♠️J ♠️Q ♠️K ♠️A |
| 9 | Straight Flush  | ♠️9 ♥️T ♦️J ♣️Q ♠️K |
| 8 | Four of a Kind  | ♣️A ♥️A ♦️A ♠️A ♣️K |
| 7 | Full House      | ♣️K ♥️K ♦️K ♣️Q ♥️Q |
| 6 | Flush           | ♥️2 ♥️5 ♥️8 ♥️9 ♥️K |
| 5 | Straight        | ♠️5 ♥️6 ♦️7 ♣️8 ♠️9 |
| 4 | Three of a Kind | ♠️J ♥️J ♦️J ♣️Q ♠️7 |
| 3 | Two Pair        | ♠️A ♥️A ♦️K ♣️K ♠️3 |
| 2 | One Pair        | ♠️T ♥️T ♦️2 ♣️5 ♠️9 |
| 1 | High Card       | ♠️2 ♥️5 ♦️8 ♣️J ♠️A |

### AI (`AI`)

#### 승률 추정 — 몬테카를로 시뮬레이션
```
estimateWinRate(hole, community, opponents, trials)
```
매 trial마다:
1. 남은 덱을 섞음
2. 알고 있는 카드(hole + community) 제거
3. 보드를 5장으로 무작위 완성
4. 상대방에게 무작위 2장씩 배분
5. 내 핸드가 모든 상대 중 최강이면 승리 카운트

반환값: `wins / trials` (0.0 ~ 1.0)

#### 액션 결정 — 성향 기반
```
aiDecide(style, winRate, toCall, stack)
```

| 성향 | 폴드 임계값 | 레이즈 임계값 |
|---|---|---|
| Tight Passive  | 38% 미만 | 70% 초과 |
| Tight Aggressive | 38% 미만 | 62% 초과 |
| Loose Passive  | 30% 미만 | 70% 초과 |
| Loose Aggressive | 30% 미만 | 62% 초과 |

- `toCall > 0` 상황에서만 폴드 발생
- Aggressive 성향은 check 상황(toCall=0)에서도 승률 55% 초과 시 레이즈

### 게임 로직 (`Game`)

#### 베팅 구조
- Small Blind: 10칩, Big Blind: 20칩
- **Pot-size raise**: 레이즈 금액 = `toCall + (pot + toCall)`
  - 예) pot=100, toCall=20 → 추가 투입 120칩
  - 올인 시 스택 한도 내로 자동 조정
- 레이즈 후 다른 active 플레이어의 `hasActed`를 리셋 → 재액션 기회 부여

#### 게임 흐름
```
createInitialState → startHand → [PREFLOP 베팅] → nextStage
→ [FLOP 베팅] → nextStage → [TURN 베팅] → nextStage
→ [RIVER 베팅] → resolveShowdown
```

- 모든 전이 함수는 기존 State를 변경하지 않고 새 State를 반환 (순수 함수)
- 스택이 0인 플레이어는 자동으로 핸드에서 제외
- `isBettingRoundOver()`: 모든 active 플레이어가 액션했고 베팅액이 동일할 때 true
- `resolveShowdown()`: 최강 핸드 소유자에게 팟 이전, 동점 시 먼저 발견된 플레이어 승

### 게임 컨트롤러 (`GameController`)
- `setPlaying(true)` 로 자동 진행 시작 (기본 700ms 간격)
- `setSpeed(ms)` 로 진행 속도 조절
- `humanAction("fold"|"call"|"check"|"raise")` 로 사람 플레이어 액션 전달
- 베팅 라운드 종료 시 자동으로 다음 스테이지 또는 쇼다운 처리
- 백그라운드 타이머와 메인 스레드 간 `synchronized` 보호
- 앱 종료 시 `shutdown()` 호출 필수

---

## 컴파일 및 실행

### JAR 파일 생성
```bash
cd Main/src/main/java
javac com/texasholdem/*.java
jar cfe TexasHoldem.jar com.texasholdem.MainApplication com/texasholdem/*.class
```

### 실행
```bash
java -jar TexasHoldem.jar
```

Swing UI가 실행되며, Texas Holdem 게임을 플레이할 수 있습니다.

### 수동 실행 (개발용)
```bash
cd Main/src/main/java
javac com/texasholdem/*.java
java com.texasholdem.MainApplication
```

---

## UI 기능

### 메인 화면
- **테이블 중앙**: 팟 금액과 커뮤니티 카드 표시
- **왼쪽 패널**: 플레이어 정보 (이름, 칩, 베팅액, 포지션, 카드)
- **오른쪽 패널**: 게임 컨트롤 버튼과 설정
- **상단**: 게임 제목과 **언어 선택 드롭다운** (한국어/English)
- **하단**: 액션 로그와 사람 액션 버튼

### 컨트롤 버튼
- **Play/Pause**: 자동 진행 시작/정지
- **Step**: 한 플레이어만 진행
- **Reset**: 새 플레이어로 초기화
- **Start Hand**: 카드 배분 및 게임 시작
- **Next Stage**: 다음 베팅 라운드 진행
- **Speed 슬라이더**: AI 진행 속도 조절
- **Omniscient Mode**: 모든 카드 공개 (디버그용)
- **Help**: 게임 사용법 표시

### 다국어 지원 (Internationalization)
- **언어 선택**: 상단 헤더의 Language 드롭다운에서 선택
- **지원 언어**: 한국어 (한국어), English (English)
- **동적 업데이트**: 언어 변경 시 모든 UI 레이블, 툴팁, 액션 로그가 실시간으로 업데이트
- **LanguageManager**: 중앙화된 i18n 관리 클래스
  - 50+ 번역 키 포함 (버튼, 라벨, 툴팁, 게임 로그 템플릿)
  - `LanguageManager.get(key)` / `LanguageManager.get(key, args...)` 메서드로 활용

### 사람 플레이
- 차례가 되면 하단 액션 버튼 활성화
- **Fold**: 핸드 포기
- **Check**: 베팅 없이 턴 넘김
- **Call**: 현재 베팅액 맞춤
- **Raise**: 베팅액 올림

### 정보 표시
- **턴 플레이어**: → 화살표로 현재 차례 표시
- **포지션**: D(딜러), SB(스몰 블라인드), BB(빅 블라인드)
- **베팅액**: (bet: 금액)으로 이번 라운드 베팅 표시
- **카드**: 유저는 항상 자신의 카드 확인 가능

---

## 게임 컨트롤러 (`GameController`)
- `setPlaying(true)` 로 자동 진행 시작 (기본 700ms 간격)
- `setSpeed(ms)` 로 진행 속도 조절
- `humanAction("fold"|"call"|"check"|"raise")` 로 사람 플레이어 액션 전달
- 베팅 라운드 종료 시 자동으로 다음 스테이지 또는 쇼다운 처리
- 백그라운드 타이머와 메인 스레드 간 `synchronized` 보호
- 앱 종료 시 `shutdown()` 호출 필수

---

## 향후 계획
- JavaFX UI로 업그레이드 (더 풍부한 그래픽)
- 사이드팟 처리 (올인 복수 플레이어)
- 동점(split pot) 처리
- 블라인드 레벨 상승 구조
- 네트워크 멀티플레이 지원
