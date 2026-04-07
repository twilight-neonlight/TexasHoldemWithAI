package com.texasholdem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 간단한 Swing UI로 Texas Holdem 게임을 실행합니다.
 * JavaFX 대신 Swing을 사용해 추가 설치 없이 실행 가능합니다.
 */
public class MainApplication extends JFrame {

    private GameController gameController;
    private JTextArea logArea;
    private JButton playPauseBtn;
    private JButton stepBtn;
    private JButton resetBtn;
    private JButton startHandBtn;
    private JButton nextStageBtn;
    private JSlider speedSlider;
    private JCheckBox omniscientCheck;
    private JPanel humanActionsPanel;
    private JLabel potLabel;
    private JPanel communityPanel;
    private JPanel playersPanel;

    public MainApplication() {
        setTitle("Texas Holdem with AI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // GameController 초기화
        gameController = new GameController(true, state -> SwingUtilities.invokeLater(() -> updateUI(state)));

        initUI();
        updateUI(gameController.getState());
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 헤더
        JPanel header = new JPanel();
        header.add(new JLabel("Texas Holdem Demo"));
        add(header, BorderLayout.NORTH);

        // 메인 패널: 테이블 + 컨트롤
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 중앙: 게임 테이블
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Poker Table"));

        // 테이블 중앙: 팟
        potLabel = new JLabel("Pot: 0", SwingConstants.CENTER);
        potLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        potLabel.setForeground(Color.GREEN);
        tablePanel.add(potLabel, BorderLayout.CENTER);

        // 테이블 위: 커뮤니티 카드
        communityPanel = new JPanel();
        communityPanel.setBorder(BorderFactory.createTitledBorder("Community Cards"));
        tablePanel.add(communityPanel, BorderLayout.NORTH);

        // 테이블 아래: 유저 액션 버튼
        humanActionsPanel = new JPanel();
        humanActionsPanel.setBorder(BorderFactory.createTitledBorder("Your Actions"));
        JButton foldBtn = new JButton("Fold");
        foldBtn.setToolTipText("핸드를 포기합니다");
        foldBtn.addActionListener(e -> gameController.humanAction("fold"));
        humanActionsPanel.add(foldBtn);
        JButton checkBtn = new JButton("Check");
        checkBtn.setToolTipText("베팅 없이 턴을 넘깁니다 (콜 금액이 0일 때)");
        checkBtn.addActionListener(e -> gameController.humanAction("check"));
        humanActionsPanel.add(checkBtn);
        JButton callBtn = new JButton("Call");
        callBtn.setToolTipText("현재 베팅액에 맞춰 칩을 추가합니다");
        callBtn.addActionListener(e -> gameController.humanAction("call"));
        humanActionsPanel.add(callBtn);
        JButton raiseBtn = new JButton("Raise");
        raiseBtn.setToolTipText("베팅액을 올려 상대를 압박합니다");
        raiseBtn.addActionListener(e -> gameController.humanAction("raise"));
        humanActionsPanel.add(raiseBtn);
        tablePanel.add(humanActionsPanel, BorderLayout.SOUTH);

        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // 왼쪽: 플레이어 정보
        playersPanel = new JPanel();
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        playersPanel.setBorder(BorderFactory.createTitledBorder("Players"));
        playersPanel.setPreferredSize(new Dimension(200, 0));
        JScrollPane playersScroll = new JScrollPane(playersPanel);
        mainPanel.add(playersScroll, BorderLayout.WEST);

        // 오른쪽: 컨트롤
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Game Controls"));
        controlPanel.setPreferredSize(new Dimension(200, 0));

        playPauseBtn = new JButton("Play");
        playPauseBtn.setToolTipText("게임을 자동으로 진행하거나 일시정지합니다");
        playPauseBtn.addActionListener(e -> {
            gameController.setPlaying(!gameController.isPlaying());
            updateUI(gameController.getState());
        });
        controlPanel.add(playPauseBtn);

        stepBtn = new JButton("Step");
        stepBtn.setToolTipText("한 플레이어의 액션을 수동으로 진행합니다");
        stepBtn.addActionListener(e -> gameController.step());
        controlPanel.add(stepBtn);

        resetBtn = new JButton("Reset");
        resetBtn.setToolTipText("게임을 초기화하고 새 플레이어로 시작합니다");
        resetBtn.addActionListener(e -> gameController.init(true));
        controlPanel.add(resetBtn);

        startHandBtn = new JButton("Start Hand");
        startHandBtn.setToolTipText("새로운 핸드를 시작합니다 (카드 배분)");
        startHandBtn.addActionListener(e -> gameController.startHand());
        controlPanel.add(startHandBtn);

        nextStageBtn = new JButton("Next Stage");
        nextStageBtn.setToolTipText("다음 베팅 라운드로 진행합니다 (Flop/Turn/River)");
        nextStageBtn.addActionListener(e -> gameController.nextStage());
        controlPanel.add(nextStageBtn);

        JPanel speedPanel = new JPanel();
        speedPanel.add(new JLabel("Speed:"));
        speedSlider = new JSlider(100, 2000, 700);
        speedSlider.setToolTipText("AI 액션 간격을 조절합니다 (100-2000ms)");
        speedSlider.addChangeListener(e -> gameController.setSpeed(speedSlider.getValue()));
        speedPanel.add(speedSlider);
        controlPanel.add(speedPanel);

        omniscientCheck = new JCheckBox("Omniscient Mode");
        omniscientCheck.setToolTipText("모든 플레이어의 카드를 공개합니다 (디버그용)");
        omniscientCheck.addActionListener(e -> {
            gameController.toggleOmniscient();
            updateUI(gameController.getState());
        });
        controlPanel.add(omniscientCheck);

        JButton helpBtn = new JButton("Help");
        helpBtn.setToolTipText("게임 사용법을 표시합니다");
        helpBtn.addActionListener(e -> showHelpDialog());
        controlPanel.add(helpBtn);

        mainPanel.add(controlPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        // 아래: 로그
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Action Log"));
        add(scrollPane, BorderLayout.SOUTH);
    }

    private void updateUI(Game.State state) {
        if (state == null) return;

        // 커뮤니티 카드
        communityPanel.removeAll();
        for (Card card : state.community) {
            JLabel cardLabel = new JLabel(card.format());
            cardLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
            communityPanel.add(cardLabel);
        }
        communityPanel.revalidate();
        communityPanel.repaint();

        // 플레이어
        playersPanel.removeAll();
        for (int i = 0; i < state.players.size(); i++) {
            Game.Player p = state.players.get(i);
            String position = "";
            if (i == state.dealer) position += "D ";
            if (i == state.smallBlindIdx) position += "SB ";
            if (i == state.bigBlindIdx) position += "BB ";

            String info = position + p.name + ": " + p.stack + " chips";
            if (p.streetBet > 0) info += " (bet: " + p.streetBet + ")";
            if (p.isHuman || gameController.isOmniscient()) {
                info += " [" + formatCards(p.hole) + "]";
            }

            JLabel playerLabel = new JLabel(info);
            if (i == state.currentPlayer) {
                playerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                playerLabel.setForeground(Color.BLUE);
                playerLabel.setText("→ " + info); // 화살표 추가
            }
            if (!p.inHand) {
                playerLabel.setForeground(Color.GRAY);
            }
            playersPanel.add(playerLabel);
        }
        playersPanel.revalidate();
        playersPanel.repaint();

        // 팟
        potLabel.setText("Pot: " + state.pot);

        // 컨트롤
        playPauseBtn.setText(gameController.isPlaying() ? "Pause" : "Play");
        omniscientCheck.setSelected(gameController.isOmniscient());

        // 액션 버튼: 항상 보이지만, 사람 턴일 때만 활성화
        Component[] components = humanActionsPanel.getComponents();
        for (Component c : components) {
            if (c instanceof JButton) {
                c.setEnabled(state.waitingForHuman);
            }
        }

        // 로그
        logArea.setText("");
        for (String line : gameController.getLog()) {
            logArea.append(line + "\n");
        }
    }

    private String formatCards(java.util.List<Card> cards) {
        if (cards.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Card c : cards) sb.append(c.format()).append(" ");
        return sb.toString().trim();
    }

    private void showHelpDialog() {
        String helpText = """
            Texas Holdem Poker 게임 사용법:

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
               - → 화살표: 현재 턴 플레이어

            즐거운 게임 되세요!
            """;

        JTextArea textArea = new JTextArea(helpText);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "게임 도움말", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainApplication().setVisible(true);
        });
    }
}