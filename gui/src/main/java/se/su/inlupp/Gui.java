package se.su.inlupp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

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

  private City markedCity1 = null;
  private City markedCity2 = null;
  private Circle markedCircle1 = null;
  private Circle markedCircle2 = null;

  @Override
  public void start(Stage primaryStage) throws IOException {
    // skapar en modell för grafen som lagrar alla platser och förbindelser på
    // kartan
    graph = new ListGraph<>();

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
    open.setOnAction(new OpenMapItemHandler());
    MenuItem save = new MenuItem("Save");
    save.setOnAction(new SaveMapItemHandler());
    MenuItem saveImage = new MenuItem("Save Image");
    MenuItem exit = new MenuItem("Exit");
    exit.setOnAction(new ExitItemHandler());

    Menu menu = new Menu("File");
    menu.getItems().addAll(newMap, open, save, saveImage, exit);
    MenuBar menuBar = new MenuBar(menu);

    // skapar en knapplist
    Button findPath = new Button("Find Path");
    findPath.setOnAction(new FindPathHandler());
    Button showConn = new Button("Show Connection");
    showConn.setOnAction(new ShowConnectionHandler());
    newPlace = new Button("New Place");
    newPlace.setOnAction(new NewPlaceHandler());
    Button newConn = new Button("New Connection");
    newConn.setOnAction(new NewConnectionHandler());
    Button changeConn = new Button("Change Connection");
    changeConn.setOnAction(new ChangeConnectionHandler());

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

  private boolean checkIfNull(String string) {
    return string == null || string.isBlank();
  }

  private void showErrorAlert(String prompt) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("Error!");
    alert.setHeaderText(null);
    alert.setContentText(prompt);
    alert.showAndWait();
  }

  private void drawPlaceOnMap(String name, double x, double y) {
    // Skapa cirkel
    Circle circle = new Circle(x, y, 12);
    circle.setFill(Color.PINK);

    // Skapa etikett
    Label cityLabel = new Label(name);
    cityLabel.setLayoutX(x + 6);
    cityLabel.setLayoutY(y + 6);
    cityLabel.setStyle("fx-font-family: 'Calibri'; -fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: black;");
    cityLabel.setLabelFor(circle);

    // Skapa behållare för stadens etikett och cirkel
    Group cityCircle = new Group();
    cityCircle.getChildren().addAll(circle, cityLabel);
    cityCircle.setOnMouseClicked(new CityCircleClickHandler());

    // Lägg in plats i kartan
    mapPane.getChildren().add(cityCircle);
  }

  private void drawConnectionOnMap(City place1, City place2, String name, int time) {
    // TODO: Kolla upp varför circle.getCenterX/Y() inte ger rätt koordinater
    // Line line = new Line(markedCityCircle1X, markedCityCircle1Y,
    // markedCityCircle2X, markedCityCircle2Y);

    // Skapa en linje mellan de två platsernas koordinater
    Line line = new Line(place1.getX(), place1.getY(), place2.getX(), place2.getY());

    // Rita linjen först och under andra noder på positionen
    mapPane.getChildren().add(1, line); 
  }

  /**
   * Återställer grafen i både modellen (graph) och vyn (mapPane).
   */
  private void resetMapGraph() {
    // Rensa eventuella markeringar
    clearSelectedPlaces();
    // Rensa karta på platser och anslutningar
    Iterator<Node> childNodes = mapPane.getChildren().iterator();
    while (childNodes.hasNext()) {
      Node node = childNodes.next();
      if (node instanceof Group || node instanceof Line) {
        // Ta bort den sist hämtade barnnoden till kartrutan ur rutans barnsamling
        childNodes.remove();
      }
    }
    // Rensa graf på noder och kanter
    graph = new ListGraph<>();
  }

  /**
   * Ändrar bakgrundsbilden på vilken kartgrafen ritas ut.
   * @param filePath
   */
  private void changeMapImage(String filePath) {
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

  class NewMapItemHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      if (edited) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning!");
        alert.setContentText("Unsaved changes, continue anyway?");
        alert.setHeaderText(null);

        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isPresent() && answer.get().equals(ButtonType.CANCEL)) {
          event.consume(); // markera att händelse avbryts
        }
      }

      if (!event.isConsumed()) { // om händelse fortfarande pågår
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
          resetMapGraph();
          changeMapImage(file.toURI().toString());

          edited = false;
        }
      }
    }
  }

  private void openMap(String filePath) {
    System.out.println(filePath);

    try (FileReader fileReader = new FileReader(filePath); BufferedReader reader = new BufferedReader(fileReader)) {
      // TODO: Byt ut till genomgång av fil med Iterator<String> lineItertor =
      // reader.lines().iterator();

      resetMapGraph();
      // Byt kartbild i fönstret
      String mapPath = reader.readLine();
      changeMapImage(mapPath);

      // Hämta och sätt in alla platser i grafen
      String cityRow = reader.readLine();
      String[] cityRecords = cityRow.split(";");
      int nameIndex = 0;
      int xIndex = 1;
      int yIndex = 2;

      while (nameIndex < cityRecords.length) {
        try {
          City city = new City(cityRecords[nameIndex], Double.parseDouble(cityRecords[xIndex]),
              Double.parseDouble(cityRecords[yIndex]));
          graph.add(city);
          drawPlaceOnMap(city.getName(), city.getX(), city.getY());

        } catch (NumberFormatException nfe) {
          System.err.println("Expected a double, but got something else: " + nfe.getMessage());
        }

        nameIndex += 3;
        xIndex = nameIndex + 1;
        yIndex = nameIndex + 2;
      }

      // Hämta och sätt in alla anslutningar mellan platser i grafen
      String connectionRow = reader.readLine();
      String[] connectionData;
      Set<City> cities = graph.getNodes();
      Optional<City> from = Optional.empty();
      Optional<City> to = Optional.empty();
      String readCityName;
      String connectionName;
      int weight;
      // Pga ordningen som anslutningarna sparas i filen med Save-funktionen,
      // och att connect-metoden skapar dubbelriktade anslutningar,
      // så kan man hålla reda på vilka städer som har varit startpunkter i tidigare
      // anslutningar
      // för att ignorera deras spegelvända anslutningar där samma städer är
      // destinationer när
      // de läses av från filen. Metoden kommer ju lägga till båda när ena fallet
      // läses av från filen.
      Set<String> visitedAsOrigin = new HashSet<>();

      while (connectionRow != null) {
        connectionData = connectionRow.split(";");

        // TODO: ta bort möjlighet att lägga till städer med samma namn i grafen,
        // istället
        // ska ett felmeddelande visas vid försök att lägga till dubbletter.

        // hitta platsen som hör till inläst startpunktsnamn
        readCityName = connectionData[0];
        for (City city : cities) {
          if (city.getName().equals(readCityName)) {
            from = Optional.of(city);
            break;
          }
        }

        // Hitta platsen som hör till inläst destinationsnamn
        readCityName = connectionData[1];
        // Kontrollera så destinationen inte redan dykt upp som en startpunkt tidigare
        if (!visitedAsOrigin.contains(readCityName)) {
          for (City city : cities) {
            if (city.getName().equals(readCityName)) {
              to = Optional.of(city);
              break;
            }
          }

          if (from.isPresent() && to.isPresent()) {
            connectionName = connectionData[2];
            try {
              weight = Integer.parseInt(connectionData[3]);
              graph.connect(from.get(), to.get(), connectionName, weight);
              drawConnectionOnMap(from.get(), to.get(), connectionName, weight);
              // Lägg till att startpunkten har besökts
              visitedAsOrigin.add(from.get().getName());
            } catch (NumberFormatException nfe) {
              System.err.println("Expected an integer, but got something else: " + nfe.getMessage());
            }

          }

        }
        // Läs in nästa rad, om den finns, med anslutningsdata
        connectionRow = reader.readLine();
      }

    } catch (FileNotFoundException ex) {
      System.err.println("Can't open file, because %s".formatted(ex.getMessage()));
    } catch (IOException ex) {
      System.err.println("IO error %s".formatted(ex.getMessage()));
    }
  }

  // TODO: extrahera ut delad funktionalitet ur OpenMapItemHandler och
  // NewMapItemHandler och ha i en metod
  class OpenMapItemHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent event) {

      if (edited) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning!");
        alert.setContentText("Unsaved changes, continue anyway?");
        alert.setHeaderText(null);

        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isPresent() && answer.get().equals(ButtonType.CANCEL)) {
          event.consume(); // markera att händelse avbryts
        }
      }

      if (!event.isConsumed()) { // om händelse fortfarande pågår
        File graphFile = fileChooser.showOpenDialog(stage);

        if (graphFile != null) {
          openMap(graphFile.getAbsolutePath());

          edited = false;
        }
      }
    }
  }

  private void saveMap(String filePath) {

    try (FileWriter fileWriter = new FileWriter(filePath); PrintWriter writer = new PrintWriter(fileWriter)) {
      // Spara kartbildens URL i filen
      String imagePath = imageView.getImage().getUrl();
      writer.println(imagePath);
      // Spara grafens platser i filen
      Set<City> cities = graph.getNodes();
      for (City city : cities) {
        // Uppgifter sparas på en rad semikolonseparerade
        writer.print(String.format("%s;%s;%s;", city.getName(), city.getX(), city.getY()));
      }
      writer.println();
      // Spara grafens anslutningar i filen
      for (City city : cities) {
        for (Edge<City> connection : graph.getEdgesFrom(city)) {
          // Uppgifter sparas rad för rad semikolonseparerade
          writer.println(String.format("%s;%s;%s;%s;", city.getName(), connection.getDestination().getName(),
              connection.getName(), connection.getWeight()));
        }
      }

    } catch (FileNotFoundException ex) {
      System.err.println("Can't open file, because %s".formatted(ex.getMessage()));
    } catch (IOException ex) {
      System.err.println("IO error %s".formatted(ex.getMessage()));
    }
  }

  class SaveMapItemHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent event) {
      File saveFile = fileChooser.showSaveDialog(stage);

      if (saveFile != null) {
        saveMap(saveFile.getAbsolutePath());

        edited = false;
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
          showErrorAlert("Incorrect input: Enter a name!");
        } else {
          // Potentiell hjälpklass createPlaceOnMap(name, x, y)

          // Hämta koordinater på muspekaren vid klick
          double x = event.getX();
          double y = event.getY();
          // Rita ut platsen på kartan
          drawPlaceOnMap(placeName, x, y);

          // Skapa en stadnod och lägg in grafmodellen
          City cityNode = new City(placeName, x, y);
          graph.add(cityNode);

          edited = true;
        }
      }

      scene.setCursor(Cursor.DEFAULT);
      newPlace.setDisable(false);
      newPlace.requestFocus();
      mapPane.setOnMouseClicked(null);
    }
  }

  class CityCircleClickHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {
      // tar fram referenser till klickad cirkel och tillhörande stad (från Group)
      Group cityGroup = (Group) event.getSource();
      ObservableList<Node> list = cityGroup.getChildren();
      Circle clickedCircle = (Circle) list.get(0);
      Label clickedLabel = (Label) list.get(1);
      String cityName = clickedLabel.getText();
      // tar fram samma stad ur grafen (modellen)
      City clickedCity = null;
      // Potentiell hjälpmetod i ListGraph för att hämta en nod, om VPL tillåter det
      Set<City> cities = graph.getNodes();
      for (City city : cities) {
        if (city.getName().equals(cityName) && city.getX() == clickedCircle.getCenterX()
            && city.getY() == clickedCircle.getCenterY()) {
          clickedCity = city;
          break;
        }
      }

      // Just in case...
      if (clickedCity == null) {
        throw new NullPointerException("City-node expected, null found!");
      }
      // potentiell hjälpklass: markCityIfPossible(City city) alternativt mark(City
      // city)/unmark(City city)

      if (markedCity1 == null && markedCity2 == null) { // inget markerat sen tidigare, markera klickad stad
        markedCity1 = clickedCity;
        markedCircle1 = clickedCircle;
        markedCircle1.setFill(Color.PURPLE);
      } else if (clickedCity.equals(markedCity1)) { // klickad stad redan markerad, ta bort markering
        markedCity1 = null;
        markedCircle1.setFill(Color.PINK);
        markedCircle1 = null;
      } else if (clickedCity.equals(markedCity2)) {
        markedCity2 = null;
        markedCircle2.setFill(Color.PINK);
        markedCircle2 = null;
      } else if (markedCity1 == null) { // klickad stad inte markerad, markera klickad stad
        markedCity1 = clickedCity;
        markedCircle1 = clickedCircle;
        markedCircle1.setFill(Color.PURPLE);
      } else if (markedCity2 == null) {
        markedCity2 = clickedCity;
        markedCircle2 = clickedCircle;
        markedCircle2.setFill(Color.PURPLE);
      } else { // markeringar upptagna, ignorera klickad stad

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

  private boolean twoPlacesSelected() {
    boolean bothSelected = !(markedCity1 == null || markedCity2 == null);
    if (!bothSelected) {
      showErrorAlert("Two places must be selected!");
    }
    return bothSelected;
  }

  /**
   * Återställer markeringar på kartan.
   */
  private void clearSelectedPlaces() {
    markedCity1 = null;
    markedCity2 = null;
    if (markedCircle1 != null) {
      markedCircle1.setFill(Color.PINK);
    }
    if (markedCircle2 != null) {
      markedCircle2.setFill(Color.PINK);
    }
  }

  private void disableButtons(boolean choice) {
    // findPath.setDisable(choice);
    // showConn.setDisable(choice);
    // newPlace.setDisable(choice);
    // newConn.setDisable(choice);
    // changeConn.setDisable(choice);
  }

  /**
   * När en dialogrutan behövs för att hantera en anslutning på något sätt,
   * så visas den via denna metod.
   * 
   * @param connectionName
   * @param time
   * @param nameEditable
   * @param timeEditable
   * @return
   */
  private Optional<Edge<City>> showConnectionForm(String connectionName, int time, boolean nameEditable,
      boolean timeEditable) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Connection");
    alert.setHeaderText("Connection from " + markedCity1.getName() + " to " + markedCity2.getName());

    Label nameLabel = new Label("Name of connection:");
    TextField nameField;
    if (connectionName != null && !connectionName.isBlank())
      nameField = new TextField(connectionName);
    else
      nameField = new TextField("");
    nameField.setEditable(nameEditable);

    Label timeLabel = new Label("Travel time:");
    TextField timeField;
    if (time > 0)
      timeField = new TextField(String.valueOf(time));
    else
      timeField = new TextField("");
    timeField.setEditable(timeEditable);
    // timeField.textProperty().addListener((observable, oldValue, newValue) -> {
    // if (!newValue.matches("\\d*")) {
    // timeField.setText(newValue.replaceAll("[^\\d]", ""));
    // }
    // });

    final int SPACING_VALUE = 12;
    GridPane grid = new GridPane();
    grid.setHgap(SPACING_VALUE);
    grid.setVgap(SPACING_VALUE);
    // grid.setPadding(new Insets(SPACING_VALUE));
    grid.add(nameLabel, 0, 0);
    grid.add(nameField, 1, 0);
    grid.add(timeLabel, 0, 1);
    grid.add(timeField, 1, 1);

    alert.getDialogPane().setContent(grid);
    alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

    // här börjar visning och svarshantering
    Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get().equals(ButtonType.OK)) {
      if (nameEditable) {
        String nameText = nameField.getText();
        // TODO: sätt in en regex kontroll så att rimliga ord kan väljas 
        if (nameText.isBlank()) {
          showErrorAlert("Incorrect input:\nEnter a name!");
          return Optional.empty();
        } else {
          connectionName = nameText;
        }
      }

      if (timeEditable) {
        String timeText = timeField.getText();
        if (timeText.isBlank()) {
          showErrorAlert("Incorrect input:\nEnter a time!");
          return Optional.empty();
        } else if (!timeText.matches("\\d+")) {
          showErrorAlert("Incorrect input:\nEnter time as a positive integer value!");
          return Optional.empty();
        } else {
          time = Integer.parseInt(timeText);
        }
      }

      return Optional.of(new ListEdge<>(markedCity2, connectionName, time));
    }
    // Hantera det som skrivits in och skicka tillbaka ett resultat från
    // dialogfönstret
    // alert.setResultConverter(
    // new Callback<ButtonType, ButtonType>() {
    // @Override
    // public ButtonType call(ButtonType dialogButton) {
    // if (dialogButton == ButtonType.OK) {
    // try {
    // String name = nameField.getText();
    // String timeText = timeField.getText();

    // if (name.isBlank()) {
    // writeErrorAlert("Incorrect input:\nEnter a name!");
    // return null;
    // } else if (timeText.isBlank()) {
    // writeErrorAlert("Incorrect input:\nEnter a time!");
    // return null;
    // } else if (!timeText.matches("\\d+")) {
    // writeErrorAlert("Incorrect input:\nEnter time as a positive integer value!");
    // return null;
    // } else {
    // int time = Integer.parseInt(timeText);
    // return ButtonType.OK;
    // }
    // } catch (NumberFormatException e) {
    // System.err.println("Something went wrong when reading time from
    // NewConnectionForm!");
    // return null;
    // }
    // } else {
    // // om denna metod returnerar null innebär det att showAndWait() returnerar en
    // // tom Optional<Edge<City>
    // return null;
    // }
    // }
    // });

    return Optional.empty();
  }

  // TODO: ta bort när showConnectionForm är helt färdig
  private class NewConnectionForm extends Dialog<Edge<City>> {
    private TextField nameField = new TextField();
    private TextField timeField = new TextField();

    public NewConnectionForm() {
      // Skapa fönsterkomponenter
      setTitle("Connection");
      setHeaderText("Connection from " + markedCity1.getName() + " to " + markedCity2.getName());
      nameField.setPromptText("Name");
      timeField.setPromptText("Time");

      GridPane grid = new GridPane();
      grid.setHgap(12);
      grid.setVgap(12);
      grid.add(new Label("Name of connection:"), 0, 0);
      grid.add(nameField, 1, 0);
      grid.add(new Label("Travel time:"), 0, 1);
      grid.add(timeField, 1, 1);

      // Rita dialogfönstret
      getDialogPane().setContent(grid);
      getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

      // Den här behövdes för att dialog.showAndWait annars returnerade en ButtonType
      // istället för en Pair med name och time.

      // Hantera det som skrivits in och skicka tillbaka ett resultat från
      // dialogfönstret
      setResultConverter(
          new Callback<ButtonType, Edge<City>>() {
            @Override
            public Edge<City> call(ButtonType dialogButton) {
              if (dialogButton == ButtonType.OK) {
                try {
                  String name = nameField.getText();
                  String timeText = timeField.getText();

                  if (name.isBlank()) {
                    showErrorAlert("Incorrect input:\nEnter a name!");
                    return null;
                  } else if (timeText.isBlank()) {
                    showErrorAlert("Incorrect input:\nEnter a time!");
                    return null;
                  } else if (!timeText.matches("\\d+")) {
                    showErrorAlert("Incorrect input:\nEnter time as a positive integer value!");
                    return null;
                  } else {
                    int time = Integer.parseInt(timeText);
                    return new ListEdge<>(markedCity2, name, time);
                  }
                } catch (NumberFormatException e) {
                  System.err.println("Something went wrong when reading time from NewConnectionForm!");
                  return null;
                }
              } else {
                // om denna metod returnerar null innebär det att showAndWait() returnerar en
                // tom Optional<Edge<City>
                return null;
              }
            }
          });
    }
  }

  class ChangeConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {

      if (twoPlacesSelected()) {
        Edge<City> connection = graph.getEdgeBetween(markedCity1, markedCity2);
        if (connection != null) {
          Optional<Edge<City>> result = showConnectionForm(connection.getName(), 0, false, true);

          try {
            int time = result.get().getWeight();
            graph.setConnectionWeight(markedCity1, markedCity2, time);

            edited = true;
          } catch (NumberFormatException e) {
            System.out.println("Invalid number input.");
          }

          clearSelectedPlaces();

        } else {
          showErrorAlert(
              "There exists no connection between " + markedCity1.getName() + " and " + markedCity2.getName() + "!");
          clearSelectedPlaces();
        }

      } else {
        // Felmeddelande för att två platser måste markeras.
        // Finns redan i twoPlacesSelected()
      }

    }
  }

  class ShowConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {

      if (twoPlacesSelected()) {
        Edge<City> connection = graph.getEdgeBetween(markedCity1, markedCity2);
        if (connection != null) {
          String connectionName = connection.getName();
          int connectionTime = connection.getWeight();

          showConnectionForm(connectionName, connectionTime, false, false);

        } else {
          showErrorAlert(
              "There exists no connection between " + markedCity1.getName() + " and " + markedCity2.getName() + "!");
          clearSelectedPlaces();
        }

      } else {
        // Felmeddelande för att två platser inte har valts behövs inte?
        // eftersom det finns i twoPlacesSelected() ??
      }
    }
  }

  class FindPathHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {
      List<Edge<City>> path = graph.getPath(markedCity1, markedCity2);
      int edgeWeightTotal = 0;

      if (twoPlacesSelected()) {
        if (path != null) {
          BorderPane borderPane = new BorderPane();
          TextArea textArea = new TextArea("");
          borderPane.setCenter(textArea);

          for (Edge<City> edge : path) {
            edgeWeightTotal += edge.getWeight();
            textArea.appendText(edge.toString() + "\n");
          }

          textArea.appendText("Total time: " + String.valueOf(edgeWeightTotal));
          textArea.setEditable(false);

          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle("Message");
          alert.setHeaderText("The Path from " + markedCity1.getName() + " to " + markedCity2.getName());
          alert.getDialogPane().setContent(borderPane);
          alert.showAndWait();

          clearSelectedPlaces();

        } else {
          showErrorAlert("There is no path from " + markedCity1.getName() + " to " + markedCity2.getName());
          clearSelectedPlaces();
        }

      } else {
        // Felmeddelande för att två platser inte är valda finns i twoPlacesSelected()
      }

    }
  }

  class NewConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      if (twoPlacesSelected()) {
        try {
          // TODO: en hjälpmetod i ListGraph (om den tillåts av VPL) som returnerar
          // boolean eller hjälpklass placesConnected() här
          Edge<City> connection = graph.getEdgeBetween(markedCity2, markedCity1);
          if (connection == null) {
            // NewConnectionForm dialog = new NewConnectionForm();
            Optional<Edge<City>> result = showConnectionForm(null, 0, true, true); // dialog.showAndWait();

            if (result.isPresent()) {
              String connectionName = result.get().getName();
              int connectionTime = result.get().getWeight();
              
              graph.connect(markedCity1, markedCity2, connectionName, connectionTime);
              drawConnectionOnMap(markedCity1, markedCity2, connectionName, connectionTime);
      
              edited = true;
            }
            // hjälpmetod för nollställning av markeringar
            clearSelectedPlaces();

          } else {
            showErrorAlert("There already exists a connection between these places!");
          }
        } catch (NoSuchElementException ex) {
          System.err.println("Two selected nodes expected, but not found!");
          ex.printStackTrace();
        }
      } else {
        // writeErrorAlert("Two places must be selected!");
      }
    }
  }

  private class ExitItemHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent arg0) {
      stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
  }

  class ExitHandler implements EventHandler<WindowEvent> {
    @Override
    public void handle(WindowEvent event) {
      if (edited) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning!");
        alert.setContentText("Unsaved changes, continue anyway?");
        alert.setHeaderText(null);

        Optional<ButtonType> answer = alert.showAndWait();
        if (answer.isPresent() && answer.get().equals(ButtonType.CANCEL)) {
          event.consume(); // stoppa nedstängningshändelse
        }
      }
    }
  }
}
