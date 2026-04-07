package com.texasholdem;

import java.util.*;

/**
 * AI 플레이어의 승률 추정과 액션 결정 로직입니다.
 */
public class AI {

    private static final Random RNG = new Random();

    // ── 승률 추정 ─────────────────────────────────────────────────────────────

    /**
     * 몬테카를로 시뮬레이션으로 승률을 추정합니다.
     *
     * 매 trial마다 남은 덱을 섞어 보드와 상대 핸드를 무작위로 완성하고,
     * 내 핸드가 가장 강한 상대보다 같거나 강하면 승리로 카운트합니다.
     *
     * @param hole      내 홀 카드 2장
     * @param community 현재 공개된 커뮤니티 카드 (0~5장)
     * @param opponents 상대방 수
     * @param trials    시뮬레이션 횟수
     * @return 0.0 ~ 1.0 사이의 승률
     */
    public static double estimateWinRate(List<Card> hole, List<Card> community,
                                         int opponents, int trials) {
        int wins = 0;

        for (int t = 0; t < trials; t++) {
            List<Card> deck = Deck.createDeck();

            Set<Card> known = new HashSet<>(hole);
            known.addAll(community);
            deck.removeIf(known::contains);

            List<Card> board = new ArrayList<>(community);
            while (board.size() < 5) board.add(deck.remove(deck.size() - 1));

            List<Card> myCards = new ArrayList<>(hole);
            myCards.addAll(board);
            int[] myScore = HandEval.eval7Cards(myCards);

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

            if (bestOpp == null || HandEval.compareHands(myScore, bestOpp) >= 0) wins++;
        }

        return (double) wins / trials;
    }

    // ── 액션 결정 ─────────────────────────────────────────────────────────────

    /**
     * 상황 적응형 AI 의사결정입니다.
     *
     * 판단 순서:
     *   1. 팟 오즈(Pot Odds) 계산 → 최소 요구 승률 도출
     *   2. 게임 단계별 foldTh / raiseTh 조정
     *   3. 기대값(EV) = winRate × pot - (1 - winRate) × toCall 계산
     *   4. 블러프 확률 적용 (Aggressive 성향 위주)
     *   5. 최종 액션 결정
     *
     * @param style   "Tight Passive" | "Tight Aggressive" | "Loose Passive" | "Loose Aggressive"
     * @param winRate estimateWinRate() 결과 (0.0 ~ 1.0)
     * @param toCall  콜에 필요한 칩
     * @param stack   현재 보유 칩
     * @param pot     현재 팟 크기
     * @param stage   현재 게임 단계
     * @return "fold" | "call" | "check" | "raise"
     */
    public static String aiDecide(String style, double winRate, int toCall,
                                   int stack, int pot, Game.Stage stage) {
        boolean tight = style.contains("Tight");
        boolean aggro = style.contains("Aggressive");

        // ── 1. 팟 오즈: 최소 요구 승률 ──────────────────────────────────────
        // 팟 오즈 = 콜 비용 / (팟 + 콜 비용)
        // 콜이 없으면(0) potOdds = 0 → 어떤 승률이든 콜 가능
        double potOdds = (toCall > 0) ? (double) toCall / (pot + toCall) : 0.0;

        // ── 2. 단계별 임계값 ─────────────────────────────────────────────────
        // Preflop: 정보가 적으므로 보수적. Flop 이후 공개 정보 기반으로 공격적으로.
        double foldTh;
        double raiseTh;
        double bluffBase; // 블러프 기본 확률

        switch (stage) {
            case PREFLOP:
                foldTh    = tight ? 0.28 : 0.20;
                raiseTh   = aggro ? 0.50 : 0.58;
                bluffBase = aggro ? 0.06 : 0.02;
                break;
            case FLOP:
                foldTh    = tight ? 0.26 : 0.18;
                raiseTh   = aggro ? 0.47 : 0.55;
                bluffBase = aggro ? 0.08 : 0.03;
                break;
            case TURN:
                foldTh    = tight ? 0.24 : 0.16;
                raiseTh   = aggro ? 0.45 : 0.53;
                bluffBase = aggro ? 0.06 : 0.02;
                break;
            case RIVER:
                // 리버: 드로우 없음 → 핸드 강도에만 집중, 블러프 소폭 허용
                foldTh    = tight ? 0.22 : 0.14;
                raiseTh   = aggro ? 0.43 : 0.50;
                bluffBase = aggro ? 0.09 : 0.03;
                break;
            default:
                foldTh    = tight ? 0.28 : 0.20;
                raiseTh   = aggro ? 0.50 : 0.58;
                bluffBase = 0.03;
        }

        // ── 3. 기대값(EV) ────────────────────────────────────────────────────
        // EV > 0: 콜/레이즈가 장기적으로 이익
        // pot은 콜 후 기준으로 계산 (콜하면 pot + toCall을 획득할 수 있음)
        double ev = winRate * (pot + toCall) - (1.0 - winRate) * toCall;

        // ── 4. 블러프 판단 ───────────────────────────────────────────────────
        // 승률이 낮아도 스택이 충분하고 확률적으로 레이즈 (상대 압박)
        // Loose 성향은 블러프 범위가 더 넓음
        double bluffProb = bluffBase * (tight ? 1.0 : 1.5);
        boolean bluff = winRate < foldTh && stack > toCall * 2
                        && RNG.nextDouble() < bluffProb;

        // ── 5. 액션 결정 ─────────────────────────────────────────────────────

        // 블러프 레이즈 (승률이 낮아도 압박)
        if (bluff) return "raise";

        // 콜 상황: 팟 오즈 + foldTh 동시 판단
        // - winRate가 potOdds보다 낮고, foldTh도 밑돌면 폴드
        if (toCall > 0 && winRate < foldTh && winRate < potOdds) return "fold";

        // EV가 음수이고 foldTh 미달이면 폴드
        if (toCall > 0 && ev < 0 && winRate < foldTh) return "fold";

        // 강한 핸드 → 레이즈
        if (winRate > raiseTh && stack > toCall) return "raise";

        // Aggressive + check 상황에서 중간 핸드도 레이즈
        if (toCall == 0 && aggro && winRate > raiseTh - 0.08) return "raise";

        // 팟 오즈상 콜이 정당한 경우: winRate >= potOdds이면 콜
        if (toCall > 0 && winRate >= potOdds) return "call";

        // EV > 0이면 콜
        if (toCall > 0 && ev > 0) return "call";

        // 그 외: 코스트 없으면 체크, 있으면 폴드
        return toCall > 0 ? "fold" : "check";
    }
}
