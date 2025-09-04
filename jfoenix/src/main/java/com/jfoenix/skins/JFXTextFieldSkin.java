/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.jfoenix.skins;

import com.jfoenix.adapters.ReflectionHelper;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.base.IFXLabelFloatControl;
import com.jfoenix.utils.JFXNodeUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.skin.TextFieldSkin;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * <h1>Material Design Text input control Skin, used for both JFXTextField/JFXPasswordField</h1>
 *
 * @author Shadi Shaheen
 * @version 2.0
 * @since 2017-01-25
 */
public class JFXTextFieldSkin<T extends JFXTextField & IFXLabelFloatControl> extends TextFieldSkin {

    private boolean invalid = true;

    private Text promptText;
    private Pane textPane;
    private Node textNode;
    private ObservableDoubleValue textRight;
    private DoubleProperty textTranslateX;

    private ValidationPane<T> errorContainer;
    private PromptLinesWrapper<T> linesWrapper;

    public JFXTextFieldSkin(T textField) {
        super(textField);
        textPane = (Pane) this.getChildren().get(0);
        // get parent fields
        textNode = ReflectionHelper.getFieldContent(TextFieldSkin.class, this, "textNode");
        textTranslateX = ReflectionHelper.getFieldContent(TextFieldSkin.class, this, "textTranslateX");
        textRight = ReflectionHelper.getFieldContent(TextFieldSkin.class, this, "textRight");

        linesWrapper = new PromptLinesWrapper<T>(
            textField,
            promptTextFillProperty(),
            textField.textProperty(),
            textField.promptTextProperty(),
            () -> promptText);

        linesWrapper.init(() -> createPromptNode(), textPane);

        ReflectionHelper.setFieldContent(TextFieldSkin.class, this, "usePromptText", linesWrapper.usePromptText);

        errorContainer = new ValidationPane<>(textField);

        getChildren().addAll(linesWrapper.line, linesWrapper.focusedLine, linesWrapper.promptContainer, errorContainer);
        updateGraphic(textField.getLeadingGraphic(), "leading");
        updateGraphic(textField.getTrailingGraphic(), "trailing");

        registerChangeListener(textField.disableProperty(), obs -> linesWrapper.updateDisabled());
        registerChangeListener(textField.focusColorProperty(), obs -> linesWrapper.updateFocusColor());
        registerChangeListener(textField.unFocusColorProperty(), obs -> linesWrapper.updateUnfocusColor());
        registerChangeListener(textField.disableAnimationProperty(), obs -> errorContainer.updateClip());
        registerChangeListener(textField.leadingGraphicProperty(), obs -> updateGraphic(((JFXTextField) getSkinnable()).getLeadingGraphic(), "leading"));
        registerChangeListener(textField.trailingGraphicProperty(), obs -> updateGraphic(((JFXTextField) getSkinnable()).getTrailingGraphic(), "trailing"));
    }

    @Override
    protected void layoutChildren(final double x, final double y, final double w, final double h) {
        final double height = getSkinnable().getHeight();
        final Node leadingGraphic = ((JFXTextField) getSkinnable()).getLeadingGraphic();
        final Node trailingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        final double leadingW = leadingGraphic == null ? 0.0 : snapSize(leadingGraphic.prefWidth(height));
        final double trailingW = trailingGraphic == null ? 0.0 : snapSize(trailingGraphic.prefWidth(height));

        final double textX = snapPosition(x) + leadingW;
        final double textW = w - snapSize(leadingW) - snapSize(trailingW);

        super.layoutChildren(textX, y, textW, h);
        linesWrapper.layoutLines(x, y, w, h, height, Math.floor(h));
        linesWrapper.layoutPrompt(textX, y, textW, h);
        errorContainer.layoutPane(x, height + linesWrapper.focusedLine.getHeight(), w, h);

        // draw leading/trailing graphics
        if (leadingGraphic != null) {
            leadingGraphic.resizeRelocate(snappedLeftInset(), 0, leadingW, height);
        }

        if (trailingGraphic != null) {
            trailingGraphic.resizeRelocate(w - trailingW + snappedLeftInset(), 0, trailingW, height);
        }

        if (getSkinnable().getWidth() > 0) {
            updateTextPos();
        }

        linesWrapper.updateLabelFloatLayout();

        if (invalid) {
            invalid = false;
            // update validation container
            errorContainer.invalid(w);
            // focus
            linesWrapper.invalid();
        }
    }

