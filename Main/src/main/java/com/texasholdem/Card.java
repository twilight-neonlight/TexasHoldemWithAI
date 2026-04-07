package com.texasholdem;

import java.util.Objects;

/**
 * 포커 카드 한 장을 나타냅니다.
 * Card는 불변(immutable) 객체입니다 — 생성 후 rank/suit가 바뀌지 않습니다.
 */
public class Card {

    // ── Suit (무늬) ────────────────────────────────────────────────────────────
    public enum Suit {
        SPADES  ('s', "♠"),
        HEARTS  ('h', "♥"),
        DIAMONDS('d', "♦"),
        CLUBS   ('c', "♣");

        public final char   symbol;  // 파싱용 단일 문자 ('s', 'h', 'd', 'c')
        public final String display; // UI 표시용 유니코드 심볼

        Suit(char symbol, String display) {
            this.symbol  = symbol;
            this.display = display;
        }

        /** 'A', 's' 같은 2-char 문자열의 두 번째 글자로 Suit를 찾습니다. */
        public static Suit fromChar(char c) {
            for (Suit s : values()) {
                if (s.symbol == c) return s;
            }
            throw new IllegalArgumentException("Invalid suit: " + c);
        }
    }

    // ── Rank (숫자/문자) ───────────────────────────────────────────────────────
    public enum Rank {
        TWO  ('2',  2), THREE('3',  3), FOUR ('4',  4),
        FIVE ('5',  5), SIX  ('6',  6), SEVEN('7',  7),
        EIGHT('8',  8), NINE ('9',  9), TEN  ('T', 10),
        JACK ('J', 11), QUEEN('Q', 12), KING ('K', 13), ACE('A', 14);

        public final char symbol; // 파싱용 단일 문자
        public final int  value;  // 비교용 숫자 값 (2~14)

        Rank(char symbol, int value) {
            this.symbol = symbol;
            this.value  = value;
        }

        /** 'A', 'T', '2' 같은 문자로 Rank를 찾습니다. */
        public static Rank fromChar(char c) {
            for (Rank r : values()) {
                if (r.symbol == c) return r;
            }
            throw new IllegalArgumentException("Invalid rank: " + c);
        }
    }

    // ── 필드 ──────────────────────────────────────────────────────────────────
    private final Rank rank;
    private final Suit suit;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    // ── 팩토리 ────────────────────────────────────────────────────────────────

    /** "As", "Td", "2h" 형태의 2-char 문자열로 Card를 생성합니다. */
    public static Card fromString(String card) {
        return new Card(
            Rank.fromChar(card.charAt(0)),
            Suit.fromChar(card.charAt(1))
        );
    }

    // ── 접근자 ────────────────────────────────────────────────────────────────

    public int  getRankValue() { return rank.value; }

    /** 파싱용 무늬 기호를 반환합니다 ('s', 'h', 'd', 'c'). */
    public char getSuit()      { return suit.symbol; }

    /** UI 표시용 문자열을 반환합니다 (예: "A♠", "T♦"). */
    public String format() {
        return rank.symbol + suit.display;
    }

    // ── Object 재정의 ─────────────────────────────────────────────────────────

    /**
     * 표시용 문자열 대신 파싱 기호 기반 키를 반환합니다 (예: "As", "Td").
     * Set/Map 키 및 로그에 사용하세요. format()은 UI 전용입니다.
     */
    @Override
    public String toString() {
        return "" + rank.symbol + suit.symbol;
    }

    /**
     * rank와 suit가 같으면 동일한 카드로 취급합니다.
     * Set<Card>나 Map<Card, ...> 사용 시 필수입니다.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        Card c = (Card) o;
        return rank == c.rank && suit == c.suit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rank, suit);
    }

    // ── 테스트 ────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // 카드 생성 테스트
        Card c1 = Card.fromString("As");
        Card c2 = Card.fromString("Td");
        System.out.println("c1: " + c1 + " -> " + c1.format());
        System.out.println("c2: " + c2 + " -> " + c2.format());

        // equals 테스트
        Card c3 = new Card(Card.Rank.ACE, Card.Suit.SPADES);
        System.out.println("c1.equals(c3): " + c1.equals(c3));
    }
}
