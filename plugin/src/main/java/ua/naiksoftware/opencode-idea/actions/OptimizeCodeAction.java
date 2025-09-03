package ua.naiksoftware.opencode-idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiService;
import ua.naiksoftware.opencode-idea.services.OpenCodeApiServiceImpl;
import org.jetbrains.annotations.NotNull;

public class OptimizeCodeAction extends AnAction {
    
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
                    "Please select some code to optimize",
                    "No Code Selected");
            return;
        }
        
        apiService.optimizeCode(selectedCode).whenComplete((optimizedCode, throwable) -> {
            if (throwable != null) {
                Messages.showErrorDialog(project,
                        "Error: " + throwable.getMessage(),
                        "OpenCode API Error");
            } else {
                int choice = Messages.showYesNoDialog(project,
                        "Optimized code:\n\n" + optimizedCode + "\n\nReplace selected code?",
                        "OpenCode Optimization",
                        Messages.getQuestionIcon());
                
                if (choice == Messages.YES) {
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        Document document = editor.getDocument();
                        int startOffset = selectionModel.getSelectionStart();
                        int endOffset = selectionModel.getSelectionEnd();
                        document.replaceString(startOffset, endOffset, optimizedCode);
                    });
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(editor != null);
    }
}