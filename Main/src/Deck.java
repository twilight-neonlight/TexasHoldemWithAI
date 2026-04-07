import java.util.*;

/**
 * 52장 표준 덱을 생성하고 섞습니다.
 * createDeck()은 매번 새로 섞인 리스트를 반환하므로 상태를 공유하지 않습니다.
 */
public class Deck {

    /**
     * 52장을 모두 포함하고 무작위로 섞인 덱을 반환합니다.
     * 카드를 뽑을 때는 list.remove(list.size() - 1) 로 맨 뒤에서 꺼냅니다.
     */
    public static List<Card> createDeck() {
        List<Card> deck = new ArrayList<>(52);

        for (Card.Rank r : Card.Rank.values()) {
            for (Card.Suit s : Card.Suit.values()) {
                deck.add(new Card(r, s));
            }
        }

        Collections.shuffle(deck);
        return deck;
    }
}
