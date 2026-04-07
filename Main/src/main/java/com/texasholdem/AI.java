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
     * 플레이어 수 + 상대적 승률 + 성향을 모두 반영하는 적응형 의사결정입니다.
     *
     * 핵심 아이디어:
     *   - 절대 승률(winRate) 대신 "평균 대비 상대적 강도(strength)"로 판단
     *     strength = winRate / baseWinRate  (baseWinRate = 1 / activePlayers)
     *     → 6인에서 winRate=0.25면 strength=1.5 (평균보다 50% 강함)
     *     → 2인에서 winRate=0.25면 strength=0.5 (평균보다 약함)
     *   - foldTh / raiseTh를 strength 단위로 설정하면 플레이어 수 변화에 자동 대응
     *   - 팟 오즈(Pot Odds), 기대값(EV), 블러프를 보조 레이어로 추가
     *
     * 판단 순서:
     *   1. strength 계산 (상대적 핸드 강도)
     *   2. 단계 × 성향 기반 foldTh / raiseTh 결정
     *   3. 팟 오즈(potOdds) 계산 → EV 음수여도 potOdds 만족 시 콜 허용
     *   4. 블러프 확률 적용
     *   5. 최종 액션 결정
     *
     * @param style         "Tight Passive" | "Tight Aggressive" | "Loose Passive" | "Loose Aggressive"
     * @param winRate       estimateWinRate() 결과 (0.0 ~ 1.0)
     * @param toCall        콜에 필요한 칩
     * @param stack         현재 보유 칩
     * @param pot           현재 팟 크기
     * @param stage         현재 게임 단계
     * @param activePlayers 현재 핸드에 남아 있는 플레이어 수 (자신 포함)
     * @return "fold" | "call" | "check" | "raise"
     */
    public static String aiDecide(String style, double winRate, int toCall,
                                   int stack, int pot, Game.Stage stage,
                                   int activePlayers) {
        boolean tight = style.contains("Tight");
        boolean aggro = style.contains("Aggressive");

        // ── 1. 상대적 핸드 강도 ──────────────────────────────────────────────
        // baseWinRate: 랜덤 핸드가 N명 중 이길 기대 승률 = 1/N
        // strength > 1.0 → 평균보다 강한 핸드
        // strength < 1.0 → 평균보다 약한 핸드
        int    n            = Math.max(activePlayers, 2); // 최소 2명 기준
        double baseWinRate  = 1.0 / n;
        double strength     = winRate / baseWinRate;

        // ── 2. 단계 × 성향 기반 임계값 ──────────────────────────────────────
        // 단위: strength 배율 (예: foldTh=1.2 → 평균 승률의 1.2배 미만이면 폴드)
        // tight → foldTh 높임 (더 보수적), aggro → raiseTh 낮춤 (더 공격적)
        double foldTh;    // 이 미만이면 폴드 고려
        double raiseTh;   // 이 초과이면 레이즈 고려
        double bluffBase; // 블러프 기본 확률

        switch (stage) {
            case PREFLOP:
                // 정보가 없으므로 보수적으로 시작
                foldTh    = tight ? 1.40 : 1.20;
                raiseTh   = aggro ? 2.20 : 2.50;
                bluffBase = aggro ? 0.06 : 0.02;
                break;
            case FLOP:
                // 커뮤니티 3장 공개 → 핸드 방향성 확인, 점차 공격적으로
                foldTh    = tight ? 1.30 : 1.10;
                raiseTh   = aggro ? 2.00 : 2.30;
                bluffBase = aggro ? 0.08 : 0.03;
                break;
            case TURN:
                // 드로우 가치 확정에 가까워짐
                foldTh    = tight ? 1.20 : 1.00;
                raiseTh   = aggro ? 1.85 : 2.10;
                bluffBase = aggro ? 0.06 : 0.02;
                break;
            case RIVER:
                // 확정된 핸드 강도로만 판단, 블러프 소폭 허용
                foldTh    = tight ? 1.10 : 0.90;
                raiseTh   = aggro ? 1.70 : 1.95;
                bluffBase = aggro ? 0.09 : 0.03;
                break;
            default:
                foldTh    = tight ? 1.40 : 1.20;
                raiseTh   = aggro ? 2.20 : 2.50;
                bluffBase = 0.03;
        }

        // ── 3. 팟 오즈 & 기대값(EV) ─────────────────────────────────────────
        // potOdds: 콜이 수학적으로 정당한 최소 승률
        //   potOdds = toCall / (pot + toCall)
        // EV: 장기 기댓값. 양수이면 콜/레이즈가 이익
        //   EV = winRate × (pot + toCall) - (1 - winRate) × toCall
        double potOdds = (toCall > 0) ? (double) toCall / (pot + toCall) : 0.0;
        double ev       = winRate * (pot + toCall) - (1.0 - winRate) * toCall;

        // ── 4. 블러프 판단 ───────────────────────────────────────────────────
        // strength < foldTh (약한 핸드)이지만 확률적으로 레이즈 → 상대 압박
        // Loose 성향은 블러프 범위 1.5배
        double bluffProb = bluffBase * (tight ? 1.0 : 1.5);
        boolean bluff    = strength < foldTh
                           && stack > toCall * 2
                           && RNG.nextDouble() < bluffProb;

        // ── 5. 최종 액션 결정 ────────────────────────────────────────────────

        // 블러프: 약한 핸드에서 확률적 레이즈
        if (bluff) return "raise";

        // 강한 핸드 → 레이즈
        if (strength >= raiseTh && stack > toCall) return "raise";

        // Aggressive + check 상황: raiseTh 약간 밑돌아도 레이즈
        if (toCall == 0 && aggro && strength >= raiseTh - 0.30) return "raise";

        // 중간 핸드 영역 (foldTh ≤ strength < raiseTh)
        if (strength >= foldTh) {
            // 팟 오즈 만족 or EV 양수이면 콜, 아니면 체크/콜
            return toCall > 0 ? "call" : "check";
        }

        // 약한 핸드 (strength < foldTh)
        // 팟 오즈가 낮아서 콜이 수학적으로 정당한 경우 → 콜 허용
        if (toCall > 0 && winRate >= potOdds) return "call";

        // EV 양수이면 콜 (비용 대비 기대 수익이 있음)
        if (toCall > 0 && ev > 0) return "call";

        // 그 외: 코스트가 없으면 체크, 있으면 폴드
        return toCall > 0 ? "fold" : "check";
    }
}
