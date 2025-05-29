package se.su.inlupp;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Gui extends Application {

  public void start(Stage stage) {
    Graph<String> graph = new ListGraph<String>();
    String javaVersion = System.getProperty("java.version");
    String javafxVersion = System.getProperty("javafx.version");
    Label label =
        new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");

    MenuBar menuBar = new MenuBar();
    
    Menu menu = new Menu("file");
    menuBar.getMenus().add(menu);
    
    MenuItem newMap = new MenuItem("New map");
    menu.getItems().add(newMap);
    MenuItem open = new MenuItem("Open");
    menu.getItems().add(open);
    MenuItem save = new MenuItem("Save");
    menu.getItems().add(save);
    MenuItem saveImg = new MenuItem("Save Image");
    menu.getItems().add(saveImg);
    MenuItem exit = new MenuItem("Exit");
    menu.getItems().add(exit);

    Button findPath = new Button("Find Path");
    Button showConn = new Button("Show Connection");
    Button newPlace = new Button("New Place");
    Button newConn = new Button("New Connection");
    Button changeConn = new Button("Change Connection");

    FlowPane buttonPane = new FlowPane(findPath, showConn, newPlace, newConn, changeConn);
    buttonPane.setAlignment(Pos.TOP_CENTER);
    VBox root = new VBox(30, menuBar, buttonPane);
    
    Scene scene = new Scene(root, 640, 480);
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
