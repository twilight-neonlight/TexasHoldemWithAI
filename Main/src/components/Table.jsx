// src/components/Table.jsx
import React from "react";
import CardView from "./CardView";

export default function Table({ state, omniscient = false }) {
  const players = state.players;

  return (
    <div className="tableWrap">
      <div className="table">
        <div className="board">
          <div className="title">Community</div>
          <div className="cardsRow">
            {Array.from({ length: 5 }).map((_, i) => (
              <CardView key={i} card={state.community[i]} hidden={!state.community[i]} />
            ))}
          </div>
          <div className="pot">Pot: <span className="mono">{state.pot}</span></div>
          <div className="meta">
            Dealer: <span className="mono">{players[state.dealer]?.name}</span>
            {" | "}
            Turn: <span className="mono">{players[state.currentPlayer]?.name}</span>
          </div>
        </div>

        <div className="players">
          {players.map((p, idx) => {
            const isTurn = idx === state.currentPlayer;
            const hideHole = !omniscient && !p.isHuman;
            return (
              <div key={p.id} className={"player " + (isTurn ? "turn" : "") + (!p.inHand ? " out" : "")}>
                <div className="pTop">
                  <div className="pName">{p.name}</div>
                  <div className="pStyle">{p.style}</div>
                  <div className="pStack mono">{p.stack}</div>
                </div>
                <div className="cardsRow">
                  <CardView card={p.hole[0]} hidden={hideHole} />
                  <CardView card={p.hole[1]} hidden={hideHole} />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}