package se.su.inlupp;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.layout.BorderPane;
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
  private FileChooser fileChooser = new FileChooser();
  private ImageView imgView = new ImageView();
  private Scene scene;
  private Pane center;
  private Button newPlace;
  //private Graph<String> graph;

  public void start(Stage primaryStage) throws IOException {
    //graph = new ListGraph<String>();
    stage = primaryStage;

    // Fixar så dialogfönster öppnas från projektets rotmapp.
    File projectRoot = new File(System.getProperty("user.dir"));
    if(projectRoot.exists()){
      fileChooser.setInitialDirectory(projectRoot);
    } else {
      throw new IOException("Project directory not found!");
    }

    BorderPane root = new BorderPane();
    center = new Pane();
    center.getChildren().add(imgView);
    root.setCenter(center);
    

    MenuBar menuBar = new MenuBar();

    Menu menu = new Menu("file");
    menuBar.getMenus().add(menu);

    MenuItem newMap = new MenuItem("New map");
    menu.getItems().add(newMap);
    newMap.setOnAction(new LoadMapHandler());
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

    FlowPane buttonPane = new FlowPane(findPath, showConn, newPlace, newConn, changeConn);
    buttonPane.setAlignment(Pos.TOP_CENTER);

    VBox top = new VBox(menuBar, buttonPane);
    root.setTop(top);

    scene = new Scene(root, 640, 480);
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }

  private void open(String filePath) {
    Image img = new Image(filePath);
    imgView.setImage(img);
  }

  class LoadMapHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {
      File file = fileChooser.showOpenDialog(stage);
      
      if (file != null) {
        open(file.toURI().toString());
        // changed = false;
      }
    
    }
    

  }

  class NewPlaceHandler implements EventHandler<ActionEvent>{
    
    public void handle(ActionEvent event){
      newPlace.setDisable(true);
      scene.setCursor(Cursor.CROSSHAIR);
      
      scene.setOnMouseClicked(secondEvent -> {
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Name");
        dialog.setHeaderText(null);
        dialog.setContentText("Name of place:");
        
        Optional<String> result = dialog.showAndWait();
        if(result.isPresent()){
          String placeName = result.get();
          double x = secondEvent.getX();
          double y = secondEvent.getY();

          Circle circle = new Circle(x, y, 5);
          circle.setFill(Color.PINK);
          center.getChildren().add(circle);

          Label city = new Label(placeName);
          city.setFont(new Font("Calibri",14));
          city.setLayoutX(x + 15);
          city.setLayoutY(y - 10);
          center.getChildren().add(city);

          //HÄR SKA EN NOD LÄGGAS TILL I GRAFEN

        }else{
          //Tror att den här behövs, eftersom man ska kunna trycka på en stad för att markera den..
          //Aka kan man råka aktivera någon listener?
          secondEvent.consume();
        }

        scene.setCursor(Cursor.DEFAULT);
        newPlace.setDisable(false);
      });
    }
  }

}
