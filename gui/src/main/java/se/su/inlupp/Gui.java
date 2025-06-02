package se.su.inlupp;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
  private VBox root;
  private FlowPane buttonPane;
  private MenuBar menuBar;
  private Graph<City> graph;

  private boolean edited;

  private Group cityCircle;
  private City markedCity1 = null;
  private City markedCity2 = null;

  @Override
  public void start(Stage primaryStage) throws IOException {
    graph = new ListGraph<>();
    stage = primaryStage;
    stage.setTitle("PathFinder");
    edited = true; // ändra till false när funktioner som utför ändringar på kartan fungerar

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

    Menu menu = new Menu("File");
    menuBar.getMenus().add(menu);
    
    MenuItem newMap = new MenuItem("New map");
    menu.getItems().add(newMap);
    newMap.setOnAction(new NewMapItemHandler());
    MenuItem open = new MenuItem("Open");
    menu.getItems().add(open);
    MenuItem save = new MenuItem("Save");
    menu.getItems().add(save);
    MenuItem saveImg = new MenuItem("Save Image");
    menu.getItems().add(saveImg);
    MenuItem exit = new MenuItem("Exit");
    menu.getItems().add(exit);
    exit.setOnAction(new ExitItemHandler());

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
    root.setPrefSize(620, menuBar.getHeight() + buttonPane.getHeight() + 20);
    root.setSpacing(10);
    // root.setAlignment(Pos.CENTER);

    scene = new Scene(root);
    stage.setScene(scene);
    stage.setOnCloseRequest(new ExitHandler());
    stage.show();

  }

  public static void main(String[] args) {
    launch(args);
  }

  private void changeMap(String filePath) {
    Image image = new Image(filePath);
    imageView.setImage(image);
    // TODO: rensa onödiga instansvariabler och skapa konstant för höjd på knapp-
    // och menypaneler tillsammans
    root.setPrefSize(image.getWidth(), image.getHeight() + buttonPane.getHeight() + menuBar.getHeight() + 20);
    stage.sizeToScene();
  }

  private boolean checkIfNull(String string) {
    if (string == null || string.trim().isEmpty()) {
      return true;
    } else {
      return false;
    }
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

          Circle circle = new Circle(0, 0, 10);
          circle.setFill(Color.PINK);

          Label city = new Label(placeName);
          city.setFont(new Font("Calibri", 15));
          city.setLayoutX(-city.getWidth() / 2);
          city.setLayoutY(-10);

          cityCircle = new Group();
          cityCircle.getChildren().addAll(circle, city);
          cityCircle.setOnMouseClicked(new CityCircleClickHandler());

          mapPane.getChildren().add(cityCircle);
          cityCircle.relocate(x, y);

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
      Object cityCircle = event.getSource();
      City city = null;
      Circle circle = null;

      // OBS! Det som brister är att:
      // Man kan markera hur många städer som helst...
      // Man kan INTE avmarkera en stad.

      // tar fram rätt stad och cirkel (från Group)
      for (Node node : ((Group) cityCircle).getChildren()) {
        if (node instanceof Circle) {
          circle = (Circle) node;
        } else if (node instanceof Label) {
          String name = ((Label) node).getText();
          Set<City> cities = graph.getNodes();

          for (City c : cities) {
            if (c.getCityName().equals(name)) {
              city = c;
              break;
            }
          }
        }
      }

      // kollar om två platser redan är markerade
      if (markedCity1 != null && markedCity2 != null) {

        // om staden redan är markerad ska den avmarkeras
        if (markedCity1.equals(city)) {
          markedCity1 = null;
          circle.setFill(Color.PINK);
        } else if (markedCity2.equals(city)) {
          markedCity2 = null;
          circle.setFill(Color.PINK);
        }

        // om de två städer som är markerade inte är staden vi trycker på ska INGENTING
        // hända
        event.consume();

      }

      // om det finns en ledig markedCity variabel ska stad som är klickad på bli
      // markerad
      if (markedCity1 == null) {
        markedCity1 = city;
      } else if (markedCity2 == null) {
        markedCity2 = city;
      }
      circle.setFill(Color.PURPLE);

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
