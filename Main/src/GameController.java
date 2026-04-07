import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * JS의 useTexasHoldem.js에 대응하는 게임 컨트롤러입니다.
 *
 * - 게임 State를 보유하고, 모든 액션을 Game.*() 순수 함수로 위임합니다.
 * - ScheduledExecutorService로 자동 진행 타이머를 관리합니다.
 * - 백그라운드 스레드(타이머)와 메인 스레드가 state를 공유하므로,
 *   state를 읽거나 쓰는 메서드는 모두 synchronized로 보호합니다.
 *
 * 사용 예:
 *   GameController gc = new GameController(false, s -> renderUI(s));
 *   gc.startHand();
 *   gc.setPlaying(true); // 자동 진행 시작
 *   ...
 *   gc.shutdown();       // 앱 종료 시 반드시 호출
 */
public class GameController {

    private Game.State state;
    private boolean    isPlaying  = false;
    private int        speedMs    = 700;   // 자동 진행 간격 (밀리초)
    private boolean    omniscient = false; // 전지적 모드 (상대 카드 공개)

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?>             timerTask;

    /**
     * 상태가 바뀔 때마다 호출되는 콜백.
     * JavaFX라면 Platform.runLater(), Swing이라면 SwingUtilities.invokeLater()로 감싸세요.
     */
    private final Consumer<Game.State> onStateChange;

    // ── 생성자 ────────────────────────────────────────────────────────────────

    /**
     * @param withHuman     사람 플레이어 포함 여부
     * @param onStateChange 상태 변경 콜백 (null 허용)
     */
    public GameController(boolean withHuman, Consumer<Game.State> onStateChange) {
        this.state         = Game.createInitialState(withHuman);
        this.onStateChange = onStateChange;
        this.scheduler     = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-timer");
            t.setDaemon(true); // JVM 종료 시 자동으로 멈춤
            return t;
        });
    }

    // ── 액션 메서드 (모두 synchronized) ──────────────────────────────────────

    /** 게임을 초기화합니다. 진행 중인 타이머도 중지됩니다. */
    public synchronized void init(boolean withHuman) {
        state = Game.createInitialState(withHuman);
        notifyChange();
    }

    /** 새 핸드를 시작합니다. */
    public synchronized void startHand() {
        state = Game.startHand(state);
        notifyChange();
    }

    /** 다음 스테이지(플랍/턴/리버/쇼다운)로 진행합니다. */
    public synchronized void nextStage() {
        state = Game.nextStage(state);
        notifyChange();
    }

    /** AI 한 명의 액션을 처리합니다. 자동 진행 타이머도 이 메서드를 사용합니다. */
    public synchronized void step() {
        state = Game.stepAI(state);
        notifyChange();
    }

    /** 사람 플레이어의 액션을 처리합니다. */
    public synchronized void humanAction(String type) {
        state = Game.humanAction(state, type);
        notifyChange();
    }

    /**
     * 자동 진행을 켜거나 끕니다.
     * true이면 speedMs 간격으로 step()을 자동 호출합니다.
     */
    public synchronized void setPlaying(boolean playing) {
        this.isPlaying = playing;
        rescheduleTimer();
    }

    /** 자동 진행 간격을 변경합니다 (밀리초). */
    public synchronized void setSpeed(int ms) {
        this.speedMs = ms;
        if (isPlaying) rescheduleTimer();
    }

    /** 전지적 모드를 토글합니다 (상대 카드 공개 여부). */
    public synchronized void toggleOmniscient() {
        this.omniscient = !this.omniscient;
    }

    // ── 상태 접근 ─────────────────────────────────────────────────────────────

    public synchronized Game.State getState()  { return state; }
    public synchronized boolean isOmniscient() { return omniscient; }
    public synchronized boolean isPlaying()    { return isPlaying; }
    public synchronized int     getSpeedMs()   { return speedMs; }

    // ── 로그 생성 ─────────────────────────────────────────────────────────────

    /**
     * 현재 상태를 기반으로 액션 로그 문자열 목록을 반환합니다.
     * JS의 useMemo(() => [...], [state]) 블록에 해당합니다.
     */
    public synchronized List<String> getLog() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format("Hand #%d | Stage: %s | Pot: %d",
            state.handNumber, state.stage, state.pot));

        if (state.lastAIName != null) {
            lines.add(String.format("[AI] %s (%s) → %s (wr≈%.2f)",
                state.lastAIName, state.lastAIStyle,
                state.lastAIAction, state.lastAIWinRate));
        }
        if (state.lastHumanAction != null) {
            lines.add("[You] → " + state.lastHumanAction);
        }
        return lines;
    }

    // ── 타이머 관리 (내부) ────────────────────────────────────────────────────

    /**
     * 기존 타이머를 취소하고, 재생 중이고 사람 차례가 아니면 새 타이머를 등록합니다.
     * 반드시 synchronized 컨텍스트 안에서 호출해야 합니다.
     */
    private void rescheduleTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
        if (isPlaying && !state.waitingForHuman) {
            timerTask = scheduler.scheduleAtFixedRate(
                this::step,          // step() 내부에서도 synchronized 획득
                speedMs, speedMs, TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * state 변경 후 공통으로 호출합니다.
     * 콜백을 실행하고 타이머 상태를 재조정합니다.
     */
    private void notifyChange() {
        if (onStateChange != null) onStateChange.accept(state);
        if (isPlaying) rescheduleTimer();
    }

    // ── 종료 ──────────────────────────────────────────────────────────────────

    /**
     * 스레드 풀을 정리합니다. 앱 종료 시 반드시 호출하세요.
     * 호출하지 않으면 타이머 스레드가 JVM 종료를 막을 수 있습니다.
     * (daemon 스레드로 설정했으므로 보통은 자동 종료되지만 명시적 호출이 안전합니다.)
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
