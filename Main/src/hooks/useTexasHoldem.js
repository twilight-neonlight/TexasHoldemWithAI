// src/hooks/useTexasHoldem.js
import { useEffect, useMemo, useReducer } from "react";
import { createInitialState, startHand, nextStage, stepAI, humanAction } from "../poker/game";

function reducer(state, action) {
  switch (action.type) {
    case "INIT":
      return createInitialState({ withHuman: action.withHuman });
    case "START_HAND":
      return startHand(state);
    case "NEXT_STAGE":
      return nextStage(state);
    case "STEP_AI":
      return stepAI(state);
    case "HUMAN":
      return humanAction(state, action.actionName);
    default:
      return state;
  }
}

export function useTexasHoldem() {
  const [state, dispatch] = useReducer(reducer, null, () => createInitialState({ withHuman: false }));
  const [ui, uiDispatch] = useReducer(
    (s, a) => ({ ...s, ...a }),
    { isPlaying: false, speed: 700, omniscient: false, autoContinue: true }
  );

  const log = useMemo(() => {
    const lines = [];
    lines.push(`Hand #${state.handNumber} | Stage: ${state.stage} | Pot: ${state.pot}`);
    if (state.lastAI) {
      lines.push(`[AI] ${state.lastAI.name} (${state.lastAI.style}) → ${state.lastAI.action} (wr≈${state.lastAI.winRate.toFixed(2)})`);
    }
    if (state.lastHuman) {
      lines.push(`[You] → ${state.lastHuman.action}`);
    }
    return lines;
  }, [state.handNumber, state.stage, state.pot, state.lastAI, state.lastHuman]);

  useEffect(() => {
    if (!ui.isPlaying) return;
    if (state.waitingForHuman) return;

    const id = window.setInterval(() => {
      dispatch({ type: "STEP_AI" });
    }, ui.speed);

    return () => window.clearInterval(id);
  }, [ui.isPlaying, ui.speed, state.waitingForHuman]);

  return {
    state,
    ui,
    log,
    actions: {
      init: (withHuman) => dispatch({ type: "INIT", withHuman }),
      startHand: () => dispatch({ type: "START_HAND" }),
      nextStage: () => dispatch({ type: "NEXT_STAGE" }),
      step: () => dispatch({ type: "STEP_AI" }),
      setPlaying: (isPlaying) => uiDispatch({ isPlaying }),
      setSpeed: (speed) => uiDispatch({ speed }),
      toggleOmni: () => uiDispatch({ omniscient: !ui.omniscient }),
      human: (actionName) => dispatch({ type: "HUMAN", actionName }),
    },
  };
}