/**
 * Inventory Transfer Reports Main Class
 *
 * @author Michael T. Cuison
 * @started 2019.06.08
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class InvParentChildList implements GReport {

    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.InvTransfer", "InvTransferReport.log");

    static String filePath = "D:/GGC_Java_Systems/excel export/";
    static String excelName = "";

    public InvParentChildList() {
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

        System.setProperty("store.default.debug", "true");
        System.setProperty("store.report.criteria.isexport", String.valueOf(true));
        System.setProperty("store.report.criteria.group", "");
        return true;
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

                    bResult = printSummary();

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
        System.out.println("Printing Summary");
        String lsCondition = "";
        String lsSQL = getReportSQLSummary();

//        System.out.println(MiscUtil.addCondition(lsSQL, lsCondition));
//        lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        ResultSet rs = _instance.executeQuery(lsSQL);
        System.out.println(lsSQL);
        while (!rs.next()) {

            _message = "No record found...";
            return false;
        }
//        rs.beforeFirst();
//        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);

        ObservableList<InvTransferModel> R1data = FXCollections.observableArrayList();
        R1data.clear();
        rs.beforeFirst();
        while (rs.next()) {
            R1data.add(new InvTransferModel(
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
                    rs.getObject("sField11").toString(),
                    rs.getObject("lField01").toString()
            ));
        }
        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);

        excelName = "Inventory Parent-Child Listing.xlsx";
        if (System.getProperty("store.report.criteria.isexport").equals("true")) {
            String[] headers = {"TYPE", "PARENT ID", "PARENT BARCODE", "PARENT DESCRIPTION", "PARENT UOM", "PARENT STATUS",
                "CHILD ID", "CHILD BARCODE", "CHILD DESCRIPTION", "CHILD UOM", "CHILD STATUS", "QUANTITY"};
            exportToExcel(R1data, headers);
        }

        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());
        params.put("sReportNm", System.getProperty("store.report.header"));
//        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");
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
            ShowMessageFX.Error(ex.getMessage(), InvParentChildList.class.getSimpleName(), "Please inform MIS Department.");
            Logger.getLogger(InvParentChildList.class.getName()).log(Level.SEVERE, null, ex);
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
                + " CASE b.cWSubUnit"
                + "  WHEN '0' THEN 'BOM'"
                + "  WHEN '1' THEN 'SUB-ITEM'"
                + "    ELSE '' "
                + " END `sField01`"
                + ", IFNULL (a.`sStockIDx`,'') `sField02`"
                + ", IFNULL (b.`sBarCodex`,'') `sField03`"
                + ", IFNULL (b.`sDescript`,'') `sField04`"
                + ", IFNULL (c.`sMeasurNm`, '') `sField05`"
                + ", CASE b.`cRecdStat`"
                + "   WHEN '0' THEN 'INACTIVE'"
                + "   WHEN '1' THEN 'ACTIVE'"
                + "    ELSE 'DELETED'"
                + " END `sField06`"
                + ", IFNULL (d.`sStockIDx`,'') `sField07`"
                + ", IFNULL (d.`sBarCodex`,'') `sField08`"
                + ", IFNULL (d.`sDescript`,'') `sField09`"
                + ", IFNULL (e.`sMeasurNm`, '') `sField10`"
                + ", CASE d.`cRecdStat`"
                + " WHEN '0' THEN 'INACTIVE'"
                + " WHEN '1' THEN 'ACTIVE'"
                + "    ELSE 'DELETED'"
                + "  END `sField11`"
                + ", IFNULL (a.nQuantity, 0.0) `lField01`"
                + " FROM Inventory_Sub_Unit a"
                + "  LEFT JOIN Inventory b on a.sStockIDx = b.sStockIDx"
                + "  LEFT JOIN Measure c ON b.sMeasurID = c.sMeasurID"
                + "  LEFT JOIN Inventory d ON a.sItmSubID = d.sStockIDx"
                + "  LEFT JOIN Measure e ON d.sMeasurID = e.sMeasurID"
                + " ORDER BY b.`sStockIDx` ASC ";

        System.out.println(lsSQL);
        return lsSQL;
    }

    public void exportToExcel(ObservableList<InvTransferModel> data, String[] headers) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory Transfer Data");

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
        for (InvTransferModel item : data) {
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
            row.createCell(10).setCellValue(item.getsField11());
            row.createCell(11).setCellValue(item.getlField01());

            row.getCell(11).setCellStyle(doubleStyle);

        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
            System.out.println("sheet width = " + sheet.getColumnWidth(i));
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
//
//            /*SET FORM MOVABLE*/
//            parent.setOnMousePressed(new EventHandler<MouseEvent>() {
//                @Override
//                public void handle(MouseEvent event) {
//                    xOffset = event.getSceneX();
//                    yOffset = event.getSceneY();
//                }
//            });
//            parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
//                @Override
//                public void handle(MouseEvent event) {
//                    stage.setX(event.getScreenX() - xOffset);
//                    stage.setY(event.getScreenY() - yOffset);
//                }
//            });
//            /*END SET FORM MOVABLE*/

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
