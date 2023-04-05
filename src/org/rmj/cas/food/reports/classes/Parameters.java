/**
 * Parameters Reports Main Class
 * @author Michael T. Cuison
 * @started 2019.05.27
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
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.sf.jasperreports.engine.JRException;
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
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class Parameters implements GReport{
    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.Parameters", "ParameterReport.log");
    
    private double xOffset = 0; 
    private double yOffset = 0;
    
    public Parameters(){
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Parameters.fxml"));
        fxmlLoader.setLocation(getClass().getResource("Parameters.fxml"));

        ParametersReportController instance = new ParametersReportController();
        instance.setGRider(_instance);
        instance.setReportID("Params");
        
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
            System.setProperty("store.report.no", String.valueOf(instance.getEntryNox()));
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.presentation", "");
            System.setProperty("store.report.criteria.group", "");
            System.setProperty("store.report.criteria.branch", "");
            System.setProperty("store.report.criteria.date", "");
            return true;
        }
        return false;
    }
    
    @Override
    public boolean processReport() {
        boolean bResult = false;
        
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
                    bResult = printBrand();
                    break;
                case 2:
                    bResult = printCategory();
                    break;
                case 3:
                    bResult = printCategory2();
                    break;
                case 4:
                    bResult = printCategory3();
                    break;    
                case 5:
                    bResult = printCategory4();
                    break;
                case 6:
                    bResult = printColor();
                    break;
                case 7:
                    bResult = printCompany();
                    break;
                case 8:
                    bResult = printInvLocations();
                    break;
                case 9:
                    bResult = printInvType();
                    break;
                case 10:
                    bResult = printMeasurement();
                    break;
                case 11:
                    bResult = printModel();
                    break;
                case 12:
                    bResult = printSupplier();
                    break;
                case 13:
                    bResult = printTerm();
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
    
    private boolean printBrand(){
        String lsSQL = "SELECT" +
                            "  a.sDescript sField01" +
                            ", IFNULL(b.sDescript, '') sField02" +
                        " FROM Brand a" +
                            " LEFT JOIN Inv_Type b" +
                                " ON a.sInvTypCd = b.sInvTypCd" +
                        " WHERE a.cRecdStat = '1'" +
                        " ORDER BY sField01, sField02";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printInvType(){
        String lsSQL = "SELECT" +
                            "  sInvTypCd sField01" +
                            ", sDescript sField02" +
                        " FROM Inv_Type" +
                        " WHERE cRecdStat = '1'" +
                        " ORDER BY sField01, sField02";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printMeasurement(){
        String lsSQL = "SELECT" +
                            "  sMeasurNm sField01" +
                        " FROM Measure" +
                        " WHERE cRecdStat = '1'" +
                        " ORDER BY sField01";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printInvLocations(){
        String lsSQL = "SELECT" +
                            "  sBriefDsc sField01" +
                            ", sDescript sField02" +
                        " FROM Inv_Location" +
                        " WHERE cRecdStat = '1'" +
                        " ORDER BY sField01, sField02";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printCategory(){
        String lsSQL = "SELECT" +
                            "  a.sDescript sField01" +
                            ", IFNULL(b.sDescript, '') sField02" +
                        " FROM Category a" +
                            " LEFT JOIN Inv_Type b" +
                                " ON a.sInvTypCd = b.sInvTypCd" +
                        " WHERE a.cRecdStat = '1'" +
                        " ORDER BY sField01, sField02";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printCategory2(){
        String lsSQL = "SELECT" +
                            "  a.sDescript sField01" +
                            ", IFNULL(b.sDescript, '') sField02" +
                            ", IFNULL(c.sDescript, '') sField03" +
                        " FROM Category_Level2 a" +
                            " LEFT JOIN Inv_Type b" +
                                " ON a.sInvTypCd = b.sInvTypCd" +
                            " LEFT JOIN Category c" +
                                " ON a.sMainCatx = c.sCategrCd" +
                        " WHERE a.cRecdStat = '1'" +
                        " ORDER BY sField01, sField02, sField03";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_obj.put("sField03", rs.getString("sField03"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printCategory3(){
        String lsSQL = "SELECT" +
                            "  a.sDescript sField01" +
                            ", IFNULL(b.sDescript, '') sField02" +
                        " FROM Category_Level3 a" +
                            " LEFT JOIN Category b" +
                                " ON a.sMainCatx = b.sCategrCd" +
                        " WHERE a.cRecdStat = '1'" +
                        " ORDER BY sField01, sField02";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printCategory4(){
        String lsSQL = "SELECT" +
                            "  a.sDescript sField01" +
                            ", IFNULL(b.sDescript, '') sField02" +
                            ", IFNULL(c.sDescript, '') sField03" +
                        " FROM Category_Level4 a" +
                            " LEFT JOIN Category b" +
                                " ON a.sMainCatx = b.sCategrCd" +
                            " LEFT JOIN Inv_Type c" +
                                " ON b.sInvTypCd = c.sInvTypCd" + 
                        " WHERE a.cRecdStat = '1'" +
                        " ORDER BY sField03, sField01, sField02";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_obj.put("sField03", rs.getString("sField03"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printColor(){
        String lsSQL = "SELECT" +
                            "  sDescript sField01" +
                        " FROM Color" +
                        " WHERE cRecdStat = '1'" +
                        " ORDER BY sField01";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printCompany(){
        String lsSQL = "SELECT" +
                            "  sCompnyCd sField01" +
                            ", sCompnyNm sField02" +
                        " FROM Company" +
                        " WHERE cRecdStat = '1'" +
                        " ORDER BY sField01";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printModel(){
        String lsSQL = "SELECT" +
                            "  a.sDescript sField01" +
                            ", a.sModelNme sField02" +
                            ", IFNULL(c.sDescript, '') sField03" +
                            ", IFNULL(b.sDescript, '') sField04" +
                            ", IFNULL(d.sDescript, '') sField05" +
                        " FROM Model a" +
                            " LEFT JOIN Inv_Type b" +
                                " ON a.sInvTypCd = b.sInvTypCd" +
                            " LEFT JOIN Brand c" +
                                " ON a.sBrandCde = c.sBrandCde" +
                            " LEFT JOIN Category d" +
                                " ON a.sCategrCd = d.sCategrCd" +
                        " WHERE a.cRecdStat = '1'" +
                        " ORDER BY sField04, sField03, sField01";
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_obj.put("sField03", rs.getString("sField03"));
                json_obj.put("sField04", rs.getString("sField04"));
                json_obj.put("sField05", rs.getString("sField05"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printTerm(){
        String lsSQL = "SELECT" +
                            "  sDescript sField02" +
                            ", CASE" +
                                    " WHEN cCoverage = '0' THEN 'Straight'" +
                                    " WHEN cCoverage = '1' THEN 'Days'" +
                                    " WHEN cCoverage = '2' THEN 'Month'" +
                                    " ELSE ''" +
                            " END sField01" +
                            ", nTermValx lField01" +
                        " FROM Term" +
                        " WHERE cRecdStat = '1'" +
                        " ORDER BY sField01, sField02";        
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_obj.put("lField02", rs.getString("lField01"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }
    
    private boolean printSupplier(){
        String lsSQL = "SELECT" +
                            "  b.sClientNm sField01" +
                            ", a.sCPerson1 sField02" +
                            ", a.sCPPosit1 sField03" +
                            ", a.sTelNoxxx sField04" +
                            ", a.sFaxNoxxx sField05" +
                        " FROM Supplier a" +
                            ", Client_Master b" +
                        " WHERE a.sClientID = b.sClientID" +
                        " ORDER BY sField01, sField02";        
        
        ResultSet rs = _instance.executeQuery(lsSQL);
        
        JSONArray json_arr = new JSONArray();
        json_arr.clear();
        
        try {
            while (rs.next()){
                JSONObject json_obj = new JSONObject(); 
                json_obj.put("sField01", rs.getString("sField01"));
                json_obj.put("sField02", rs.getString("sField02"));
                json_obj.put("sField03", rs.getString("sField03"));
                json_obj.put("sField04", rs.getString("sField04"));
                json_obj.put("sField05", rs.getString("sField05"));
                json_arr.add(json_obj);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sPrintdBy", _instance.getUserID());
                
        try {
            InputStream stream = new ByteArrayInputStream(json_arr.toJSONString().getBytes("UTF-8"));
            JsonDataSource jrjson = new JsonDataSource(stream); 
        
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"), params, jrjson);
 
        } catch (JRException | UnsupportedEncodingException  ex) {
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
        return "";
    }
}
