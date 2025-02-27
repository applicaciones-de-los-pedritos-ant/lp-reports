/**
 * Inventory Reports Main Class New
 *
 * @author Maynard Valencia
 * @started 2024.11.02
 */
package org.rmj.cas.food.reports.classes;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    static String filePath = "D:/GGC_Java_Systems/excel export/";

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
        _rptparam.add("store.report.criteria.isexport");
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
            System.setProperty("store.report.criteria.isexport", String.valueOf(instance.isExport()));
            return true;
        }
        return false;
    }

    boolean bResult = false;

    @Override
    public boolean processReport() {
        bResult = false;
        Task<Boolean> reportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                updateMessage("Loading report...");
                bResult = false;

                // Simulate long-running task
                Thread.sleep(1000);

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
                        bResult = false;
                        return bResult;
                    }
                    System.setProperty("store.report.file", loRS.getString("sFileName"));
                    System.setProperty("store.report.header", loRS.getString("sReportHd"));

                    switch (Integer.valueOf(System.getProperty("store.report.no"))) {
                        //                case 1:
                        //                    bResult = printSummary();
                        //                    break;
                        case 2:
                            bResult = printDetails();
                    }

                    if (!bResult) {
                        closeReport();
                        bResult = false;
                        stage.close();
                        return bResult;
                    }
                    if (System.getProperty("store.report.is_log").equalsIgnoreCase("true")) {
                        logReport();
                    }
                    JasperViewer jv = new JasperViewer(_jrprint, false);
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
                    Rectangle screenBounds = defaultScreen.getDefaultConfiguration().getBounds();
                    Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(defaultScreen.getDefaultConfiguration());
                    int adjustedHeight = screenBounds.height - screenInsets.bottom;
                    Rectangle adjustedBounds = new Rectangle(screenBounds.x, screenBounds.y, screenBounds.width, adjustedHeight);
                    jv.setBounds(adjustedBounds);
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
                    bResult = false;
                    return bResult;
                }

                closeReport();
                return bResult;
            }

        };

        // Handle task completion
        reportTask.setOnSucceeded(e -> {
            stage.close();
            System.out.println("Report loaded successfully!");
        });

        reportTask.setOnFailed(e -> {
            stage.close();
            System.out.println("Report failed load!!!");
//            progressIndicator.setVisible(false);
//            System.err.println("Failed to load the report: " + reportTask.getException().getMessage());
        });

        // Run the task in a background thread
        new Thread(reportTask).start();
        displayProgress();
        return bResult;
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
        String lsDateFrom = "";
        String lsDateThru = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {
            lsDateFrom = System.getProperty("store.report.criteria.datefrom");
            lsDateThru = System.getProperty("store.report.criteria.datethru");

            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));

            lsCondition = "e.dTransact BETWEEN " + lsDate;
        } else {
            lsCondition = "0=1";
        }

        if (!System.getProperty("store.report.criteria.branch").equals("")) {
            lsCondition = " a.sBranchCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch"));
        } else {
            if (!_instance.isMainOffice() && !_instance.isWarehouse()) {
                lsCondition = " a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode());
            }
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
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);

//        ObservableList<InventoryModel> R1data = FXCollections.observableArrayList();
//        rs.beforeFirst();
//        while (rs.next()) {
//            R1data.add(new InventoryModel(
//                    rs.getObject("sField00").toString(),
//                    rs.getObject("sField01").toString(),
//                    rs.getObject("sField02").toString(),
//                    rs.getObject("sField03").toString(),
//                    rs.getObject("sField04").toString(),
//                    rs.getObject("sField05").toString(),
//                    rs.getObject("lField01").toString(),
//                    rs.getObject("lField02").toString(),
//                    rs.getObject("lField03").toString(),
//                    rs.getObject("lField04").toString(),
//                    rs.getObject("lField05").toString(),
//                    rs.getObject("lField06").toString()
//            ));
//        }
//        System.out.println("R1data.size = " + R1data.size());
//        for(int lnCtr = 0; lnCtr <= R1data.size()-1; lnCtr++){
//            if(!lsDateFrom.isEmpty()){
//                R1data.get(lnCtr).setlField02(getBegQuantity(R1data.get(lnCtr).getsField00(), lsDateFrom).toString());
//                R1data.get(lnCtr).setlField05(getEndInv(R1data.get(lnCtr).getsField00(), lsDateThru).toString());
//            }
//        }
        rs.beforeFirst();
        //Convert the data-source to JasperReport data-source
        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);

