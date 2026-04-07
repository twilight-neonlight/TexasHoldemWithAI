import java.util.*;

/**
 * 게임 상태(State)와 순수 함수형 전이(transition) 로직을 담당합니다.
 *
 * 모든 public 메서드는 기존 State를 변경하지 않고 새 State를 반환합니다.
 * 스레드 안전성은 GameController에서 synchronized로 보장합니다.
 */
public class Game {

    // ── Stage ─────────────────────────────────────────────────────────────────
    public enum Stage { READY, PREFLOP, FLOP, TURN, RIVER, SHOWDOWN }

    // ── Player ────────────────────────────────────────────────────────────────
    public static class Player {
        public final String  id;
        public final String  name;
        public final String  style;    // "Tight Aggressive" 등 AI 성향
        public final boolean isHuman;
        public int           stack;
        public List<Card>    hole;
        public boolean       inHand;
        public int           streetBet; // 이번 스트릿에 베팅한 누적 금액
        public boolean       hasActed;  // 이번 스트릿에 자발적으로 액션했는지
                                        // (블라인드 포스트는 자발적 액션 아님)

        public Player(String id, String name, String style, boolean isHuman, int stack) {
            this.id        = id;
            this.name      = name;
            this.style     = style;
            this.isHuman   = isHuman;
            this.stack     = stack;
            this.hole      = new ArrayList<>();
            this.inHand    = true;
            this.streetBet = 0;
            this.hasActed  = false;
        }

        /** Card는 불변이므로 hole 리스트만 새로 만들면 됩니다. */
        public Player copy() {
            Player p     = new Player(id, name, style, isHuman, stack);
            p.hole       = new ArrayList<>(hole);
            p.inHand     = inHand;
            p.streetBet  = streetBet;
            p.hasActed   = hasActed;
            return p;
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────
    public static class State {
        public List<Player> players;
        public List<Card>   community;
        public int          pot;
        public int          dealer;       // 딜러 버튼 인덱스
        public Stage        stage;
        public int          handNumber;
        public List<Card>   deck;
        public int          currentPlayer;
        public boolean      waitingForHuman;
        public boolean      humanEnabled;

        // 베팅 관련
        public int  currentBet;    // 이번 스트릿 최고 베팅액
        public int  smallBlindAmt;
        public int  bigBlindAmt;
        public int  smallBlindIdx; // 이번 핸드의 SB 플레이어 인덱스
        public int  bigBlindIdx;   // 이번 핸드의 BB 플레이어 인덱스
        public int  toCall;        // 현재 플레이어가 콜하려면 필요한 금액 (UI 표시용)

        // 로그용 마지막 이벤트 (매 State마다 최신 1건)
        public String lastAIName;
        public String lastAIStyle;
        public double lastAIWinRate;
        public String lastAIAction;
        public String lastHumanAction;
        public String lastEvent; // "AI_1 wins with Full House" 등 핸드 결과
    }

    // ── 상수 ──────────────────────────────────────────────────────────────────
    private static final String[] STYLES = {
        "Tight Passive", "Tight Aggressive", "Loose Passive", "Loose Aggressive"
    };
    private static final int STARTING_STACK = 2000;
    private static final int SMALL_BLIND    = 10;
    private static final int BIG_BLIND      = 20;

    // ── 공개 전이 함수 ────────────────────────────────────────────────────────

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
        s.smallBlindAmt   = SMALL_BLIND;
        s.bigBlindAmt     = BIG_BLIND;
        s.currentBet      = 0;
        s.toCall          = 0;
        return s;
    }