    private void updateGraphic(Node graphic, String id) {
        Node old = getSkinnable().lookup("#" + id);
        getChildren().remove(old);
        if (graphic != null) {
            graphic.setId(id);
            graphic.setManaged(false);
            // add tab events handler as there is a bug in javafx traversing engine
//            TODO REVIEW
            Set<Control> controls = JFXNodeUtils.getAllChildren(graphic, Control.class);
            controls.forEach(control -> {
                control.addEventHandler(KeyEvent.KEY_PRESSED, JFXNodeUtils.TRAVERSE_HANDLER);
                control.addEventHandler(KeyEvent.KEY_TYPED, Event::consume);
            });

            getChildren().add(graphic);
        }
    }

    private void updateTextPos() {
        double textWidth = textNode.getLayoutBounds().getWidth();
        final double promptWidth = promptText == null ? 0 : promptText.getLayoutBounds().getWidth();
        switch (getSkinnable().getAlignment().getHpos()) {
            case CENTER:
                linesWrapper.promptTextScale.setPivotX(promptWidth / 2);
                double midPoint = textRight.get() / 2;
                double newX = midPoint - textWidth / 2;
                if (newX + textWidth <= textRight.get()) {
                    textTranslateX.set(newX);
                }
                break;
            case LEFT:
                linesWrapper.promptTextScale.setPivotX(0);
                break;
            case RIGHT:
                linesWrapper.promptTextScale.setPivotX(promptWidth);
                break;
        }

    }

    private void createPromptNode() {
        if (promptText != null || !linesWrapper.usePromptText.get()) {
            return;
        }
        promptText = new Text();
        promptText.setManaged(false);
        promptText.getStyleClass().add("text");
        promptText.visibleProperty().bind(linesWrapper.usePromptText);
        promptText.fontProperty().bind(getSkinnable().fontProperty());
        promptText.textProperty().bind(getSkinnable().promptTextProperty());
        promptText.fillProperty().bind(linesWrapper.animatedPromptTextFill);
        promptText.setLayoutX(1);
        promptText.getTransforms().add(linesWrapper.promptTextScale);
        linesWrapper.promptContainer.getChildren().add(promptText);
        if (getSkinnable().isFocused() && ((IFXLabelFloatControl) getSkinnable()).isLabelFloat()) {
            promptText.setTranslateY(-Math.floor(textPane.getHeight()));
            linesWrapper.promptTextScale.setX(0.85);
            linesWrapper.promptTextScale.setY(0.85);
        }

        try {
            Field field = ReflectionHelper.getField(TextFieldSkin.class, "promptNode");
            Object oldValue = field.get(this);
            if (oldValue != null) {
                textPane.getChildren().remove(oldValue);
            }
            field.set(this, promptText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected double computePrefWidth(double h, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double w = super.computePrefWidth(h, topInset, rightInset, bottomInset, leftInset);
        Node leadingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        Node trailingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        final double leadingW = leadingGraphic == null ? 0.0 : snapSizeX(leadingGraphic.prefWidth(h));
        final double trailingW = trailingGraphic == null ? 0.0 : snapSizeX(trailingGraphic.prefWidth(h));
        return w + trailingW + leadingW;
    }

    @Override
    protected double computePrefHeight(double w, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double h = super.computePrefHeight(w, topInset, rightInset, bottomInset, leftInset);
        Node leadingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        Node trailingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        final double leadingH = leadingGraphic == null ? 0.0 : snapSizeX(leadingGraphic.prefHeight(w));
        final double trailingH = trailingGraphic == null ? 0.0 : snapSizeX(trailingGraphic.prefHeight(w));
        return Math.max(Math.max(h, leadingH), trailingH);
    }

    @Override
    protected double computeMinWidth(double h, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double w = super.computeMinWidth(h, topInset, rightInset, bottomInset, leftInset);
        Node leadingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        Node trailingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        final double leadingW = leadingGraphic == null ? 0.0 : snapSizeX(leadingGraphic.minWidth(h));
        final double trailingW = trailingGraphic == null ? 0.0 : snapSizeX(trailingGraphic.minWidth(h));
        return w + trailingW + leadingW;
    }

    @Override
    protected double computeMinHeight(double w, double topInset, double rightInset, double bottomInset, double leftInset) {
        final double h = super.computeMinHeight(w, topInset, rightInset, bottomInset, leftInset);
        Node leadingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        Node trailingGraphic = ((JFXTextField) getSkinnable()).getTrailingGraphic();
        final double leadingH = leadingGraphic == null ? 0.0 : snapSizeX(leadingGraphic.minHeight(w));
        final double trailingH = trailingGraphic == null ? 0.0 : snapSizeX(trailingGraphic.minHeight(w));
        return Math.max(Math.max(h, leadingH), trailingH);
    }

}
