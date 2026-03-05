// src/poker/game.js
import { createDeck } from "./cards";
import { estimateWinRate, aiDecide } from "./ai";

export const STAGE = {
  READY: "ready",
  PREFLOP: "preflop",
  FLOP: "flop",
  TURN: "turn",
  RIVER: "river",
  SHOWDOWN: "showdown",
};

export function createInitialState({ withHuman = false } = {}) {
  const styles = ["Tight Passive", "Tight Aggressive", "Loose Passive", "Loose Aggressive"];
  const players = [];

  const aiCount = withHuman ? 5 : 6;
  for (let i = 0; i < aiCount; i++) {
    players.push({
      id: `ai-${i}`,
      name: `AI_${i + 1}`,
      style: styles[Math.floor(Math.random() * styles.length)],
      isHuman: false,
      stack: 2000,
      hole: [],
      inHand: true,
    });
  }

  if (withHuman) {
    players.push({
      id: "human",
      name: "You",
      style: "Human",
      isHuman: true,
      stack: 2000,
      hole: [],
      inHand: true,
    });
  }

  return {
    players,
    community: [],
    pot: 0,
    dealer: 0,
    stage: STAGE.READY,
    handNumber: 0,
    deck: createDeck(),
    currentPlayer: 0,
    waitingForHuman: false,
    humanEnabled: withHuman,
  };
}

export function startHand(state) {
  const deck = createDeck();
  const players = state.players.map((p) => ({
    ...p,
    hole: [deck.pop(), deck.pop()],
    inHand: p.stack > 0,
  }));

  return {
    ...state,
    deck,
    players,
    community: [],
    pot: 0,
    stage: STAGE.PREFLOP,
    handNumber: state.handNumber + 1,
    currentPlayer: (state.dealer + 1) % players.length,
    waitingForHuman: players[(state.dealer + 1) % players.length]?.isHuman ?? false,
  };
}

export function nextStage(state) {
  const deck = [...state.deck];
  let community = [...state.community];

  if (state.stage === STAGE.PREFLOP) {
    community = [deck.pop(), deck.pop(), deck.pop()];
    return { ...state, deck, community, stage: STAGE.FLOP };
  }
  if (state.stage === STAGE.FLOP) {
    community = [...community, deck.pop()];
    return { ...state, deck, community, stage: STAGE.TURN };
  }
  if (state.stage === STAGE.TURN) {
    community = [...community, deck.pop()];
    return { ...state, deck, community, stage: STAGE.RIVER };
  }
  if (state.stage === STAGE.RIVER) {
    return { ...state, stage: STAGE.SHOWDOWN };
  }
  return state;
}

export function stepAI(state) {
  const players = [...state.players];
  const idx = state.currentPlayer;
  const p = players[idx];

  if (!p || !p.inHand) {
    const next = (idx + 1) % players.length;
    return { ...state, currentPlayer: next, waitingForHuman: players[next]?.isHuman ?? false };
  }

  if (p.isHuman) {
    return { ...state, waitingForHuman: true };
  }

  const alive = players.filter((x) => x.inHand).length;
  const opponents = Math.max(0, alive - 1);

  const winRate = estimateWinRate(p.hole, state.community, opponents, 250);
  // 시연용: toCall=0으로 단순화
  const action = aiDecide({ style: p.style, winRate, toCall: 0, stack: p.stack });

  // 시연용 행동 처리: fold는 탈락만, raise는 pot에 조금 추가
  let pot = state.pot;
  let newP = p;

  if (action.type === "fold") {
    newP = { ...p, inHand: false };
  } else if (action.type === "raise") {
    const bet = Math.min(40, p.stack);
    newP = { ...p, stack: p.stack - bet };
    pot += bet;
  }

  players[idx] = newP;
  const next = (idx + 1) % players.length;

  return {
    ...state,
    players,
    pot,
    currentPlayer: next,
    waitingForHuman: players[next]?.isHuman ?? false,
    lastAI: { name: p.name, style: p.style, winRate, action: action.type },
  };
}

export function humanAction(state, type) {
  const players = [...state.players];
  const idx = state.currentPlayer;
  const p = players[idx];
  if (!p?.isHuman) return state;

  let pot = state.pot;
  let newP = p;

  if (type === "fold") newP = { ...p, inHand: false };
  if (type === "raise") {
    const bet = Math.min(40, p.stack);
    newP = { ...p, stack: p.stack - bet };
    pot += bet;
  }
  // call/check은 시연용: 변화 없음

  players[idx] = newP;
  const next = (idx + 1) % players.length;

  return {
    ...state,
    players,
    pot,
    currentPlayer: next,
    waitingForHuman: players[next]?.isHuman ?? false,
    lastHuman: { action: type },
  };
}