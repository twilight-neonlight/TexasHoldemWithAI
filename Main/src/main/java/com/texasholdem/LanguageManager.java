package com.texasholdem;

import java.util.*;

/**
 * 애플리케이션의 언어 관리 및 국제화(i18n)를 담당합니다.
 * 한국어(KO)와 영어(EN)를 지원합니다.
 */
public class LanguageManager {

    public enum Language {
        KO("한국어"),
        EN("English");

        public final String displayName;
        Language(String displayName) { this.displayName = displayName; }
    }

    private static Language currentLanguage = Language.KO;
    private static final Map<String, Map<Language, String>> translations = new HashMap<>();

    static {
        // 버튼 레이블
        addTranslation("button.play", "Play", "시작");
        addTranslation("button.pause", "Pause", "일시정지");
        addTranslation("button.step", "Step", "단계 진행");
        addTranslation("button.reset", "Reset", "초기화");
        addTranslation("button.startHand", "Start Hand", "핸드 시작");
        addTranslation("button.nextStage", "Next Stage", "다음 단계");
        addTranslation("button.fold", "Fold", "폴드");
        addTranslation("button.check", "Check", "체크");
        addTranslation("button.call", "Call", "콜");
        addTranslation("button.raise", "Raise", "레이즈");
        addTranslation("button.help", "Help", "도움말");

        // 체크박스
        addTranslation("checkbox.omniscient", "Omniscient Mode", "전지적 모드");

        // 라벨
        addTranslation("label.language", "Language:", "언어:");
        addTranslation("label.speed", "Speed:", "속도:");
        addTranslation("label.pot", "Pot:", "팟:");
        addTranslation("label.gameTitle", "Texas Holdem Demo", "텍사스 홀덤 데모");

        // 패널 제목
        addTranslation("panel.pokerTable", "Poker Table", "포커 테이블");
        addTranslation("panel.communityCards", "Community Cards", "커뮤니티 카드");
        addTranslation("panel.yourActions", "Your Actions", "당신의 액션");
        addTranslation("panel.players", "Players", "플레이어");
        addTranslation("panel.gameControls", "Game Controls", "게임 컨트롤");
        addTranslation("panel.actionLog", "Action Log", "액션 로그");

        // 툴팁
        addTranslation("tooltip.play", "Start automatic game progression", "게임을 자동으로 진행하거나 일시정지합니다");
        addTranslation("tooltip.step", "Manually advance one player's action", "한 플레이어의 액션을 수동으로 진행합니다");
        addTranslation("tooltip.reset", "Initialize game with new players", "게임을 초기화하고 새 플레이어로 시작합니다");
        addTranslation("tooltip.startHand", "Start a new hand (deal cards)", "새로운 핸드를 시작합니다 (카드 배분)");
        addTranslation("tooltip.nextStage", "Proceed to next betting round", "다음 베팅 라운드로 진행합니다 (Flop/Turn/River)");
        addTranslation("tooltip.speed", "Adjust AI action interval (100-2000ms)", "AI 액션 간격을 조절합니다 (100-2000ms)");
        addTranslation("tooltip.omniscient", "Reveal all players' cards (debug mode)", "모든 플레이어의 카드를 공개합니다 (디버그용)");
        addTranslation("tooltip.fold", "Fold your hand", "핸드를 포기합니다");
        addTranslation("tooltip.check", "Check (pass turn with no bet)", "베팅 없이 턴을 넘깁니다 (콜 금액이 0일 때)");
        addTranslation("tooltip.call", "Call current bet", "현재 베팅액에 맞춰 칩을 추가합니다");
        addTranslation("tooltip.raise", "Raise the bet", "베팅액을 올려 상대를 압박합니다");
        addTranslation("tooltip.help", "Show game instructions", "게임 사용법을 표시합니다");

        // 게임 로그
        addTranslation("log.hand", "Hand #%d | Stage: %s | Pot: %d | Bet: %d", "핸드 #%d | 단계: %s | 팟: %d | 베팅: %d");
        addTranslation("log.ai", "[AI] %s (%s) toCall=%d → %s (wr=%.2f)", "[AI] %s (%s) 콜금액=%d → %s (승률=%.2f)");
        addTranslation("log.human", "[You] → %s", "[당신] → %s");
        addTranslation("log.event", "*** %s ***", "*** %s ***");

        // 포지션
        addTranslation("position.dealer", "D", "D");
        addTranslation("position.smallBlind", "SB", "SB");
        addTranslation("position.bigBlind", "BB", "BB");

        // 플레이어 정보
        addTranslation("player.chips", "chips", "칩");
        addTranslation("player.bet", "bet:", "베팅:");

        // 도움말 제목 및 내용
        addTranslation("help.title", "Game Instructions", "게임 도움말");
        addTranslation("help.content", """
Texas Holdem Poker Game Instructions:

1. Starting the Game:
   - Click 'Reset' to create new players
   - Click 'Start Hand' to deal cards and begin

2. Automatic Progression:
   - Click 'Play' to start automatic AI progression
   - Click 'Pause' to pause
   - Use Speed slider to adjust pace

3. Manual Progression:
   - Click 'Step' to advance one AI player
   - Click 'Next Stage' to proceed to next betting round

4. Playing as Human:
   - When it's your turn, use action buttons:
     * Fold: Give up your hand
     * Check: Pass without betting
     * Call: Match current bet
     * Raise: Increase the bet

5. Information Display:
   - Left: Player info (chips, bets, positions)
   - Center: Pot and community cards
   - Bottom: Action log

6. Positions:
   - D: Dealer
   - SB: Small Blind
   - BB: Big Blind
   - →: Current player's turn

Enjoy the game!""", """
텍사스 홀덤 포커 게임 사용법:

1. 게임 시작:
   - 'Reset'으로 새 플레이어 생성
   - 'Start Hand'로 카드 배분 및 게임 시작

2. 자동 진행:
   - 'Play'로 AI 자동 진행 시작
   - 'Pause'로 일시정지
   - Speed 슬라이더로 속도 조절

3. 수동 진행:
   - 'Step'으로 한 AI씩 진행
   - 'Next Stage'로 베팅 라운드 진행

4. 사람 플레이:
   - 당신 차례에 액션 버튼 사용:
     * Fold: 핸드 포기
     * Check: 베팅 없이 넘김
     * Call: 현재 베팅 맞춤
     * Raise: 베팅 올림

5. 정보 표시:
   - 왼쪽: 플레이어 정보 (칩, 베팅, 포지션)
   - 중앙: 팟과 커뮤니티 카드
   - 아래: 액션 로그

6. 포지션:
   - D: 딜러
   - SB: 스몰 블라인드
   - BB: 빅 블라인드
   - →: 현재 턴 플레이어

즐거운 게임 되세요!""");
    }

    private static void addTranslation(String key, String en, String ko) {
        Map<Language, String> langMap = new HashMap<>();
        langMap.put(Language.EN, en);
        langMap.put(Language.KO, ko);
        translations.put(key, langMap);
    }

    public static void setLanguage(Language lang) {
        currentLanguage = lang;
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static String get(String key) {
        Map<Language, String> langMap = translations.get(key);
        if (langMap == null) return key;
        return langMap.getOrDefault(currentLanguage, key);
    }

    public static String get(String key, Object... args) {
        String template = get(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
}