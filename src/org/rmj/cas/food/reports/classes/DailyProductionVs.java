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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.sql.rowset.CachedRowSet;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.rmj.appdriver.GLogger;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class DailyProductionVs implements GReport{
    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private JasperViewer jrViewer = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.Inventory", "InventoryReport.log");
    private ObservableList<DailyVsRequest> data = FXCollections.observableArrayList();
    private double xOffset = 0; 
    private double yOffset = 0;
    
    public DailyProductionVs(){
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
            ShowMessageFX.Error(e.getMessage(), DailyProductionVs.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }
        
        if (!instance.isCancelled()){
            System.setProperty("store.default.debug", "true");
//            System.setProperty("store.report.criteria.presentation", "1");
            System.setProperty("store.report.criteria.presentation", String.valueOf(instance.getIndex()));
            System.setProperty("store.report.criteria.dteF", instance.getDateFrom());
            System.setProperty("store.report.criteria.dteT", instance.getDateTo());
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
    
    private boolean printSummary() throws SQLException{
        String lsCondition = "";
        String lsDate = "", lsDate1 = "";
        
        if (!System.getProperty("store.report.criteria.dteF").equals("")){
            lsDate = System.getProperty("store.report.criteria.dteF");
            lsDate1 = System.getProperty("store.report.criteria.dteT");
            lsCondition = "a.dTransact BETWEEN " + SQLUtil.toSQL(lsDate) + " AND " + SQLUtil.toSQL(lsDate1);
        } else lsCondition = "0=1";
        System.out.println("Summary");
        System.out.println(MiscUtil.addCondition(getReportSQL_ProductRequest(), lsCondition));
        System.out.println(MiscUtil.addCondition(getReportSQL_DailyProduction(), lsCondition));
        ResultSet rs = _instance.executeQuery(MiscUtil.addCondition(getReportSQL_ProductRequest(), lsCondition));
        ResultSet rs1 = _instance.executeQuery(MiscUtil.addCondition(getReportSQL_DailyProduction(), lsCondition));
        //Convert the data-source to JasperReport data-source
        DailyVsRequest lsModel = new DailyVsRequest();
        
            
        lsModel.setIndex01("0");
        lsModel.setIndex02("0");
        while(rs.next()){
            if(rs.getString("nField01") != null){
                lsModel.setIndex01(rs.getString("nField01"));
            }
        }
        while(rs1.next()){
            if(rs1.getString("nField01") != null){
                lsModel.setIndex02(rs1.getString("nField01"));
            }
        }

        data.add(lsModel);
        JRBeanCollectionDataSource beanColDataSource1 = new JRBeanCollectionDataSource(data);
        
//        JRResultSetDataSource jrRS = new JRResultSetDataSource((ResultSet) data);
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("nReqTotal", lsModel.getIndex01());  
        params.put("nDlyTotal", lsModel.getIndex02());  
        params.put("sDateFrom", lsDate);  
        params.put("sDateThru", lsDate1);  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", !lsDate.equals("") ? lsDate + " - " + lsDate1: "");
        params.put("sPrintdBy", _instance.getUserID());
        
        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"),
                                                    params, 
                                                    beanColDataSource1);
        } catch (JRException ex) {
            Logger.getLogger(DailyProductionVs.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
        
    }
    
    private boolean printDetail() throws SQLException{
        String lsCondition = "";
        String lsDate = "",lsDate1 = "";
        
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (!System.getProperty("store.report.criteria.dteF").equals("")){
            lsDate = System.getProperty("store.report.criteria.dteF");
            lsDate1 = System.getProperty("store.report.criteria.dteT");
            
         startDate = LocalDate.parse(lsDate);
         endDate = LocalDate.parse(lsDate1);
            lsCondition = "a.dTransact BETWEEN " + SQLUtil.toSQL(lsDate) + " AND " + SQLUtil.toSQL(lsDate1) + " GROUP BY a.dTransact " +
            "   ORDER BY a.dTransact ASC;";
        } else lsCondition = "0=1";
        System.out.println("Detail");

        for (LocalDate date = startDate; date.isBefore(endDate.plusDays(1)); date = date.plusDays(1)) {
            System.out.println("date = " + date);
            // do something with date
            DailyVsRequest lsModel = new DailyVsRequest();
            lsCondition = "a.dTransact = " + SQLUtil.toSQL(date.toString());
            ResultSet rs = _instance.executeQuery(MiscUtil.addCondition(getReportSQL_ProductRequest(), lsCondition));
            ResultSet rs1 = _instance.executeQuery(MiscUtil.addCondition(getReportSQL_DailyProduction(), lsCondition));
            System.out.println(MiscUtil.addCondition(getReportSQL_ProductRequest(), lsCondition));
            System.out.println(MiscUtil.addCondition(getReportSQL_DailyProduction(), lsCondition));
            
            lsModel.setIndex01(date.toString());
            lsModel.setIndex02("0");
            lsModel.setIndex03("0");
            while(rs.next()){
                if(rs.getString("nField01") != null){
                    lsModel.setIndex02(rs.getString("nField01"));
                }
            }
            while(rs1.next()){
                if(rs1.getString("nField01") != null){
                    lsModel.setIndex03(rs1.getString("nField01"));
                }
            }
            
            data.add(lsModel);
        }
        JRBeanCollectionDataSource beanColDataSource1 = new JRBeanCollectionDataSource(data);
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sDateFrom", !lsDate.equals("") ? lsDate : "");  
        params.put("sDateThru", !lsDate1.equals("") ? lsDate1 : "");  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", !lsDate.equals("") ? lsDate : "");
        params.put("sPrintdBy", _instance.getUserID());
        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath() + 
                                                    System.getProperty("store.report.file"),
                                                    params, 
                                                    beanColDataSource1);
        } catch (JRException ex) {
            Logger.getLogger(DailyProductionVs.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private String getReportSQL_DailyProduction(){
        return "SELECT " +
                    " SUM(IFNull(b.nQuantity,0)) `nField01`" +
                " FROM Daily_Production_Master a" +
                    ", Daily_Production_Detail b" +
                " WHERE a.sTransNox = b.sTransNox" +                    
                    " AND LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode());
    
    }
    private String getReportSQL_ProductRequest(){
        return "SELECT " +
                    " SUM(IFNull(b.nQuantity,0)) `nField01`" +
                " FROM Product_Request_Master a" +
                    ", Product_Request_Detail b" +
                " WHERE a.sTransNox = b.sTransNox" +                    
                    " AND LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode());
    
    }
       
}
