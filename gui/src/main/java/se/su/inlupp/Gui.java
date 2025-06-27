package se.su.inlupp;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
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
    MenuItem save = new MenuItem("Save");
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
    return string == null || string.isBlank();
  }

  private void writeErrorAlert(String prompt) {
    Alert alert = new Alert(AlertType.ERROR);
    alert.setTitle("Error!");
    alert.setHeaderText(null);
    alert.setContentText(prompt);
    alert.showAndWait();
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

      if(!event.isConsumed()){ // om händelse fortfarande pågår
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
          changeMap(file.toURI().toString());

          edited = false;
        }
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
          writeErrorAlert("Incorrect input: Enter a name!");
        } else {
          // Potentiell hjälpklass createPlaceOnMap(name, x, y)

          // Hämta koordinater på muspekaren vid klick
          double x = event.getX();
          double y = event.getY();

          // Skapa cirkel
          Circle circle = new Circle(x, y, 12);
          circle.setFill(Color.PINK);

          // Skapa etikett
          Label city = new Label(placeName);
          city.setLayoutX(x + 6);
          city.setLayoutY(y + 6);
          city.setStyle("fx-font-family: 'Calibri'; -fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: black;");
          city.setLabelFor(circle);

          // Skapa behållare för stadens etikett och cirkel
          Group cityCircle = new Group();
          cityCircle.getChildren().addAll(circle, city);
          cityCircle.setOnMouseClicked(new CityCircleClickHandler());

          // Lägg in platsbehållare i kartans behållare
          mapPane.getChildren().add(cityCircle);

          // Skapa en stadnod och lägg in grafmodellen
          City cityNode = new City(placeName, x, y);
          graph.add(cityNode);

          edited = true;
        }
        // } else {
        // event.consume();
      }

      scene.setCursor(Cursor.DEFAULT);
      newPlace.setDisable(false);
      mapPane.setOnMouseClicked(null);

    }
  }

  class CityCircleClickHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {
      // tar fram referenser till klickad cirkel och tillhörande stad (från Group)
      Group cityCircle = (Group) event.getSource();
      ObservableList<Node> list = cityCircle.getChildren();
      Circle clickedCircle = (Circle) list.get(0);
      Label clickedLabel = (Label) list.get(1);
      String cityName = clickedLabel.getText();
      // tar fram samma stad ur grafen (modellen)
      City clickedCity = null;
      // Potentiell hjälpmetod i ListGraph för att hämta en nod, om VPL tillåter det
      Set<City> cities = graph.getNodes();
      for (City city : cities) {
        if (city.getCityName().equals(cityName) && city.getX() == clickedCircle.getCenterX()
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

  // TODO: Försök använda en färdig fönstertyp med rätt symbol istället
  private Optional<Edge<City>> showConnectionForm(String connectionName, int time, boolean nameEditable, boolean timeEditable) {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Connection");
    alert.setHeaderText("Connection from " + markedCity1.getCityName() + " to " + markedCity2.getCityName());

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
    //   if (!newValue.matches("\\d*")) {
    //     timeField.setText(newValue.replaceAll("[^\\d]", ""));
    //   }
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

    if(result.isPresent() && result.get().equals(ButtonType.OK)){
      if(nameEditable){
        String nameText = nameField.getText();
        if(nameText.isBlank()){
          writeErrorAlert("Incorrect input:\nEnter a name!");
          return Optional.empty();
        } else {
          connectionName = nameText;
        }
      }

      if(timeEditable){
        String timeText = timeField.getText();
        if(timeText.isBlank()){
          writeErrorAlert("Incorrect input:\nEnter a time!");
          return Optional.empty();
        } else if (!timeText.matches("\\d+")) {
          writeErrorAlert("Incorrect input:\nEnter time as a positive integer value!");
          return Optional.empty();
        } else{
          time = Integer.parseInt(timeText);
        }
      }
      
      return Optional.of(new ListEdge<>(markedCity2, connectionName, time));
    }
    // Hantera det som skrivits in och skicka tillbaka ett resultat från
    // dialogfönstret
    // alert.setResultConverter(
    //     new Callback<ButtonType, ButtonType>() {
    //       @Override
    //       public ButtonType call(ButtonType dialogButton) {
    //         if (dialogButton == ButtonType.OK) {
    //           try {
    //             String name = nameField.getText();
    //             String timeText = timeField.getText();

    //             if (name.isBlank()) {
    //               writeErrorAlert("Incorrect input:\nEnter a name!");
    //               return null;
    //             } else if (timeText.isBlank()) {
    //               writeErrorAlert("Incorrect input:\nEnter a time!");
    //               return null;
    //             } else if (!timeText.matches("\\d+")) {
    //               writeErrorAlert("Incorrect input:\nEnter time as a positive integer value!");
    //               return null;
    //             } else {
    //               int time = Integer.parseInt(timeText);
    //               return ButtonType.OK;
    //             }
    //           } catch (NumberFormatException e) {
    //             System.err.println("Something went wrong when reading time from NewConnectionForm!");
    //             return null;
    //           }
    //         } else {
    //           // om denna metod returnerar null innebär det att showAndWait() returnerar en
    //           // tom Optional<Edge<City>
    //           return null;
    //         }
    //       }
    //     });

      return Optional.empty();
  }

  private class NewConnectionForm extends Dialog<Edge<City>> {
    private TextField nameField = new TextField();
    private TextField timeField = new TextField();

    public NewConnectionForm() {
      // Skapa fönsterkomponenter
      setTitle("Connection");
      setHeaderText("Connection from " + markedCity1.getCityName() + " to " + markedCity2.getCityName());
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
        String cityName1 = markedCity1.getCityName();
        String cityName2 = markedCity2.getCityName();

        Edge<City> connection = graph.getEdgeBetween(markedCity1, markedCity2);
        if (connection != null) {
          String connectionName = connection.getName();

          Alert alert = new Alert(AlertType.CONFIRMATION);

          alert.setTitle("Connection");
          alert.setHeaderText("Connection from " + cityName1 + " to " + cityName2);

          GridPane grid = new GridPane();
          grid.setHgap(12);
          grid.setVgap(12);
          grid.setPadding(new Insets(12));

          Label nameLabel = new Label("Name of connection:");
          TextField nameField = new TextField(connectionName);
          nameField.setEditable(false);

          Label timeLabel = new Label("Time:");
          TextField timeField = new TextField(" ");
          timeField.setEditable(true);
          timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
              writeErrorAlert("Invalid input!!!");
              timeField.setText(newValue.replaceAll("[^\\d]", ""));
            }
          });

          grid.add(nameLabel, 0, 0);
          grid.add(nameField, 1, 0);
          grid.add(timeLabel, 0, 1);
          grid.add(timeField, 1, 1);

          alert.getDialogPane().setContent(grid);

          alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

          Optional<ButtonType> result = alert.showAndWait();
          if (result.isPresent() && result.get() == ButtonType.OK) {
            String timeText = timeField.getText();
            try {
              int time = Integer.parseInt(timeText);
              graph.setConnectionWeight(markedCity1, markedCity2, time);
              
              edited = true;
            } catch (NumberFormatException e) {
              System.out.println("Invalid number input.");
            }

          }

          clearSelectedPlaces();

        } else {
          writeErrorAlert("There is no connection to change between these cities.");
          clearSelectedPlaces();
        }

      } else {
        // Felmeddelande för att två platser måste markeras.
        // Finns redan i twoPlacesSelected()
      }

    }
  }

  class ShowConnectionHandler implements EventHandler<ActionEvent> {
    public void handle(ActionEvent event) {

      if (twoPlacesSelected()) {
        String cityName1 = markedCity1.getCityName();
        String cityName2 = markedCity2.getCityName();
        Edge<City> connection = graph.getEdgeBetween(markedCity1, markedCity2);
        if (connection != null) {
          String connectionName = connection.getName();
          int connectionTime = connection.getWeight();

          Alert alert = new Alert(AlertType.CONFIRMATION);
          alert.setTitle("Connection");
          alert.setHeaderText("Connection from " + cityName1 + " to " + cityName2);

          GridPane grid = new GridPane();
          grid.setHgap(12);
          grid.setVgap(12);
          grid.setPadding(new Insets(12));

          Label nameLabel = new Label("Name of connection:");
          TextField nameField = new TextField(connectionName);
          nameField.setEditable(false);

          Label timeLabel = new Label("Time:");
          TextField timeField = new TextField(String.valueOf(connectionTime));
          timeField.setEditable(false);

          grid.add(nameLabel, 0, 0);
          grid.add(nameField, 1, 0);
          grid.add(timeLabel, 0, 1);
          grid.add(timeField, 1, 1);

          alert.getDialogPane().setContent(grid);
          alert.showAndWait();
          clearSelectedPlaces();

        } else {
          writeErrorAlert("There exists no connection between " + cityName1 + " and " + cityName2 + "!");
          clearSelectedPlaces();
        }

      } else {
        // Felmeddelande för att två platser inte har valts behövs inte?
        // eftersom det finns i twoPlacesSelected() ??
      }
    }
  }
  
  class FindPathHandler implements EventHandler<ActionEvent>{
    public void handle(ActionEvent event){
      List<Edge<City>> path = new LinkedList<>();
      int edgeWeightTotal = 0;
      
      if(twoPlacesSelected()){
        if(graph.getPath(markedCity1, markedCity2) != null){
          path = graph.getPath(markedCity1, markedCity2);

          BorderPane borderPane = new BorderPane();
          TextArea textArea = new TextArea("");
          borderPane.setCenter(textArea);

          for(Edge<City> edge : path){
            // String textLine = "to " + edge.getDestination().getCityName() + " by " + edge.getName() + " takes " + String.valueOf(edgeWeight) + "\n";
            edgeWeightTotal += edge.getWeight();
            textArea.appendText(edge.toString()+"\n");
          }

          textArea.appendText("Total time: " + String.valueOf(edgeWeightTotal));
          textArea.setEditable(false);

          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle("Message");
          alert.setHeaderText("The Path from " + markedCity1.getCityName() + " to " + markedCity2.getCityName());
          alert.getDialogPane().setContent(borderPane);
          alert.showAndWait();
          
          clearSelectedPlaces();

        }else{
          writeErrorAlert("There is no possible path from " + markedCity1.getCityName() + " to " + markedCity2.getCityName());
          clearSelectedPlaces();
        }

      }else{
        //Felmeddelande för att två platser inte är valda finns i twoPlacesSelected()
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
          Edge<City> existingEdge = graph.getEdgeBetween(markedCity2, markedCity1);

          if (existingEdge == null) {
            // NewConnectionForm dialog = new NewConnectionForm();
            Optional<Edge<City>> result = showConnectionForm(null, 0, true, true); // dialog.showAndWait();

            if (result.isPresent()) {
              String connectionName = result.get().getName();
              int connectionTime = result.get().getWeight();
              graph.connect(markedCity1, markedCity2, connectionName, connectionTime);
              // Potentiell hjälpmetod drawConnection() som ritar ut en linje på kartan
              // TODO: Kolla upp varför circle.getCenterX/Y() inte ger rätt koordinater
              // Line line = new Line(markedCityCircle1X, markedCityCircle1Y,
              // markedCityCircle2X, markedCityCircle2Y);
              Line line = new Line(markedCity1.getX(), markedCity1.getY(), markedCity2.getX(), markedCity2.getY());
              mapPane.getChildren().add(1, line); // ritar linje först och under andra noder på positionen

              edited = true;
            }
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
