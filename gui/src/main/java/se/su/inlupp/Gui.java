package se.su.inlupp;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Gui extends Application {

  private Stage stage;
  private FileChooser fileChooser;
  private ImageView imageView;
  private Pane mapPane;
  private Scene scene;
  private Button newPlace;
  private VBox root;
  private FlowPane buttonPane;
  private MenuBar menuBar;
  // private Graph<String> graph;

  @Override
  public void start(Stage primaryStage) throws IOException {
    // graph = new ListGraph<String>();
    stage = primaryStage;

    // Fixar så dialogfönster öppnas från projektets rotmapp.
    File projectRoot = new File(System.getProperty("user.dir"));
    if (projectRoot.exists()) {
      fileChooser = new FileChooser();
      fileChooser.setInitialDirectory(projectRoot);
    } else {
      throw new IOException("Project directory not found!");
    }

    mapPane = new Pane();
    imageView = new ImageView();
    mapPane.getChildren().add(imageView);

    menuBar = new MenuBar();
    
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

    newPlace = new Button("New Place");
    newPlace.setOnAction(new NewPlaceHandler());

    Button newConn = new Button("New Connection");
    Button changeConn = new Button("Change Connection");

    buttonPane = new FlowPane(findPath, showConn, newPlace, newConn, changeConn);
    buttonPane.setAlignment(Pos.CENTER);
    buttonPane.setHgap(10);

    root = new VBox(menuBar, buttonPane, mapPane);
    root.setPrefSize(620, menuBar.getHeight()+buttonPane.getHeight()+20);
    root.setSpacing(10);
  
    scene = new Scene(root);  
    stage.setScene(scene);
    stage.show();

    
  }

  public static void main(String[] args) {
    launch(args);
  }

  private void changeMap(String filePath) {
    Image image = new Image(filePath);
    imageView.setImage(image);
    // TODO: rensa onödiga instansvariabler och skapa konstant för höjd på knapp- och menypaneler tillsammans 
    root.setPrefSize(image.getWidth(), image.getHeight()+buttonPane.getHeight()+menuBar.getHeight()+20);
    stage.sizeToScene();
  }

  class LoadMapHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {
      // TODO: kontroll för att se om ändringar finns

      File file = fileChooser.showOpenDialog(stage);

      if (file != null) {
        changeMap(file.toURI().toString());
        // changed = false;
      }

    }

  }

  class NewPlaceHandler implements EventHandler<ActionEvent> {

    public void handle(ActionEvent event) {
      newPlace.setDisable(true);
      scene.setCursor(Cursor.CROSSHAIR);

      // TODO: dela upp kod genom att skapa hanterare som sätter igång lyssnare efter knapptryck och en lyssnare som stängs av efter musklick 
      mapPane.setOnMouseClicked(secondEvent -> {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Name");
        dialog.setHeaderText(null);
        dialog.setContentText("Name of place:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
          String placeName = result.get();
          double x = secondEvent.getX();
          double y = secondEvent.getY();

          Circle circle = new Circle(x, y, 5);
          circle.setFill(Color.PINK);
          mapPane.getChildren().add(circle);

          Label city = new Label(placeName);
          city.setFont(new Font("Calibri", 14));
          city.setLayoutX(x + 15);
          city.setLayoutY(y - 10);
          mapPane.getChildren().add(city);

          // HÄR SKA EN NOD LÄGGAS TILL I GRAFEN

        } else {
          // Tror att den här behövs, eftersom man ska kunna trycka på en stad för att
          // markera den..
          // Aka kan man råka aktivera någon listener?
          secondEvent.consume();
        }

        scene.setCursor(Cursor.DEFAULT);
        newPlace.setDisable(false);
        mapPane.setOnMouseClicked(null);
      });
    }
  }

}
