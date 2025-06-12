package se.su.inlupp;

//import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.Pair;

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
  private Circle markedCircle1 = null;
  private Circle markedCircle2 = null;
  private double markedCityCircle1X = 0;
  private double markedCityCircle1Y = 0;
  private double markedCityCircle2X = 0;
  private double markedCityCircle2Y = 0;

  @Override
  public void start(Stage primaryStage) throws IOException {
    graph = new ListGraph<City>();
    stage = primaryStage;
    stage.setTitle("PathFinder");
    edited = true;

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
    newConn.setOnAction(new NewConnectionHandler());

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
      Object cityCircle = event.getSource();
      City city = null;
      Circle circle = null;

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

      // Om staden redan är markerad ska den avmarkeras
      if (markedCity1 != null && markedCity1.equals(city)) {
        markedCity1 = null;
        circle.setFill(Color.PINK);
        event.consume();
        return;
      } else if (markedCity2 != null && markedCity2.equals(city)) {
        markedCity2 = null;
        circle.setFill(Color.PINK);
        event.consume();
        return;
      }

      // Ifall de två platser som är markerade INTE är
      // den vi trycker på ska ingenting hända.
      if (markedCity1 != null && markedCity2 != null) {
        event.consume();
        return;
      }

      // Om det finns en ledig markedCity variabel ska stad som är klickad på bli
      // markerad
      if (markedCity1 == null) {
        markedCity1 = city;
        markedCircle1 = circle;
        markedCityCircle1X = city.getX();
        markedCityCircle1Y = city.getY();
        circle.setFill(Color.PURPLE);
        event.consume();
        return;
      } else if (markedCity2 == null) {
        markedCity2 = city;
        markedCircle2 = circle;
        markedCityCircle2X = city.getX();
        markedCityCircle2Y = city.getY();
        circle.setFill(Color.PURPLE);
        event.consume();
        return;
      }
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

  // TODO: flytta ovanför inre klasser
  private boolean twoPlacesSelected() {
    boolean bothSelected = !(markedCity1 == null || markedCity2 == null);
    if (!bothSelected) {
      writeErrorAlert("Two places must be selected!");
    }
    return bothSelected;
  }

  private void clearSelectedPlaces() {
    markedCity1 = null;
    markedCity2 = null;
    markedCircle1.setFill(Color.PINK);
    markedCircle2.setFill(Color.PINK);
  }

  class NewConnectionHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {

      // Hjälpmetod som kontrollerar markeringar
      if (twoPlacesSelected()) {
        try {
          // Använd getEdgeBetween() och fånga NoSuchElementException alternativt
          // connect() och fånga IllegalStateException
          // TODO: en hjälpmetod i ListGraph (om den tillåts av VPL) som returnerar
          // boolean
          Edge<City> existingEdge = graph.getEdgeBetween(markedCity2, markedCity1);

          if (existingEdge == null) {

            // // TODO: titta i kursmaterial om det inte finns en färdig fönstertyp med rätt
            // // symbol
            // Alert ConnectionPrompt = new Alert(AlertType.CONFIRMATION);
            // ConnectionPrompt.showAndWait();
            // // fortsätt skapa en egen subklass till
            // // alert.......................................

            // TODO: skriv en egen ConnectionForm klass som ärver av Dialog<String[]>
            // ----------------------------------------------------------------------
            Dialog<Pair<String, Integer>> dialog = new Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + markedCity1.getCityName() + " to " + markedCity2.getCityName());

            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            TextField nameField = new TextField();
            nameField.setPromptText("Name");
            TextField timeField = new TextField();
            timeField.setPromptText("Travel time:");

            // TODO: gör kontroll efter inmatning istället och visa ett felmeddedelande vid
            // felaktig indata
            // Gör att endast 0-9 kan skrivas in i timeField
            // timeField.textProperty().addListener((obs, oldVal, newVal) -> {
            //   if (!newVal.matches("\\d*")) {
            //     // writeErrorAlert("Incorrect input: Enter a valid time!");
            //     timeField.setText(oldVal);
            //   }
            // });

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(12);

            grid.add(new Label("Name of connection:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Travel time:"), 0, 1);
            grid.add(timeField, 1, 1);

            dialog.getDialogPane().setContent(grid);

            // Den här behövdes för att dialog.showAndWait annars returnerade en ButtonType
            // Istället för en Pair med name och time.
            dialog.setResultConverter(dialogButton -> {
              if (dialogButton == ButtonType.OK) {
                String name = nameField.getText();
                String timeText = timeField.getText();

                if (name.isBlank()) {
                  writeErrorAlert("Incorrect input:\nEnter a name!");
                  return null;
                } else if (timeText.isBlank()) {
                  writeErrorAlert("Incorrect input:\nEnter a time!");
                  return null;
                } else if (!timeText.matches("\\d+")) {
                  writeErrorAlert("Incorrect input:\nEnter time as a positive integer value!");
                  return null;
                } else {
                  int time = Integer.parseInt(timeText);
                  return new Pair<>(name, time);
                }
              }
              return null;
            });
            // --------------------------------------------------------------

            Optional<Pair<String, Integer>> result = dialog.showAndWait();

            if (result.isPresent()) {
              Pair<String, Integer> connectionInput = result.get();
              // String connectionName = input.getKey();
              // int connectionTime = input.getValue();
              graph.connect(markedCity1, markedCity2, connectionInput.getKey(), connectionInput.getValue());
              // TODO: hjälpmetod drawConnection() som ritar ut en linje på kartan och kolla upp varför circle.getCenterX/Y() inte ger rätt koordinater
              // Line line = new Line(markedCityCircle1X, markedCityCircle1Y, markedCityCircle2X, markedCityCircle2Y);
              Line line = new Line(markedCity1.getX(), markedCity1.getY(), markedCity2.getX(), markedCity2.getY());
              mapPane.getChildren().add(1, line); // ritar linje först och under andra noder på positionen
            }

            // result.ifPresent(pair -> {
            // String connectionName = pair.getKey();
            // int connectionTime = pair.getValue();
            // graph.connect(markedCity1, markedCity2, connectionName, connectionTime);
            // Line line = new Line(markedCityCircle1X, markedCityCircle1Y,
            // markedCityCircle2X, markedCityCircle2Y);
            // mapPane.getChildren().add(1, line);
            // });

            // hjälpmetod för nollställning av markeringar
            clearSelectedPlaces();

          } else {
            writeErrorAlert("There already exists a connection between these places!");
          }
        } catch (NoSuchElementException ex) {
          System.err.println("Two selected nodes expected, but not found!");
          ex.printStackTrace();
        }
      } else {
        // writeErrorAlert("Two places must be selected!");
      }

      // TODO: hjälpmetod som kontrollerar markeringar
      // if(markedCity1 == null || markedCity2 == null){
      // writeErrorAlert("Two places must be selected!");
      // event.consume();
      // return;
      // }

      // TODO: använd getEdgeBetween() och fånga NoSuchElementException alternativt
      // connect() och fånga IllegalStateException
      // if (graph.pathExists(markedCity1, markedCity2)) {
      // writeErrorAlert("There already exists a connection between these cities!");
      // } else {
      // // Onödig del
      // String cityName1 = markedCity1.getCityName();
      // String cityName2 = markedCity2.getCityName();

      // // TODO: titta i kursmaterial om det inte finns en färdig fönstertyp med rätt
      // Dialog<Pair<String, Integer>> dialog = new Dialog<>();
      // dialog.setTitle("Connection");
      // dialog.setHeaderText("Connection from " + markedCity1.getCityName() + " to "
      // + markedCity2.getCityName());

      // dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,
      // ButtonType.CANCEL);

      // TextField nameField = new TextField();
      // nameField.setPromptText("Name");
      // TextField timeField = new TextField();
      // timeField.setPromptText("Travel time:");

      // // TODO: gör kontroll efter inmatning istället och visa ett felmeddedelande
      // vid
      // // felaktig indata
      // // Gör att endast 0-9 kan skrivas in i timeField
      // timeField.textProperty().addListener((obs, oldVal, newVal) -> {
      // if (!newVal.matches("\\d*")) { // TODO: testa \\d+ istället och se om tomt
      // värde inte accepteras
      // timeField.setText(oldVal);
      // }
      // });

      // GridPane grid = new GridPane();
      // grid.setHgap(12);
      // grid.setVgap(12);

      // grid.add(new Label("Name of connection:"), 0, 0);
      // grid.add(nameField, 1, 0);
      // grid.add(new Label("Travel time:"), 0, 1);
      // grid.add(timeField, 1, 1);

      // dialog.getDialogPane().setContent(grid);

      // // Den här behövdes för att dialog.showAndWait annars returnerade en
      // ButtonType
      // // Istället för en Pair med name och time.
      // dialog.setResultConverter(dialogButton -> {
      // if (dialogButton == ButtonType.OK) {
      // String name = nameField.getText();
      // String timeText = timeField.getText();

      // if (name.isBlank() || timeText.isBlank()) {
      // return null;
      // }

      // try {
      // int time = Integer.parseInt(timeField.getText());
      // return new Pair<>(name, time);
      // } catch (NumberFormatException e) {
      // return null;
      // }

      // }
      // return null;
      // });

      // Optional<Pair<String, Integer>> result = dialog.showAndWait();

      // result.ifPresent(pair -> {
      // String connectionName = pair.getKey();
      // int connectionTime = pair.getValue();

      // graph.connect(markedCity1, markedCity2, connectionName, connectionTime);
      // // TODO: hjälpmetod för nollställning av markeringar
      // markedCity1 = null;
      // markedCity2 = null;
      // markedCircle1.setFill(Color.PINK);
      // markedCircle2.setFill(Color.PINK);
      // // TODO: hjälpmetod som ritar ut en linje på kartan
      // Line line = new Line(markedCityCircle1X, markedCityCircle1Y,
      // markedCityCircle2X, markedCityCircle2Y);
      // mapPane.getChildren().add(1, line);
      // // line.setStartX(markedCityCircle1X);
      // // line.setStartY(markedCityCircle1Y);
      // // line.setEndX(markedCityCircle2X);
      // // line.setEndY(markedCityCircle2Y);

      // });

      // // TODO: behöver bara göras i slutet på handle() ty då har eventuella fel
      // // fångats upp redan
      // markedCity1 = null;
      // markedCity2 = null;
      // markedCircle1.setFill(Color.PINK);
      // markedCircle2.setFill(Color.PINK);

      // event.consume(); // Behöver man verkligen konsumera ett event i slutet på
      // dess hanterarmetod?
      // return;

      // }

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