    /**
     * 새 핸드를 시작합니다.
     * 딜러 이동 → 카드 배분 → 블라인드 포스트 → 첫 액션 플레이어 설정
     */
    public static State startHand(State prev) {
        State s = copyState(prev);
        s.deck  = Deck.createDeck();
        s.pot   = 0;

        s.dealer = nextActiveIdx(prev.players, prev.dealer);

        for (Player p : s.players) {
            p.hole.clear();
            p.streetBet = 0;
            p.hasActed  = false;
            p.inHand    = p.stack > 0;
            if (p.inHand) {
                p.hole.add(s.deck.remove(s.deck.size() - 1));
                p.hole.add(s.deck.remove(s.deck.size() - 1));
            }
        }

        s.smallBlindIdx = nextActiveIdx(s.players, s.dealer);
        s.bigBlindIdx   = nextActiveIdx(s.players, s.smallBlindIdx);
        postBlind(s.players.get(s.smallBlindIdx), s.smallBlindAmt, s);
        postBlind(s.players.get(s.bigBlindIdx),   s.bigBlindAmt,   s);

        s.currentBet      = s.bigBlindAmt;
        s.community       = new ArrayList<>();
        s.stage           = Stage.PREFLOP;
        s.handNumber      = prev.handNumber + 1;
        s.lastEvent       = null;

        // 프리플랍 첫 액션: BB 다음 플레이어
        s.currentPlayer   = nextActiveIdx(s.players, s.bigBlindIdx);
        Player cur        = s.players.get(s.currentPlayer);
        s.toCall          = Math.max(0, s.currentBet - cur.streetBet);
        s.waitingForHuman = cur.isHuman;
        return s;
    }

    /**
     * 다음 스테이지로 진행하면서 커뮤니티 카드를 추가하고 베팅을 초기화합니다.
     * RIVER 종료 시에는 Stage만 SHOWDOWN으로 변경합니다.
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
                return s;
            default:
                return s;
        }

        // 새 스트릿 베팅 초기화
        s.currentBet = 0;
        for (Player p : s.players) {
            p.streetBet = 0;
            p.hasActed  = false;
        }

        // 포스트플랍 첫 액션: 딜러 다음 active 플레이어
        s.currentPlayer   = nextActiveIdx(s.players, s.dealer);
        Player cur        = s.players.get(s.currentPlayer);
        s.toCall          = 0;
        s.waitingForHuman = cur.isHuman;
        return s;
    }

    /**
     * 현재 차례 AI 플레이어의 액션을 처리합니다.
     * 폴드한 플레이어는 건너뛰고, 사람 차례면 대기합니다.
     */
    public static State stepAI(State prev) {
        State  s   = copyState(prev);
        int    idx = s.currentPlayer;
        Player p   = s.players.get(idx);

        if (!p.inHand) return advance(s, idx);
        if (p.isHuman) { s.waitingForHuman = true; return s; }

        long   alive     = s.players.stream().filter(x -> x.inHand).count();
        int    opponents = (int) Math.max(0, alive - 1);
        int    toCall    = Math.max(0, s.currentBet - p.streetBet);

        double winRate = AI.estimateWinRate(p.hole, s.community, opponents, 250);
        String action  = AI.aiDecide(p.style, winRate, toCall, p.stack);

        applyAction(s, p, action, toCall);

        s.lastAIName    = p.name;
        s.lastAIStyle   = p.style;
        s.lastAIWinRate = winRate;
        s.lastAIAction  = action;

        return advance(s, idx);
    }

    /** 사람 플레이어의 액션("fold"|"call"|"check"|"raise")을 처리합니다. */
    public static State humanAction(State prev, String type) {
        State  s   = copyState(prev);
        int    idx = s.currentPlayer;
        Player p   = s.players.get(idx);
        if (!p.isHuman) return s;

        int toCall = Math.max(0, s.currentBet - p.streetBet);
        applyAction(s, p, type, toCall);
        s.lastHumanAction = type;

        return advance(s, idx);
    }

    /**
     * 현재 베팅 라운드가 끝났는지 확인합니다.
     *
     * 종료 조건:
     *   - active 플레이어 1명 이하 (모두 폴드)
     *   - 모든 active 플레이어가 액션했고 스택 있는 플레이어는 currentBet과 streetBet이 동일
     *     (올인 플레이어는 streetBet != currentBet이어도 허용)
     */
    public static boolean isBettingRoundOver(State s) {
        if (s.stage == Stage.READY || s.stage == Stage.SHOWDOWN) return false;

        List<Player> active = new ArrayList<>();
        for (Player p : s.players) if (p.inHand) active.add(p);

        if (active.size() <= 1) return true;

        for (Player p : active) {
            if (!p.hasActed) return false;
            if (p.stack > 0 && p.streetBet != s.currentBet) return false;
        }
        return true;
    }

