package demos.components;

import com.jfoenix.controls.JFXComboBox;
import demos.JFXComboBoxInstaller;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ComboBoxDemo extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        JFXComboBox<String> combo = new JFXComboBox<>();
//        combo.getItems().add(new Label("Java"));
//        combo.getItems().add(new Label("Python"));
//        combo.getItems().add(new Label("C"));
//        combo.getItems().add(new Label("JavaScript"));
//        combo.getItems().add(new Label("Kafka"));
//        combo.getItems().add(new Label("PostgreSQL"));
//        combo.getItems().add(new Label("SQL"));
//        combo.getItems().add(new Label("Spring"));
//        combo.getItems().add(new Label("Spring Boot"));
//        combo.getItems().add(new Label("Spring MVC"));
//        combo.getItems().add(new Label("MySQL"));



//        combo.setEditable(true);
//        combo.setPromptText("Select Java Version");
//        combo.setConverter(new StringConverter<Label>() {
//            @Override
//            public String toString(Label object) {
//                return object==null? "" : object.getText();
//            }
//
//            @Override
//            public Label fromString(String string) {
//                return new Label(string);
//            }
//        });

        JFXComboBoxInstaller.install(combo, false);

        ObservableList<String> items = FXCollections.observableArrayList(
                "Mexico", "Canada", "USA", "Brazil", "Argentina", "Chile"
        );
        combo.setItems(items);
//
//        FilteredList<String> filteredItems = new FilteredList<>(items, s -> true);
//        combo.setItems(filteredItems);
//
//        combo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
//            final String filter = newVal.toLowerCase();
//            filteredItems.setPredicate(item ->
//                    item.toLowerCase().contains(filter)
//            );
//
//            if (!combo.isShowing()) {
//                combo.show();
//            }
//        });



        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if(newVal != null && !newVal.equals(oldVal)) {
                System.out.println("new value: " + newVal);
            }
        });

        HBox pane = new HBox(100);
        HBox.setMargin(combo, new Insets(20));
        pane.setStyle("-fx-background-color:WHITE");
        pane.getChildren().add(combo);

        final Scene scene = new Scene(pane, 300, 300);
        scene.getStylesheets().add(ComboBoxDemo.class.getResource("/css/jfoenix-components.css").toExternalForm());

        primaryStage.setTitle("JFX ComboBox Demo");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