//        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
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

    private boolean printDetails() throws SQLException {
        String lsCondition = "";
        String lsDate = "";
        String lsDateFrom = "";
        String lsDateThru = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {
            lsDateFrom = System.getProperty("store.report.criteria.datefrom");
            lsDateThru = System.getProperty("store.report.criteria.datethru");
            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));

            lsCondition += "e.dTransact BETWEEN " + lsDate;
        } else {
            lsCondition = "0=1";
        }

        if (!System.getProperty("store.report.criteria.branch").equals("")) {
            lsCondition = " a.sBranchCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch"));
        } else {
            if (!_instance.isMainOffice() && !_instance.isWarehouse()) {
                lsCondition = " a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode());
            }
        }
        if (!System.getProperty("store.report.criteria.type").equals("")) {
            lsCondition += " AND b.sInvTypCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.type"));
        }

        System.out.println(MiscUtil.addCondition(getReportSQL(), lsCondition));
        ResultSet rs = _instance.executeQuery(MiscUtil.addCondition(getReportSQL(), lsCondition));
        if (MiscUtil.RecordCount(rs) == 0) {
            _message = "No record found...";
            return false;
        }
//        while (!rs.next()) {
//            _message = "No record found...";
//            return false;
//        }
        //recalculate data

        ObservableList<InventoryModel> R1data = FXCollections.observableArrayList();
        rs.beforeFirst();
        while (rs.next()) {
            R1data.add(new InventoryModel(
                    rs.getObject("sField00").toString(),
                    rs.getObject("sField01").toString(),
                    rs.getObject("sField02").toString(),
                    rs.getObject("sField03").toString(),
                    rs.getObject("sField04").toString(),
                    rs.getObject("sField05").toString(),
                    rs.getObject("sField06").toString(),
                    rs.getObject("sField07").toString(),
                    rs.getDouble("lField01"),
                    rs.getDouble("lField02"),
                    rs.getDouble("lField03"),
                    rs.getDouble("lField04"),
                    rs.getDouble("lField05")
            ));
        }
//        System.out.println("R1data.size = " + R1data.size());
        for (int lnCtr = 0; lnCtr <= R1data.size() - 1; lnCtr++) {
            if (!lsDateFrom.isEmpty()) {
                R1data.get(lnCtr).setlField02(getBegQuantity(R1data.get(lnCtr).getsField00(), lsDateFrom).toString());
                R1data.get(lnCtr).setlField05(
                        getEndInv(R1data.get(lnCtr).getsField00().toString(),
                                lsDateThru,
                        R1data.get(lnCtr).getsField07()).toString());
            }
        }

//        rs.beforeFirst();
//        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);

        if (System.getProperty("store.report.criteria.isexport").equals("true")) {
            String[] headers = {"Branch", "Inventory Type", "Barcode", "Desciption", "Brand", "Measure", "QOH", "Beg. Inv", "Qty-In", "Qty-Out", "End Inv"};
            exportToExcel(R1data, "Inventory", headers);
        }
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
//            _jrprint = JasperFillManager.fillReport(_instance.getReportPath()
//                    + System.getProperty("store.report.file"),
//                    params,
//                    jrRS);
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

    private Object getBegQuantity(String StockIDx, String date) {
        String lsSQL = "SELECT"
                + " nQtyOnHnd "
                + " FROM Inv_Ledger "
                + " WHERE sStockIDx = " + SQLUtil.toSQL(StockIDx)
                + " AND sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode());

        if (!System.getProperty("store.report.criteria.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sInvTypCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.type")));
        }

        ResultSet rsBegQty = _instance.executeQuery(MiscUtil.addCondition(lsSQL, "dTransact < " + SQLUtil.toSQL(date))
                + "ORDER BY nLedgerNo DESC LIMIT 1");
        try {
            if (!rsBegQty.next()) {
                return 0.00;
            }
//            System.out.println(rsBegQty.getObject("nQtyOnHnd"));
            return rsBegQty.getObject("nQtyOnHnd");
        } catch (SQLException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
            return 0.00;
        }

    }

    private Object getEndInv(String StockIDx, String date,String fsBranch) {
        String lsSQL = "SELECT"
                + " nQtyOnHnd "
                + " FROM Inv_Ledger "
                + " WHERE sStockIDx = " + SQLUtil.toSQL(StockIDx)
                + " AND sBranchCd = " + SQLUtil.toSQL(fsBranch);

        if (!System.getProperty("store.report.criteria.type").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "b.sInvTypCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.type")));
        }

