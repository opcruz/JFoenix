package com.jfoenix.skins;

import com.jfoenix.controls.JFXPasswordField;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.css.PseudoClass;

public class JFXPasswordFieldSkin extends JFXTextFieldSkin<JFXPasswordField> {

    // For use with PasswordField
    public static final char BULLET = '\u25cf';

    private final PseudoClass PSEUDO_UNMASKED = PseudoClass.getPseudoClass("unmasked");
    private final InvalidationListener invalidationListener = observable ->
        getSkinnable().pseudoClassStateChanged(PSEUDO_UNMASKED, !((JFXPasswordField) getSkinnable()).isMaskedText());

    private final WeakInvalidationListener maskedListener = new WeakInvalidationListener(invalidationListener);

    public JFXPasswordFieldSkin(JFXPasswordField passwordField) {
        super(passwordField);
        passwordField.maskedTextProperty().addListener(maskedListener);
        invalidationListener.invalidated(null);
        registerChangeListener(passwordField.maskedTextProperty(), obs -> handleMaskedTextProperty());
    }

    private void handleMaskedTextProperty() {
        getSkinnable().appendText("#");
        final int length = getSkinnable().getLength();
        getSkinnable().deleteText(length - 1, length);
    }

    @Override
    protected String maskText(String txt) {
        // handler mask text
        if (getSkinnable() != null && ((JFXPasswordField) getSkinnable()).isMaskedText()) {
            int n = txt.length();
            StringBuilder passwordBuilder = new StringBuilder(n);
            for (int i = 0; i < n; i++) {
                passwordBuilder.append(BULLET);
            }
            return passwordBuilder.toString();
        } else {
            return txt;
        }
    }
}
