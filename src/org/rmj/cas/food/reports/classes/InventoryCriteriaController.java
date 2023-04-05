package org.rmj.cas.food.reports.classes;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.CommonUtils;


public class InventoryCriteriaController implements Initializable {

    @FXML private AnchorPane dataPane;
    @FXML private StackPane stack;
    @FXML private Pane pnePresentation;
    @FXML private RadioButton radioBtn01;
    @FXML private RadioButton radioBtn02;
    @FXML private Pane pneGroup;
    @FXML private RadioButton radioBtn03;
    @FXML private Button btnOk;
    @FXML private Button btnCancel;
    @FXML private RadioButton radioBtn04;
    @FXML private RadioButton radioBtn05;
    @FXML private Button btnExit;
    @FXML private FontAwesomeIconView glyphExit;
    @FXML private Pane pnePresentation1;
    @FXML private TextField txtField00;
    
    private boolean pbCancelled = true;
    private String psPresentation = "";
    private String psGroupBy = "";
    
    private boolean pbDetailedOnly;
    
    ToggleGroup tgPresentation;
    ToggleGroup tgGroupBy;    
    
    public boolean isCancelled(){return pbCancelled;}
    public String Presentation(){return psPresentation;}
    public String GroupBy(){return psGroupBy;}
    
    public void isDetailedOnly(boolean fbValue){pbDetailedOnly = fbValue;}

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnExit.setOnAction(this::cmdButton_Click);
        btnOk.setOnAction(this::cmdButton_Click);
        btnCancel.setOnAction(this::cmdButton_Click);
        
        radioBtn01.setOnAction(this::radioButton_Click);
        radioBtn02.setOnAction(this::radioButton_Click);
        radioBtn03.setOnAction(this::radioButton_Click);
        radioBtn04.setOnAction(this::radioButton_Click);
        radioBtn05.setOnAction(this::radioButton_Click);
        
        txtField00.focusedProperty().addListener(txtField_Focus);
        
        tgPresentation = new ToggleGroup();
        tgGroupBy = new ToggleGroup();
        
        tgPresentation.getToggles().addAll(radioBtn01, radioBtn02);
        tgGroupBy.getToggles().addAll(radioBtn03, radioBtn04, radioBtn05);
        
        initButton();
        pbLoaded = true;
        
    }
    
    private Stage getStage(){
        return (Stage) btnOk.getScene().getWindow();
    }
    
    private void initButton(){
        radioBtn01.setDisable(pbDetailedOnly);
        
        txtField00.setText(CommonUtils.xsDateMedium((Date) java.sql.Date.valueOf(LocalDate.now())));
        radioBtn02.setSelected(true);
        radioBtn03.setSelected(true);
        psPresentation = "1";
        psGroupBy = "";
    }
    
    private void radioButton_Click(ActionEvent event){
        String lsRadio = ((RadioButton) event.getSource()).getId();
        switch (lsRadio){
            case "radioBtn01":
                pneGroup.setDisable(true);
                radioBtn03.setSelected(true);
                psPresentation = "0";
                psGroupBy = "";
                break;
            case "radioBtn02":
                pneGroup.setDisable(false);
                radioBtn03.setSelected(true);
                psPresentation = "1";
                break;
            case "radioBtn03":
                psGroupBy = ""; break;
            case "radioBtn04":
                psGroupBy = "sBinNamex"; break;
            case "radioBtn05":
                psGroupBy = "sInvTypCd";
        }
        
    }
    
    private void cmdButton_Click (ActionEvent event){
        String lsButton = ((Button)event.getSource()).getId();
        switch(lsButton){
            case "btnCancel":
                pbCancelled = true; break;
            case "btnOk":
                pbCancelled = false; break;
            case "btnExit":
                pbCancelled = true; break;
            default:
                ShowMessageFX.Warning(null, InventoryCriteriaController.class.getSimpleName(), "Button with name "+ lsButton +" not registered!");
        }
        CommonUtils.closeStage(btnExit);
    }
    
    private static GRider poGRider;
    private boolean pbLoaded = false;
    private String pxeModuleName = "Inventory Report Criteria";
    private final String pxeDateFormat = "yyyy-MM-dd";
    private String psDateFrom = null;
    
    final ChangeListener<? super Boolean> txtField_Focus = (o,ov,nv)->{
        if (!pbLoaded) return;
        
        TextField txtField = (TextField)((ReadOnlyBooleanPropertyBase)o).getBean();
        int lnIndex = Integer.parseInt(txtField.getId().substring(8, 10));
        String lsValue = txtField.getText();
        
        if (lsValue == null) return;
        
        if(!nv){ /*Lost Focus*/     
            switch (lnIndex){
                case 0: /*dReceived*/
                         if (CommonUtils.isDate(lsValue, pxeDateFormat)){
                             psDateFrom = lsValue;
                             txtField.setText(CommonUtils.xsDateMedium(CommonUtils.toDate(psDateFrom)));
                         }else{
                             ShowMessageFX.Warning("Invalid date entry.", pxeModuleName, "Date format must be yyyy-MM-dd (e.g. 1991-07-07)");
                             psDateFrom = java.sql.Date.valueOf(LocalDate.now()).toString();
                             txtField.setText(CommonUtils.xsDateMedium(CommonUtils.toDate(psDateFrom)));
                         }
                         return;
                 }
             } else{
                 switch (lnIndex){
                     case 0: /*dReceived*/
                         try{
                             txtField.setText(CommonUtils.xsDateShort(lsValue));
                         }catch(ParseException e){
                             ShowMessageFX.Error(e.getMessage(), pxeModuleName, null);
                         }
                         txtField.selectAll();
                         break;
                     default:
                 }
                 txtField.selectAll();
             }
    };
    
    public String getDateTransNox(){
        return psDateFrom;
    }
    
    private void setDateTransNox(String fdTransNox){
        this.psDateFrom = fdTransNox;
    }
    
}
