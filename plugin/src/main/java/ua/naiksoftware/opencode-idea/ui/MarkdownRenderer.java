package ua.naiksoftware.opencodeidea.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class MarkdownRenderer {
    private static final Parser parser;
    private static final HtmlRenderer renderer;
    private static final HTMLEditorKit editorKit;
    
    static {
        List<Extension> extensions = Arrays.asList(TablesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).build();
        
        editorKit = new HTMLEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        
        // Add CSS styles for better markdown rendering - simplified for compatibility
        try {
            styleSheet.addRule("body { font-family: monospace; font-size: 12px; margin: 8px; }");
            styleSheet.addRule("pre { background-color: #f5f5f5; padding: 8px; }");
            styleSheet.addRule("code { background-color: #f0f0f0; padding: 2px; font-family: monospace; }");
            styleSheet.addRule("blockquote { margin-left: 0; padding-left: 16px; color: #666666; }");
            styleSheet.addRule("table { border-collapse: collapse; width: 100%; margin: 8px; }");
            styleSheet.addRule("th { background-color: #f2f2f2; font-weight: bold; }");
            styleSheet.addRule("td { padding: 8px; }");
            styleSheet.addRule("th { padding: 8px; }");
            styleSheet.addRule("h1 { font-size: 16px; margin: 16px; }");
            styleSheet.addRule("h2 { font-size: 15px; margin: 12px; }");
            styleSheet.addRule("h3 { font-size: 14px; margin: 10px; }");
            styleSheet.addRule("p { margin: 8px; }");
            styleSheet.addRule("ul { margin: 8px; padding-left: 24px; }");
            styleSheet.addRule("ol { margin: 8px; padding-left: 24px; }");
            styleSheet.addRule("li { margin: 2px; }");
        } catch (Exception e) {
            // If CSS parsing fails, continue without custom styles
            System.err.println("Warning: Could not apply CSS styles to MarkdownRenderer: " + e.getMessage());
        }
    }
    
    public static String renderToHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        
        Node document = parser.parse(markdown);
        String html = renderer.render(document);
        
        // Wrap in a styled HTML document with theme-aware CSS
        return "<html><head><style>" + getThemeAwareCSS() + "</style></head><body>" + html + "</body></html>";
    }
    
    public static HTMLEditorKit getEditorKit() {
        return editorKit;
    }
    
    private static String getThemeAwareCSS() {
        // Get theme colors
        Color textColor = UIUtil.getLabelForeground();
        Color backgroundColor = UIUtil.getPanelBackground();
        Color codeBackgroundColor = UIUtil.getTextFieldBackground();
        Color borderColor = UIUtil.getBoundsColor();
        
        // Convert colors to hex strings
        String textHex = String.format("#%02x%02x%02x", textColor.getRed(), textColor.getGreen(), textColor.getBlue());
        String bgHex = String.format("#%02x%02x%02x", backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue());
        String codeBgHex = String.format("#%02x%02x%02x", codeBackgroundColor.getRed(), codeBackgroundColor.getGreen(), codeBackgroundColor.getBlue());
        String borderHex = String.format("#%02x%02x%02x", borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue());
        
        // Theme-aware CSS using IntelliJ colors
        return String.format("""
            body { 
                font-family: sans-serif; 
                font-size: 13px; 
                margin: 8px; 
                color: %s;
                background-color: %s;
            }
            pre { 
                background-color: %s; 
                padding: 8px; 
                font-family: monospace;
                font-size: 12px;
                border: 1px solid %s;
                border-radius: 4px;
            }
            code { 
                background-color: %s; 
                padding: 2px 4px; 
                font-family: monospace; 
                font-size: 12px;
                border-radius: 3px;
            }
            blockquote { 
                margin: 8px 0; 
                padding-left: 16px; 
                color: %s;
                border-left: 4px solid %s;
            }
            table { 
                width: 100%%; 
                margin: 8px 0;
                border-collapse: collapse;
            }
            th { 
                background-color: %s; 
                font-weight: bold; 
                padding: 8px;
                border: 1px solid %s;
            }
            td { 
                padding: 8px; 
                border: 1px solid %s;
            }
            h1, h2, h3 { 
                margin: 16px 0 8px 0; 
                font-weight: bold;
                color: %s;
            }
            h1 { font-size: 18px; }
            h2 { font-size: 16px; }
            h3 { font-size: 14px; }
            p { margin: 8px 0; }
            ul, ol { margin: 8px 0; padding-left: 20px; }
            li { margin: 2px 0; }
            a { color: %s; }
            strong { font-weight: bold; }
            em { font-style: italic; }
            """, 
            textHex, bgHex, codeBgHex, borderHex, codeBgHex, textHex, borderHex, 
            codeBgHex, borderHex, borderHex, textHex, textHex);
    }
}