    /**
     * 쇼다운 처리: 남은 플레이어 중 최강 핸드 소유자에게 팟을 줍니다.
     * 1명만 남으면(모두 폴드) 해당 플레이어가 받습니다.
     */
    public static State resolveShowdown(State prev) {
        State s = copyState(prev);
        s.stage = Stage.SHOWDOWN;

        List<Player> active = new ArrayList<>();
        for (Player p : s.players) if (p.inHand) active.add(p);

        if (active.isEmpty()) return s;

        Player winner;
        String winDesc;

        if (active.size() == 1) {
            winner  = active.get(0);
            winDesc = winner.name + " wins (all folded)";
        } else {
            winner = active.get(0);
            int[] bestScore = HandEval.eval7Cards(merged(winner.hole, s.community));

            for (int i = 1; i < active.size(); i++) {
                Player p     = active.get(i);
                int[]  score = HandEval.eval7Cards(merged(p.hole, s.community));
                if (HandEval.compareHands(score, bestScore) > 0) {
                    winner    = p;
                    bestScore = score;
                }
            }
            winDesc = winner.name + " wins with " + HandEval.handName(bestScore);
        }

        winner.stack += s.pot;
        s.pot         = 0;
        s.lastEvent   = winDesc;
        return s;
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /**
     * 액션을 State와 Player에 반영합니다.
     *
     * raise: 최소 레이즈 = toCall + 1BB
     *   레이즈 후 다른 active 플레이어의 hasActed를 false로 리셋해
     *   레이즈에 반응할 기회를 다시 부여합니다.
     */
    private static void applyAction(State s, Player p, String action, int toCall) {
        switch (action) {
            case "fold":
                p.inHand = false;
                break;

            case "call":
                int callAmt = Math.min(toCall, p.stack);
                p.stack    -= callAmt;
                p.streetBet += callAmt;
                s.pot       += callAmt;
                break;

            case "raise":
                // Pot-size raise: 콜 금액 + 콜 후 팟 크기
                // 예) pot=100, toCall=20 → 추가 베팅 = 20 + (100+20) = 140
                int potSizeRaise = toCall + (s.pot + toCall);
                potSizeRaise     = Math.max(potSizeRaise, s.bigBlindAmt); // 최소 1BB 보장
                potSizeRaise     = Math.min(potSizeRaise, p.stack);       // 스택 초과 불가 (올인)
                p.stack         -= potSizeRaise;
                p.streetBet     += potSizeRaise;
                s.pot           += potSizeRaise;
                s.currentBet     = p.streetBet;
                // 레이즈에 반응할 기회를 다른 active 플레이어에게 다시 부여
                for (Player other : s.players) {
                    if (other != p && other.inHand) other.hasActed = false;
                }
                break;

            case "check":
                // toCall == 0일 때만 유효. 칩 변화 없음.
                break;
        }
        p.hasActed = true;
    }

    /**
     * 블라인드 강제 베팅. hasActed는 false 유지
     * (BB는 레이즈 없으면 check 옵션을 가집니다).
     */
    private static void postBlind(Player p, int amount, State s) {
        int actual   = Math.min(amount, p.stack);
        p.stack     -= actual;
        p.streetBet  = actual;
        s.pot       += actual;
    }

    /** currentPlayer를 idx 다음 active 플레이어로 이동합니다. */
    private static State advance(State s, int idx) {
        int    next = nextActiveIdx(s.players, idx);
        Player cur  = s.players.get(next);
        s.currentPlayer   = next;
        s.toCall          = Math.max(0, s.currentBet - cur.streetBet);
        s.waitingForHuman = cur.isHuman && cur.inHand;
        return s;
    }

    /**
     * from 이후 첫 번째 inHand 플레이어의 인덱스를 반환합니다.
     * 전원 폴드 시 fallback으로 (from+1)%n을 반환합니다.
     */
    private static int nextActiveIdx(List<Player> players, int from) {
        int n = players.size();
        for (int i = 1; i <= n; i++) {
            int idx = (from + i) % n;
            if (players.get(idx).inHand) return idx;
        }
        return (from + 1) % n;
    }

    private static List<Card> merged(List<Card> a, List<Card> b) {
        List<Card> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }

    private static State copyState(State prev) {
        State s           = new State();
        s.players         = new ArrayList<>(prev.players.size());
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
        s.currentBet      = prev.currentBet;
        s.smallBlindAmt   = prev.smallBlindAmt;
        s.bigBlindAmt     = prev.bigBlindAmt;
        s.smallBlindIdx   = prev.smallBlindIdx;
        s.bigBlindIdx     = prev.bigBlindIdx;
        s.toCall          = prev.toCall;
        return s;
    }
}
