/**
 * Food Reports Main Class
 * @author Michael T. Cuison
 * @started 2018.11.24
 */

package org.rmj.cas.food.reports.classes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rmj.appdriver.GLogger;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.constants.UserRight;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class DailyProduction implements GReport{
    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private JasperViewer jrViewer = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.Inventory", "InventoryReport.log");
    
    private double xOffset = 0; 
    private double yOffset = 0;
    
    public DailyProduction(){
        _rptparam = new LinkedList();
        _rptparam.add("store.report.id");
        _rptparam.add("store.report.no");
        _rptparam.add("store.report.name");
        _rptparam.add("store.report.jar");
        _rptparam.add("store.report.class");
        _rptparam.add("store.report.is_save");
        _rptparam.add("store.report.is_log");
        
        _rptparam.add("store.report.criteria.presentation");
        _rptparam.add("store.report.criteria.branch");      
        _rptparam.add("store.report.criteria.group");        
        _rptparam.add("store.report.criteria.datefrom");
        _rptparam.add("store.report.criteria.datethru");
    }
    
    @Override
    public void setGRider(Object foApp) {
        _instance = (GRider) foApp;
    }
    
    @Override
    public void hasPreview(boolean show) {
        _preview = show;
    }
    
    @Override
    public boolean getParam() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DateCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("DateCriteria.fxml"));

        DateCriteriaController instance = new DateCriteriaController();
        instance.singleDayOnly(false);
        
        try {
            
            fxmlLoader.setController(instance);
            Parent parent = fxmlLoader.load();
            Stage stage = new Stage();

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
            /*END SET FORM MOVABLE*/

            Scene scene = new Scene(parent);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setAlwaysOnTop(true);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            ShowMessageFX.Error(e.getMessage(), DailyProduction.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }
        
        if (!instance.isCancelled()){
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.presentation", String.valueOf(instance.getIndex()));
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.branch", "");
            System.setProperty("store.report.criteria.group", "");
            return true;
        }
        return false;
    }
    
    @Override
    public boolean processReport() {
        boolean bResult = false;
        
        //Get the criteria as extracted from getParam()
        if (System.getProperty("store.report.criteria.presentation").equals("0")){
            System.setProperty("store.report.no", "1");
        } else{
            System.setProperty("store.report.no", "2");
        }
        
        //Load the jasper report to be use by this object
        String lsSQL = "SELECT sFileName, sReportHd" + 
                      " FROM xxxReportDetail" + 
                      " WHERE sReportID = " + SQLUtil.toSQL(System.getProperty("store.report.id")) +
                        " AND nEntryNox = " + SQLUtil.toSQL(System.getProperty("store.report.no"));
        
        //Check if in debug mode...
        if(System.getProperty("store.default.debug").equalsIgnoreCase("true")){
            System.out.println(System.getProperty("store.report.class") + ".processReport: " + lsSQL);
        }
        
        ResultSet loRS = _instance.executeQuery(lsSQL);
        
        try {
            if(!loRS.next()){
                _message = "Invalid report was detected...";
                closeReport();
                return false;
            }
            System.setProperty("store.report.file", loRS.getString("sFileName"));
            System.setProperty("store.report.header", loRS.getString("sReportHd"));
            
            switch(Integer.valueOf(System.getProperty("store.report.no"))){
                case 1:
                    bResult = printSummary();
                    break;
                case 2: 
                    bResult = printDetail();
            }
            
            if(!bResult){
                closeReport();
                return false;
            }
            if(System.getProperty("store.report.is_log").equalsIgnoreCase("true")){
                logReport();
            }
            JasperViewer jv = new JasperViewer(_jrprint, false);     
            jv.setVisible(true);  
            jv.setAlwaysOnTop(bResult);
            
        } catch (SQLException ex) {
            _message = ex.getMessage();
            //Check if in debug mode...
            if(System.getProperty("store.default.debug").equalsIgnoreCase("true")){
                ex.printStackTrace();
            }            
            GLogger.severe(System.getProperty("store.report.class"), "processReport", ExceptionUtils.getStackTrace(ex));
            
            closeReport();
            return false;
        }
        
        closeReport();
        return true;
    }

    @Override
    public void list() {
        _rptparam.forEach(item->System.out.println(item));
    }
    
    private boolean printSummary() throws SQLException {
        String lsSQL = getReportSQLMaster();
        String lsCondition = "";
        String lsDate = "";
        
        if (!System.getProperty("store.report.criteria.datefrom").equals("") &&
                !System.getProperty("store.report.criteria.datethru").equals("")){

            lsDate = System.getProperty("store.report.criteria.datefrom") + " to " + System.getProperty("store.report.criteria.datethru");
            
            lsCondition = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND " +
                            SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));
            
            
            lsCondition = "a.dTransact BETWEEN " + lsCondition;
        } else lsCondition = "0 = 1";
        
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");
        
        lsSQL = "SELECT sClientNm FROM Client_Master" +
                " WHERE sClientID IN (" +
                    "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(_instance.getUserID()) + ")";
        
        ResultSet loRS = _instance.executeQuery(lsSQL);
        
        if (loRS.next()){
            params.put("sPrintdBy", loRS.getString("sClientNm"));
        } else {
            params.put("sPrintdBy", "");
        }

        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"),
                                                    params, 
                                                    jrRS);
        } catch (JRException ex) {
            Logger.getLogger(DailyProduction.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printDetail() throws SQLException{
        String lsCondition = "";
        String lsSQL = "";
        String lsDate = "";
        
        if (!System.getProperty("store.report.criteria.datefrom").equals("") &&
                !System.getProperty("store.report.criteria.datethru").equals("")){

            lsDate = System.getProperty("store.report.criteria.datefrom") + " to " + System.getProperty("store.report.criteria.datethru");
            
            lsCondition = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND " +
                            SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));
            
            
            lsCondition = "a.dTransact BETWEEN " + lsCondition;
        } else lsCondition = "0 = 1";

        
        lsSQL = MiscUtil.addCondition(getReportSQL(), lsCondition);
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        while (!rs.next()) {
            _message = "No record found...";
            return false;
        }
        
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", !lsDate.equals("") ? lsDate : "");
        
        lsSQL = "SELECT sClientNm FROM Client_Master" +
                " WHERE sClientID IN (" +
                    "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(_instance.getUserID()) + ")";
        
        ResultSet loRS = _instance.executeQuery(lsSQL);
        
        if (loRS.next()){
            params.put("sPrintdBy", loRS.getString("sClientNm"));
        } else {
            params.put("sPrintdBy", "");
        }
        
        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"),
                                                    params, 
                                                    jrRS);
            
        } catch (JRException ex) {
            Logger.getLogger(DailyProduction.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
    private void closeReport(){
        _rptparam.forEach(item->System.clearProperty((String) item));
        System.clearProperty("store.report.file");
        System.clearProperty("store.report.header");
    }
    
    private void logReport(){
        _rptparam.forEach(item->System.clearProperty((String) item));
        System.clearProperty("store.report.file");
        System.clearProperty("store.report.header");
    }
    
    private String getReportSQL(){
        String lsSQL = "SELECT" +
                            "  DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField01`" +
                            ", c.sBarCodex `sField02`" +
                            ", c.sDescript `sField03`" +
                            ", IFNULL(d.`sMeasurNm`, '') `sField04`" +
                            ", b.nQuantity `nField01`" + 
                            ", c.nUnitPrce `lField01`" +
                            ", a.sTransNox `sField05`" +
                        " FROM Daily_Production_Master a" +
                            ", Daily_Production_Detail b" +
                            " LEFT JOIN Inventory c" +
                                " ON b.sStockIDx = c.sStockIDx" +
                            " LEFT JOIN Measure d" +
                                " ON c.sMeasurID = d.sMeasurID" + 
                        " WHERE a.sTransNox = b.sTransNox" +
                            " AND LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode());
        
        if (_instance.getUserLevel() < UserRight.ENGINEER){
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }        
        
        return lsSQL;
    }
    private String getReportSQLMaster(){
        String lsSQL = "SELECT" +
                            "  DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField01`" +
                            ", c.sBarCodex `sField02`" +
                            ", c.sDescript `sField03`" +
                            ", IFNULL(d.`sMeasurNm`, '') `sField04`" +
                            ", SUM(b.nQuantity) `nField01`" + 
                            ", c.nUnitPrce `lField01`" +
                        " FROM Daily_Production_Master a" +
                            ", Daily_Production_Detail b" +
                            " LEFT JOIN Inventory c" +
                                " ON b.sStockIDx = c.sStockIDx" +
                            " LEFT JOIN Measure d" +
                                " ON c.sMeasurID = d.sMeasurID" + 
                        " WHERE a.sTransNox = b.sTransNox" +
                        " GROUP BY c.sBarCodex";
        
        if (_instance.getUserLevel() < UserRight.ENGINEER){
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }
        
        return lsSQL;
    }
}
