package com.texasholdem;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * JS의 useTexasHoldem.js에 대응하는 게임 컨트롤러입니다.
 *
 * - 게임 State를 보유하고, 모든 액션을 Game.*() 순수 함수로 위임합니다.
 * - 자동 진행 시 tick()이 반복 호출되며:
 *     1. 베팅 라운드가 끝났으면 → nextStage 또는 resolveShowdown
 *     2. 진행 중이면 → AI 한 명 액션 (stepAI)
 * - 백그라운드 스레드와 메인 스레드 간 state 접근은 synchronized로 보호합니다.
 */
public class GameController {

    private Game.State state;
    private boolean    isPlaying  = false;
    private int        speedMs    = 700;
    private boolean    omniscient = false; // 전지적 모드: 상대 카드 공개

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?>             timerTask;

    /**
     * 상태 변경 시 호출되는 콜백.
     * JavaFX → Platform.runLater(), Swing → SwingUtilities.invokeLater()로 감싸세요.
     */
    private final Consumer<Game.State> onStateChange;

    // ── 생성자 ────────────────────────────────────────────────────────────────

    public GameController(boolean withHuman, Consumer<Game.State> onStateChange) {
        this.state         = Game.createInitialState(withHuman);
        this.onStateChange = onStateChange;
        this.scheduler     = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-timer");
            t.setDaemon(true); // JVM 종료 시 자동으로 멈춤
            return t;
        });
    }

    // ── 액션 메서드 ───────────────────────────────────────────────────────────

    public synchronized void init(boolean withHuman) {
        stopTimer();
        state = Game.createInitialState(withHuman);
        notify(state);
    }

    public synchronized void startHand() {
        state = Game.startHand(state);
        notify(state);
    }

    public synchronized void nextStage() {
        state = Game.nextStage(state);
        notify(state);
    }

    public synchronized void step() {
        state = Game.stepAI(state);
        notify(state);
    }

    public synchronized void humanAction(String type) {
        state = Game.humanAction(state, type);
        notify(state);
        // 사람이 액션한 후 즉시 다음 턴 진행
        if (isPlaying) {
            stopTimer(); // 타이머 중지
            processTurn(); // 즉시 다음 턴 처리
        }
    }

    public synchronized void setPlaying(boolean playing) {
        this.isPlaying = playing;
        rescheduleTimer();
    }

    public synchronized void setSpeed(int ms) {
        this.speedMs = ms;
        if (isPlaying) rescheduleTimer();
    }

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
     * 현재 State 기반 로그 라인 목록을 반환합니다.
     */
    public synchronized List<String> getLog() {
        List<String> lines = new ArrayList<>();
        lines.add(LanguageManager.get("log.hand", 
            state.handNumber, state.stage, state.pot, state.currentBet));

        if (state.lastAIName != null) {
            lines.add(LanguageManager.get("log.ai",
                state.lastAIName, state.lastAIStyle,
                state.toCall, state.lastAIAction, state.lastAIWinRate));
        }
        if (state.lastHumanAction != null) {
            lines.add(LanguageManager.get("log.human", state.lastHumanAction));
        }
        if (state.lastEvent != null) {
            lines.add(LanguageManager.get("log.event", state.lastEvent));
        }
        return lines;
    }

    // ── 타이머 / 자동 진행 ────────────────────────────────────────────────────

    /**
     * 자동 진행의 핵심 로직입니다.
     * tick()과 humanAction()에서 공유합니다.
     */
    private synchronized void processTurn() {
        if (!isPlaying) return;
        if (state.stage == Game.Stage.READY) return;
        if (state.waitingForHuman) return;

        if (state.stage == Game.Stage.SHOWDOWN) {
            // 쇼다운 화면을 잠시 보여준 뒤 자동으로 멈춤
            isPlaying = false;
            stopTimer();
            return;
        }

        long activeCount = state.players.stream().filter(p -> p.inHand).count();

        if (Game.isBettingRoundOver(state)) {
            if (activeCount <= 1 || state.stage == Game.Stage.RIVER) {
                // 핸드 종료: 쇼다운 처리
                state = Game.resolveShowdown(state);
                notify(state);
                isPlaying = false;
                stopTimer();
            } else {
                // 다음 스테이지로 진행
                state = Game.nextStage(state);
                notify(state);
                // 새 스테이지에서 사람 차례면 타이머 정지
                if (state.waitingForHuman) stopTimer();
            }
        } else {
            // AI 한 명 액션
            state = Game.stepAI(state);
            notify(state);
            // 사람 차례가 됐으면 타이머 정지
            if (state.waitingForHuman) stopTimer();
        }
    }

    /**
     * 자동 진행의 핵심 tick입니다. speedMs 간격으로 호출됩니다.
     */
    private synchronized void tick() {
        processTurn();
    }

    /** 기존 타이머를 취소하고 조건에 맞으면 새 타이머를 등록합니다. */
    private void rescheduleTimer() {
        stopTimer();
        if (isPlaying && !state.waitingForHuman
                && state.stage != Game.Stage.READY
                && state.stage != Game.Stage.SHOWDOWN) {
            timerTask = scheduler.scheduleAtFixedRate(
                this::tick, speedMs, speedMs, TimeUnit.MILLISECONDS
            );
        }
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    private void notify(Game.State s) {
        if (onStateChange != null) onStateChange.accept(s);
    }

    // ── 종료 ──────────────────────────────────────────────────────────────────

    /** 앱 종료 시 반드시 호출하세요. 스레드 풀을 정리합니다. */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
