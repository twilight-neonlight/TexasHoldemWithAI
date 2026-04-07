import java.util.*;

/**
 * AI 플레이어의 승률 추정과 액션 결정 로직입니다.
 */
public class AI {

    /**
     * 몬테카를로 시뮬레이션으로 승률을 추정합니다.
     *
     * 매 trial마다 남은 덱을 섞어 보드와 상대 핸드를 무작위로 완성하고,
     * 내 핸드가 가장 강한 상대보다 같거나 강하면 승리로 카운트합니다.
     *
     * @param hole      내 홀 카드 2장
     * @param community 현재 공개된 커뮤니티 카드 (0~5장)
     * @param opponents 상대방 수
     * @param trials    시뮬레이션 횟수 (많을수록 정확, 느림)
     * @return 0.0 ~ 1.0 사이의 승률
     */
    public static double estimateWinRate(List<Card> hole, List<Card> community,
                                         int opponents, int trials) {
        int wins = 0;

        for (int t = 0; t < trials; t++) {
            List<Card> deck = Deck.createDeck();

            // Card.equals()가 정의되어 있으므로 Set<Card>로 정확히 제거
            Set<Card> known = new HashSet<>(hole);
            known.addAll(community);
            deck.removeIf(known::contains);

            // 보드를 5장으로 완성
            List<Card> board = new ArrayList<>(community);
            while (board.size() < 5) board.add(deck.remove(deck.size() - 1));

            // 내 7장 평가
            List<Card> myCards = new ArrayList<>(hole);
            myCards.addAll(board);
            int[] myScore = HandEval.eval7Cards(myCards);

            // 상대 중 최고 핸드 탐색
            int[] bestOpp = null;
            for (int i = 0; i < opponents; i++) {
                List<Card> oppCards = new ArrayList<>();
                oppCards.add(deck.remove(deck.size() - 1));
                oppCards.add(deck.remove(deck.size() - 1));
                oppCards.addAll(board);
                int[] oppScore = HandEval.eval7Cards(oppCards);
                if (bestOpp == null || HandEval.compareHands(oppScore, bestOpp) > 0) {
                    bestOpp = oppScore;
                }
            }

            // opponents = 0이면 무조건 승리 처리
            if (bestOpp == null || HandEval.compareHands(myScore, bestOpp) >= 0) wins++;
        }

        return (double) wins / trials;
    }

    /**
     * 플레이 스타일과 승률을 기반으로 액션을 결정합니다.
     *
     * 스타일 조합:
     *   Tight/Loose  → foldThreshold 결정 (Tight는 더 보수적)
     *   Aggressive/Passive → raiseThreshold 결정 (Aggressive는 더 공격적)
     *
     * @param style   플레이 스타일 문자열 ("Tight Aggressive" 등)
     * @param winRate estimateWinRate()의 결과
     * @param toCall  콜에 필요한 칩 (0이면 check 가능)
     * @param stack   현재 보유 칩
     * @return "fold" | "call" | "check" | "raise"
     */
    public static String aiDecide(String style, double winRate, int toCall, int stack) {
        boolean tight = style.contains("Tight");
        boolean aggro = style.contains("Aggressive");

        double foldTh  = tight ? 0.38 : 0.30; // 이 승률 미만이면 폴드
        double raiseTh = aggro ? 0.62 : 0.70; // 이 승률 초과이면 레이즈

        if (toCall > 0 && winRate < foldTh)             return "fold";
        if (winRate > raiseTh && stack > toCall)         return "raise";
        if (toCall == 0 && aggro && winRate > 0.55)      return "raise";
        return toCall > 0 ? "call" : "check";
    }
}
