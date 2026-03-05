// src/components/ActionLog.jsx
import React from "react";

export default function ActionLog({ lines }) {
  return (
    <div className="log">
      {lines.map((l, i) => (
        <div key={i} className="logLine">{l}</div>
      ))}
    </div>
  );
}