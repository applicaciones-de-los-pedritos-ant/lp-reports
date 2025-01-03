/**
 * Inventory Reports Main Class New
 *
 * @author Maynard Valencia
 * @started 2024.11.02
 */
package org.rmj.cas.food.reports.classes;

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
import org.rmj.appdriver.agentfx.CommonUtils;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class InventoryLedger implements GReport {

    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.Inventory", "InventoryReport.log");

    private double xOffset = 0;
    private double yOffset = 0;
    static String filePath = "D:/GGC_Java_Systems/excel export/";
    static String excelName = "";

    public InventoryLedger() {
        _rptparam = new LinkedList();
        _rptparam.add("store.report.id");
        _rptparam.add("store.report.no");
        _rptparam.add("store.report.name");
        _rptparam.add("store.report.jar");
        _rptparam.add("store.report.class");
        _rptparam.add("store.report.is_save");
        _rptparam.add("store.report.is_log");

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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("InventoryLedgerCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("InventoryLedgerCriteria.fxml"));

        InventoryLedgerCriteriaController instance = new InventoryLedgerCriteriaController();
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
            ShowMessageFX.Error(e.getMessage(), InventoryLedger.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }

        if (!instance.isCancelled()) {
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.branch", instance.getBranch());
            System.setProperty("store.report.criteria.stock", instance.getStockID());
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

                System.setProperty("store.report.no", "1");
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

                    bResult = printInventory();

                    if (!bResult) {
                        closeReport();
                        stage.close();
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

    private boolean printInventory() throws SQLException {

        try {
            String lsCondition = "";
            String lsDate = "";
            String lsDateFrom = "";
            String lsDateThru = "";
            String lsBranch = "";
            String lsExcelDate = "";

            if (!System.getProperty("store.report.criteria.datefrom").equals("")
                    && !System.getProperty("store.report.criteria.datethru").equals("")) {
                lsDateFrom = System.getProperty("store.report.criteria.datefrom");
                lsDateThru = System.getProperty("store.report.criteria.datethru");
                lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                        + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));

                lsExcelDate = ExcelDate(lsDateFrom, lsDateThru);
                lsCondition += "a.dTransact BETWEEN " + lsDate;
            } else {
                lsCondition = "0=1";
            }

            if (!System.getProperty("store.report.criteria.branch").equals("")) {
                lsCondition += " AND a.sBranchCd = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch"));
            } else {
                lsCondition += " AND a.sBranchCd = " + SQLUtil.toSQL(_instance.getBranchCode());
            }

            if (!System.getProperty("store.report.criteria.stock").equals("")) {
                lsCondition += " AND a.sStockIDx = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.stock"));
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
                        rs.getObject("sField01").toString(),
                        rs.getObject("sField02").toString(),
                        rs.getObject("sField03").toString(),
                        rs.getObject("sField04").toString(),
                        rs.getObject("sField05").toString(),
                        rs.getObject("sField06").toString(),
                        rs.getObject("sField07").toString(),
                        rs.getObject("sField08").toString(),
                        rs.getObject("sField09").toString(),
                        rs.getObject("sField10").toString(),
                        rs.getObject("lField01").toString(),
                        rs.getObject("lField02").toString(),
                        rs.getObject("lField03").toString()
                ));
            }
//        rs.beforeFirst();
//        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);

            JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);

            excelName = "Inventory Ledger as of - " + ExcelDateThru(lsDateThru) + ".xlsx";

            if (System.getProperty("store.report.criteria.isexport").equals("true")) {
                String[] headers = {"Original Branch", "Source / Destination", "Barcode", "Description", "Brand", "Model", "Measure", "Source No.", "Source", "Date", "Qty. In", "Qty. Out", "QOH"};
                exportToExcel(R1data, headers);
            }
            //Create the parameter
            Map<String, Object> params = new HashMap<>();
            params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");
            params.put("sBranchNm", _instance.getBranchName());
            params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());
            params.put("sReportNm", System.getProperty("store.report.header") + " as of " + CommonUtils.toDate(lsDateThru));
            params.put("sReportDt", "");

            String lsSQL = "SELECT sClientNm FROM Client_Master"
                    + " WHERE sClientID IN ("
                    + "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(_instance.getUserID()) + ")";

            ResultSet loRS = _instance.executeQuery(lsSQL);

            if (loRS.next()) {
                params.put("sPrintdBy", loRS.getString("sClientNm"));
            } else {
                params.put("sPrintdBy", "");
            }

            _jrprint = JasperFillManager.fillReport(_instance.getReportPath()
                    + System.getProperty("store.report.file"),
                    params,
                    jrRS);
