package org.shunya.dli;

import org.markdown4j.Markdown4jProcessor;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class HtmlContentPanel extends JPanel {
    private JTextPane jTextPane;
    private final Markdown4jProcessor markdown4jProcessor = new Markdown4jProcessor();

    public HtmlContentPanel(String url) {
        setLayout(new GridLayout(1, 0));
        jTextPane = new JTextPane();
        jTextPane.setContentType("text/html");
        jTextPane.setMargin(new Insets(5, 5, 5, 5));
        jTextPane.setEditable(false);
        final HTMLEditorKit kit = new HTMLEditorKit();
        jTextPane.setEditorKit(kit);
        jTextPane.setContentType(kit.getContentType());
        jTextPane.getCaret().setVisible(false);
        setHtmlDocFont();

        JScrollPane pane = new JScrollPane(jTextPane);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(pane);

        final InputStream resourceAsStream = HtmlContentPanel.class.getClassLoader().getResourceAsStream(url);
        try {
            jTextPane.setText(markdown4jProcessor.process(Utils.readContent(resourceAsStream)));
            jTextPane.setCaretPosition(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setHtmlDocFont() {
        Font font = new Font("Arial", Font.TRUETYPE_FONT, 12);
        String bodyRule = "body { font-family: " + font.getFamily() + "; " + "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) jTextPane.getStyledDocument()).getStyleSheet().addRule(bodyRule);
    }
}
