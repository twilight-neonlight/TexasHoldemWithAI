// src/App.jsx
import React from "react";
import { useTexasHoldem } from "./hooks/useTexasHoldem";
import Table from "./components/Table";
import Controls from "./components/Controls";
import ActionLog from "./components/ActionLog";
import "./index.css";

export default function App() {
  const { state, ui, log, actions } = useTexasHoldem();

  return (
    <div className="app">
      <header className="header">
        <div className="hTitle">Texas Holdem Demo</div>
        <div className="hSub mono">http://localhost:5173</div>
      </header>

      <main className="main">
        <Table state={state} omniscient={ui.omniscient} />

        <Controls
          isPlaying={ui.isPlaying}
          speed={ui.speed}
          waitingForHuman={state.waitingForHuman}
          onPlayPause={() => actions.setPlaying(!ui.isPlaying)}
          onStep={() => actions.step()}
          onReset={() => actions.init(state.humanEnabled)}
          onStartHand={() => actions.startHand()}
          onNextStage={() => actions.nextStage()}
          onSpeed={(v) => actions.setSpeed(v)}
          onToggleHuman={() => actions.init(!state.humanEnabled)}
        />

        {state.waitingForHuman && (
          <div className="humanActions">
            <button className="btn danger" onClick={() => actions.human("fold")}>Fold</button>
            <button className="btn" onClick={() => actions.human("check")}>Check</button>
            <button className="btn" onClick={() => actions.human("call")}>Call</button>
            <button className="btn primary" onClick={() => actions.human("raise")}>Raise</button>
          </div>
        )}

        <div className="row">
          <button className="btn secondary" onClick={() => actions.toggleOmni?.()}>
            (옵션) Omni
          </button>
          <button className="btn secondary" onClick={() => actions.toggleOmni?.()} style={{ display: "none" }} />
        </div>

        <ActionLog lines={log} />
      </main>
    </div>
  );
}