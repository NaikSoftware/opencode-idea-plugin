package ua.naiksoftware.opencode-idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiService;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiServiceImpl;
import org.jetbrains.annotations.NotNull;

public class AskOpenCodeAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            return;
        }
        
        OpenCodeApiService apiService = OpenCodeApiServiceImpl.getInstance();
        if (!apiService.isConfigured()) {
            Messages.showErrorDialog(project, 
                    "Please configure the OpenCode API in Settings > Tools > OpenCode AI Assistant",
                    "OpenCode Not Configured");
            return;
        }
        
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedCode = selectionModel.getSelectedText();
        
        if (selectedCode == null || selectedCode.trim().isEmpty()) {
            Messages.showWarningDialog(project, 
                    "Please select some code to ask about",
                    "No Code Selected");
            return;
        }
        
        String question = Messages.showInputDialog(project,
                "What would you like to ask about this code?",
                "Ask OpenCode AI",
                Messages.getQuestionIcon());
        
        if (question != null && !question.trim().isEmpty()) {
            apiService.sendRequest(question, selectedCode).whenComplete((response, throwable) -> {
                if (throwable != null) {
                    Messages.showErrorDialog(project,
                            "Error: " + throwable.getMessage(),
                            "OpenCode API Error");
                } else {
                    Messages.showInfoMessage(project, response, "OpenCode Response");
                }
            });
        }
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(editor != null);
    }
}