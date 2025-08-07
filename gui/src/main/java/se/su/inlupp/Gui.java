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
import javafx.event.Event;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * Huvudklassen i programmet. Klassen har hand om programmets grafiska vy och
 * interaktionen mellan den och programmets bakomliggande logiska modellen.
 */
public class Gui extends Application {
  /*
   * Reguljärt uttryck som matchas mot en ordföljd som börjar med och innehåller
   * Unicode-bokstäver och siffror, uppdelade ord för ord med ett blanksteg,
   * bindesstreck, eller en punkt med ett efterföljande blanksteg
   */
  private final String PROPER_NAME_REGEX = "^[\\p{L}0-9]+(?:(?:[\s-]|\\. )[\\p{L}0-9]+)*$";

  private Stage stage;
  private FileChooser fileChooser;
  private ExtensionFilter noFilter;
  private ExtensionFilter txtFilter;
  private ExtensionFilter imageFilter;
  private ImageView imageView;
  private Pane mapPane;
  private ScrollPane scrollPane;
  private FlowPane buttonPane;
  private Button newPlace;
  private Scene scene;
  private VBox vbox;
  private HBox hbox;
  private Graph<Place> graph;
  private BorderPane root;

  private boolean edited;

  private Place selectedPlace1 = null;
  private Place selectedPlace2 = null;
  private Circle selectedCircle1 = null;
  private Circle selectedCircle2 = null;

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
    // Fix: Skapa ändelsefilter för text- och bildfiler som används i
    // fildialogfönster
    noFilter = new ExtensionFilter("All files (*)", "*");
    txtFilter = new ExtensionFilter("Text files (*.txt)", "*.txt");
    imageFilter = new ExtensionFilter("Image files (*.png, *.jpg)", "*.png", "*.jpg");
    fileChooser.getExtensionFilters().addAll(txtFilter, imageFilter);

    // grafen som lagrar alla platser och förbindelser
    graph = new ListGraph<>();
    // filmeny
    MenuItem newMap = new MenuItem("New map");
    newMap.setOnAction(new NewMapItemHandler());
    MenuItem open = new MenuItem("Open");
    open.setOnAction(new OpenMapItemHandler());
    MenuItem save = new MenuItem("Save");
    save.setOnAction(new SaveMapItemHandler());
    MenuItem saveImage = new MenuItem("Save Image");
    saveImage.setOnAction(new SaveImageItemHandler());
    MenuItem exit = new MenuItem("Exit");
    exit.setOnAction(new ExitItemHandler());
    Menu menu = new Menu("File");
    menu.getItems().addAll(newMap, open, save, saveImage, exit);
    MenuBar menuBar = new MenuBar(menu);
    // knapplist
    Button findPath = new Button("Find Path");
    findPath.setOnAction(new FindPathHandler());
    Button showConnection = new Button("Show Connection");
    showConnection.setOnAction(new ShowConnectionHandler());
    newPlace = new Button("New Place");
    newPlace.setOnAction(new NewPlaceHandler());
    Button newConnection = new Button("New Connection");
    newConnection.setOnAction(new NewConnectionHandler());
    Button changeConnection = new Button("Change Connection");
    changeConnection.setOnAction(new ChangeConnectionHandler());
    buttonPane = new FlowPane(findPath, showConnection, newPlace, newConnection, changeConnection);
    buttonPane.setAlignment(Pos.CENTER);
    buttonPane.setOrientation(Orientation.HORIZONTAL);
    buttonPane.setHgap(5);
    buttonPane.setVgap(5);
    buttonPane.setPadding(new Insets(5));
    buttonPane.setDisable(true);
    // behållare för alla knapp- och menykomponenter
    vbox = new VBox(menuBar, buttonPane);
    // karta med en bildvy
    imageView = new ImageView();
    // imageView.setFitWidth(0);
    // imageView.setFitHeight(0);
    // imageView.imageProperty().addListener((obs, oldImage, newImage) -> {
    //   if (newImage != null) {
    //     mapPane.setManaged(true);
    //     mapPane.setVisible(true);
    //     scrollPane.setManaged(true);
    //     scrollPane.setVisible(true);
    //     // imageView.setFitWidth(newImage.getWidth());
    //     // imageView.setFitHeight(newImage.getHeight());
    //     // centerPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    //     // centerPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    //     double width = newImage.getWidth();
    //     double height = newImage.getHeight();
    //     scrollPane.setPrefViewportWidth(width);
    //     scrollPane.setPrefViewportHeight(height);
    //     scrollPane.setPrefSize(width, height);
    //     //XOR
    //     mapPane.setPrefSize(width, height);
    //     int scrollbarAllowance = 10;
    //     // root.setPrefSize(width, height + vbox.getHeight());
    //     root.setPrefWidth(width + scrollbarAllowance);
    //     stage.sizeToScene(); // Anpassa fönstret
    //     root.setPrefHeight(height + scrollbarAllowance + vbox.getHeight());
    //     stage.sizeToScene();
    //   }
    // });

