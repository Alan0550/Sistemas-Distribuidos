package edu.upb.desktop.util;

import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

public final class SpinnerUtil {
    private SpinnerUtil() {
    }

    public static void makeEditableInteger(Spinner<Integer> spinner) {
        if (spinner == null) {
            return;
        }
        spinner.setEditable(true);
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            return newText.matches("\\d*") ? change : null;
        };
        spinner.getEditor().setTextFormatter(new TextFormatter<String>(filter));
    }
}
