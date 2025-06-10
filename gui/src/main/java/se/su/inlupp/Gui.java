package se.su.inlupp;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Gui extends Application {

  private Stage stage;
  private FileChooser fileChooser;
  private ImageView imageView;
  private Pane mapPane;
  private Scene scene;
  private Button newPlace;
  private VBox vbox;
  private Graph<City> graph;
  private BorderPane root;

  private boolean edited;

  private Group cityCircle;
  private City markedCity1 = null;
  private City markedCity2 = null;

  @Override
  public void start(Stage primaryStage) throws IOException {
    // skapar en modell för grafen som lagrar alla platser och förbindelser på
    // kartan
    graph = new ListGraph<>();

    // ta bort när funktioner som utför ändringar på kartan fungerar
    // (instansvariabeln sätts till false utan explicit tilldelning)
    edited = true;

    // Fixar så dialogfönster öppnas från projektets rotmapp.
    File projectRoot = new File(System.getProperty("user.dir"));
    if (projectRoot.exists()) {
      fileChooser = new FileChooser();
      fileChooser.setInitialDirectory(projectRoot);
    } else {
      throw new IOException("Project directory not found!");
    }

    // skapar en filmeny
    MenuItem newMap = new MenuItem("New map");
    newMap.setOnAction(new NewMapItemHandler());
    MenuItem open = new MenuItem("Open");
    MenuItem save = new MenuItem("Save");
    MenuItem saveImage = new MenuItem("Save Image");
    MenuItem exit = new MenuItem("Exit");
    exit.setOnAction(new ExitItemHandler());

    Menu menu = new Menu("File");
    menu.getItems().addAll(newMap, open, save, saveImage, exit);
    MenuBar menuBar = new MenuBar(menu);

    // skapar en knapplist
    Button findPath = new Button("Find Path");
    Button showConn = new Button("Show Connection");
    newPlace = new Button("New Place");
    newPlace.setOnAction(new NewPlaceHandler());
    Button newConn = new Button("New Connection");
    Button changeConn = new Button("Change Connection");

    FlowPane buttonPane = new FlowPane(findPath, showConn, newPlace, newConn, changeConn);
    buttonPane.setAlignment(Pos.CENTER);
    buttonPane.setOrientation(Orientation.HORIZONTAL);
    buttonPane.setHgap(5);
    buttonPane.setVgap(5);
    buttonPane.setPadding(new Insets(5));

    // skapar en behållare för alla knapp- och menykomponenter
    vbox = new VBox(menuBar, buttonPane);

    // skapar en karta som med en bildvy
    imageView = new ImageView();
    mapPane = new Pane(imageView);
    mapPane.setStyle("-fx-background-color: lightblue;"); // TODO: ta bort efter att fönsteruppdatering fungerar som
                                                          // önskat

    // skapar en behållare till alla kartkomponenter
    FlowPane centerPane = new FlowPane(mapPane);
    centerPane.setAlignment(Pos.CENTER);

    // skapar en behållare (rotnod) till alla komponenter i fönstret
    root = new BorderPane();
    root.setTop(vbox);
    root.setCenter(centerPane);
    root.setPrefSize(520, vbox.getHeight());

    stage = primaryStage;
    stage.setTitle("PathFinder");
    stage.setOnCloseRequest(new ExitHandler());
    scene = new Scene(root);
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }

  private void changeMap(String filePath) {
    // steg 1: hämtar bild på karta och ritar ut i kartvy
    Image image = new Image(filePath);
    imageView.setImage(image);
    // steg 2: anpassar rot och fönster efter (önskad) bildbredd
    root.setPrefWidth(image.getWidth());
    stage.sizeToScene();
    // steg 3: anpassar rot och fönster efter ny (önskad) bredd på komponenter
    root.setPrefHeight(image.getHeight() + vbox.getHeight());
    stage.sizeToScene();
  }

  private boolean checkIfNull(String string) {
    return string == null || string.trim().isEmpty();
  }

  private void writeErrorAlert(String prompt) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("Error!");
    alert.setHeaderText(null);
    alert.setContentText(prompt);
    alert.showAndWait();
  }

  class NewMapItemHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {
      // TODO: kontroll för att se om ändringar finns

      File file = fileChooser.showOpenDialog(stage);

      if (file != null) {
        changeMap(file.toURI().toString());
        // changed = false;
      }

    }

  }

  class MapClickHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {

      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("Name");
      dialog.setHeaderText(null);
      dialog.setContentText("Name of place:");

      Optional<String> result = dialog.showAndWait();
      if (result.isPresent()) {

        String placeName = result.get();

        if (checkIfNull(placeName)) {
          writeErrorAlert("Name of place can not be null!");
        } else {
          double x = event.getX();
          double y = event.getY();

          Circle circle = new Circle(0, 0, 15);
          circle.setFill(Color.PINK);

          Label city = new Label(placeName);
          city.setFont(new Font("Calibri", 18));

          cityCircle = new Group();
          cityCircle.getChildren().addAll(circle, city);
          cityCircle.setOnMouseClicked(new CityCircleClickHandler());

          mapPane.getChildren().add(cityCircle);
          cityCircle.relocate(x - 15, y - 15);

          City cityObject = new City(placeName, x, y);
          graph.add(cityObject);
          // System.err.println(graph);

        }
      } else {
        event.consume();
      }

      scene.setCursor(Cursor.DEFAULT);
      newPlace.setDisable(false);
      mapPane.setOnMouseClicked(null);

    }
  }

  class CityCircleClickHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {
      // HÄR SKA MAN KUNNA MARKERA EN STAD (max 2st)

      // Group cityCircle = (Group) event.getSource();
      // City clickedCity = null;
      // Circle clickedCircle = null;

      // tar fram referenser till klickad cirkel och tillhörande stad (från Group)
      Group cityCircle = (Group) event.getSource();
      ObservableList<Node> list = cityCircle.getChildren();
      Circle clickedCircle = (Circle) list.get(0);
      Label clickedLabel = (Label) list.get(1);
      String cityName = clickedLabel.getText();
      // tar fram samma stad ur grafen (modellen)
      City clickedCity = null;
      // TODO: skapa hjälpmetod i ListGraph för att hämta en nod
      Set<City> cities = graph.getNodes();
      for (City city : cities) {
        if (city.getCityName().equals(cityName)) {
          clickedCity = city;
          break;
        }
      }

      // Just in case...
      if(clickedCity == null){
        throw new NullPointerException("City-node expected, null found!");
      }

      if(clickedCity.equals(markedCity1)){
        markedCity1 = null;
        clickedCircle.setFill(Color.PINK);
      } else if(clickedCity.equals(markedCity2)){
        //TODO: ändra till att städer är lika omm koordinater och namn överensstämmer
        markedCity2 = null;
        clickedCircle.setFill(Color.PINK);
      } else if(markedCity1 == null){
        markedCity1 = clickedCity;
        clickedCircle.setFill(Color.PURPLE);
      } else if(markedCity2 == null){
        markedCity2 = clickedCity;
        clickedCircle.setFill(Color.PURPLE);
      } else{
        // nothin' 2 do, they be both unavailable, move on bro... move on
      }

      // kollar om ingen plats är markerade
      
      // om staden redan är markerad ska den avmarkeras

      // om det finns en ledig markedCity ska stad som är klickad på bli
      // markerad
 
      // .........................................................
    }
  }

  class NewPlaceHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent event) {
      newPlace.setDisable(true);
      scene.setCursor(Cursor.CROSSHAIR);

      mapPane.setOnMouseClicked(new MapClickHandler());

    }
  }

  private class ExitItemHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent arg0) {
      stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

  }

  class ExitHandler implements EventHandler<WindowEvent> {
    public void handle(WindowEvent event) {
      if (edited) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning!");
        alert.setContentText("Unsaved changes, continue anyway?");
        alert.setHeaderText(null);

        Optional<ButtonType> ans = alert.showAndWait();
        if (ans.isPresent() && ans.get().equals(ButtonType.CANCEL)) {
          event.consume(); // stoppa nedstängningshändelse
        }
      }
    }
  }
}
