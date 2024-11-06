/**
 * Inventory Reports Main Class New
 *
 * @author Maynard Valencia
 * @started 2024.11.02
 */
package org.rmj.cas.food.reports.classes;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
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
import org.rmj.appdriver.constants.EditMode;
import org.rmj.appdriver.constants.RecordStatus;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class Inventory implements GReport {

    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.Inventory", "InventoryReport.log");

    private double xOffset = 0;
    private double yOffset = 0;

    public Inventory() {
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InventoryNewCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("InventoryNewCriteria.fxml"));

        InventoryNewCriteriaController instance = new InventoryNewCriteriaController();
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
            ShowMessageFX.Error(e.getMessage(), Inventory.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }

        if (!instance.isCancelled()) {
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.branch", instance.getBranch());
            System.setProperty("store.report.criteria.group", "");
            System.setProperty("store.report.criteria.type", instance.getInvType());
            return true;
        }
        return false;
    }

    @Override
    public boolean processReport() {
        boolean bResult = false;

        //Get the criteria as extracted from getParam()
//        if(System.getProperty("store.report.criteria.presentation").equals("0")){
//            System.setProperty("store.report.no", "1");
//        }else if(System.getProperty("store.report.criteria.group").equalsIgnoreCase("sBinNamex")) {
//            System.setProperty("store.report.no", "3");
//        }else if(System.getProperty("store.report.criteria.group").equalsIgnoreCase("sInvTypCd")) {
//            System.setProperty("store.report.no", "4");
//        }else{
//            System.setProperty("store.report.no", "2");
//        }
        System.setProperty("store.report.no", "2");

        //Load the jasper report to be use by this object
        String lsSQL = "SELECT sFileName, sReportHd"
                + " FROM xxxReportDetail"
                + " WHERE sReportID = " + SQLUtil.toSQL(System.getProperty("store.report.id"))
                + " AND nEntryNox = " + SQLUtil.toSQL(System.getProperty("store.report.no"));

        //Check if in debug mode...
        if (System.getProperty("store.default.debug").equalsIgnoreCase("true")) {
            System.out.println(System.getProperty("store.report.class") + ".processReport: " + lsSQL);
        }

        ResultSet loRS = _instance.executeQuery(lsSQL);

        try {
            if (!loRS.next()) {
                _message = "Invalid report was detected...";
                closeReport();
                return false;
            }
            System.setProperty("store.report.file", loRS.getString("sFileName"));
            System.setProperty("store.report.header", loRS.getString("sReportHd"));

            switch (Integer.valueOf(System.getProperty("store.report.no"))) {
//                case 1:
//                    bResult = printSummary();
//                    break;
                case 2:
                    bResult = printDetail();
            }

            if (!bResult) {
                closeReport();
                return false;
            }
            if (System.getProperty("store.report.is_log").equalsIgnoreCase("true")) {
                logReport();
            }
            JasperViewer jv = new JasperViewer(_jrprint, false);
            jv.setVisible(true);
            jv.setAlwaysOnTop(bResult);

        } catch (SQLException ex) {
            _message = ex.getMessage();
            //Check if in debug mode...
            if (System.getProperty("store.default.debug").equalsIgnoreCase("true")) {
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
        _rptparam.forEach(item -> System.out.println(item));
    }

    private boolean printSummary() throws SQLException {
        System.out.println("Printing Summary");
        return true;
    }

    private boolean printDetail() throws SQLException {
        String lsCondition = "";
        String lsDate = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {

            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));

            lsCondition = "e.dTransact BETWEEN " + lsDate;
        } else {
            lsCondition = "0=1";
        }

        if (!System.getProperty("store.report.criteria.branch").equals("")) {
            lsCondition = " a.sBranchCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch"));
        } else {
            lsCondition = " a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode());
        }
        System.out.println(MiscUtil.addCondition(getReportSQL(), lsCondition));
        ResultSet rs = _instance.executeQuery(MiscUtil.addCondition(getReportSQL(), lsCondition));
        while (!rs.next()) {

            _message = "No record found...";
            return false;
        }
        //recalculate data
        rs.beforeFirst();
        while (rs.next()) {
            if (System.getProperty("store.report.criteria.branch").equals("") && _instance.isOnline()) {
                NeoRecalculate(rs.getString("sField00"));
            } else {
                break;
            }
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

        String lsSQL = "SELECT sClientNm FROM Client_Master"
                + " WHERE sClientID IN ("
                + "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(_instance.getUserID()) + ")";

        ResultSet loRS = _instance.executeQuery(lsSQL);

        if (loRS.next()) {
            params.put("sPrintdBy", loRS.getString("sClientNm"));
        } else {
            params.put("sPrintdBy", "");
        }

        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath()
                    + System.getProperty("store.report.file"),
                    params,
                    jrRS);
        } catch (JRException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    private void closeReport() {
        _rptparam.forEach(item -> System.clearProperty((String) item));
        System.clearProperty("store.report.file");
        System.clearProperty("store.report.header");
    }

    private void logReport() {
        _rptparam.forEach(item -> System.clearProperty((String) item));
        System.clearProperty("store.report.file");
        System.clearProperty("store.report.header");
    }

    private String getReportSQL() {
        String lsSQL = "SELECT"
                + "  IFNULL(b.sStockIDx, '') `sField00`"
                + ", IFNULL(c.sDescript, '') `sField01`"
                + ", b.sBarCodex `sField02`"
                + ", IFNULL(b.`sDescript`, '') `sField03`"
                + ", IFNULL(d.`sDescript`, '') `sField05`"
                + ", IFNULL(f.sMeasurNm, '') `sField04`"
                + ", a.nQtyOnHnd `lField01`"
                + ", a.nBegQtyxx `lField02`"
                + ", SUM(IFNULL(e.nQtyInxxx, '0')) `lField03`"
                + ", SUM(IFNULL(e.nQtyOutxx, '0')) `lField04`"
                + " FROM Inv_Master a"
                + " , Inventory b"
                + " LEFT JOIN Inv_Type c"
                + " ON b.sInvTypCd = c.sInvTypCd"
                + " LEFT JOIN Brand d"
                + " ON b.sBrandCde = d.sBrandCde"
                + " LEFT JOIN Measure f"
                + " ON b.sMeasurID = f.sMeasurID"
                + " LEFT JOIN Inv_Ledger e"
                + " ON b.sStockIDx = e.sStockIDx"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " GROUP BY b.sStockIDx ";

        if (!System.getProperty("store.report.criteria.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sInvTypCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.type")));
        }

        return lsSQL;
    }

    private String getReportSQLDate() {
        String lsDate = System.getProperty("store.report.date.criteria");

        return "SELECT * FROM"
                + " (SELECT"
                + "  b.sBarCodex `sField01`"
                + ", b.sDescript `sField02`"
                + ", a.nBinNumbr `sField03`"
                + ", a.nQtyOnHnd `nField01`"
                + ", b.nSelPrice `lField01`"
                + ", IFNULL(c.sDescript, '') `sField04`"
                + ", IFNULL(d.sDescript, '') `sField05`"
                + ", IFNULL(e.sDescript, '') `sField06`"
                + ", IFNULL(f.sMeasurNm, '') `sField07`"
                + ", g.nQtyOnHnd `nField02`"
                + ", a.sStockIDx"
                + " FROM Inv_Master a"
                + " LEFT JOIN Inv_Ledger g"
                + " ON a.sStockIDx = g.sStockIDx"
                + " AND a.sBranchCd = g.sBranchCd"
                + " AND g.dTransact <= " + SQLUtil.toSQL(lsDate)
                + ", Inventory b"
                + " LEFT JOIN Inv_Type c"
                + " ON b.sInvTypCd = c.sInvTypCd"
                + " LEFT JOIN Brand d"
                + " ON b.sBrandCde = d.sBrandCde"
                + " LEFT JOIN Model e"
                + " ON b.sModelCde = e.sModelCde"
                + " LEFT JOIN Measure f"
                + " ON b.sMeasurID = f.sMeasurID"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode())
                + " AND a.dBegInvxx <= " + SQLUtil.toSQL(lsDate)
                + " ORDER BY"
                + " g.nLedgerNo DESC) xSourceTable"
                + " GROUP BY"
                + "  sStockIDx"
                + " ORDER BY"
                + "  sField04";
    }

    public boolean NeoRecalculate(String fsStockIDx) throws SQLException {
        if (fsStockIDx == null) {
            return false;
        }

        String lsSQL = "SELECT a.sStockIDx,a.nBegQtyxx"
                + " FROM Inv_Master a"
                + ", Inventory b"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode())
                + " AND a.cRecdStat = '1'"
                + " AND b.cRecdStat = '1'";

        ResultSet loRS = _instance.executeQuery(lsSQL);

        int lnMax = (int) MiscUtil.RecordCount(loRS);
        if (lnMax <= 0) {
            return false;
        }

        loRS.beforeFirst();
        int lnRow = 1;

        _instance.beginTrans();
        while (loRS.next()) {
            if (!Recalculate(loRS.getString("sStockIDx"), loRS.getDouble("nBegQtyxx"))) {
                lnRow += 1;
            }

            System.out.println(lnRow);
        }

        _instance.commitTrans();

        return true;
    }

    public boolean Recalculate(String fsStockIDx, double fnBegQtyxx) throws SQLException {

        String lsSQL;
        ResultSet loRSLedger;
        ResultSet loRSInvMaster;

        int lnLedgerNo = 0;
        lsSQL = "SELECT *"
                + " FROM Inv_Ledger"
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCD = " + SQLUtil.toSQL(_instance.getBranchCode())
                + " ORDER BY dTransact, nLedgerNo";
        loRSLedger = _instance.executeQuery(lsSQL);

        double lnQtyOnHnd = fnBegQtyxx;

        while (loRSLedger.next()) {

            lnQtyOnHnd += (loRSLedger.getFloat("nQtyInxxx") - loRSLedger.getFloat("nQtyOutxx"));
            lnLedgerNo++;
        }

        StringBuilder loSQL = new StringBuilder();
        if (lnLedgerNo != loRSLedger.getInt("nLedgerNo")) {
            loSQL.append(", ").append("nLedgerNo = ").append(lnLedgerNo);
        }

        if (Double.compare(loRSLedger.getDouble("nQtyOnHnd"), lnQtyOnHnd) != 0) {
            loSQL.append(", ").append("nQtyOnHnd = ").append(lnQtyOnHnd);
        }

        if (loSQL.length() > 0) {
            lsSQL = "UPDATE Inv_Ledger"
                    + " SET " + loSQL.toString().substring(2)
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                    + " AND sBranchCD = " + SQLUtil.toSQL(_instance.getBranchCode())
                    + " AND sSourceCd = " + SQLUtil.toSQL(loRSLedger.getString("sSourceCd"))
                    + " AND sSourceNo = " + SQLUtil.toSQL(loRSLedger.getString("sSourceNo"));
            System.out.println(lsSQL);
            _instance.executeQuery(lsSQL, "Inv_Ledger", _instance.getBranchCode(), "");
        }

        loSQL = new StringBuilder();
        lsSQL = "SELECT *"
                + " FROM Inv_Master"
                + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                + " AND sBranchCD = " + SQLUtil.toSQL(_instance.getBranchCode());

        loRSInvMaster = _instance.executeQuery(lsSQL);
        if (lnLedgerNo != loRSLedger.getDouble("nLedgerNo")) {
            loSQL.append(", ").append("nLedgerNo = ").append(lnLedgerNo);
        }

        if (Double.compare(loRSLedger.getDouble("nQtyOnHnd"), lnQtyOnHnd) != 0) {
            loSQL.append(", ").append("nQtyOnHnd = ").append(lnQtyOnHnd);
        }

        if (loSQL.length() > 0) {
            lsSQL = "UPDATE Inv_Master"
                    + " SET " + loSQL.toString().substring(2)
                    + " sModified = " + SQLUtil.toSQL(_instance.getUserID())
                    + " dModified = " + SQLUtil.toSQL(_instance.getServerDate())
                    + " WHERE sStockIDx = " + SQLUtil.toSQL(fsStockIDx)
                    + " AND sBranchCD = " + SQLUtil.toSQL(_instance.getBranchCode());
            _instance.executeQuery(lsSQL, "Inv_Master", _instance.getBranchCode(), "");
        }

        return true;

    }
}
