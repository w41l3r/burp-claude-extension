package com.claudeburp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.persistence.Preferences;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class ClaudePanel {

    private static final String PREF_API_KEY = "claude_api_key";
    private static final String PREF_MODEL   = "claude_model";

    private static final String[] MODELS = {
        "claude-sonnet-4-6",
        "claude-opus-4-7",
        "claude-haiku-4-5-20251001"
    };

    private final MontoyaApi api;
    private final ClaudeApiClient apiClient;
    private final Preferences prefs;

    private JPanel mainPanel;
    private JPasswordField apiKeyField;
    private JCheckBox showKeyCheckbox;
    private JComboBox<String> modelCombo;
    private JTextArea requestArea;
    private JTextArea systemPromptArea;
    private JTextArea responseArea;
    private JButton analyzeButton;
    private JLabel statusLabel;

    public ClaudePanel(MontoyaApi api, ClaudeApiClient apiClient) {
        this.api = api;
        this.apiClient = apiClient;
        this.prefs = api.persistence().preferences();
        buildUI();
        loadPreferences();
    }

    private void buildUI() {
        mainPanel = new JPanel(new BorderLayout(6, 6));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── Top: API key + model ──────────────────────────────────────────────
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        configPanel.add(new JLabel("API Key:"));
        apiKeyField = new JPasswordField(32);
        apiKeyField.setToolTipText("Your Anthropic API key (sk-ant-...)");
        configPanel.add(apiKeyField);

        showKeyCheckbox = new JCheckBox("Show");
        showKeyCheckbox.addActionListener(e ->
            apiKeyField.setEchoChar(showKeyCheckbox.isSelected() ? (char) 0 : '•'));
        configPanel.add(showKeyCheckbox);

        configPanel.add(Box.createHorizontalStrut(12));
        configPanel.add(new JLabel("Model:"));
        modelCombo = new JComboBox<>(MODELS);
        configPanel.add(modelCombo);

        JButton saveConfigButton = new JButton("Save");
        saveConfigButton.setToolTipText("Persist API key and model selection");
        saveConfigButton.addActionListener(e -> savePreferences());
        configPanel.add(saveConfigButton);

        // ── Center: three-way split ───────────────────────────────────────────
        // Left column: system prompt (top) + HTTP input (bottom)
        systemPromptArea = new JTextArea(5, 40);
        systemPromptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        JScrollPane promptScroll = new JScrollPane(systemPromptArea);
        promptScroll.setBorder(titledBorder("System Prompt (leave blank for default)"));

        requestArea = new JTextArea();
        requestArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane requestScroll = new JScrollPane(requestArea);
        requestScroll.setBorder(titledBorder("HTTP Request / Response"));

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, promptScroll, requestScroll);
        leftSplit.setDividerLocation(120);
        leftSplit.setResizeWeight(0.2);

        // Right column: Claude's analysis
        responseArea = new JTextArea();
        responseArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        responseArea.setEditable(false);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        JScrollPane responseScroll = new JScrollPane(responseArea);
        responseScroll.setBorder(titledBorder("Claude Analysis"));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, responseScroll);
        centerSplit.setDividerLocation(0.5);
        centerSplit.setResizeWeight(0.5);

        // ── Bottom: action bar ────────────────────────────────────────────────
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        analyzeButton = new JButton("Analyze with Claude");
        analyzeButton.setFont(analyzeButton.getFont().deriveFont(Font.BOLD));
        analyzeButton.addActionListener(e -> runAnalysis());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            requestArea.setText("");
            responseArea.setText("");
            statusLabel.setText("Ready.");
        });

        statusLabel = new JLabel("Ready.");
        statusLabel.setForeground(Color.DARK_GRAY);

        actionPanel.add(analyzeButton);
        actionPanel.add(clearButton);
        actionPanel.add(new JSeparator(SwingConstants.VERTICAL));
        actionPanel.add(statusLabel);

        mainPanel.add(configPanel, BorderLayout.NORTH);
        mainPanel.add(centerSplit, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
    }

    private void runAnalysis() {
        String apiKey = new String(apiKeyField.getPassword()).trim();
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel,
                "Enter your Anthropic API key before analyzing.",
                "Missing API Key", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String content = requestArea.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel,
                "Paste an HTTP request/response first, or use right-click → 'Analyze with Claude AI'.",
                "No Content", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String model       = (String) modelCombo.getSelectedItem();
        String systemPrompt = systemPromptArea.getText().trim();

        analyzeButton.setEnabled(false);
        statusLabel.setText("Sending to Claude...");
        responseArea.setText("");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.analyze(apiKey, model, systemPrompt, content);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    responseArea.setText(result);
                    responseArea.setCaretPosition(0);
                    statusLabel.setText("Analysis complete.");
                } catch (ExecutionException ex) {
                    responseArea.setText("Error: " + ex.getCause().getMessage());
                    statusLabel.setText("Analysis failed.");
                    api.logging().logToError("[Claude AI] " + ex.getCause().getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    statusLabel.setText("Interrupted.");
                } finally {
                    analyzeButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    public void setRequest(HttpRequestResponse reqRes) {
        StringBuilder sb = new StringBuilder();
        if (reqRes.request() != null) {
            sb.append("=== REQUEST ===\n").append(reqRes.request().toString());
        }
        if (reqRes.response() != null) {
            sb.append("\n\n=== RESPONSE ===\n").append(reqRes.response().toString());
        }
        SwingUtilities.invokeLater(() -> {
            requestArea.setText(sb.toString());
            requestArea.setCaretPosition(0);
            responseArea.setText("");
            statusLabel.setText("Request loaded. Click 'Analyze with Claude'.");
        });
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    private void savePreferences() {
        prefs.setString(PREF_API_KEY, new String(apiKeyField.getPassword()));
        prefs.setString(PREF_MODEL, (String) modelCombo.getSelectedItem());
        statusLabel.setText("Configuration saved.");
    }

    private void loadPreferences() {
        String savedKey = prefs.getString(PREF_API_KEY);
        if (savedKey != null) apiKeyField.setText(savedKey);

        String savedModel = prefs.getString(PREF_MODEL);
        if (savedModel != null) {
            for (int i = 0; i < modelCombo.getItemCount(); i++) {
                if (modelCombo.getItemAt(i).equals(savedModel)) {
                    modelCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private static TitledBorder titledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), title);
    }
}
