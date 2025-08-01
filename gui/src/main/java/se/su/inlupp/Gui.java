package se.su.inlupp;

import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
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

/**
 * Huvudklassen i programmet. Klassen har hand om programmets grafiska vy och
 * interaktionen mellan den och programmets bakomliggande logiska modellen.
 */
public class Gui extends Application {

  private Stage stage;
  private FileChooser fileChooser;
  private ImageView imageView;
  private Pane mapPane;
  private FlowPane centerPane;
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

  /**
   * Startmetoden för JavaFX-applikation. Skapar och visar programmets grafiska
   * gränssnitt.
   * 
   * @param primaryStage fönstret i vilket gränssnittet byggs upp
   */
  @Override
  public void start(Stage primaryStage) throws IOException {
    // Fix: så dialogfönster öppnas från projektets rotmapp.
    File projectRoot = new File(System.getProperty("user.dir"));
    if (projectRoot.exists()) {
      fileChooser = new FileChooser();
      fileChooser.setInitialDirectory(projectRoot);
    } else {
      throw new IOException("Project directory not found!");

    }

    // skapar en modell för grafen som lagrar alla platser och förbindelser
    graph = new ListGraph<>();

    // skapar en filmeny
    MenuItem newMap = new MenuItem("New map");
    newMap.setOnAction(new NewMapItemHandler());
    MenuItem open = new MenuItem("Open");
    open.setOnAction(new OpenMapItemHandler());
    MenuItem save = new MenuItem("Save");
    save.setOnAction(new SaveMapItemHandler());
    MenuItem saveImage = new MenuItem("Save Image");
    saveImage.setOnAction(new SaveImageItemHandler()); // se frl video efter 01:29:00
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
    centerPane = new FlowPane(mapPane);
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

  /**
   * GUI-klassens startmetod. Körs först i programmet och sjösätter
   * JavaFX-applikationen.
   * 
   * @param args eventuella argument som angetts utifrån vid programanrop
   */
  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Visar ett fönster med angivet felmeddelande för användaren.
   * 
   * @param prompt felmeddelandet som ska visas
   */
  private void showErrorAlert(String prompt) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("Error!");
    alert.setHeaderText(null);
    alert.setContentText(prompt);
    alert.showAndWait();
  }

  /**
   * Skapar och ritar ut en plats på kartan.
   * 
   * @param name platsens namn
   * @param x    horisontell koordinat på kartan
   * @param y    vertikal koordinat på kartan
   */
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
    cityCircle.setOnMouseClicked(new CityGroupClickHandler());

