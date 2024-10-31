/**
 * Inventory Transfer Reports Main Class
 * @author Michael T. Cuison
 * @started 2019.06.08
 */

package org.rmj.cas.food.reports.classes;

import java.io.IOException;
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
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
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

public class InvTransfer implements GReport{
    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.InvTransfer", "InvTransferReport.log");
    
    private double xOffset = 0; 
    private double yOffset = 0;
    
    public InvTransfer(){
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
        _rptparam.add("store.report.criteria.destinat");
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InventoryTransferCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("InventoryTransferCriteria.fxml"));

        InventoryTransferCriteriaController instance = new InventoryTransferCriteriaController();
        instance.singleDayOnly(false);
        instance.setGRider(_instance);
        
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
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.branch", instance.getOrigin());
            System.setProperty("store.report.criteria.destinat", instance.getDestination());
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
        System.out.println("Printing Summary");
        String lsCondition = "";
        String lsSQL = getReportSQLSummary();
        String lsDate = "";
        
        if (!System.getProperty("store.report.criteria.datefrom").equals("") &&
                !System.getProperty("store.report.criteria.datethru").equals("")){
            
            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND " +
                        SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));
            
            lsCondition = "a.dTransact BETWEEN " + lsDate;
        } else lsCondition = "0=1";
        
        System.out.println(MiscUtil.addCondition(getReportSQL(), lsCondition));
        lsSQL = MiscUtil.addCondition(getReportSQLSummary(), lsCondition);
        ResultSet rs = _instance.executeQuery(lsSQL);
        System.out.println (lsSQL);
         while (!rs.next()) {
            
            _message = "No record found...";
            return false;
        }
         rs.beforeFirst();
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");
        params.put("sPrintdBy", _instance.getUserID());
        
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
        String lsDate = "";
        
        if (!System.getProperty("store.report.criteria.datefrom").equals("") &&
                !System.getProperty("store.report.criteria.datethru").equals("")){
            
            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND " +
                        SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));
            
            lsCondition = "a.dTransact BETWEEN " + lsDate;
        } else lsCondition = "0=1";
        
        System.out.println(MiscUtil.addCondition(getReportSQL(), lsCondition));
        ResultSet rs = _instance.executeQuery(MiscUtil.addCondition(getReportSQL(), lsCondition));
         while (!rs.next()) {
            
            _message = "No record found...";
            return false;
        }
         rs.beforeFirst();
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");
        
        String lsSQL = "SELECT sClientNm FROM Client_Master" +
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
                    "  h.sBranchNm `sField10`" +
                    "  g.sBranchNm `sField01`" +
                    ", a.sTransNox `sField02`" +
                    ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField03`" +
                    ", h.sDescript `sField04`" +
                    ", c.sBarCodex `sField05`" +
                    ", IFNULL(c.sDescript, '') `sField06`" +
                    ", IFNULL(f.sMeasurNm, '') `sField07`" +
                    ", IFNULL(e.sDescript, '') `sField08`" +
                    ", b.nQuantity `lField01`" +
                    ", c.nUnitPrce `lField02`" +
                    ", CASE a.cTranStat" +
                    " WHEN '0' THEN 'OPEN'" +
                    " WHEN '1' THEN 'CONFIRMED'" +
                    " WHEN '2' THEN 'RECIEVED'" +
                    " WHEN '3' THEN 'CANCELLED'" +
                    " WHEN '4' THEN 'VOID'" +
                    " END `sField09`" +
                " FROM Inv_Transfer_Master a" +
                        " LEFT JOIN Branch h" +
                            " ON LEFT(a.sTransNox, 4) = h.sBranchCd" +
                        " LEFT JOIN Branch g" +
                            " ON a.sDestinat = g.sBranchCd" +
                    ", Inv_Transfer_Detail b" +
                        " LEFT JOIN Inventory c" +
                            " ON b.sStockIDx = c.sStockIDx" +
                        " LEFT JOIN Brand e" + 
                            " ON c.sBrandCde = e.sBrandCde" + 
                        " LEFT JOIN Measure f" +
                            " ON c.sMeasurID = f.sMeasurID" + 
                        " LEFT JOIN Inv_Type h" +
                            " ON c.sInvTypCd = h.sInvTypCd" +
                " WHERE a.sTransNox = b.sTransNox" + 
                " AND a.cTranStat NOT IN('0','3')" +
                " ORDER BY a.sDestinat, a.sTransNox, b.nEntryNox";
        
        if (_instance.getUserLevel() >= UserRight.ENGINEER){
            if (!System.getProperty("store.report.criteria.branch").isEmpty()){
                lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch")));
            }
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }
        
        if (!System.getProperty("store.report.criteria.destinat").isEmpty()){
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDestinat = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.destinat")));
        }
        
        return lsSQL;
    }
    
    private String getReportSQLSummary(){
        String lsSQL = "SELECT" +
                            "  e.sBranchNm `sField01`" +
                            ", d.sBranchNm `sField02`" +
                            ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField03`" +
                            ", a.sTransNox `sField04`" +
                            ", SUM(b.nQuantity)  `lField01`" +
                            ", SUM(b.nQuantity * c.nUnitPrce) `lField02`" +
                            ", CASE a.cTranStat" +
                                " WHEN '0' THEN 'OPEN'" +
                                " WHEN '1' THEN 'CONFIRMED'" +
                                " WHEN '2' THEN 'RECIEVED'" +
                                " WHEN '3' THEN 'CANCELLED'" +
                                " WHEN '4' THEN 'VOID'" +
                            " END `sField05`" +
                        " FROM Inv_Transfer_Master a" +
                            " LEFT JOIN Branch d ON a.sDestinat = d.sBranchCd" +
                            " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd" +
                            ", Inv_Transfer_Detail b" +
                            " LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx" +
                        " WHERE a.sTransNox = b.sTransNox" +    
                        " AND a.cTranStat NOT IN('0','3')" +
                        " GROUP BY a.sTransNox" +
                        " ORDER BY a.sDestinat, a.sTransNox, b.nEntryNox";
        
        if (_instance.getUserLevel() >= UserRight.ENGINEER){
            if (!System.getProperty("store.report.criteria.branch").isEmpty()){
                lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch")));
            }
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }
        
        if (!System.getProperty("store.report.criteria.destinat").isEmpty()){
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDestinat = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.destinat")));
        }
//        System.out.println (lsSQL);
        return lsSQL;
    }
}