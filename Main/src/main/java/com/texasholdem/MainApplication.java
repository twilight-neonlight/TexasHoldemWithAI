package com.texasholdem;

import javax.swing.*;
import java.awt.*;

/**
 * Swing UI로 Texas Holdem 게임을 실행합니다.
 * 다국어 지원 (한국어, 영어)
 */
public class MainApplication extends JFrame {

    private GameController gameController;
    private JTextArea logArea;
    private JButton playPauseBtn, stepBtn, resetBtn, startHandBtn, nextStageBtn;
    private JButton foldBtn, checkBtn, callBtn, raiseBtn, helpBtn;
    private JSlider speedSlider;
    private JCheckBox omniscientCheck;
    private JPanel humanActionsPanel, communityPanel, playersPanel;
    private JLabel potLabel;

    public MainApplication() {
        setTitle("Texas Holdem with AI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        gameController = new GameController(true, state -> SwingUtilities.invokeLater(() -> updateUI(state)));
        initUI();
        updateUI(gameController.getState());
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 헤더: 제목 + 언어 선택
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JLabel titleLabel = new JLabel(LanguageManager.get("label.gameTitle"));
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        header.add(titleLabel, BorderLayout.WEST);

        JPanel languagePanel = new JPanel();
        languagePanel.add(new JLabel(LanguageManager.get("label.language")));
        JComboBox<LanguageManager.Language> languageCombo = new JComboBox<>(LanguageManager.Language.values());
        languageCombo.setSelectedItem(LanguageManager.getLanguage());
        languageCombo.addActionListener(e -> {
            LanguageManager.setLanguage((LanguageManager.Language) languageCombo.getSelectedItem());
            updateAllLabels();
            updateUI(gameController.getState());
        });
        languagePanel.add(languageCombo);
        header.add(languagePanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // 메인 패널
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 중앙: 게임 테이블
        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(LanguageManager.get("panel.pokerTable")));

        potLabel = new JLabel(LanguageManager.get("label.pot") + " 0", SwingConstants.CENTER);
        potLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        potLabel.setForeground(Color.GREEN);
        tablePanel.add(potLabel, BorderLayout.CENTER);

        communityPanel = new JPanel();
        communityPanel.setBorder(BorderFactory.createTitledBorder(LanguageManager.get("panel.communityCards")));
        tablePanel.add(communityPanel, BorderLayout.NORTH);

        humanActionsPanel = new JPanel();
        humanActionsPanel.setBorder(BorderFactory.createTitledBorder(LanguageManager.get("panel.yourActions")));
        
        foldBtn = createButton("button.fold", "tooltip.fold", e -> gameController.humanAction("fold"));
        checkBtn = createButton("button.check", "tooltip.check", e -> gameController.humanAction("check"));
        callBtn = createButton("button.call", "tooltip.call", e -> gameController.humanAction("call"));
        raiseBtn = createButton("button.raise", "tooltip.raise", e -> gameController.humanAction("raise"));
        
        humanActionsPanel.add(foldBtn);
        humanActionsPanel.add(checkBtn);
        humanActionsPanel.add(callBtn);
        humanActionsPanel.add(raiseBtn);
        tablePanel.add(humanActionsPanel, BorderLayout.SOUTH);

        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // 왼쪽: 플레이어 정보
        playersPanel = new JPanel();
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        playersPanel.setBorder(BorderFactory.createTitledBorder(LanguageManager.get("panel.players")));
        playersPanel.setPreferredSize(new Dimension(220, 0));
        JScrollPane playersScroll = new JScrollPane(playersPanel);
        mainPanel.add(playersScroll, BorderLayout.WEST);

        // 오른쪽: 컨트롤
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder(LanguageManager.get("panel.gameControls")));
        controlPanel.setPreferredSize(new Dimension(190, 0));

        playPauseBtn = createButton("button.play", "tooltip.play", e -> {
            gameController.setPlaying(!gameController.isPlaying());
            updateUI(gameController.getState());
        });
        stepBtn = createButton("button.step", "tooltip.step", e -> gameController.step());
        resetBtn = createButton("button.reset", "tooltip.reset", e -> gameController.init(true));
        startHandBtn = createButton("button.startHand", "tooltip.startHand", e -> gameController.startHand());
        nextStageBtn = createButton("button.nextStage", "tooltip.nextStage", e -> gameController.nextStage());

        controlPanel.add(playPauseBtn);
        controlPanel.add(stepBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(startHandBtn);
        controlPanel.add(nextStageBtn);

        JPanel speedPanel = new JPanel();
        speedPanel.add(new JLabel(LanguageManager.get("label.speed")));
        speedSlider = new JSlider(100, 2000, 700);
        speedSlider.setToolTipText(LanguageManager.get("tooltip.speed"));
        speedSlider.addChangeListener(e -> gameController.setSpeed(speedSlider.getValue()));
        speedPanel.add(speedSlider);
        controlPanel.add(speedPanel);

        omniscientCheck = new JCheckBox(LanguageManager.get("checkbox.omniscient"));
        omniscientCheck.setToolTipText(LanguageManager.get("tooltip.omniscient"));
        omniscientCheck.addActionListener(e -> {
            gameController.toggleOmniscient();
            updateUI(gameController.getState());
        });
        controlPanel.add(omniscientCheck);

        helpBtn = createButton("button.help", "tooltip.help", e -> showHelpDialog());
        controlPanel.add(helpBtn);

        mainPanel.add(controlPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        // 로그
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(LanguageManager.get("panel.actionLog")));
        add(scrollPane, BorderLayout.SOUTH);
    }

    private JButton createButton(String labelKey, String tooltipKey, javax.swing.event.ChangeListener action) {
        JButton btn = new JButton(LanguageManager.get(labelKey));
        btn.setToolTipText(LanguageManager.get(tooltipKey));
        if (action instanceof java.awt.event.ActionListener) {
            btn.addActionListener((java.awt.event.ActionListener) action);
        }
        return btn;
    }

    private void updateAllLabels() {
        playPauseBtn.setText(gameController.isPlaying() ? LanguageManager.get("button.pause") : LanguageManager.get("button.play"));
        stepBtn.setText(LanguageManager.get("button.step"));
        resetBtn.setText(LanguageManager.get("button.reset"));
        startHandBtn.setText(LanguageManager.get("button.startHand"));
        nextStageBtn.setText(LanguageManager.get("button.nextStage"));
        
        foldBtn.setText(LanguageManager.get("button.fold"));
        foldBtn.setToolTipText(LanguageManager.get("tooltip.fold"));
        checkBtn.setText(LanguageManager.get("button.check"));
        checkBtn.setToolTipText(LanguageManager.get("tooltip.check"));
        callBtn.setText(LanguageManager.get("button.call"));
        callBtn.setToolTipText(LanguageManager.get("tooltip.call"));
        raiseBtn.setText(LanguageManager.get("button.raise"));
        raiseBtn.setToolTipText(LanguageManager.get("tooltip.raise"));
        
        omniscientCheck.setText(LanguageManager.get("checkbox.omniscient"));
        helpBtn.setText(LanguageManager.get("button.help"));
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
            StringBuilder sb = new StringBuilder();
            
            if (i == state.dealer) sb.append(LanguageManager.get("position.dealer")).append(" ");
            if (i == state.smallBlindIdx) sb.append(LanguageManager.get("position.smallBlind")).append(" ");
            if (i == state.bigBlindIdx) sb.append(LanguageManager.get("position.bigBlind")).append(" ");

            sb.append(p.name).append(": ").append(p.stack).append(" ").append(LanguageManager.get("player.chips"));
            if (p.streetBet > 0) {
                sb.append(" (").append(LanguageManager.get("player.bet")).append(" ").append(p.streetBet).append(")");
            }
            if (p.isHuman || gameController.isOmniscient()) {
                sb.append(" [").append(formatCards(p.hole)).append("]");
            }

            JLabel playerLabel = new JLabel(sb.toString());
            if (i == state.currentPlayer) {
                playerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
                playerLabel.setForeground(Color.BLUE);
                playerLabel.setText("→ " + sb.toString());
            }
            if (!p.inHand) {
                playerLabel.setForeground(Color.GRAY);
            }
            playersPanel.add(playerLabel);
        }
        playersPanel.revalidate();
        playersPanel.repaint();

        potLabel.setText(LanguageManager.get("label.pot") + " " + state.pot);
        playPauseBtn.setText(gameController.isPlaying() ? LanguageManager.get("button.pause") : LanguageManager.get("button.play"));
        omniscientCheck.setSelected(gameController.isOmniscient());

        Component[] components = humanActionsPanel.getComponents();
        for (Component c : components) {
            if (c instanceof JButton) {
                c.setEnabled(state.waitingForHuman);
            }
        }

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
        JTextArea textArea = new JTextArea(LanguageManager.get("help.content"));
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, LanguageManager.get("help.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainApplication().setVisible(true));
    }
}