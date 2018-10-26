package biometrics;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.IOException;


/**
 * Created by boska - 06-04-17
 */
public class FiltersController {

    //    public TextField transParameter;
    @FXML
    public GridPane maskGrid;
    @FXML
    private Button okButton;

    private int[][] mask;
    private int size;
    private int param;
    private ObservableList<Node> children;
    private Controller controller;


    protected Stage window;
    public String windowLabel;


    public void display(Controller controller, int[][] mask, int size) throws IOException {
        this.controller = controller;
        this.mask = mask;
        this.size = size;


        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        Parent root = FXMLLoader.load(getClass().getResource("filtersMode.fxml"));
        window.setScene(new Scene(root, 300, 300));
        GridPane gridPane= (GridPane) root.lookup("#maskGrid");
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                TextField textField = new TextField();
                textField.setMaxWidth(30);
                gridPane.add(textField, j, i);
            }
        }
        children = ((GridPane) root.getChildrenUnmodifiable().get(0)).getChildren();

        window.showAndWait();

    }

    public void displayTransformationWindow(Controller controller, int operation) throws IOException {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        Parent root = FXMLLoader.load(getClass().getResource("transformationsMode.fxml"));
        window.setScene(new Scene(root, 250, 150));
        children = ((HBox) root.getChildrenUnmodifiable().get(0)).getChildren();
        Label windowLabel = (Label) root.lookup("#label");


        this.controller = controller;
        if (operation == 0) {
            windowLabel.setText("Adding transformation");
        } else if (operation == 1) {
            windowLabel.setText("Subtracting transformation");
        } else if (operation == 2) {
            windowLabel.setText("Multiplying transformation");
        } else if (operation == 3) {
            windowLabel.setText("Dividing transformation");
        } else {
            Label paramLabel = (Label) root.lookup("#paramLabel");
            paramLabel.setText("Mask size: ");
            windowLabel.setText("Custom Mask");
//
        }
        window.showAndWait();


    }

    public void displayBinarizationWindow(Controller controller, int version) throws IOException {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        Parent root = FXMLLoader.load(getClass().getResource("transformationsMode.fxml"));
        window.setScene(new Scene(root, 250, 150));
        children = ((HBox) root.getChildrenUnmodifiable().get(0)).getChildren();
        Label windowLabel = (Label) root.lookup("#label");


        this.controller = controller;
        if (version == 0) {
            windowLabel.setText("Percent Black Selection");
            Label paramLabel = (Label) root.lookup("#paramLabel");
            paramLabel.setText("Percent black pixels: ");
        } else {
            windowLabel.setText("Custom Mask");
        }
        window.showAndWait();


    }


    public int[][] getMask() {
        mask = new int[this.size][this.size];


        for (Node node : children) {
            System.out.println("elo");
            int i = GridPane.getColumnIndex(node);

            int j = GridPane.getRowIndex(node);


            TextField t1 = (TextField) node;

            mask[j][i] = Integer.valueOf(t1.getText());
            System.out.println("mask: " + i + " " + j + " = " + mask[j][i]);
        }

        return mask;
    }

    public int getParameter() {
        TextField t1 = (TextField) children.get(1);

        return Integer.valueOf(t1.getText());
    }

    public void handleOkButton() {
        okButton.getScene().getWindow().hide();
    }

    public void handleOkButtonParam() {
        okButton.getScene().getWindow().hide();

    }


}