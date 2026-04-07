package com.texasholdem;

import java.util.*;

/**
 * 포커 핸드 평가 유틸리티입니다.
 *
 * 점수는 int[] 배열로 표현됩니다: [category, kicker1, kicker2, ...]
 * category가 클수록 강한 족보이며, 같을 경우 kicker로 순서를 결정합니다.
 *
 *   9 = Straight Flush
 *   8 = Four of a Kind
 *   7 = Full House
 *   6 = Flush
 *   5 = Straight
 *   4 = Three of a Kind
 *   3 = Two Pair
 *   2 = One Pair
 *   1 = High Card
 */
public class HandEval {

    /**
     * 스트레이트 여부를 검사합니다. A-low(A2345)도 인식합니다.
     *
     * @param values 카드 숫자 값 리스트 (중복 허용, 정렬 불필요)
     * @return 스트레이트가 있으면 highest card 값(5~14), 없으면 0
     */
    public static int isStraight(List<Integer> values) {
        // 중복 제거 후 내림차순 정렬
        List<Integer> vals = new ArrayList<>(new HashSet<>(values));
        vals.sort(Collections.reverseOrder());

        // A(14)가 있으면 로우 에이스(1)도 추가해 A2345 스트레이트 처리
        if (vals.contains(14)) vals.add(1);

        // 연속된 5장 구간을 슬라이딩 윈도우로 탐색
        for (int i = 0; i <= vals.size() - 5; i++) {
            List<Integer> window = vals.subList(i, i + 5);
            // 이미 중복 제거된 상태이므로 범위 차이만 확인
            if (window.get(0) - window.get(4) == 4) {
                // A2345인 경우 로우 에이스(1)가 high이므로 5를 반환
                return window.get(0) == 1 ? 5 : window.get(0);
            }
        }
        return 0;
    }

    /**
     * 5장 카드를 평가해 점수 배열을 반환합니다.
     * 배열 비교는 compareHands()를 사용하세요.
     */
    public static int[] eval5Cards(List<Card> cards) {
        List<Integer>   values = new ArrayList<>(5);
        List<Character> suits  = new ArrayList<>(5);

        for (Card c : cards) {
            values.add(c.getRankValue());
            suits.add(c.getSuit());
        }
        values.sort(Collections.reverseOrder());

        // ── rank별 개수 집계 → groups: [value, count] 내림차순 ─────────────
        Map<Integer, Integer> count = new HashMap<>();
        for (int v : values) count.merge(v, 1, Integer::sum);

        List<int[]> groups = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            groups.add(new int[]{e.getKey(), e.getValue()});
        }
        // count 많은 순 → 같으면 value 높은 순
        groups.sort((a, b) -> b[1] != a[1] ? b[1] - a[1] : b[0] - a[0]);

        // ── 패턴 판정 ──────────────────────────────────────────────────────────
        // Character.equals()로 박싱 객체를 안전하게 비교 (== 사용 금지)
        char firstSuit = suits.get(0);
        boolean isFlush = suits.stream().allMatch(s -> s == firstSuit);
        int straightHigh = isStraight(values);

        // Straight Flush
        if (isFlush && straightHigh > 0) {
            return new int[]{9, straightHigh};
        }

        // Four of a Kind: [8, quad값, kicker]
        if (groups.get(0)[1] == 4) {
            return new int[]{8, groups.get(0)[0], groups.get(1)[0]};
        }

        // Full House: [7, 트리플값, 페어값]
        if (groups.get(0)[1] == 3 && groups.get(1)[1] == 2) {
            return new int[]{7, groups.get(0)[0], groups.get(1)[0]};
        }

        // Flush: [6, 1st, 2nd, 3rd, 4th, 5th]
        if (isFlush) {
            int[] score = new int[6];
            score[0] = 6;
            for (int i = 0; i < 5; i++) score[i + 1] = values.get(i);
            return score;
        }

        // Straight: [5, highest]
        if (straightHigh > 0) {
            return new int[]{5, straightHigh};
        }

        // Three of a Kind: [4, 트리플값, kicker1, kicker2]
        if (groups.get(0)[1] == 3) {
            List<Integer> kickers = new ArrayList<>();
            for (int i = 1; i < groups.size(); i++) kickers.add(groups.get(i)[0]);
            kickers.sort(Collections.reverseOrder());
            return new int[]{4, groups.get(0)[0], kickers.get(0), kickers.get(1)};
        }

        // Two Pair: [3, 높은페어, 낮은페어, kicker]
        if (groups.get(0)[1] == 2 && groups.get(1)[1] == 2) {
            int highPair = Math.max(groups.get(0)[0], groups.get(1)[0]);
            int lowPair  = Math.min(groups.get(0)[0], groups.get(1)[0]);
            int kicker   = groups.get(2)[0];
            return new int[]{3, highPair, lowPair, kicker};
        }

        // One Pair: [2, 페어값, kicker1, kicker2, kicker3]
        if (groups.get(0)[1] == 2) {
            List<Integer> kickers = new ArrayList<>();
            for (int i = 1; i < groups.size(); i++) kickers.add(groups.get(i)[0]);
            kickers.sort(Collections.reverseOrder());
            return new int[]{2, groups.get(0)[0], kickers.get(0), kickers.get(1), kickers.get(2)};
        }

        // High Card: [1, 1st, 2nd, 3rd, 4th, 5th]
        int[] score = new int[6];
        score[0] = 1;
        for (int i = 0; i < 5; i++) score[i + 1] = values.get(i);
        return score;
    }

    /**
     * 7장 중 가능한 모든 5장 조합(C(7,5) = 21가지)을 평가해 최고 점수를 반환합니다.
     * hole 2장 + community 5장을 합친 7장 리스트를 넘기면 됩니다.
     */
    public static int[] eval7Cards(List<Card> cards7) {
        int[] best = null;
        int n = cards7.size();

        for (int a = 0; a < n - 4; a++)
        for (int b = a + 1; b < n - 3; b++)
        for (int c = b + 1; c < n - 2; c++)
        for (int d = c + 1; d < n - 1; d++)
        for (int e = d + 1; e < n; e++) {
            List<Card> hand = Arrays.asList(
                cards7.get(a), cards7.get(b), cards7.get(c),
                cards7.get(d), cards7.get(e)
            );
            int[] score = eval5Cards(hand);
            if (best == null || compareHands(score, best) > 0) best = score;
        }
        return best;
    }

    /**
     * 두 점수 배열을 비교합니다.
     * @return scoreA > scoreB이면 양수, 같으면 0, scoreA < scoreB이면 음수
     */
    public static int compareHands(int[] scoreA, int[] scoreB) {
        int len = Math.max(scoreA.length, scoreB.length);
        for (int i = 0; i < len; i++) {
            int a = i < scoreA.length ? scoreA[i] : 0;
            int b = i < scoreB.length ? scoreB[i] : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    /** 점수 배열의 족보 이름을 반환합니다. */
    public static String handName(int[] score) {
        switch (score[0]) {
            case 9:  return "Straight Flush";
            case 8:  return "Four of a Kind";
            case 7:  return "Full House";
            case 6:  return "Flush";
            case 5:  return "Straight";
            case 4:  return "Three of a Kind";
            case 3:  return "Two Pair";
            case 2:  return "One Pair";
            default: return "High Card";
        }
    }
}
