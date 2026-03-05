// src/components/Controls.jsx
import React from "react";
import { Play, Pause, SkipForward, RotateCcw } from "lucide-react";

export default function Controls({
  isPlaying,
  speed,
  waitingForHuman,
  onPlayPause,
  onStep,
  onReset,
  onStartHand,
  onNextStage,
  onSpeed,
  onToggleHuman,
}) {
  return (
    <div className="controls">
      <div className="row">
        <button className="btn" onClick={onStartHand}>Start Hand</button>
        <button className="btn" onClick={onNextStage}>Next Street</button>
        <button className="btn" onClick={onReset} title="Reset">
          <RotateCcw size={16} /> Reset
        </button>
      </div>

      <div className="row">
        <button className="btn" onClick={onPlayPause} disabled={waitingForHuman}>
          {isPlaying ? <><Pause size={16} /> Pause</> : <><Play size={16} /> Play</>}
        </button>
        <button className="btn" onClick={onStep} disabled={waitingForHuman}>
          <SkipForward size={16} /> Step
        </button>

        <label className="label">
          Speed
          <input
            className="range"
            type="range"
            min="150"
            max="1500"
            step="50"
            value={speed}
            onChange={(e) => onSpeed(Number(e.target.value))}
          />
          <span className="mono">{speed}ms</span>
        </label>

        <button className="btn secondary" onClick={onToggleHuman}>
          Toggle Human
        </button>
      </div>

      {waitingForHuman && <div className="hint">Waiting for your action…</div>}
    </div>
  );
}