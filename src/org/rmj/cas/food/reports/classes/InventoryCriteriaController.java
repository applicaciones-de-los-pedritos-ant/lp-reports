package org.rmj.cas.food.reports.classes;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.net.URL;
import java.text.ParseException;
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
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.UP;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ui.showFXDialog;


public class InventoryCriteriaController implements Initializable {
    @FXML private AnchorPane dataPane;
    @FXML private StackPane stack;
    @FXML private Pane pnePresentation;
    @FXML private RadioButton radioBtn01;
    @FXML private RadioButton radioBtn02;
    @FXML private Button btnOk;
    @FXML private Button btnCancel;
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
    public String InvType(){return psInvTypCd;}
    
    public void isDetailedOnly(boolean fbValue){pbDetailedOnly = fbValue;}
    public void setGRider(GRider foValue){poGRider = foValue;}

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        btnExit.setOnAction(this::cmdButton_Click);
        btnOk.setOnAction(this::cmdButton_Click);
        btnCancel.setOnAction(this::cmdButton_Click);
        
        txtField00.setOnKeyPressed(this::txtField_KeyPressed);
        txtField00.focusedProperty().addListener(txtField_Focus);
        
        tgPresentation = new ToggleGroup();
        tgGroupBy = new ToggleGroup();
        
        tgPresentation.getToggles().addAll(radioBtn01, radioBtn02);
        
        initButton();
        pbLoaded = true;
    }
    
    private Stage getStage(){
        return (Stage) btnOk.getScene().getWindow();
    }
    
    private void initButton(){
        radioBtn01.setDisable(pbDetailedOnly);
        
        txtField00.setText("");
        radioBtn02.setSelected(true);
        
        psPresentation = "1";
    }
    
    private void radioButton_Click(ActionEvent event){
        String lsRadio = ((RadioButton) event.getSource()).getId();
        switch (lsRadio){
            case "radioBtn01":
                psPresentation = "0";
                break;
            case "radioBtn02":
                psPresentation = "1";
                break;
        }
        
    }
    
    private void txtField_KeyPressed(KeyEvent event){
        TextField txtField = (TextField)event.getSource();
        int lnIndex = Integer.parseInt(txtField.getId().substring(8, 10));
        String lsValue = txtField.getText();
        JSONObject loJSON = null;
        
        if (lnIndex == 0){
            if (event.getCode() == KeyCode.F3){
                loJSON = searchType(lsValue);
                
                if (loJSON != null){
                    psInvTypCd = (String) loJSON.get("sInvTypCd");
                    txtField00.setText((String) loJSON.get("sDescript"));
                } else{
                    psInvTypCd = "";
                    txtField00.setText("");
                }                    
            }
        }
        
        switch(event.getCode()){
            case DOWN:
            case ENTER:
                CommonUtils.SetNextFocus(txtField);
                break;
            case UP:
                CommonUtils.SetPreviousFocus(txtField);
        }
    }
    
    private JSONObject searchType(String fsValue){
        String lsSQL = "SELECT sInvTypCd, sDescript" +
                        " FROM Inv_Type" +
                        " WHERE cRecdStat = '1'";
        
        return showFXDialog.jsonSearch(poGRider, lsSQL, fsValue, "ID»Type", "sInvTypCd»sDescript", "sInvTypCd»sDescript", 1);
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
    private String psInvTypCd = "";
    private int pnIndex = -1;

    final ChangeListener<? super Boolean> txtField_Focus = (o,ov,nv)->{
        if (!pbLoaded) return;
        
        TextField txtField = (TextField)((ReadOnlyBooleanPropertyBase)o).getBean();
        int lnIndex = Integer.parseInt(txtField.getId().substring(8, 10));
        String lsValue = txtField.getText();
        
        if (lsValue == null) return;
        
        if(!nv){ /*Lost Focus*/
            pnIndex = lnIndex;
        }else{

            pnIndex = lnIndex;
            txtField.selectAll();
        }
    };
}
