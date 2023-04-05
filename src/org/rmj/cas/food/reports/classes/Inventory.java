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
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GLogger;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class Inventory implements GReport{
    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.Inventory", "InventoryReport.log");
    
    private double xOffset = 0; 
    private double yOffset = 0;
    
    public Inventory(){
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
        _rptparam.add("store.report.criteria.date");     
        _rptparam.add("store.report.date.criteria");
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InventoryCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("InventoryCriteria.fxml"));

        InventoryCriteriaController instance = new InventoryCriteriaController();
        instance.isDetailedOnly(true);
        
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
            System.setProperty("store.report.criteria.presentation", instance.Presentation());
            System.setProperty("store.report.criteria.group", instance.GroupBy());
            System.setProperty("store.report.criteria.branch", "");
            System.setProperty("store.report.criteria.date", "");
            System.setProperty("store.report.date.criteria", CommonUtils.xsDateShort(CommonUtils.toDate(instance.getDateTransNox())));
            return true;
        }
        return false;
    }
    
    @Override
    public boolean processReport() {
        boolean bResult = false;
        
        //Get the criteria as extracted from getParam()
        if(System.getProperty("store.report.criteria.presentation").equals("0")){
            System.setProperty("store.report.no", "1");
        }else if(System.getProperty("store.report.criteria.group").equalsIgnoreCase("sBinNamex")) {
            System.setProperty("store.report.no", "3");
        }else if(System.getProperty("store.report.criteria.group").equalsIgnoreCase("sInvTypCd")) {
            System.setProperty("store.report.no", "4");
        }else{
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
                    break;
                case 3:
                    bResult = printInvGrpByBin();
                    break;
                case 4:
                    bResult = printInvGrpByType();
            }
            
            if(bResult){
                if(System.getProperty("store.report.is_log").equalsIgnoreCase("true")){
                    logReport();
                }
                JasperViewer jv = new JasperViewer(_jrprint, false);     
                jv.setVisible(true);                
            }
            
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
    
    private boolean printSummary(){
        System.out.println("Printing Summary");
        return true;
    }
    
    private boolean printDetail(){
        
        ResultSet rs = _instance.executeQuery(getReportSQLDate()+ " ,sField01, sField02");
        //Convert the data-source to JasperReport data-source
        
        System.out.println(getReportSQLDate() + " ,sField01, sField02");
        JSONArray json_mcsales = new JSONArray();
        json_mcsales.clear();
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        //Create the parameter
        
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", "As of " + CommonUtils.xsDateMedium(CommonUtils.toDate(System.getProperty("store.report.date.criteria"))));
        params.put("sPrintdBy", _instance.getUserID());
        
        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"),
                                                    params, 
                                                    jrRS);
        } catch (JRException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    //group by inventory type
    private boolean printInvGrpByType(){
        ResultSet rs = _instance.executeQuery(getReportSQLDate()+ " ,sField04, sField01, sField02");
        
        JSONArray json_mcsales = new JSONArray();
        json_mcsales.clear();
        try {
            while (rs.next()) {
                JSONObject json_mcsale = new JSONObject(); 
                json_mcsale.put("sField01", rs.getString("sField01"));
                json_mcsale.put("sField02", rs.getString("sField02"));
                json_mcsale.put("sField03", rs.getString("sField03"));
                json_mcsale.put("sField05", rs.getString("sField05"));
                json_mcsale.put("sField06", rs.getString("sField06"));
                json_mcsale.put("sField07", rs.getString("sField07"));
                if(rs.getString("nField02")==null){
                    json_mcsale.put("nField01", rs.getInt("nField01"));
                }else{
                    json_mcsale.put("nField01", rs.getInt("nField02"));
                }
//                json_mcsale.put("nField01", rs.getInt("nField01"));
                json_mcsale.put("lField01", rs.getDouble("lField01"));
                json_mcsale.put("sField04", rs.getString("sField04"));
                json_mcsales.add(json_mcsale);
            }              
        } catch (SQLException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JsonDataSource jrjson;
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", "As of " + CommonUtils.xsDateMedium(CommonUtils.toDate(System.getProperty("store.report.date.criteria"))));
        params.put("sPrintdBy", _instance.getUserID());
        
        try {
            InputStream stream = new ByteArrayInputStream(json_mcsales.toJSONString().getBytes("UTF-8"));
            jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    //group by bin location
    private boolean printInvGrpByBin(){
        ResultSet rs = _instance.executeQuery(getReportSQLDate()+ " ,sField03, sField01, sField02" );
        
        JSONArray json_mcsales = new JSONArray();
        json_mcsales.clear();
        try {
            while (rs.next()) {
                JSONObject json_mcsale = new JSONObject(); 
                json_mcsale.put("sField01", rs.getString("sField01"));
                json_mcsale.put("sField02", rs.getString("sField02"));
                json_mcsale.put("sField03", rs.getString("sField03"));
                json_mcsale.put("sField05", rs.getString("sField05"));
                json_mcsale.put("sField06", rs.getString("sField06"));
                json_mcsale.put("sField07", rs.getString("sField07"));
                if(rs.getString("nField02")==null){
                    json_mcsale.put("nField01", rs.getInt("nField01"));
                }else{
                    json_mcsale.put("nField01", rs.getInt("nField02"));
                }
                json_mcsale.put("lField01", rs.getDouble("lField01"));
                json_mcsales.add(json_mcsale);
            }              
        } catch (SQLException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JsonDataSource jrjson;
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", "As of " + CommonUtils.xsDateMedium(CommonUtils.toDate(System.getProperty("store.report.date.criteria"))));
        params.put("sPrintdBy", _instance.getUserID());
        
        try {
            InputStream stream = new ByteArrayInputStream(json_mcsales.toJSONString().getBytes("UTF-8"));
            jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
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
        return "SELECT" +
                    "  b.sBarCodex `sField01`" +
                    ", b.sDescript `sField02`" +
                    ", a.nBinNumbr `sField03`" +
                    ", a.nQtyOnHnd `nField01`" +
                    ", b.nSelPrice `lField01`" +
                    ", IFNULL(c.sDescript, '') `sField04`" +
                    ", IFNULL(d.sDescript, '') `sField05`" +
                    ", IFNULL(e.sDescript, '') `sField06`" +
                    ", IFNULL(f.sMeasurNm, '') `sField07`" +
                " FROM Inv_Master a" +
                    ", Inventory b" +
                        " LEFT JOIN Inv_Type c" +
                            " ON b.sInvTypCd = c.sInvTypCd" +
                        " LEFT JOIN Brand d" +
                            " ON b.sBrandCde = d.sBrandCde" +
                        " LEFT JOIN Model e" +
                            " ON b.sModelCde = e.sModelCde" +
                        " LEFT JOIN Measure f" +
                            " ON b.sMeasurID = f.sMeasurID" +
                " WHERE a.sStockIDx = b.sStockIDx" +
                    " AND a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode()) +
                    " AND IFNULL(a.nQtyOnHnd, 0) > 0";
        
        //" AND IFNULL(a.nQtyOnHnd, 0) > 0" +
    }
    
    private String getReportSQLDate(){
        String lsDate = System.getProperty("store.report.date.criteria");
        
        return "SELECT * FROM" +
                    " (SELECT" +
                        "  b.sBarCodex `sField01`" +
                        ", b.sDescript `sField02`" +
                        ", a.nBinNumbr `sField03`" +
                        ", a.nQtyOnHnd `nField01`" +
                        ", b.nSelPrice `lField01`" +
                        ", IFNULL(c.sDescript, '') `sField04`" +
                        ", IFNULL(d.sDescript, '') `sField05`" +
                        ", IFNULL(e.sDescript, '') `sField06`" +
                        ", IFNULL(f.sMeasurNm, '') `sField07`" +
                        ", g.nQtyOnHnd `nField02`" +
                        ", a.sStockIDx" +
                    " FROM Inv_Master a" +
                        " LEFT JOIN Inv_Ledger g" +
                            " ON a.sStockIDx = g.sStockIDx" +
                            " AND a.sBranchCd = g.sBranchCd" +
                            " AND g.dTransact <= " + SQLUtil.toSQL(lsDate) +
                        ", Inventory b" +
                            " LEFT JOIN Inv_Type c" +
                                " ON b.sInvTypCd = c.sInvTypCd" +
                            " LEFT JOIN Brand d" +
                                " ON b.sBrandCde = d.sBrandCde" +
                            " LEFT JOIN Model e" +
                                " ON b.sModelCde = e.sModelCde" +
                            " LEFT JOIN Measure f" +
                                " ON b.sMeasurID = f.sMeasurID" +
                    " WHERE a.sStockIDx = b.sStockIDx" +
                        " AND a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode()) +
                        " AND IFNULL(a.nQtyOnHnd, 0) > 0" +
                        " AND a.dBegInvxx <= " + SQLUtil.toSQL(lsDate) +
                    " ORDER BY" +
                        " g.nLedgerNo DESC) xSourceTable" +
                " GROUP BY" +
                    "  sStockIDx" +
                " ORDER BY" +
                    "  sField04";
    }
}
