package com.claudeburp;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ClaudeContextMenu implements ContextMenuItemsProvider {

    private final ClaudePanel panel;

    public ClaudeContextMenu(ClaudePanel panel) {
        this.panel = panel;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        boolean hasSelection = !event.selectedRequestResponses().isEmpty();
        boolean hasEditor    = event.messageEditorRequestResponse().isPresent();

        if (!hasSelection && !hasEditor) {
            return List.of();
        }

        JMenuItem item = new JMenuItem("Analyze with Claude AI");
        item.addActionListener(e -> {
            HttpRequestResponse reqRes = hasSelection
                    ? event.selectedRequestResponses().get(0)
                    : event.messageEditorRequestResponse().get().requestResponse();
            panel.setRequest(reqRes);
        });

        return List.of(item);
    }
}
