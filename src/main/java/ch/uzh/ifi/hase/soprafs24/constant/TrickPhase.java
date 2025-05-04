package ch.uzh.ifi.hase.soprafs24.constant;

public enum TrickPhase {
    RUNNING, // Trick has one or more but but fewer than four cards.
    JUSTCOMPLETED, // The fourth card was just added to the trick.
    READY; // Trick is waiting for first leading trick player to play a card.
}