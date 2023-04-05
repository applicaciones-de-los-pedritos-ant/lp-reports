/**
 * Food Reports Runnable Class
 * @author Michael T. Cuison
 * @started 2018.11.24
 */

package org.rmj.cas.food.reports.classes;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.rmj.appdriver.GRider;

public class FoodReports extends Application {
    private static GRider oApp;
    
    private double xOffset = 0; 
    private double yOffset = 0;
    
    public void setGRider(GRider foApp){
        oApp = foApp;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("BrowseReport.fxml"));
        
        BrowseReportController instance = new BrowseReportController();
        instance.setGRider(oApp);
        
        fxmlLoader.setController(instance);
        Parent parent = fxmlLoader.load();
        
        /*SET FORM MOVABLE*/
        parent.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
        /*END - SET FORM MOVABLE*/
        
        Scene scene = new Scene(parent);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Standard Reports");
        stage.getIcons().add(new Image("org/rmj/cas/food/reports/resources/ic_launcher1.png"));
        stage.showAndWait();
        
        if (!instance.isCancelled()){
            ProcReport loReport = new ProcReport();
            loReport.setGRider(oApp);
            loReport.setReportID(instance.getReportID());
            loReport.showReport();
            loReport = null;
        }
        
        instance = null;
    }

    public static void main(String[] args) {
        launch(args);
    }
    
}