//        System.out.println("\n" + MiscUtil.addCondition(lsSQL, "dTransact <= " + SQLUtil.toSQL(date))
//                + "ORDER BY nLedgerNo DESC LIMIT 1" + "\n");
        ResultSet rsEndInv = _instance.executeQuery(MiscUtil.addCondition(lsSQL, "dTransact <= " + SQLUtil.toSQL(date))
                + "ORDER BY nLedgerNo DESC LIMIT 1");
//        System.out.println(lsSQL);
        try {
            if (!rsEndInv.next()) {
                return 0.00;
            }
//            System.out.println("rsEndInv == " + rsEndInv.getObject("nQtyOnHnd"));
            return rsEndInv.getObject("nQtyOnHnd");
        } catch (SQLException ex) {
            Logger.getLogger(Inventory.class.getName()).log(Level.SEVERE, null, ex);
            return 0.00;
        }
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
                + ", 0 `lField05`"
                + ", g.sBranchNm `sField06`"
                + ", g.sBranchCd `sField07`"
                + " FROM Inv_Master a"
                + " LEFT JOIN Branch g ON a.sBranchCd = g.sBranchCd"
                + " LEFT JOIN Inv_Ledger e ON a.sStockIDx = e.sStockIDx AND a.sBranchCd = e.sBranchCd"
                + " , Inventory b"
                + " LEFT JOIN Inv_Type c ON b.sInvTypCd = c.sInvTypCd"
                + " LEFT JOIN Brand d ON b.sBrandCde = d.sBrandCde"
                + " LEFT JOIN Measure f ON b.sMeasurID = f.sMeasurID"
                + " WHERE a.sStockIDx = b.sStockIDx"
                + " AND a.cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                + " GROUP BY  a.sBranchCd, b.sStockIDx ";

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

    public static void exportToExcel(ObservableList<InventoryModel> data, String fileName, String[] headers) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory Data");

        // Create header row
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(getHeaderCellStyle(workbook));
        }

//        System.out.println("getHeightInPoints = " + sheet.getRow(0).getHeightInPoints());

        headerRow.setHeightInPoints(20);

        // Create a CellStyle with double format (e.g., two decimal places)
        CellStyle doubleStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        doubleStyle.setDataFormat(format.getFormat("#,##0.00")); // Adjust format as needed
        // Populate data rows
        int rowIndex = 1;
        for (InventoryModel item : data) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getsField06());
            row.createCell(1).setCellValue(item.getsField01());
            row.createCell(2).setCellValue(item.getsField02());
            row.createCell(3).setCellValue(item.getsField03());
            row.createCell(4).setCellValue(item.getsField05());
            row.createCell(5).setCellValue(item.getsField04());
            row.createCell(6).setCellValue(item.getlField01());
            row.createCell(7).setCellValue(item.getlField02());
            row.createCell(8).setCellValue(item.getlField03());
            row.createCell(9).setCellValue(item.getlField04());
            row.createCell(10).setCellValue(item.getlField05());

            row.getCell(6).setCellStyle(doubleStyle);
            row.getCell(7).setCellStyle(doubleStyle);
            row.getCell(8).setCellStyle(doubleStyle);
            row.getCell(9).setCellStyle(doubleStyle);
            row.getCell(10).setCellStyle(doubleStyle);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
//            System.out.println("sheet width = " + sheet.getColumnWidth(i));
        }

        // Write to Excel file
        try (FileOutputStream fileOut = new FileOutputStream(filePath + fileName + ".xlsx")) {
            workbook.write(fileOut);
            System.out.println("Exported to Excel successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();

        // Set background color
        headerStyle.setFillForegroundColor(IndexedColors.OLIVE_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 12);
        headerStyle.setFont(font);

        // Set center alignment
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // Set borders for the header cells
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setTopBorderColor(IndexedColors.WHITE.getIndex()); // Set top border color to black
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBottomBorderColor(IndexedColors.WHITE.getIndex()); // Set bottom border color to black
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setLeftBorderColor(IndexedColors.WHITE.getIndex()); // Set left border color to black
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setRightBorderColor(IndexedColors.WHITE.getIndex()); // Set right border color to black

        return headerStyle;
    }

    Stage stage;

    private void displayProgress() {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("progess_dialog.fxml"));
            fxmlLoader.setLocation(getClass().getResource("progess_dialog.fxml"));

            Parent parent = fxmlLoader.load();

            stage = new Stage();

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
        } catch (IOException ex) {
            Logger.getLogger(Purchases.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
