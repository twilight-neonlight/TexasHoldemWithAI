// src/components/CardView.jsx
import React from "react";
import { formatCard } from "../poker/cards";

export default function CardView({ card, hidden = false }) {
  return (
    <span className={"card " + (hidden ? "cardHidden" : "")}>
      {hidden ? "🂠" : formatCard(card)}
    </span>
  );
}