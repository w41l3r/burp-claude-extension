package com.claudeburp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class ClaudeExtension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Claude AI Analyzer");

        ClaudeApiClient apiClient = new ClaudeApiClient();
        ClaudePanel panel = new ClaudePanel(api, apiClient);

        api.userInterface().registerSuiteTab("Claude AI", panel.getComponent());
        api.userInterface().registerContextMenuItemsProvider(new ClaudeContextMenu(panel));

        api.logging().logToOutput("[Claude AI] Extension loaded successfully.");
    }
}
