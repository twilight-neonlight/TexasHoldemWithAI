// src/poker/handEval.js
import { cardRank, cardSuit } from "./cards";

// Straight 검사 (A는 로우도 가능)
export function isStraight(values) {
  const vals = [...new Set(values)].sort((a, b) => b - a);
  if (vals.includes(14)) vals.push(1); // A low

  for (let i = 0; i <= vals.length - 5; i++) {
    const window = vals.slice(i, i + 5);
    if (window[0] - window[4] === 4 && new Set(window).size === 5) {
      // A2345이면 high를 5로
      return window[0] === 1 ? 5 : window[0];
    }
  }
  return null;
}

// 5장 평가: 결과를 "큰 값이 더 좋은" 튜플(배열)로 리턴
// [category, ...kickers] 형태. category가 클수록 강함.
export function eval5Cards(cards) {
  const values = cards.map(cardRank).sort((a, b) => b - a);
  const suits = cards.map(cardSuit);

  // count ranks
  const count = {};
  for (const v of values) count[v] = (count[v] || 0) + 1;
  const groups = Object.entries(count)
    .map(([v, c]) => ({ v: Number(v), c }))
    .sort((a, b) => (b.c - a.c) || (b.v - a.v));

  const isFlush = suits.every((s) => s === suits[0]);
  const straightHigh = isStraight(values);

  // Straight Flush
  if (isFlush && straightHigh) {
    return [9, straightHigh];
  }

  // Four of a kind
  if (groups[0].c === 4) {
    const four = groups[0].v;
    const kicker = groups[1].v;
    return [8, four, kicker];
  }

  // Full House
  if (groups[0].c === 3 && groups[1].c === 2) {
    return [7, groups[0].v, groups[1].v];
  }

  // Flush
  if (isFlush) {
    return [6, ...values];
  }

  // Straight
  if (straightHigh) {
    return [5, straightHigh];
  }

  // Three of a kind
  if (groups[0].c === 3) {
    const trips = groups[0].v;
    const kickers = groups.slice(1).map((g) => g.v).sort((a, b) => b - a);
    return [4, trips, ...kickers];
  }

  // Two Pair
  if (groups[0].c === 2 && groups[1].c === 2) {
    const highPair = Math.max(groups[0].v, groups[1].v);
    const lowPair = Math.min(groups[0].v, groups[1].v);
    const kicker = groups[2].v;
    return [3, highPair, lowPair, kicker];
  }

  // One Pair
  if (groups[0].c === 2) {
    const pair = groups[0].v;
    const kickers = groups.slice(1).map((g) => g.v).sort((a, b) => b - a);
    return [2, pair, ...kickers];
  }

  // High Card
  return [1, ...values];
}

export function eval7Cards(cards7) {
  // 7장 중 5장 조합을 전부 평가해서 최고를 선택
  let best = null;
  const n = cards7.length;
  for (let a = 0; a < n - 4; a++) {
    for (let b = a + 1; b < n - 3; b++) {
      for (let c = b + 1; c < n - 2; c++) {
        for (let d = c + 1; d < n - 1; d++) {
          for (let e = d + 1; e < n; e++) {
            const hand = [cards7[a], cards7[b], cards7[c], cards7[d], cards7[e]];
            const score = eval5Cards(hand);
            if (!best || compareHands(score, best) > 0) best = score;
          }
        }
      }
    }
  }
  return best;
}

export function compareHands(scoreA, scoreB) {
  const len = Math.max(scoreA.length, scoreB.length);
  for (let i = 0; i < len; i++) {
    const a = scoreA[i] ?? 0;
    const b = scoreB[i] ?? 0;
    if (a !== b) return a > b ? 1 : -1;
  }
  return 0;
}

export function handName(score) {
  const cat = score[0];
  switch (cat) {
    case 9: return "Straight Flush";
    case 8: return "Four of a Kind";
    case 7: return "Full House";
    case 6: return "Flush";
    case 5: return "Straight";
    case 4: return "Three of a Kind";
    case 3: return "Two Pair";
    case 2: return "One Pair";
    default: return "High Card";
  }
}