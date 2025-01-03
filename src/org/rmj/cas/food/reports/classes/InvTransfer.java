/**
 * Inventory Transfer Reports Main Class
 *
 * @author Michael T. Cuison
 * @started 2019.06.08
 */
package org.rmj.cas.food.reports.classes;

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
import org.rmj.appdriver.constants.UserRight;
import org.rmj.appdriver.iface.GReport;
import org.rmj.replication.utility.LogWrapper;

public class InvTransfer implements GReport {

    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper("org.rmj.foodreports.classes.InvTransfer", "InvTransferReport.log");

    private double xOffset = 0;
    private double yOffset = 0;
    static String filePath = "D:/GGC_Java_Systems/excel export/";
    static String excelName = "";

    public InvTransfer() {
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
            ShowMessageFX.Error(e.getMessage(), InvTransfer.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }

        if (!instance.isCancelled()) {
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.presentation", instance.Presentation());
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.branch", instance.getOrigin());
            System.setProperty("store.report.criteria.destinat", instance.getDestination());
            System.setProperty("store.report.criteria.isexport", String.valueOf(instance.isExport()));
            System.setProperty("store.report.criteria.group", "");
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
                //Get the criteria as extracted from getParam()
                if (System.getProperty("store.report.criteria.presentation").equals("0")) {
                    System.setProperty("store.report.no", "1");
                } else if (System.getProperty("store.report.criteria.group").equalsIgnoreCase("sBinNamex")) {
                    System.setProperty("store.report.no", "3");
                } else if (System.getProperty("store.report.criteria.group").equalsIgnoreCase("sInvTypCd")) {
                    System.setProperty("store.report.no", "4");
                } else {
                    System.setProperty("store.report.no", "2");
                }

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
        String lsDate = "";
        String lsExcelDate = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {
            lsExcelDate = ExcelDate(System.getProperty("store.report.criteria.datefrom"), System.getProperty("store.report.criteria.datethru"));
            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru") + " 23:59:59");

            lsCondition = "a.dTransact BETWEEN " + lsDate;
        } else {
            lsCondition = "0=1";
        }

        System.out.println(MiscUtil.addCondition(getReportSQL(), lsCondition));
        lsSQL = MiscUtil.addCondition(getReportSQLSummary(), lsCondition);
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
                    rs.getObject("lField01").toString(),
                    rs.getObject("lField02").toString()
            ));
        }
        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        
        excelName = "Inventory Transfer Summary - " + lsExcelDate + ".xlsx";
        if(System.getProperty("store.report.criteria.isexport").equals("true")){
            String[] headers = { "Origin", "Destination", "Date", "Trans. No.", "TTL Qty", "TTL Amount", "Status"};
            exportToExcel(R1data, headers);
        }


        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", _instance.getClientName());
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());
        params.put("sReportNm", System.getProperty("store.report.header"));
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");
        params.put("sPrintdBy", _instance.getUserID());

        try {
            _jrprint = JasperFillManager.fillReport(_instance.getReportPath()
                    + System.getProperty("store.report.file"),
                    params,
                    jrRS);
        } catch (JRException ex) {
            Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    private boolean printDetail() throws SQLException {
        String lsCondition = "";
        String lsDate = "";
        String lsExcelDate = "";

        if (!System.getProperty("store.report.criteria.datefrom").equals("")
                && !System.getProperty("store.report.criteria.datethru").equals("")) {
            lsExcelDate = ExcelDate(System.getProperty("store.report.criteria.datefrom"), System.getProperty("store.report.criteria.datethru"));
            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND "
                    + SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru")  + " 23:59:59");

            lsCondition = "a.dTransact BETWEEN " + lsDate;
        } else {
            lsCondition = "0=1";
        }

        System.out.println(MiscUtil.addCondition(getReportSQL(), lsCondition));
        ResultSet rs = _instance.executeQuery(MiscUtil.addCondition(getReportSQL(), lsCondition));
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
            double nTotal = Double.parseDouble(String.valueOf(rs.getObject("lField02"))) * Double.parseDouble(String.valueOf(rs.getObject("lField01")));
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
                    rs.getObject("lField01").toString(),
                    rs.getObject("lField02").toString(),
                    String.valueOf(nTotal),
                    rs.getObject("sField11").toString()
            ));
        }
        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        
        excelName = "Inventory Transfer Detail - " + lsExcelDate + ".xlsx";
        if(System.getProperty("store.report.criteria.isexport").equals("true")){
            String[] headers = { "Origin", "Destination", "Trans. No.", "Date", "Inv. Tp", "Barcode", "Description",
                    "Brand", "Measure", "Qty", "Cost", "Total", "Status", "D. Modified"};
            exportToExcel(R1data, headers);
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
        } catch (JRException ex) {
            Logger.getLogger(InvTransfer.class.getName()).log(Level.SEVERE, null, ex);
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
                + "  i.sBranchNm `sField10`"
                + ", g.sBranchNm `sField01`"
                + ", a.sTransNox `sField02`"
                + ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField03`"
                + ", h.sDescript `sField04`"
                + ", c.sBarCodex `sField05`"
                + ", IFNULL(c.sDescript, '') `sField06`"
                + ", IFNULL(f.sMeasurNm, '') `sField07`"
                + ", IFNULL(e.sDescript, '') `sField08`"
                + ", b.nQuantity `lField01`"
                + ", c.nUnitPrce `lField02`"
                + ", CASE a.cTranStat"
                + " WHEN '0' THEN 'OPEN'"
                + " WHEN '1' THEN 'CONFIRMED'"
                + " WHEN '2' THEN 'RECIEVED'"
                + " WHEN '3' THEN 'CANCELLED'"
                + " WHEN '4' THEN 'VOID'"
                + " END `sField09`"
                + ", a.dModified `sField11`"
                + " FROM Inv_Transfer_Master a"
                + " LEFT JOIN Branch i"
                + " ON LEFT(a.sTransNox, 4) = i.sBranchCd"
                + " LEFT JOIN Branch g"
                + " ON a.sDestinat = g.sBranchCd"
                + ", Inv_Transfer_Detail b"
                + " LEFT JOIN Inventory c"
                + " ON b.sStockIDx = c.sStockIDx"
                + " LEFT JOIN Brand e"
                + " ON c.sBrandCde = e.sBrandCde"
                + " LEFT JOIN Measure f"
                + " ON c.sMeasurID = f.sMeasurID"
                + " LEFT JOIN Inv_Type h"
                + " ON c.sInvTypCd = h.sInvTypCd"
                + " WHERE a.sTransNox = b.sTransNox"
                + " AND a.cTranStat NOT IN('0','3')"
                + " ORDER BY a.sDestinat, a.sTransNox, b.nEntryNox";

        if (_instance.getUserLevel() >= UserRight.SUPERVISOR) {
            if (!System.getProperty("store.report.criteria.branch").isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch")));
            }
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }

        if (!System.getProperty("store.report.criteria.destinat").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDestinat = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.destinat")));
        }

        return lsSQL;
    }

    private String getReportSQLSummary() {
        String lsSQL = "SELECT"
                + "  e.sBranchNm `sField01`"
                + ", d.sBranchNm `sField02`"
                + ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField03`"
                + ", a.sTransNox `sField04`"
                + ", SUM(b.nQuantity)  `lField01`"
                + ", SUM(b.nQuantity * c.nUnitPrce) `lField02`"
                + ", CASE a.cTranStat"
                + " WHEN '0' THEN 'OPEN'"
                + " WHEN '1' THEN 'CONFIRMED'"
                + " WHEN '2' THEN 'RECIEVED'"
                + " WHEN '3' THEN 'CANCELLED'"
                + " WHEN '4' THEN 'VOID'"
                + " END `sField05`"
                + " FROM Inv_Transfer_Master a"
                + " LEFT JOIN Branch d ON a.sDestinat = d.sBranchCd"
                + " LEFT JOIN Branch e ON a.sBranchCd = e.sBranchCd"
                + ", Inv_Transfer_Detail b"
                + " LEFT JOIN Inventory c ON b.sStockIDx = c.sStockIDx"
                + " WHERE a.sTransNox = b.sTransNox"
                + " AND a.cTranStat NOT IN('0','3')"
                + " GROUP BY a.sTransNox"
                + " ORDER BY a.sDestinat, a.sTransNox, b.nEntryNox";

        if (_instance.getUserLevel() >= UserRight.SUPERVISOR) {
            if (!System.getProperty("store.report.criteria.branch").isEmpty()) {
                lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.branch")));
            }
        } else {
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }

        if (!System.getProperty("store.report.criteria.destinat").isEmpty()) {
            lsSQL = MiscUtil.addCondition(lsSQL, "a.sDestinat = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.destinat")));
        }
