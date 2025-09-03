package ua.naiksoftware.opencodeidea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiService;
import ua.naiksoftware.opencodeidea.services.OpenCodeApiServiceImpl;
import org.jetbrains.annotations.NotNull;

public class ExplainCodeAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            return;
        }
        
        OpenCodeApiService apiService = OpenCodeApiServiceImpl.getInstance();

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedCode = selectionModel.getSelectedText();
        
        if (selectedCode == null || selectedCode.trim().isEmpty()) {
            Messages.showWarningDialog(project,
                    "Please select some code to explain",
                    "No Code Selected");
            return;
        }
        
        apiService.explainCode(selectedCode).whenComplete((explanation, throwable) -> {
            if (throwable != null) {
                Messages.showErrorDialog(project,
                        "Error: " + throwable.getMessage(),
                        "OpenCode API Error");
            } else {
                Messages.showInfoMessage(project, explanation, "Code Explanation");
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(editor != null);
    }
}