//            _jrprint = JasperFillManager.fillReport(_instance.getReportPath()
//                    + System.getProperty("store.report.file"),
//                    params,
//                    jrRS);
        } catch (JRException ex) {
            Logger.getLogger(InventoryLedger.class.getName()).log(Level.SEVERE, null, ex);
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
                + " e.sBranchNm sField01"
                + " , CASE a.sSourceCd "
                + " WHEN 'Dlvr' THEN d.sBranchNm "
                + " WHEN 'AcDl' THEN f.sBranchNm "
                + " ELSE e.sBranchNm "
                + " END sField02 "
                + ", h.sBarCodex sField03"
                + ", h.sDescript sField04"
                + ", IFNULL(i.sDescript,'') sField05"
                + ", IFNULL(j.sDescript,'') sField06"
                + ", IFNULL(k.sMeasurNm,'') sField07"
                + ", a.sSourceNo sField08"
                + ", b.sDescript sField09"
                + ", a.dTransact sField10"
                + ", a.nQtyInxxx lField01"
                + ", a.nQtyOutxx lField02"
                + ", a.nQtyOnHnd lField03"
                + " FROM Inv_Ledger a"
                + " LEFT JOIN xxxSource_Transaction b"
                + " ON a.sSourceCd = b.sSourceCd  "
                + " LEFT JOIN Inv_Transfer_Master c"
                + " ON a.sSourceNo = c.sTransNox AND a.sSourceCd = 'Dlvr'"
                + " LEFT JOIN Branch d"
                + " ON c.sDestinat = d.sBranchCd"
                + " LEFT JOIN Inv_Transfer_Master g "
                + " ON a.sSourceNo = g.sTransNox AND a.sSourceCd = 'AcDl' "
                + " LEFT JOIN Branch f"
                + " ON LEFT(g.sTransNox,4) = f.sBranchCd"
                + " LEFT JOIN Branch e"
                + " ON LEFT(a.sSourceNo,4) = e.sBranchCd"
                + " LEFT JOIN Inventory h"
                + " ON a.sStockIDx =  h.sStockIDx"
                + " LEFT JOIN Brand i"
                + " ON h.sBrandCde =  i.sBrandCde"
                + " LEFT JOIN Model j"
                + " ON h.sModelCde =  j.sModelCde"
                + " LEFT JOIN Measure k"
                + " ON h.sMeasurID =  k.sMeasurID"
                + " ORDER BY a.dTransact ASC, a.nLedgerNo ASC";
        return lsSQL;
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
            Logger.getLogger(Purchases.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Purchases.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }

    }

    public static void exportToExcel(ObservableList<InventoryModel> data, String[] headers) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory Data");

        // Create header row
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(getHeaderCellStyle(workbook));
        }

        System.out.println("getHeightInPoints = " + sheet.getRow(0).getHeightInPoints());

        headerRow.setHeightInPoints(20);

        // Create a CellStyle with double format (e.g., two decimal places)
        CellStyle doubleStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        doubleStyle.setDataFormat(format.getFormat("#,##0.00")); // Adjust format as needed
        // Populate data rows
        int rowIndex = 1;
        for (InventoryModel item : data) {
            Row row = sheet.createRow(rowIndex++);

            row.createCell(0).setCellValue(item.getsField01());
            row.createCell(1).setCellValue(item.getsField02());
            row.createCell(2).setCellValue(item.getsField03());
            row.createCell(3).setCellValue(item.getsField04());
            row.createCell(4).setCellValue(item.getsField05());
            row.createCell(5).setCellValue(item.getsField06());
            row.createCell(6).setCellValue(item.getsField07());
            row.createCell(7).setCellValue(item.getsField08());
            row.createCell(8).setCellValue(item.getsField09());
            row.createCell(9).setCellValue(item.getsField10());
            row.createCell(10).setCellValue(item.getlField01());
            row.createCell(11).setCellValue(item.getlField02());
            row.createCell(12).setCellValue(item.getlField03());

            row.getCell(6).setCellStyle(doubleStyle);
            row.getCell(7).setCellStyle(doubleStyle);

        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
            System.out.println("sheet width = " + sheet.getColumnWidth(i));
        }

        // Write to Excel file
        try (FileOutputStream fileOut = new FileOutputStream(filePath + excelName)) {
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