//        System.out.println (lsSQL);
        return lsSQL;
    }
    private String ExcelDate(String lsDateFrom, String lsDateThru){
        
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
        DataFormat format  = workbook.createDataFormat();
        doubleStyle.setDataFormat(format.getFormat("#,##0.00")); // Adjust format as needed

        // Populate data rows
        int rowIndex = 1;
        for (InvTransferModel item : data) {
            Row row = sheet.createRow(rowIndex++);
            
            if(System.getProperty("store.report.criteria.presentation").equals("1")){
                row.createCell(0).setCellValue(item.getsField10());
                row.createCell(1).setCellValue(item.getsField01());
                row.createCell(2).setCellValue(item.getsField02());
                row.createCell(3).setCellValue(item.getsField03());
                row.createCell(4).setCellValue(item.getsField04());
                row.createCell(5).setCellValue(item.getsField05());
                row.createCell(6).setCellValue(item.getsField06());
                row.createCell(7).setCellValue(item.getsField08());
                row.createCell(8).setCellValue(item.getsField07());
                row.createCell(9).setCellValue(item.getlField01());
                row.createCell(10).setCellValue(item.getlField02());
                row.createCell(11).setCellValue(item.getlField03());
                row.createCell(12).setCellValue(item.getsField09());
                row.createCell(13).setCellValue(item.getsField11());
                // Apply the double format style to the appropriate columns (10, 11, 12)
                row.getCell(9).setCellStyle(doubleStyle);
                row.getCell(10).setCellStyle(doubleStyle);
                row.getCell(11).setCellStyle(doubleStyle);
            }else{
                row.createCell(0).setCellValue(item.getsField01());
                row.createCell(1).setCellValue(item.getsField02());
                row.createCell(2).setCellValue(item.getsField03());
                row.createCell(3).setCellValue(item.getsField04());
                row.createCell(4).setCellValue(item.getlField01());
                row.createCell(5).setCellValue(item.getlField02());
                row.createCell(6).setCellValue(item.getsField05());
                
                row.getCell(4).setCellStyle(doubleStyle);
                row.getCell(5).setCellStyle(doubleStyle);
            }
            
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
    private void displayProgress(){
        
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
