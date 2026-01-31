/**
 * Food Reports Main Class
 *
 * @author Michael T. Cuison
 * @started 2018.11.24
 */
package org.rmj.cas.food.reports.classes;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
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
import org.rmj.appdriver.constants.UserRight;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class InvWaste implements GReport {

    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private JasperViewer jrViewer = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.InvWaste", "InvWasteReport.log");
    boolean bResult = false;
    Stage stage;
    static String excelName = "";
    static String filePath = "D:/GGC_Java_Systems/excel export/";

    private double xOffset = 0;
    private double yOffset = 0;

    public InvWaste() {
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DailyProductionCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("DailyProductionCriteria.fxml"));

        DailyProductionCriteriaController instance = new DailyProductionCriteriaController();
        instance.singleDayOnly(false);
        instance.setGRider(_instance);
        instance.setCriteriaTitle("Inventory Waste Criteria");

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
            ShowMessageFX.Error(e.getMessage(), InvWaste.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }

        if (!instance.isCancelled()) {
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.presentation", String.valueOf(instance.Presentation()));
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.branch", instance.getBranch());
            System.setProperty("store.report.criteria.group", "");
            System.setProperty("store.report.criteria.isexport", String.valueOf(instance.isExport()));
            return true;
        }
        return false;
    }

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
                System.setProperty("store.report.no", System.getProperty("store.report.criteria.presentation"));

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
                        stage.close();
                        return false;
                    }
                    System.setProperty("store.report.file", loRS.getString("sFileName"));
                    System.setProperty("store.report.header", loRS.getString("sReportHd"));

                    switch (Integer.valueOf(System.getProperty("store.report.no"))) {
                        case 1:
                            bResult = printSummary();
                            break;
                        case 2:
                            bResult = printDetail();
                            break;
                    }

                    if (!bResult) {
                        closeReport();
                        stage.close();
                        return false;
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
                    stage.close();
                    return false;
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
            System.out.println("Report failed to load");
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

        String lsCondition = "";
        String lsDate = "";
        String lsExcelDate = "";
        String lsDateFrom = "";
        String lsDateThru = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {
            lsDateFrom = System.getProperty("store.report.criteria.datefrom");
            lsDateThru = System.getProperty("store.report.criteria.datethru");
            lsExcelDate = ExcelDate(lsDateFrom, lsDateThru);
            lsDate = System.getProperty("store.report.criteria.datefrom") + " to " + System.getProperty("store.report.criteria.datethru");

            lsCondition = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));

            lsCondition = "a.dTransact BETWEEN " + lsCondition;
        } else {
            lsCondition = "0 = 1";
        }
        String lsSQL = getReportSQLSummary();
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranStat NOT IN ('0','3','4')");

        ResultSet rs = _instance.executeQuery(lsSQL);
        System.out.println("Report Query: " + lsSQL);

        ObservableList<InvWasteModel> R1data = FXCollections.observableArrayList();
        rs.beforeFirst();
        while (rs.next()) {
            R1data.add(new InvWasteModel(
                    rs.getObject("sField01").toString(),
                    rs.getObject("sField02").toString(),
                    rs.getObject("sField03").toString(),
                    rs.getObject("sField04").toString(),
                    rs.getObject("sField05").toString(),
                    rs.getObject("sField06").toString(),
                    rs.getDouble("nField01"),
                    rs.getDouble("nField02"),
                    rs.getDouble("nField03")
            ));
        }

        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        excelName = "Inventory Waste Summarized- " + lsExcelDate + ".xlsx";
        if (System.getProperty("store.report.criteria.isexport").equals("true")) {
            String[] headers = {"Branch", "Barcode", "Description", "Brand", "Measure", "Inv. Type", "Quantity", "Inv. Cost", "Total Amount"};
            exportToExcel(R1data, headers);
        }
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());
        params.put("sReportNm", System.getProperty("store.report.header"));
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");

        lsSQL = "SELECT sClientNm FROM Client_Master"
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

            Platform.runLater(() -> {
                ShowMessageFX.Error(ex.getMessage(), InvWaste.class.getSimpleName(), "Please inform MIS Department.");
            });
            Logger.getLogger(InvWaste.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    private boolean printDetail() throws SQLException {

        String lsCondition = "";
        String lsDate = "";
        String lsExcelDate = "";
        String lsDateFrom = "";
        String lsDateThru = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {
            lsDateFrom = System.getProperty("store.report.criteria.datefrom");
            lsDateThru = System.getProperty("store.report.criteria.datethru");
            lsExcelDate = ExcelDate(lsDateFrom, lsDateThru);
            lsDate = System.getProperty("store.report.criteria.datefrom") + " to " + System.getProperty("store.report.criteria.datethru");

            lsCondition = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));

            lsCondition = "a.dTransact BETWEEN " + lsCondition;
        } else {
            lsCondition = "0 = 1";
        }
        String lsSQL = getReportSQLDetailed();
        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        lsSQL = MiscUtil.addCondition(lsSQL, " a.cTranStat NOT IN ('0','3','4')");

        ResultSet rs = _instance.executeQuery(lsSQL);
        System.out.println("Report Query: " + lsSQL);
        ObservableList<InvWasteModel> R1data = FXCollections.observableArrayList();
        rs.beforeFirst();
        while (rs.next()) {
            R1data.add(new InvWasteModel(
                    rs.getObject("sField01").toString(),
                    rs.getObject("sField02").toString(),
                    rs.getObject("sField03").toString(),
                    rs.getObject("sField04").toString(),
                    rs.getObject("sField05").toString(),
                    rs.getObject("sField06").toString(),
                    rs.getObject("sField07").toString(),
                    rs.getObject("sField08").toString(),
                    rs.getDouble("nField01"),
                    rs.getDouble("nField02"),
                    rs.getDouble("nField03")
            ));
        }
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);

        excelName = "Inventory Waste Detailed - " + lsExcelDate + ".xlsx";
        if (System.getProperty("store.report.criteria.isexport").equals("true")) {
            String[] headers = {"Branch", "Date", "Transaction No", "Barcode", "Description", "Brand", "Measure", "Inv. Type", "Quantity", "Inv. Cost", "Total Amount"};
            exportToExcel(R1data, headers);
        }
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());
        params.put("sReportNm", System.getProperty("store.report.header"));
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");

        lsSQL = "SELECT sClientNm FROM Client_Master"
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

            Platform.runLater(() -> {
                ShowMessageFX.Error(ex.getMessage(), InvWaste.class.getSimpleName(), "Please inform MIS Department.");
            });
            Logger.getLogger(InvWaste.class.getName()).log(Level.SEVERE, null, ex);
            return false;
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

    private String getReportSQLSummary() {
        String lsSQL = "SELECT"
                + " g.sBranchNm sField01"
                + " , IFNULL(c.sBarCodex, '') sField02"
                + " , IFNULL(c.sDescript, '') sField03"
                + " , IFNULL(e.sDescript, '') sField04"
                + " , IFNULL(d.sMeasurNm, '') sField05"
                + " , IFNULL(f.sDescript, '') sField06"
                + " , SUM(b.nQuantity) nField01"
                + " , b.nInvCostx nField02"
                + " , SUM(b.nQuantity * b.nInvCostx) nField03"
                + "  FROM Inv_Waste_Master a"
                + "         , Inv_Waste_Detail b"
                + "  LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + "  LEFT JOIN Measure d ON c.sMeasurID = d.sMeasurID"
                + "  LEFT JOIN Brand e ON c.sBrandCde = e.sBrandCde"
                + "  LEFT JOIN Inv_Type f ON c.sInvTypCd = f.sInvTypCd"
                + "  LEFT JOIN Branch g ON g.sBranchCd = LEFT(b.sTransNox,4)"
                + "     WHERE a.sTransNox = b.sTransNox"
                + "         GROUP BY g.sBranchCd,c.sBarCodex"
                + "             ORDER BY b.sTransNox, c.sDescript";

        if (!System.getProperty("store.report.criteria.branch").equals("")) {
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox,4) = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch")));
        } else {
            if (_instance.getUserLevel() < UserRight.SUPERVISOR) {
                lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(sTransNox,4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
            }
        }

        return lsSQL;
    }

    private String getReportSQLDetailed() {
        String lsSQL = "SELECT"
                + " f.sBranchNm sField01"
                + " , a.sTransNox sField02"
                + " , DATE_FORMAT(a.dTransact, '%Y-%m-%d') sField03"
                + " , c.sBarCodex sField04"
                + " , c.sDescript sField05"
                + " , IFNULL(e.sDescript, '') sField06"
                + " , IFNULL(d.sMeasurNm, '') sField07"
                + " , IFNULL(g.sDescript, '') sField08"
                + " , SUM(b.nQuantity) nField01"
                + " , b.nInvCostx nField02"
                + " , SUM(b.nQuantity * b.nInvCostx) nField03"
                + "  FROM Inv_Waste_Master a"
                + " , Inv_Waste_Detail b"
                + "  LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + "  LEFT JOIN Measure d ON c.sMeasurID = d.sMeasurID"
                + "  LEFT JOIN Brand e ON c.sBrandCde = e.sBrandCde"
                + "  LEFT JOIN Branch f ON  f.sBranchCd = LEFT(b.sTransNox,4)"
                + "  LEFT JOIN Inv_Type g ON g.sInvTypCd = c.sInvTypCd"
                + "     WHERE a.sTransNox = b.sTransNox"
                + "         GROUP BY a.sTransNox,c.sBarCodex"
                + "             ORDER BY b.sTransNox, c.sDescript";

        if (!System.getProperty("store.report.criteria.branch").equals("")) {
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox,4) = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch")));
        } else {
            if (_instance.getUserLevel() < UserRight.SUPERVISOR) {
                lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(sTransNox,4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
            }
        }

        return lsSQL;
    }

    private void displayProgress() {

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("progess_dialog.fxml"));
            fxmlLoader.setLocation(getClass().getResource("progess_dialog.fxml"));

            Parent parent = fxmlLoader.load();

            stage = new Stage();
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

    public static void exportToExcel(ObservableList<InvWasteModel> data, String[] headers) {
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
        for (InvWasteModel item : data) {
            Row row = sheet.createRow(rowIndex++);
            //summary

            if (System.getProperty("store.report.criteria.presentation").equals("1")) {
                row.createCell(0).setCellValue(item.getsField01().toString());
                row.createCell(1).setCellValue(item.getsField02().toString());
                row.createCell(2).setCellValue(item.getsField03().toString());
                row.createCell(3).setCellValue(item.getsField04().toString());
                row.createCell(4).setCellValue(item.getsField05().toString());
                row.createCell(5).setCellValue(item.getsField06().toString());
                row.createCell(6).setCellValue(item.getnField01().toString());
                row.createCell(7).setCellValue(item.getnField02().toString());
                row.createCell(8).setCellValue(item.getnField03().toString());
            } else {
                //detailed
                row.createCell(0).setCellValue(item.getsField01().toString());
                row.createCell(1).setCellValue(item.getsField02().toString());
                row.createCell(2).setCellValue(item.getsField03().toString());
                row.createCell(3).setCellValue(item.getsField04().toString());
                row.createCell(4).setCellValue(item.getsField05().toString());
                row.createCell(5).setCellValue(item.getsField06().toString());
                row.createCell(6).setCellValue(item.getsField07().toString());
                row.createCell(7).setCellValue(item.getsField08().toString());
                row.createCell(8).setCellValue(item.getnField01().toString());
                row.createCell(9).setCellValue(item.getnField02().toString());
                row.createCell(10).setCellValue(item.getnField03().toString());
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
//            System.out.println("sheet width = " + sheet.getColumnWidth(i));
        }

        // Ensure the directory exists
        File directory = new File(filePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate a unique file name if the file already exists
        String fileFullPath = filePath + excelName;
        File file = new File(fileFullPath);
        int count = 1;

        while (file.exists()) {
            String baseName = excelName.contains(".")
                    ? excelName.substring(0, excelName.lastIndexOf("."))
                    : excelName;
            String extension = excelName.contains(".")
                    ? excelName.substring(excelName.lastIndexOf("."))
                    : "";
            fileFullPath = filePath + baseName + "-" + count + extension;
            file = new File(fileFullPath);
            count++;
        }

        // Write to the Excel file
        try (FileOutputStream fileOut = new FileOutputStream(fileFullPath)) {
            workbook.write(fileOut);
            System.out.println("Exported to Excel successfully: " + fileFullPath);
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

    private String ExcelDate(String lsDateFrom, String lsDateThru) {

        try {
            // Parse the date string to a Date object
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date dateFrom, dateThru;
            dateFrom = inputFormat.parse(lsDateFrom);
            dateThru = inputFormat.parse(lsDateThru);

            // Define the desired output format
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy");

            // Convert the Date object to the desired string format
            String formattedDateFrom = outputFormat.format(dateFrom);
            String formattedDateThru = outputFormat.format(dateThru);
            return formattedDateFrom + " to " + formattedDateThru;
        } catch (ParseException ex) {
            Logger.getLogger(InvWaste.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }

    }

    private String ExcelDateThru(String lsDateThru) {

        try {
            // Parse the date string to a Date object
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date dateThru;
            dateThru = inputFormat.parse(lsDateThru);

            // Define the desired output format
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy");

            String formattedDateThru = outputFormat.format(dateThru);
            return formattedDateThru;
        } catch (ParseException ex) {
            Logger.getLogger(InvWaste.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }

    }

}
