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
import com.jfoenix.controls.behavior.JFXGenericPickerBehavior;
import com.sun.javafx.scene.control.IDisconnectable;
import com.sun.javafx.scene.control.ListenerHelper;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxBaseSkin;
import javafx.scene.control.skin.ComboBoxPopupControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class JFXGenericPickerSkin<T> extends ComboBoxPopupControl<T> {

    protected JFXGenericPickerBehavior<T> behavior;

    private final EventHandler<MouseEvent> mouseEnteredEventHandler = event -> behavior.mouseEntered(event);
    private final EventHandler<MouseEvent> mousePressedEventHandler = event -> {
        behavior.mousePressed(event);
        event.consume();
    };
    private final EventHandler<MouseEvent> mouseReleasedEventHandler = event -> {
        behavior.mouseReleased(event);
        event.consume();
    };
    private final EventHandler<MouseEvent> mouseExitedEventHandler = event -> behavior.mouseExited(event);

    // reference of the arrow button node in getChildren (not the actual field)
    protected StackPane arrowButton;
    protected PopupControl popup;

    protected final List<IDisconnectable> iDisconnectables;

    public JFXGenericPickerSkin(ComboBoxBase<T> comboBoxBase) {
        super(comboBoxBase);
        behavior = new JFXGenericPickerBehavior<>(comboBoxBase);

        iDisconnectables = getItemsListenerHelper();
        // remove all parent listeners except KeyEvent (the last one added)
        for (int i = iDisconnectables.size() - 2; i >= 0; i--) {
            IDisconnectable d = iDisconnectables.remove(i);
            d.disconnect();
        }

        initAndUnregisterParentArrowButton();
        updateArrowButtonListeners();
        registerChangeListener(comboBoxBase.editableProperty(), obs-> {
            updateArrowButtonListeners();
            reflectUpdateDisplayArea();
        });

        initPopupAndRemoveParentListener();
    }

    private List<IDisconnectable> getItemsListenerHelper() {
        ListenerHelper lh = ListenerHelper.get(this);
        return ReflectionHelper.getFieldContent(ListenerHelper.class, lh, "items");
    }

    private void parentArrowTerminator(String handlerName, EventType<?> eventType) {
        EventHandler<? super javafx.event.Event> handler = ReflectionHelper.getFieldContent(ComboBoxBaseSkin.class, this, handlerName);
        arrowButton.removeEventHandler(eventType, handler);
    }

    private void initAndUnregisterParentArrowButton() {
        arrowButton = (StackPane) getChildren().get(0);
        parentArrowTerminator("mouseEnteredEventHandler", MouseEvent.MOUSE_ENTERED);
        parentArrowTerminator("mousePressedEventHandler", MouseEvent.MOUSE_PRESSED);
        parentArrowTerminator("mouseReleasedEventHandler", MouseEvent.MOUSE_RELEASED);
        parentArrowTerminator("mouseExitedEventHandler", MouseEvent.MOUSE_EXITED);
    }

    private void initPopupAndRemoveParentListener() {
        popup = ReflectionHelper.invoke(ComboBoxPopupControl.class, this, "getPopup");
        popup.setOnAutoHide(event -> behavior.onAutoHide(popup)); // override parent auto hide
        iDisconnectables.get(1).disconnect(); // unregister popup mouse clicked
        popup.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> behavior.onAutoHide(popup));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.behavior != null) {
            this.behavior.dispose();
        }
    }

    /***************************************************************************
     *                                                                         *
     * Reflections internal API                                                *
     *                                                                         *
     **************************************************************************/

    private void updateArrowButtonListeners() {
        if (getSkinnable().isEditable()) {
            arrowButton.addEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
            arrowButton.addEventHandler(MouseEvent.MOUSE_PRESSED, mousePressedEventHandler);
            arrowButton.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
            arrowButton.addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedEventHandler);
        } else {
            arrowButton.removeEventHandler(MouseEvent.MOUSE_ENTERED, mouseEnteredEventHandler);
            arrowButton.removeEventHandler(MouseEvent.MOUSE_PRESSED, mousePressedEventHandler);
            arrowButton.removeEventHandler(MouseEvent.MOUSE_RELEASED, mouseReleasedEventHandler);
            arrowButton.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseExitedEventHandler);
        }
    }


    /***************************************************************************
     *                                                                         *
     * Reflections internal API for ComboBoxPopupControl                       *
     *                                                                         *
     **************************************************************************/

    private final HashMap<String, Method> parentCachedMethods = new HashMap<>();

    Function<String, Method> methodSupplier = name -> {
        if(!parentCachedMethods.containsKey(name)){
            try {
                Method method = ReflectionHelper.getMethod(ComboBoxPopupControl.class, name);
                parentCachedMethods.put(name, method);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return parentCachedMethods.get(name);
    };

    Consumer<Method> methodInvoker = method -> {
        try {
            method.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    Function<Method, Object> methodReturnInvoker = method -> {
        try {
            return method.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };

    protected void reflectUpdateDisplayArea() {
        methodInvoker.accept(methodSupplier.apply("updateDisplayArea"));
    }

    protected void reflectSetTextFromTextFieldIntoComboBoxValue() {
        methodInvoker.accept(methodSupplier.apply("setTextFromTextFieldIntoComboBoxValue"));
    }

    protected TextField reflectGetEditableInputNode(){
        return (TextField) methodReturnInvoker.apply(methodSupplier.apply("getEditableInputNode"));
    }

    protected void reflectUpdateDisplayNode() {
        methodInvoker.accept(methodSupplier.apply("updateDisplayNode"));
    }
}