    // scrollPane.setHvalue((root.getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth()) / 2);
    // scrollPane.setVvalue((root.getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight()) / 2);

    mapPane = new Pane(imageView);
    mapPane.setBackground(Background.fill(null));
    // mapPane.setManaged(false);
    // mapPane.setVisible(false);

    // Behållare som visar scrollbars om innehållet är större än dess viewport
    // scrollPane = new ScrollPane(mapPane);
    // scrollPane.setManaged(false);
    // scrollPane.setVisible(false);
    // scrollPane.setBackground(Background.fill(Color.BROWN));

    // behållare till alla kartkomponenter
    FlowPane centerPane = new FlowPane(mapPane);
    centerPane.setAlignment(Pos.CENTER);
    

    // behållare (rotnod) till alla komponenter i fönstret
    root = new BorderPane();
    root.setBackground(Background.fill(Color.BEIGE));
    root.setTop(vbox);
    root.setCenter(mapPane);
    // root.setCenter(scrollPane);
    root.setPrefSize(520, vbox.getHeight());

    stage = primaryStage;
    stage.setTitle("PathFinder");
    stage.setOnCloseRequest(new ExitHandler());
    scene = new Scene(root);
    stage.setScene(scene);
    stage.show();
    // hbox.setDisable(true);
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
   * Kontrollerar om det finns osparade ändringar och varna användaren om så är
   * fallet.
   * 
   * @param event som är kopplat till att kontrollen triggats
   */
  private void checkForUnsavedChanges(Event event) {
    if (edited) {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.setTitle("Warning!");
      alert.setContentText("Unsaved changes, continue anyway?");
      alert.setHeaderText(null);
      Optional<ButtonType> answer = alert.showAndWait();
      if (answer.isPresent() && answer.get().equals(ButtonType.CANCEL)) {
        event.consume(); // markera att händelse ska avbrytas
      }
    }
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
    cityCircle.setOnMouseClicked(new GroupClickHandler());
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
  private void drawConnectionOnMap(Place place1, Place place2) {
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
   * @return om bilden ändrats eller inte
   */
  private boolean changeMapImage(String filePath) {
    boolean changed = false;
    if (filePath.matches(".+\\.(?i)(png|img)")) {
      // steg 1: hämtar bild på karta och ritar ut i kartvy
      Image image = new Image(filePath);
      imageView.setImage(image);
      // steg 2: anpassar rot och fönster efter (önskad) bildvidd
      root.setPrefWidth(image.getWidth());
      stage.sizeToScene();
      // steg 3: anpassar rot och fönster efter (önskad) gemensam komponenthöjd givet
      // den nya vidden
      root.setPrefHeight(image.getHeight() + vbox.getHeight());
      stage.sizeToScene();

      Rectangle clip = new Rectangle(image.getWidth(), image.getHeight());
      mapPane.setClip(clip);

      changed = true;
    } else {
      showErrorAlert("Invalid file for map image. Choose a PNG- or IMG-file.");
    }
    return changed;
  }

  /**
   * Letar efter en plats i grafen baserat på dess namn.
   * 
   * @param name platsen namn
   * @return korresponderande platsnod, om den hittas
   */
  private Optional<Place> findLocation(String name) {
    for (Place location : graph.getNodes()) {
      if (location.getName().equals(name)) {
        return Optional.of(location);
      }
    }
    return Optional.empty();
  }

  /**
   * Kontrollerar om två platser markerats på kartan.
   * 
   * @return sant om det stämmer, annars falskt
   */
  private boolean twoPlacesSelected() {
    boolean bothSelected = !(selectedPlace1 == null || selectedPlace2 == null);
    if (!bothSelected) {
      showErrorAlert("Two places must be selected!");
    }
    return bothSelected;
  }

  /**
   * Återställer markeringar på kartan.
   */
  private void clearSelectedPlaces() {
    selectedPlace1 = null;
    selectedPlace2 = null;
    if (selectedCircle1 != null) {
      selectedCircle1.setFill(Color.PINK);
    }
    if (selectedCircle2 != null) {
      selectedCircle2.setFill(Color.PINK);
    }
  }

  /**
   * När en dialogrutan behövs för att hantera en anslutning på något sätt,
   * så visas den via denna metod.
   * 
   * @param connectionName namnet på anslutningen
   * @param time           restiden att använda anslutningen
   * @param nameEditable   namn går att redigera
   * @param timeEditable   tid går att redigera
   * @return en kant som representerar anslutningen, om allt går bra
   */
  private Optional<Edge<Place>> showConnectionForm(String connectionName, int time, boolean nameEditable,
      boolean timeEditable) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Connection");
    alert.setHeaderText("Connection from " + selectedPlace1.getName() + " to " + selectedPlace2.getName());

    Label nameLabel = new Label("Name of connection:");
    TextField nameField;
    if (connectionName != null && connectionName.matches(PROPER_NAME_REGEX))
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

    final int SPACING_VALUE = 12;
    GridPane grid = new GridPane();
    grid.setHgap(SPACING_VALUE);
    grid.setVgap(SPACING_VALUE);
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
          showErrorAlert("Input missing: Enter a name!");
          return Optional.empty();
        } else if (nameText.matches(PROPER_NAME_REGEX)) {
          connectionName = nameText;
        } else {
          showErrorAlert("Invalid format: Enter a proper name!");
          return Optional.empty();
        }
      }
      if (timeEditable) {
        String timeText = timeField.getText();
        if (timeText.isBlank()) {
          showErrorAlert("Input missing: Enter a time!");
          return Optional.empty();
        } else if (!timeText.matches("\\d+")) {
          showErrorAlert("Invalid format: Enter time as a positive integer value!");
          return Optional.empty();
        } else {
          time = Integer.parseInt(timeText);
        }
      }
      return Optional.of(new ListEdge<>(selectedPlace2, connectionName, time));
    }
    return Optional.empty();
  }

  /**
   * Hämtar en anslutning mellan de två valda platserna och ger ett felmeddelande
   * utgående från om anslutningen förväntas hittas eller inte.
   * 
   * @param connectionExpected om en anslutning förväntas hittas
   * @return en kant som representerar anslutningen, om den finns
   */
  private Optional<Edge<Place>> getConnectionBetweenSelectedPlaces(boolean connectionExpected) {
    Optional<Edge<Place>> result = Optional.empty();
    Edge<Place> connection = graph.getEdgeBetween(selectedPlace1, selectedPlace2);
    if (connectionExpected) {
      if (connection == null) {
        showErrorAlert("There exists no connection between " + selectedPlace1.getName() + " and "
            + selectedPlace2.getName() + "!");
      } else {
        result = Optional.of(connection);
      }
    } else {
      if (connection != null) {
        showErrorAlert("There already exists a connection between these places!");
      }
    }
    return result;
  }

  /**
   * Hanterar vad som ska ske när "New map"-menyvalet väljs.
   */
  class NewMapItemHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      checkForUnsavedChanges(event);
      if (!event.isConsumed()) {
        fileChooser.setSelectedExtensionFilter(imageFilter);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
          resetMapGraph();
          if (changeMapImage(file.toURI().toString())) {
            edited = false;
            buttonPane.setDisable(false);
          }
        }
      }
    }
  }

  /**
   * Hanterar vad som ska ske när "Open"-menyvalet väljs.
   */
  class OpenMapItemHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent event) {
      checkForUnsavedChanges(event);
      if (!event.isConsumed()) { // om händelse fortfarande pågår
        fileChooser.setSelectedExtensionFilter(txtFilter);
        File graphFile = fileChooser.showOpenDialog(stage);
        if (graphFile != null) {
          openMap(graphFile.getAbsolutePath());
          edited = false;
          buttonPane.setDisable(false);
          buttonPane.requestFocus();
        }
      }
    }

    /**
     * Öppnar en karta från en fil i programfönstret.
     * 
     * @param filePath sökväg till formaterad fil med kartdata
     */
    private void openMap(String filePath) {
      boolean opened = false;
      try (FileReader fileReader = new FileReader(filePath); BufferedReader reader = new BufferedReader(fileReader)) {
        // TODO: Byt ut till genomgång av fil med Iterator<String> lineItertor =
        // reader.lines().iterator();
        resetMapGraph();
        // Byt kartbild i fönstret
        String mapPath = reader.readLine();
        // TODO: skriv ut felmeddelande om filen inte är i korrekt format
        if (changeMapImage(mapPath)) {
          // Hämta och sätt in alla platser i grafen
          String locationRow = reader.readLine();
          drawLocations(locationRow.split(";"));
          // Hämta och sätt in alla anslutningar mellan platser i grafen
          drawConnections(reader);
          opened = true;
        } else {

        }

      } catch (FileNotFoundException ex) {
        showErrorAlert("Can't open file, because %s".formatted(ex.getMessage()));
      } catch (IOException ex) {
        showErrorAlert("IO error %s".formatted(ex.getMessage()));
      }
    }

    /**
     * Tolkar platsdata och ritar ut alla platser på kartan.
     */
    private void drawLocations(String[] LocationsData) {
      int nameIndex = 0;
      int xIndex = 1;
      int yIndex = 2;
      while (nameIndex < LocationsData.length) {
        try {
          Place city = new Place(LocationsData[nameIndex], Double.parseDouble(LocationsData[xIndex]),
              Double.parseDouble(LocationsData[yIndex]));
          graph.add(city);
          drawPlaceOnMap(city.getName(), city.getX(), city.getY());
        } catch (NumberFormatException nfe) {
          showErrorAlert("Expected a double, but got something else: " + nfe.getMessage());
        }
        nameIndex += 3;
        xIndex = nameIndex + 1;
        yIndex = nameIndex + 2;
      }
    }

    /**
     * Tolkar anslutningsdata och ritar ut alla anslutningar på kartan.
     * 
     * @param reader sköter filläsning
     * @throws IOException ifall det inte går att läsa från fil
     */
    private void drawConnections(BufferedReader reader) throws IOException {
      final int originIndex = 0;
      final int destinationIndex = 1;
      final int connectionNameIndex = 2;
      final int connectionWeightIndex = 3;
      Optional<Place> origin;
      Optional<Place> destination;
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
              drawConnectionOnMap(origin.get(), destination.get());
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
  }

  /**
   * Hanterar vad som ska ske när "Save"-menyvalet väljs.
   */
  class SaveMapItemHandler implements EventHandler<ActionEvent> {

    @Override
    public void handle(ActionEvent event) {
      fileChooser.setSelectedExtensionFilter(txtFilter);
      fileChooser.setInitialFileName("graph.txt");
      File saveFile = fileChooser.showSaveDialog(stage);
      if (saveFile != null) {
        saveMap(saveFile.getAbsolutePath());
        edited = false;
      }
    }

    /**
     * Sparar innehållet i kartan till en fil.
     * 
     * @param filePath sökväg till filen där kartinnehåll sparas
     */
    private void saveMap(String filePath) {
      try (FileWriter fileWriter = new FileWriter(filePath); PrintWriter writer = new PrintWriter(fileWriter)) {
        // Spara kartbildens URL i filen
        String imagePath = imageView.getImage().getUrl();
        writer.println(imagePath);
        // Spara grafens platser i filen
        Set<Place> places = graph.getNodes();
        for (Place place : places) {
          // Uppgifter sparas på en rad semikolonseparerade
          writer.print(String.format("%s;%s;%s;", place.getName(), place.getX(), place.getY()));
        }
        writer.println();
        // Spara grafens anslutningar i filen
        for (Place origin : places) {
          for (Edge<Place> connection : graph.getEdgesFrom(origin)) {
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
     * @param filePath
     * @param extensions
     * @return
     */
    private boolean hasValidExtension(String filePath, List<String> extensions) {
      for (String extension : extensions) {
        if (filePath.endsWith(extension)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Hanterar vad som ska ske när "Save Image"-menyvalet väljs.
   */
  class SaveImageItemHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {
      if (imageView.getImage() != null) {
        Alert alert;
        try {
          WritableImage image = scrollPane.snapshot(null, null); // TODO: kolla så att snapshot får med hela bilden
                                                                 // eller bara det som är synligt i fönstret
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

  /**
   * Hanterar vad som ska ske när "New Place"-knappen trycks på.
   */
  class NewPlaceHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      buttonPane.setDisable(true);
      mapPane.requestFocus();
      mapPane.setCursor(Cursor.CROSSHAIR);
      mapPane.setOnMouseClicked(new MapClickHandler());
    }
  }

  /**
   * Hanterar vad som ska ske när en position på kartan klickas på.
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
        if (placeName.matches(PROPER_NAME_REGEX)) {
          placeName = changeStartingLettersToUpperCase(placeName);
          // Kontrollera om en plats med samma namn redan finns
          if (findLocation(placeName).isPresent()) {
            showErrorAlert(placeName + " already exists!");
          } else {
            // Hämta koordinater på muspekaren vid klick
            double x = event.getX();
            double y = event.getY();
            // Skapa en platsnod och lägg in grafmodellen
            Place placeNode = new Place(placeName, x, y);
            graph.add(placeNode);
            // Rita ut platsen på kartan
            drawPlaceOnMap(placeName, x, y);
            edited = true;
          }
        } else {
          showErrorAlert("Invalid format: Enter a proper name!");
        }
      }
      mapPane.setCursor(Cursor.DEFAULT);
      buttonPane.setDisable(false);
      newPlace.requestFocus();
      mapPane.setOnMouseClicked(null);
    }

    /**
     * Sätt första bokstav i varje delord till versal
     * 
     * @param name består av ett eller flera delnamn
     * @return samma delnamn fast som inleds med stor bokstav
     */
    private String changeStartingLettersToUpperCase(String name) {
      String[] subNames = name.split("[\\s-]");
      for (int i = 0; i < subNames.length; i++) {
        if (subNames[i].length() > 0)
          subNames[i] = subNames[i].substring(0, 1).toUpperCase() + subNames[i].substring(1);
      }
      String upperCaseName = subNames[0];
      int indexfOfNextDivider = subNames[0].length();
      String nextdivider;
      for (int i = 1; i < subNames.length; i++) {
        nextdivider = name.substring(indexfOfNextDivider, indexfOfNextDivider + 1);
        switch (nextdivider) {
          case " ":
            upperCaseName += " " + subNames[i];
            break;
          case "-":
            upperCaseName += "-" + subNames[i];
            break;
          default:
            System.err.println("Something went wrong. This is not a valid divider: " + nextdivider);
            break;
        }
        indexfOfNextDivider += 1 + subNames[i].length();
      }
      return upperCaseName;
    }
  }

  /**
   * Hanterar vad som ska ske när en platsgrupp klickas på.
   */
  class GroupClickHandler implements EventHandler<MouseEvent> {
    private Place clickedPlace;
    private Circle clickedCircle;

    @Override
    public void handle(MouseEvent event) {
      // Ta fram vilken cirkel vars grupp har klickats på
      Group placeGroup = (Group) event.getSource();
      ObservableList<Node> list = placeGroup.getChildren();
      clickedCircle = (Circle) list.get(0);
      Label clickedLabel = (Label) list.get(1);
      String locationName = clickedLabel.getText();
      // Ta fram vilken stad cirkeln refererar till
      try {
        clickedPlace = findLocation(locationName).get(); // OBS: fungerar så länge plats inte får ha samma namn
        selectPlaceOnMap();
        // Just in case...
      } catch (NoSuchElementException e) {
        showErrorAlert("City-node expected, null found!\n%s".formatted(e.getMessage()));
      }
    }

    /**
     * Markerar klickad plats, om två platser inte är redan markerade.
     * Avmarkerar klickad plats om den är markerad.
     */
    private void selectPlaceOnMap() {
      if (selectedPlace1 == null && selectedPlace2 == null) { // inget markerat sen tidigare, markera klickad plats
        selectedPlace1 = clickedPlace;
        selectedCircle1 = clickedCircle;
        selectedCircle1.setFill(Color.PURPLE);
      } else if (clickedPlace.equals(selectedPlace1)) { // klickad plats redan markerad, ta bort markering
        selectedPlace1 = null;
        selectedCircle1.setFill(Color.PINK);
        selectedCircle1 = null;
      } else if (clickedPlace.equals(selectedPlace2)) {
        selectedPlace2 = null;
        selectedCircle2.setFill(Color.PINK);
        selectedCircle2 = null;
      } else if (selectedPlace1 == null) { // klickad plats inte markerad, markera klickad stad
        selectedPlace1 = clickedPlace;
        selectedCircle1 = clickedCircle;
        selectedCircle1.setFill(Color.PURPLE);
      } else if (selectedPlace2 == null) {
        selectedPlace2 = clickedPlace;
        selectedCircle2 = clickedCircle;
        selectedCircle2.setFill(Color.PURPLE);
      } else { // markeringar upptagna, ignorera klickad plats

      }
    }
  }

  /**
   * Hanterar vad som ska ske när "Change Connection"-knappet trycks på.
   */
  class ChangeConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      if (twoPlacesSelected()) {
        Optional<Edge<Place>> connection = getConnectionBetweenSelectedPlaces(true);
        if (connection.isPresent()) {
          Optional<Edge<Place>> result = showConnectionForm(connection.get().getName(), 0, false, true);
          if (result.isPresent()) {
            try {
              int time = result.get().getWeight();
              graph.setConnectionWeight(selectedPlace1, selectedPlace2, time);
              edited = true;
            } catch (NumberFormatException e) {
              showErrorAlert("Invalid number input.\n%s".formatted(e.getMessage()));
            }
          }
        } else {
          clearSelectedPlaces();
        }
      }
    }
  }

  /**
   * Hanterar vad som ska ske när "Show Connection"-knappen trycks på.
   */
  class ShowConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      if (twoPlacesSelected()) {
        Optional<Edge<Place>> connection = getConnectionBetweenSelectedPlaces(true);
        if (connection.isPresent()) {
          showConnectionForm(connection.get().getName(), connection.get().getWeight(), false, false);
        } else {
          clearSelectedPlaces();
        }
      }
    }
  }

  /**
   * Hanterar vad som ska ske när "Find Path"-knappen trycks på.
   */
  class FindPathHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      int edgeWeightTotal = 0;
      if (twoPlacesSelected()) {
        List<Edge<Place>> path = graph.getPath(selectedPlace1, selectedPlace2);
        if (path != null) {
          BorderPane borderPane = new BorderPane();
          TextArea textArea = new TextArea("");
          borderPane.setCenter(textArea);
          for (Edge<Place> edge : path) {
            edgeWeightTotal += edge.getWeight();
            textArea.appendText(edge.toString() + "\n");
          }
          textArea.appendText("Total time: " + String.valueOf(edgeWeightTotal));
          textArea.setEditable(false);

          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle("Message");
          alert.setHeaderText("The Path from " + selectedPlace1.getName() + " to " + selectedPlace2.getName());
          alert.getDialogPane().setContent(borderPane);
          alert.showAndWait();
        } else {
          showErrorAlert("There is no path from " + selectedPlace1.getName() + " to " + selectedPlace2.getName());
        }
        clearSelectedPlaces();
      }
    }
  }

  /**
   * Hanterar vad som ska ske när "New Connection"-knappen trycks på.
   */
  class NewConnectionHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent event) {
      if (twoPlacesSelected()) {
        try {
          Optional<Edge<Place>> existingConnection = getConnectionBetweenSelectedPlaces(false);
          if (existingConnection.isEmpty()) {
            Optional<Edge<Place>> result = showConnectionForm(null, 0, true, true);
            if (result.isPresent()) {
              String connectionName = result.get().getName();
              int connectionTime = result.get().getWeight();
              graph.connect(selectedPlace1, selectedPlace2, connectionName, connectionTime);
              drawConnectionOnMap(selectedPlace1, selectedPlace2);
              edited = true;
            }
            clearSelectedPlaces();
          }
        } catch (NoSuchElementException ex) {
          showErrorAlert("Two selected nodes expected, but not found!\n%s".formatted(ex.getMessage()));
        }
      }
    }
  }

  /**
   * Hanterar vad som ska ske när "Exit"-menyvalet väljs.
   */
  class ExitItemHandler implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent arg0) {
      stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }
  }

  /**
   * Hanterar programmets nedstängningsrutin.
   */
  class ExitHandler implements EventHandler<WindowEvent> {
    @Override
    public void handle(WindowEvent event) {
      checkForUnsavedChanges(event);
    }
  }
}
