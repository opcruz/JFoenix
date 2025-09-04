package demos;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.skins.JFXComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.function.Consumer;

public class JFXComboBoxInstaller<T> {

  private ObservableList<T> originalItems;

  private final JFXComboBox<T> comboBox;

  private JFXComboBoxInstaller(JFXComboBox<T> comboBox, boolean isAutocompleted) {
    this.comboBox = comboBox;
    if (isAutocompleted) {
      final StringProperty filter = new SimpleStringProperty("");
      final BooleanProperty autocompleteRunning = new SimpleBooleanProperty(false);
      originalItems = FXCollections.observableArrayList(comboBox.getItems());

      comboBox
              .getItems()
              .addListener(
                      (ListChangeListener<T>)
                              c -> {
                                if (!autocompleteRunning.get()) {
                                  originalItems = FXCollections.observableArrayList(comboBox.getItems());
                                }
                              });

      final Popup popup;
      popup = new Popup();
      Label popupContent = new Label();
      popupContent.textProperty().bind(filter);
      VBox vbox = new VBox(popupContent);
      VBox.setMargin(
              popupContent, new Insets(10, 10, 0, 10)); // Margen de 10 píxeles en la parte superior y a la izquierda
      popupContent.setStyle(
              "-fx-background-color: #333333; "
                      + // Fondo oscuro
                      "-fx-border-color: #f0f0f0; "
                      + // Borde claro
                      "-fx-border-width: 1px; "
                      + // Ancho del borde
                      "-fx-padding: 8px 8px 8px 8px; "
                      + // Padding interno
                      "-fx-text-fill: white; "
                      + // Color del texto blanco
                      "-fx-background-radius: 6px; "
                      + // Fondo con esquinas redondeadas
                      "-fx-border-radius: 6px;" // Borde con esquinas redondeadas
      );
      popup.getContent().add(vbox);

      comboBox.setOnHidden(handleOnHiding(filter, popup, autocompleteRunning));
      comboBox.setOnShown(e -> {
        if(!filter.get().isEmpty()) {
          final Window stage = comboBox.getScene().getWindow();
          double posX = stage.getX() + comboBox.localToScene(comboBox.getBoundsInLocal()).getMinX();
          double posY = stage.getY() + comboBox.localToScene(comboBox.getBoundsInLocal()).getMinY();
          popup.show(stage, posX, posY);
//          popup.show();
        }
      });

      this.comboBox.setSkin(
              new CustomComboBoxListViewSkin<>(this.comboBox, handleOnKeyPressed(filter, popup, autocompleteRunning)));
    } else {
      this.comboBox.setSkin(new CustomComboBoxListViewSkin<>(this.comboBox, evt -> {}));
    }

    comboBox.setOnShowing(
            event -> {
              if (comboBox.getValue() != null) {
                ListView<?> popupListView = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
                int index = popupListView.getItems().indexOf(comboBox.getValue());
                if (index != -1) {
                  Platform.runLater(
                          () -> {
                            popupListView.getFocusModel().focus(index);
                            scrollToIfNecessary(comboBox, popupListView, index);
                          });
                }
              }
            });

    comboBox.addEventFilter(
            KeyEvent.KEY_PRESSED,
            event -> {
              if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                comboBox.show();
                event.consume();
              }
            });
  }

  public static <T> void install(JFXComboBox<T> comboBox, boolean isAutocompleted) {
    new JFXComboBoxInstaller<>(comboBox, isAutocompleted);
  }

  public Consumer<KeyEvent> handleOnKeyPressed(
          StringProperty filter, Popup popup, BooleanProperty autocompleteRunning) {
    return e -> {
      autocompleteRunning.set(true);
      ObservableList<T> filteredList;
      KeyCode code = e.getCode();
      String originalValue = filter.getValue();

      if (code.isLetterKey() || code == KeyCode.SPACE) {
        filter.setValue(originalValue += e.getText());
        e.consume();
      }
      if (code == KeyCode.BACK_SPACE && !originalValue.isEmpty()) {
        filter.setValue(originalValue.substring(0, originalValue.length() - 1));
      }
      if (code == KeyCode.ESCAPE) {
        filter.set("");
      }
      if (filter.getValue().isEmpty()) {
        filteredList = originalItems;
        popup.hide();
      } else {
        String filterValue = filter.getValue().toLowerCase();
        filteredList = new FilteredList<>(originalItems, t -> t.toString().toLowerCase().contains(filterValue));
        showPopup(popup);

      }
      comboBox.getItems().setAll(filteredList);
      autocompleteRunning.set(false);
    };
  }

  private void showPopup(Popup popup) {
    final Window stage = comboBox.getScene().getWindow();
    double posX = stage.getX() + comboBox.localToScene(comboBox.getBoundsInLocal()).getMinX();
    double posY = stage.getY() + comboBox.localToScene(comboBox.getBoundsInLocal()).getMinY();
    popup.show(stage, posX, posY);
  }

  public EventHandler<Event> handleOnHiding(StringProperty filter, Popup popup, BooleanProperty autocompleteRunning) {
    return e -> {
//      filter.setValue("");
      popup.hide();
//      Platform.runLater(
//              () -> {
//                autocompleteRunning.set(true);
////                comboBox.getItems().setAll(originalItems);
//                autocompleteRunning.set(false);
//              });
    };
  }

  private static class CustomComboBoxListViewSkin<T> extends JFXComboBoxListViewSkin<T> {
    public CustomComboBoxListViewSkin(JFXComboBox<T> comboBox, Consumer<KeyEvent> autocompleteHandler) {
      super(comboBox);

      ListView<?> listView = (ListView<?>) getPopupContent();

      listView.addEventFilter(
              KeyEvent.KEY_PRESSED,
              event -> {
                switch (event.getCode()) {
                  case UP, DOWN -> {
                    int currentIndex = listView.getFocusModel().getFocusedIndex();
                    int newIndex;
                    if (event.getCode() == KeyCode.UP) {
                      newIndex = (currentIndex - 1 + listView.getItems().size()) % listView.getItems().size();
                    } else {
                      newIndex = (currentIndex + 1) % listView.getItems().size();
                    }
                    listView.getFocusModel().focus(newIndex);
                    scrollToIfNecessary(comboBox, listView, newIndex);
                    event.consume();
                  }
                  case ENTER -> {
                    int focusedIndex = listView.getFocusModel().getFocusedIndex();
                    if (focusedIndex >= 0) {
                      listView.getSelectionModel().select(focusedIndex);
                      getSkinnable().hide();
                    }
                    event.consume();
                  }
                  default -> autocompleteHandler.accept(event);
                }
              });
    }
  }

  private static <T> void scrollToIfNecessary(ComboBox<T> comboBox, ListView<?> listView, int index) {
    if (comboBox.getVisibleRowCount() < listView.getItems().size()) {
      try {
        ListViewSkin<?> skin = (ListViewSkin<?>) listView.getSkin();
        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().get(0);
        int firstVisibleCell = flow.getFirstVisibleCell().getIndex();
        int lastVisibleCell = flow.getLastVisibleCell().getIndex();

        if (index <= firstVisibleCell || index >= lastVisibleCell) {
          listView.scrollTo(index);
        }
      } catch (Exception ex) {
        listView.scrollTo(index);
      }
    }
  }
}
