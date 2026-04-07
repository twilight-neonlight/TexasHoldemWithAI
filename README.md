# TexasHoldemWithAI

Java로 구현한 텍사스 홀덤 포커 게임 엔진입니다.  
몬테카를로 시뮬레이션 기반 승률 추정과 성향별 AI 플레이어를 포함합니다.

---

## 프로젝트 구조

```
TexasHoldemWithAI/
└── Main/
    └── src/
        ├── Card.java           # 카드 단일 객체 (Rank, Suit enum 포함)
        ├── Deck.java           # 52장 덱 생성 및 셔플
        ├── HandEval.java       # 핸드 평가 (족보 판정, 7장→최고 5장)
        ├── AI.java             # 승률 추정 (몬테카를로) + 액션 결정
        ├── Game.java           # 게임 상태 및 전이 로직 (순수 함수형)
        └── GameController.java # 게임 흐름 제어, 자동 진행 타이머
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

| category | 족보 |
|---|---|
| 9 | Straight Flush |
| 8 | Four of a Kind |
| 7 | Full House |
| 6 | Flush |
| 5 | Straight |
| 4 | Three of a Kind |
| 3 | Two Pair |
| 2 | One Pair |
| 1 | High Card |

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

```bash
cd Main/src
javac *.java
```

`GameController`를 직접 사용하거나, `main()` 진입점을 별도로 작성해 실행합니다.

```java
GameController gc = new GameController(true, state -> {
    // 상태 변경 시 UI 갱신 (JavaFX: Platform.runLater, Swing: SwingUtilities.invokeLater)
    System.out.println(state.stage + " | pot=" + state.pot);
});

gc.startHand();
gc.setPlaying(true);  // 자동 진행
// gc.humanAction("call");  // 사람 차례에 호출
// gc.shutdown();           // 종료 시
```

---

## 향후 계획
- JavaFX / Swing UI 연동
- 사이드팟 처리 (올인 복수 플레이어)
- 동점(split pot) 처리
- 블라인드 레벨 상승 구조