    // Lägg in plats i kartan
    mapPane.getChildren().add(cityCircle);
  }

  /**
   * Skapar och ritar ut en anslutning mellan två platser på kartan.
   * 
   * @param place1 en plats
   * @param place2 en annan plats
   * @param name   namn på anslutningen
   * @param time   restiden att färdas via anslutningen
   */
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
   * 
   * @param filePath sökväg till ny bakgrundsbild
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

  /**
   * Hanterar vad som ska ske när "New map"-menyvalet väljs.
   */
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

  // TODO: Extrahera ut delad funktionalitet ur OpenMapItemHandler och
  // NewMapItemHandler och ha i en hjälpmetod public boolean
  // saveRequired(ActionEvent event){return event.isConsumed();}
  /**
   * 
   */
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

    /**
     * Öppnar en karta från en fil i programfönstret.
     * 
     * @param filePath sökväg till formaterad fil med kartdata
     */
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
        drawLocations(cityRow.split(";"));

        // Hämta och sätt in alla anslutningar mellan platser i grafen
        drawConnections(reader);

      } catch (FileNotFoundException ex) {
        System.err.println("Can't open file, because %s".formatted(ex.getMessage()));
      } catch (IOException ex) {
        System.err.println("IO error %s".formatted(ex.getMessage()));
      }
    }

    /**
     * Tolkar platsdata och skriver ut alla platser på kartan.
     */
    private void drawLocations(String[] LocationsData) {
      int nameIndex = 0;
      int xIndex = 1;
      int yIndex = 2;

      while (nameIndex < LocationsData.length) {
        try {
          City city = new City(LocationsData[nameIndex], Double.parseDouble(LocationsData[xIndex]),
              Double.parseDouble(LocationsData[yIndex]));
          graph.add(city);
          drawPlaceOnMap(city.getName(), city.getX(), city.getY());

        } catch (NumberFormatException nfe) {
          System.err.println("Expected a double, but got something else: " + nfe.getMessage());
        }
        nameIndex += 3;
        xIndex = nameIndex + 1;
        yIndex = nameIndex + 2;
      }
    }

    /**
     * 
     * @param reader
     * @throws IOException
     */
    private void drawConnections(BufferedReader reader) throws IOException {
      final int originIndex = 0;
      final int destinationIndex = 1;
      final int connectionNameIndex = 2;
      final int connectionWeightIndex = 3;
      Optional<City> origin;
      Optional<City> destination;
      int weight;
      /*
       * Pga ordningen som anslutningarna sparas i filen med Save-funktionen,
       * och att connect-metoden skapar dubbelriktade anslutningar, så bör
       * spegelvända anslutningar i filen där startpunkt och destination bytt plats
       * ignoreras. Annars får man ett felmeddelande när man försöker sätta in
       * en redan befintlig koppling igen i grafen. Genom att spara städer som har
       * varit startpunkter i tidigare anslutningar kan dessa försök att lägga till
       * en anslutning igen undvikas.
       */
      Set<String> visitedAsOrigin = new HashSet<>();
      String connectionRow = reader.readLine();
      String[] connectionData;

      while (connectionRow != null) {
        connectionData = connectionRow.split(";");

        // Hitta platsen som hör till inläst startpunktsnamn
        origin = findLocation(connectionData[originIndex]);

        // Hitta platsen som hör till inläst destinationsnamn
        // Kontrollera så destinationen inte redan dykt upp som en startpunkt tidigare
        if (!visitedAsOrigin.contains(connectionData[destinationIndex])) {
          destination = findLocation(connectionData[destinationIndex]);
          if (origin.isPresent() && destination.isPresent()) {
            try {
              weight = Integer.parseInt(connectionData[connectionWeightIndex]);
              graph.connect(origin.get(), destination.get(), connectionData[connectionNameIndex], weight);
              drawConnectionOnMap(origin.get(), destination.get(), connectionData[connectionNameIndex], weight);
              // Lägg till att startpunkten har besökts
              visitedAsOrigin.add(origin.get().getName());
            } catch (NumberFormatException nfe) {
              System.err.println("Expected an integer, but got something else: " + nfe.getMessage());
            }
          }
        }
        // Läs in nästa rad, om den finns, med anslutningsdata
        connectionRow = reader.readLine();
      }
    }


    /**
     * Letar efter en plats i grafen baserat på dess namn.
     * @param name platsen namn
     * @return korresponderande platsnod, om den hittas
     */
    private Optional<City> findLocation(String name) {
      for (City city : graph.getNodes()) {
        if (city.getName().equals(name)) {
          return Optional.of(city);
        }
      }
      return Optional.empty();
    }
  }

  /**
   * 
   * @param filePath
   */
  private void saveMap(String filePath) {

    try (FileWriter fileWriter = new FileWriter(filePath); PrintWriter writer = new PrintWriter(fileWriter)) {
      // Spara kartbildens URL i filen
      String imagePath = imageView.getImage().getUrl();
      writer.println(imagePath);
      // Spara grafens platser i filen
      Set<City> places = graph.getNodes();
      for (City place : places) {
        // Uppgifter sparas på en rad semikolonseparerade
        writer.print(String.format("%s;%s;%s;", place.getName(), place.getX(), place.getY()));
      }
      writer.println();
      // Spara grafens anslutningar i filen
      for (City origin : places) {
        for (Edge<City> connection : graph.getEdgesFrom(origin)) {
          // Uppgifter sparas rad för rad semikolonseparerade
          writer.println(String.format("%s;%s;%s;%s;", origin.getName(), connection.getDestination().getName(),
              connection.getName(), connection.getWeight()));
        }
      }

    } catch (FileNotFoundException ex) {
      System.err.println("Can't open file, because %s".formatted(ex.getMessage()));
    } catch (IOException ex) {
      System.err.println("IO error %s".formatted(ex.getMessage()));
    }
  }

  /**
   * 
   */
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

  /**
   * 
   */
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
        // Kontrollera om inmatning är en ordföljd som börjar med och innehåller
        // bokstäver, siffror och understreck, uppdelade ord för ord med ett blanktecken
        // eller bindesstreck
        if (placeName.matches("^[\\p{L}0-9_]+(?:[\\s-][\\p{L}0-9_]+)*$")) {

          // Potentiell hjälpmetod: där ordet behandlas för en separator (/s,-,_) åt
          // gången
          // Sätt första bokstav i varje delord till versal
          String[] subNames = placeName.split("\s");
          for (int i = 0; i < subNames.length; i++) {
            if (subNames[i].length() > 0)
              subNames[i] = subNames[i].substring(0, 1).toUpperCase() + subNames[i].substring(1);
          }
          placeName = subNames[0];
          for (int i = 1; i < subNames.length; i++) {
            placeName += " " + subNames[i];
          }

          // Kontrollera om en plats med samma namn redan finns
          Iterator<City> places = graph.getNodes().iterator();
          boolean found = false;
          String existingPlaceName = null;
          while (places.hasNext() && !found) {
            existingPlaceName = places.next().getName();
            if (existingPlaceName.equals(placeName)) {
              found = true;
            }
          }

          if (found) {
            showErrorAlert(existingPlaceName + " already exists!");
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
        } else {
          showErrorAlert("Invalid format: Enter a proper name!");
        }
      }
      scene.setCursor(Cursor.DEFAULT);
      newPlace.setDisable(false);
      newPlace.requestFocus();
      mapPane.setOnMouseClicked(null);
    }
  }

  /**
   * 
   */
  class CityGroupClickHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {
      // Ta fram vilken cirkel vars grupp har klickats på (i vyn)
      Group cityGroup = (Group) event.getSource();
      ObservableList<Node> list = cityGroup.getChildren();
      Circle clickedCircle = (Circle) list.get(0);
      Label clickedLabel = (Label) list.get(1);
      String cityName = clickedLabel.getText();
      // Ta fram vilken stad den refererar till (i modellen)
      City clickedCity = null;
      // Potentiell hjälpmetod: getCity(String name)För att hämta en stad med givet
      // namn
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
      // Potentiell hjälpmetod: markCityIfPossible(City city) alternativt mark(City
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

  /**
   * Hanterar vad som ska ske när New Place-knappen väljs.
   */
  class NewPlaceHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      newPlace.setDisable(true);
      scene.setCursor(Cursor.CROSSHAIR);
      mapPane.setOnMouseClicked(new MapClickHandler());
    }
  }

  /**
   * Kontrollerar om två platser markerats på kartan.
   * 
   * @return sant om det stämmer, annars falskt
   */
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

  /**
   * 
   * @param choice
   */
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
    if (connectionName != null && connectionName.matches("^[\\p{L}0-9_]+(?:[\\s-][\\p{L}0-9_]+)*$"))
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

    // Här börjar visning och svarshantering
    Optional<ButtonType> result = alert.showAndWait();

    if (result.isPresent() && result.get().equals(ButtonType.OK)) {

      if (nameEditable) {
        String nameText = nameField.getText();

        if (nameText.isBlank()) {
          showErrorAlert("Input missing:\nEnter a name!");
          return Optional.empty();
        } else if (nameText.matches("^[\\p{L}0-9_]+(?:[\\s-][\\p{L}0-9_]+)*$")) {
          connectionName = nameText;
        } else {
          showErrorAlert("Invalid format:\nEnter a proper name!");
          return Optional.empty();
        }
      }

      if (timeEditable) {
        String timeText = timeField.getText();
        if (timeText.isBlank()) {
          showErrorAlert("Input missing:\nEnter a time!");
          return Optional.empty();
        } else if (!timeText.matches("\\d+")) {
          showErrorAlert("Invalid format:\nEnter time as a positive integer value!");
          return Optional.empty();
        } else {
          time = Integer.parseInt(timeText);
        }
      }

      return Optional.of(new ListEdge<>(markedCity2, connectionName, time));
    }

    return Optional.empty();
  }

  /**
   * 
   */
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

  /**
   * 
   */
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

  /**
   * 
   */
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

  /**
   * 
   */
  class NewConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      if (twoPlacesSelected()) {
        try {
          // Potentiell hjälpmetod: placesConnected() som returnerar boolean och skapar
          // eventuellt en felmeddelande
          Edge<City> connection = graph.getEdgeBetween(markedCity2, markedCity1);
          if (connection == null) {
            Optional<Edge<City>> result = showConnectionForm(null, 0, true, true);

            if (result.isPresent()) {
              String connectionName = result.get().getName();
              int connectionTime = result.get().getWeight();

              graph.connect(markedCity1, markedCity2, connectionName, connectionTime);
              drawConnectionOnMap(markedCity1, markedCity2, connectionName, connectionTime);

              edited = true;
            }
            // Nollställ markeringar
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

  /**
   * 
   */
  private class ExitItemHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent arg0) {
      stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
  }

  /**
   * 
   */
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
          // Stoppa nedstängningshändelse
          event.consume();
        }
      }
    }
  }

  /**
   * Hanterar vad som ska ske när "Save Image"-menyvalet väljs.
   */
  private class SaveImageItemHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {
      Alert alert;
      try {
        WritableImage image = centerPane.snapshot(null, null);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bufferedImage, "png", new File("capture.png"));
        alert = new Alert(AlertType.INFORMATION, "Snapshot saved as capture.png");
        alert.setHeaderText(null);
      } catch (IOException e) {
        alert = new Alert(Alert.AlertType.ERROR, "IO Error " + e.getMessage());
      }
      alert.showAndWait();
    }
  }
}
