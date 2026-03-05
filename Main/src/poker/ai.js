// src/poker/ai.js
import { createDeck } from "./cards";
import { eval7Cards, compareHands } from "./handEval";

export function estimateWinRate(hole, community, opponents = 5, trials = 400) {
  // 매우 단순한 몬테카를로 (빠른 데모용)
  let wins = 0;
  for (let t = 0; t < trials; t++) {
    const deck = createDeck();
    // remove known cards
    const known = new Set([...hole, ...community]);
    const live = deck.filter((c) => !known.has(c));

    const board = [...community];
    while (board.length < 5) board.push(live.pop());

    const myScore = eval7Cards([...hole, ...board]);
    let bestOpp = null;

    for (let i = 0; i < opponents; i++) {
      const oppHole = [live.pop(), live.pop()];
      const oppScore = eval7Cards([...oppHole, ...board]);
      if (!bestOpp || compareHands(oppScore, bestOpp) > 0) bestOpp = oppScore;
    }
    if (compareHands(myScore, bestOpp) >= 0) wins++;
  }
  return wins / trials;
}

// 스타일 기반 초간단 액션
export function aiDecide({ style, winRate, toCall, stack }) {
  // style: Tight Passive / Tight Aggressive / Loose Passive / Loose Aggressive
  const tight = style.includes("Tight");
  const aggro = style.includes("Aggressive");

  const foldTh = tight ? 0.38 : 0.30;
  const raiseTh = aggro ? 0.62 : 0.70;

  if (toCall > 0 && winRate < foldTh) return { type: "fold" };
  if (winRate > raiseTh && stack > toCall) return { type: "raise" };
  if (toCall === 0 && aggro && winRate > 0.55) return { type: "raise" };
  return { type: toCall > 0 ? "call" : "check" };
}