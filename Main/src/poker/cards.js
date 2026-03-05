// src/poker/cards.js
export const RANKS = "23456789TJQKA";
export const SUITS = "shdc";
export const RANK_VALUE = Object.fromEntries([...RANKS].map((r, i) => [r, i + 2]));

export function createDeck() {
  const deck = [];
  for (const r of RANKS) for (const s of SUITS) deck.push(r + s);
  // shuffle
  for (let i = deck.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [deck[i], deck[j]] = [deck[j], deck[i]];
  }
  return deck;
}

export function cardRank(card) {
  return RANK_VALUE[card[0]];
}
export function cardSuit(card) {
  return card[1];
}

export function formatCard(card) {
  if (!card) return "??";
  const r = card[0];
  const s = card[1];
  const suitMap = { s: "♠", h: "♥", d: "♦", c: "♣" };
  return `${r}${suitMap[s] ?? s}`;
}