import java.util.*;

/**
 * 게임 상태(State)와 순수 함수형 전이(transition) 로직을 담당합니다.
 *
 * 모든 public 메서드는 기존 State를 변경하지 않고 새 State를 반환합니다.
 * (JS 원본의 불변 상태 패턴을 그대로 유지)
 * 스레드 안전성은 GameController에서 synchronized로 보장합니다.
 */
public class Game {

    // ── Stage (게임 단계) ──────────────────────────────────────────────────────
    public enum Stage { READY, PREFLOP, FLOP, TURN, RIVER, SHOWDOWN }

    // ── Player ────────────────────────────────────────────────────────────────
    public static class Player {
        public final String  id;
        public final String  name;
        public final String  style;    // "Tight Aggressive" 등 AI 스타일
        public final boolean isHuman;
        public int           stack;    // 보유 칩
        public List<Card>    hole;     // 홀 카드 2장
        public boolean       inHand;   // 현재 핸드 참여 여부

        public Player(String id, String name, String style, boolean isHuman, int stack) {
            this.id      = id;
            this.name    = name;
            this.style   = style;
            this.isHuman = isHuman;
            this.stack   = stack;
            this.hole    = new ArrayList<>();
            this.inHand  = true;
        }

        /** 얕은 복사 (Card는 불변이므로 hole 리스트만 새로 만들면 됨) */
        public Player copy() {
            Player p  = new Player(id, name, style, isHuman, stack);
            p.hole    = new ArrayList<>(hole);
            p.inHand  = inHand;
            return p;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    public static class State {
        public List<Player> players;
        public List<Card>   community;    // 공개된 커뮤니티 카드
        public int          pot;          // 현재 팟 크기
        public int          dealer;       // 딜러 버튼 인덱스
        public Stage        stage;
        public int          handNumber;
        public List<Card>   deck;         // 남은 덱
        public int          currentPlayer; // 현재 액션 차례 인덱스
        public boolean      waitingForHuman;
        public boolean      humanEnabled;

        // 로그 출력용 마지막 액션 정보 (매 State마다 최신 1건만 보존)
        public String lastAIName;
        public String lastAIStyle;
        public double lastAIWinRate;
        public String lastAIAction;
        public String lastHumanAction;
    }

    // ── 상수 ──────────────────────────────────────────────────────────────────
    private static final String[] STYLES = {
        "Tight Passive", "Tight Aggressive", "Loose Passive", "Loose Aggressive"
    };
    private static final int STARTING_STACK = 2000;

    // ── 공개 전이 함수 ────────────────────────────────────────────────────────

    /**
     * 플레이어를 생성하고 초기 State를 반환합니다.
     * @param withHuman true이면 6인 중 1인을 사람 플레이어로 설정
     */
    public static State createInitialState(boolean withHuman) {
        State  s   = new State();
        Random rng = new Random();

        s.players = new ArrayList<>();
        int aiCount = withHuman ? 5 : 6;
        for (int i = 0; i < aiCount; i++) {
            s.players.add(new Player(
                "ai-" + i, "AI_" + (i + 1),
                STYLES[rng.nextInt(STYLES.length)], false, STARTING_STACK
            ));
        }
        if (withHuman) {
            s.players.add(new Player("human", "You", "Human", true, STARTING_STACK));
        }

        s.community       = new ArrayList<>();
        s.pot             = 0;
        s.dealer          = 0;
        s.stage           = Stage.READY;
        s.handNumber      = 0;
        s.deck            = Deck.createDeck();
        s.currentPlayer   = 0;
        s.waitingForHuman = false;
        s.humanEnabled    = withHuman;
        return s;
    }

    /**
     * 새 핸드를 시작합니다. 덱을 새로 섞고 각 플레이어에게 홀 카드를 2장씩 배분합니다.
     * 스택이 0인 플레이어는 자동으로 제외(inHand = false)됩니다.
     */
    public static State startHand(State prev) {
        State s  = copyState(prev);
        s.deck   = Deck.createDeck();

        for (Player p : s.players) {
            p.hole.clear();
            if (p.stack > 0) {
                p.hole.add(s.deck.remove(s.deck.size() - 1));
                p.hole.add(s.deck.remove(s.deck.size() - 1));
                p.inHand = true;
            } else {
                p.inHand = false;
            }
        }

        s.community       = new ArrayList<>();
        s.pot             = 0;
        s.stage           = Stage.PREFLOP;
        s.handNumber      = prev.handNumber + 1;
        s.currentPlayer   = (prev.dealer + 1) % s.players.size();
        s.waitingForHuman = s.players.get(s.currentPlayer).isHuman;
        return s;
    }

    /**
     * 현재 Stage에서 다음 Stage로 진행합니다.
     * PREFLOP → FLOP(3장), FLOP → TURN(1장), TURN → RIVER(1장), RIVER → SHOWDOWN
     */
    public static State nextStage(State prev) {
        State s = copyState(prev);

        switch (s.stage) {
            case PREFLOP:
                s.community.add(s.deck.remove(s.deck.size() - 1));
                s.community.add(s.deck.remove(s.deck.size() - 1));
                s.community.add(s.deck.remove(s.deck.size() - 1));
                s.stage = Stage.FLOP;
                break;
            case FLOP:
                s.community.add(s.deck.remove(s.deck.size() - 1));
                s.stage = Stage.TURN;
                break;
            case TURN:
                s.community.add(s.deck.remove(s.deck.size() - 1));
                s.stage = Stage.RIVER;
                break;
            case RIVER:
                s.stage = Stage.SHOWDOWN;
                break;
            default:
                break;
        }
        return s;
    }

    /**
     * 현재 차례 AI 플레이어의 액션을 한 단계 처리합니다.
     * - 이미 폴드한 플레이어면 건너뜁니다.
     * - 사람 차례이면 waitingForHuman = true를 세우고 멈춥니다.
     * - AI이면 Monte Carlo 승률 계산 후 액션을 결정합니다.
     */
    public static State stepAI(State prev) {
        State  s   = copyState(prev);
        int    idx = s.currentPlayer;
        Player p   = s.players.get(idx);

        // 이미 폴드한 플레이어: 다음으로 넘김
        if (!p.inHand) {
            return advance(s, idx);
        }

        // 사람 차례: 대기
        if (p.isHuman) {
            s.waitingForHuman = true;
            return s;
        }

        // AI 액션 처리
        long   alive     = s.players.stream().filter(x -> x.inHand).count();
        int    opponents = (int) Math.max(0, alive - 1);
        double winRate   = AI.estimateWinRate(p.hole, s.community, opponents, 250);
        String action    = AI.aiDecide(p.style, winRate, 0, p.stack);

        if ("fold".equals(action)) {
            p.inHand = false;
        } else if ("raise".equals(action)) {
            int bet  = Math.min(40, p.stack);
            p.stack -= bet;
            s.pot   += bet;
        }

        // 로그 기록
        s.lastAIName    = p.name;
        s.lastAIStyle   = p.style;
        s.lastAIWinRate = winRate;
        s.lastAIAction  = action;

        return advance(s, idx);
    }

    /**
     * 사람 플레이어의 액션을 처리합니다.
     * @param type "fold" | "call" | "check" | "raise"
     */
    public static State humanAction(State prev, String type) {
        State  s   = copyState(prev);
        int    idx = s.currentPlayer;
        Player p   = s.players.get(idx);
        if (!p.isHuman) return s; // 안전장치: 사람 차례가 아니면 무시

        if ("fold".equals(type)) {
            p.inHand = false;
        } else if ("raise".equals(type)) {
            int bet  = Math.min(40, p.stack);
            p.stack -= bet;
            s.pot   += bet;
        }
        // call/check: 칩 변화 없음 (시연용 단순화)

        s.lastHumanAction = type;
        return advance(s, idx);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /** currentPlayer를 다음으로 넘기고 waitingForHuman을 갱신합니다. */
    private static State advance(State s, int idx) {
        int next = (idx + 1) % s.players.size();
        s.currentPlayer   = next;
        s.waitingForHuman = s.players.get(next).isHuman;
        return s;
    }

    /**
     * State 깊은 복사. Player 리스트와 Card 리스트를 새로 만듭니다.
     * Card 자체는 불변이므로 참조만 복사해도 안전합니다.
     * 로그 필드(lastAI*, lastHuman*)는 복사하지 않아 매 State마다 초기화됩니다.
     */
    private static State copyState(State prev) {
        State s = new State();
        s.players = new ArrayList<>(prev.players.size());
        for (Player p : prev.players) s.players.add(p.copy());
        s.community       = new ArrayList<>(prev.community);
        s.pot             = prev.pot;
        s.dealer          = prev.dealer;
        s.stage           = prev.stage;
        s.handNumber      = prev.handNumber;
        s.deck            = new ArrayList<>(prev.deck);
        s.currentPlayer   = prev.currentPlayer;
        s.waitingForHuman = prev.waitingForHuman;
        s.humanEnabled    = prev.humanEnabled;
        return s;
    }
}
