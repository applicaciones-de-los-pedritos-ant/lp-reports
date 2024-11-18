/**
 * Waste Inventory Reports Main Class
 * @author Michael T. Cuison
 * @started 2019.06.07
 */

package org.rmj.cas.food.reports.classes;

import java.awt.Polygon;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.swing.JFrame;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.swing.JRViewer;
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
import static org.rmj.cas.food.reports.classes.Inventory.filePath;
import org.rmj.replication.utility.LogWrapper;

public class Purchases implements GReport{
    private GRider _instance;
    private boolean _preview = true;
    private String _message = "";
    private LinkedList _rptparam = null;
    private JasperPrint _jrprint = null;
    private LogWrapper logwrapr = new LogWrapper(this.getClass().getName(), "Purchases.log");
    
    private double xOffset = 0; 
    private double yOffset = 0;
    static String filePath = "D:/GGC_Java_Systems/excel export/";
    static String excelName = "";
    
    public Purchases(){
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
        _rptparam.add("store.report.criteria.supplier");
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PurchasesCriteria.fxml"));
        fxmlLoader.setLocation(getClass().getResource("PurchasesCriteria.fxml"));

        PurchasesCriteriaController instance = new PurchasesCriteriaController();
        instance.setGRider(_instance);
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
            ShowMessageFX.Error(e.getMessage(), DailyProduction.class.getSimpleName(), "Please inform MIS Department.");
            System.exit(1);
        }
        
        if (!instance.isCancelled()){            
            System.setProperty("store.default.debug", "true");
            System.setProperty("store.report.criteria.presentation", instance.Presentation());
            System.setProperty("store.report.criteria.datefrom", instance.getDateFrom());
            System.setProperty("store.report.criteria.datethru", instance.getDateTo());
            System.setProperty("store.report.criteria.supplier", instance.getSupplier());
            System.setProperty("store.report.criteria.isexport", String.valueOf(instance.isExport()));
            
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

//            if(System.getProperty("store.report.criteria.isexport").equals("true")){
//                // Configure the XLSX exporter
//                JRXlsxExporter exporter = new JRXlsxExporter();
//                exporter.setExporterInput(new SimpleExporterInput(_jrprint));
//                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(new File(filePath + excelName)));
//
//                // Export the report to Excel
//                exporter.exportReport();
//                System.out.println("Report exported to Excel at: " + excelName);
//            }
//            isExported = true;
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

    private boolean printDetail() throws SQLException{
        String lsSQL = getReportSQL();
        String lsCondition = "";
        String lsDate = "";
        
        if (!System.getProperty("store.report.criteria.datefrom").equals("") &&
                !System.getProperty("store.report.criteria.datethru").equals("")){

            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND " +
                        SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));
            
            lsCondition = lsDate;
            
            lsSQL = MiscUtil.addCondition(lsSQL, "a.dTransact BETWEEN " + lsCondition);
        }
        
        if (!System.getProperty("store.report.criteria.supplier").equals("")){
            lsCondition = "a.sSupplier = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.supplier"));
            
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }

        System.out.println(lsSQL);
        ResultSet rs = _instance.executeQuery(lsSQL + " ORDER BY sField02 ASC");
        while (!rs.next()) {
           _message = "No record found...";
           return false;
        }
        ObservableList<PurchasesModel> R1data = FXCollections.observableArrayList();
        R1data.clear();
        rs.beforeFirst();
        while (rs.next()) {
            double nTotal = Double.parseDouble(String.valueOf(rs.getObject("lField01"))) * Double.parseDouble(String.valueOf(rs.getObject("nField01")));
            R1data.add(new PurchasesModel(
                    rs.getObject("sField01").toString(),
                    rs.getObject("sField02").toString(),
                    rs.getObject("sField03").toString(),
                    rs.getObject("sField04").toString(),
                    rs.getObject("sField05").toString(),
                    rs.getObject("sField06").toString(),
                    rs.getObject("sField07").toString(),
                    rs.getObject("nField01").toString(),
                    rs.getObject("lField01").toString(),
                    String.valueOf(nTotal)
            ));
        }
        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        
        excelName = "Purchases Detail.xlsx";
        if(System.getProperty("store.report.criteria.isexport").equals("true")){
            String[] headers = { "Refer #", "Date", "Supplier", "Barcode", "Description", "Brand", "Measure", "Qty", "Cost", "Total"};
            exportToExcel(R1data, headers);
        }

        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");  
        params.put("sBranchNm", _instance.getBranchName());
        params.put("sAddressx", _instance.getAddress() + " " + _instance.getTownName() + ", " + _instance.getProvince());      
        params.put("sReportNm", System.getProperty("store.report.header"));      
        params.put("sReportDt", !lsDate.equals("") ? lsDate.replace("AND", "to").replace("'", "") : "");
        
        lsSQL = "SELECT sClientNm FROM Client_Master" +
                " WHERE sClientID IN (" +
                    "SELECT sEmployNo FROM xxxSysUser WHERE sUserIDxx = " + SQLUtil.toSQL(_instance.getUserID()) + ")";
        
        ResultSet loRS = _instance.executeQuery(lsSQL);
        
        if (loRS.next()){
            params.put("sPrintdBy", loRS.getString("sClientNm"));
        } else {
            params.put("sPrintdBy", "");
        }
        
        try {
            _jrprint = JasperFillManager.fillReport(
                _instance.getReportPath() + System.getProperty("store.report.file"),
                params, 
                jrRS
            );
        } catch (JRException ex) {
            System.err.println("Error filling report: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    private boolean printSummary() throws SQLException{
        String lsSQL = getReportSQLSum();
        String lsCondition = "";
        String lsDate = "";
        
        if (!System.getProperty("store.report.criteria.datefrom").equals("") &&
                !System.getProperty("store.report.criteria.datethru").equals("")){

            lsDate = SQLUtil.toSQL(System.getProperty("store.report.criteria.datefrom")) + " AND " +
                        SQLUtil.toSQL(System.getProperty("store.report.criteria.datethru"));
            
            lsCondition = lsDate;
            
            lsSQL = MiscUtil.addCondition(lsSQL, "a.dTransact BETWEEN " + lsCondition);
        }
        
        if (!System.getProperty("store.report.criteria.supplier").equals("")){
            lsCondition = "a.sSupplier = " + SQLUtil.toSQL(System.getProperty("store.report.criteria.supplier"));
            
            lsSQL = MiscUtil.addCondition(lsSQL, lsCondition);
        }

        System.out.println(lsSQL);
        ResultSet rs = _instance.executeQuery(lsSQL + " ORDER BY sField02 ASC");
        
        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        
        ObservableList<PurchasesModel> R1data = FXCollections.observableArrayList();
        R1data.clear();
        rs.beforeFirst();
        while (rs.next()) {
            R1data.add(new PurchasesModel(
                    rs.getObject("sField01").toString(),
                    rs.getObject("sField02").toString(),
                    rs.getObject("sField03").toString(),
                    rs.getObject("sField04").toString(),
                    rs.getObject("sField05").toString(),
                    rs.getObject("sField06").toString(),
                    rs.getObject("lField01").toString()
            ));
        }
        //Convert the data-source to JasperReport data-source
//        JRResultSetDataSource jrRS = new JRResultSetDataSource(rs);
        JRBeanCollectionDataSource jrRS = new JRBeanCollectionDataSource(R1data);
        
        excelName = "Purchases Summary.xlsx";
        if(System.getProperty("store.report.criteria.isexport").equals("true")){
            String[] headers = { "Refer #", "Date", "Supplier", "Destination", "Term", "Total", "Status"};
            exportToExcel(R1data, headers);
        }
        //Create the parameter
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Los Pedritos Bakeshop & Restaurant");  
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
        if (_instance.getUserLevel() > UserRight.MANAGER){
            return "SELECT" +
                        "  a.sReferNox `sField01`" +
                        ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField02`" +
                        ", g.sClientNm `sField03`" +	
                        ", c.sBarCodex `sField04`" +
                        ", CONCAT(c.sDescript, IF(IFNULL(d.sDescript, '') = '', '', CONCAT(' / ', d.sDescript))) `sField05`" +
                        ", IFNULL(e.`sDescript`, '') `sField07`" +
                        ", IFNULL(f.sMeasurNm, '') `sField06`" +
                        ", b.nQuantity `nField01`" +
                        ", b.nUnitPrce `lField01`" +
                        ", 0 `lField02`" +
                    " FROM PO_Master a" +
                        " LEFT JOIN Client_Master g" + 
                            " ON a.sSupplier = g.sClientID" + 
                        " LEFT JOIN Branch h" +
                            " ON a.sBranchCd = h.sBranchCd" +
                        ", PO_Detail b" +
                            " LEFT JOIN Inventory c" +
                                " ON b.sStockIDx = c.sStockIDx" +
                            " LEFT JOIN Model d" +
                                " ON c.sModelCde = d.sModelCde" + 
                            " LEFT JOIN Brand e" + 
                                " ON c.sBrandCde = e.sBrandCde" + 
                            " LEFT JOIN Measure f" +
                                " ON c.sMeasurID = f.sMeasurID" + 
                    " WHERE a.sTransNox = b.sTransNox" +                
                        " AND LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()) +
                        " AND a.cTranStat <> '3'";
        } else {
            return "SELECT" +
                        "  a.sReferNox `sField01`" +
                        ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField02`" +
                        ", g.sClientNm `sField03`" +	
                        ", c.sBarCodex `sField04`" +
                        ", CONCAT(c.sDescript, IF(IFNULL(d.sDescript, '') = '', '', CONCAT(' / ', d.sDescript))) `sField05`" +
                        ", IFNULL(f.sMeasurNm, '') `sField06`" +
                        ", IFNULL(e.sDescript, '') `sField07`" +
                        ", b.nQuantity `nField01`" +
                        ", 0.00 `lField01`" +
                        ", 0 `lField02`" +
                    " FROM PO_Master a" +
                        " LEFT JOIN Client_Master g" + 
                            " ON a.sSupplier = g.sClientID" + 
                        " LEFT JOIN Branch h" +
                            " ON a.sBranchCd = h.sBranchCd" +
                        ", PO_Detail b" +
                            " LEFT JOIN Inventory c" +
                                " ON b.sStockIDx = c.sStockIDx" +
                            " LEFT JOIN Model d" +
                                " ON c.sModelCde = d.sModelCde" + 
                            " LEFT JOIN Brand e" + 
                                " ON c.sBrandCde = e.sBrandCde" + 
                            " LEFT JOIN Measure f" +
                                " ON c.sMeasurID = f.sMeasurID" + 
                    " WHERE a.sTransNox = b.sTransNox" +                
                        " AND LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()) +
                        " AND a.cTranStat <> '3'";
        }
        
    }
    
    private String getReportSQLSum(){
        String lsSQL =  "SELECT" +
                            "  a.sReferNox `sField01`" +
                            ", DATE_FORMAT(a.dTransact, '%Y-%m-%d') `sField02`" +
                            ", b.sClientNm `sField03`" +
                            ", IFNULL(c.sBranchNm, '') `sField04`" +
                            ", IFNULL(d.sDescript, '') `sField05`" +
                            ", a.nTranTotl `lField01`" +
                            ", CASE a.cTranStat" +
                                " WHEN '0' THEN 'OPEN'" +
                                " WHEN '1' THEN 'APPROVED'" +
                                " WHEN '2' THEN 'POSTED'" +
                                " WHEN '3' THEN 'CANCELLED'" +
                                " WHEN '4' THEN 'VOID'" +
                                " END `sField06`" +
                        " FROM PO_Master a" + 
                            " LEFT JOIN Branch c  ON a.sBranchCd = c.sBranchCd" +
                            " LEFT JOIN Term d ON a.sTermCode = d.sTermCode" +
                            ", Client_Master b" +
                        " WHERE a.sSupplier = b.sClientID" + 
                            " AND a.cTranStat <> '3'";
        
        if (_instance.getUserLevel() < UserRight.ENGINEER){
            lsSQL = MiscUtil.addCondition(lsSQL, "LEFT(a.sTransNox, 4) = " + SQLUtil.toSQL(_instance.getBranchCode()));
        }
        
        return lsSQL;
    }
    
    public void exportToExcel(ObservableList<PurchasesModel> data, String[] headers) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchases Data");

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
        for (PurchasesModel item : data) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getsField01());
            row.createCell(1).setCellValue(item.getsField02());
            row.createCell(2).setCellValue(item.getsField03());
            row.createCell(3).setCellValue(item.getsField04());
            row.createCell(4).setCellValue(item.getsField05());
            if(System.getProperty("store.report.criteria.presentation").equals("1")){
                row.createCell(5).setCellValue(item.getsField07());
                row.createCell(6).setCellValue(item.getsField06());
                row.createCell(7).setCellValue(item.getnField01());
                row.createCell(8).setCellValue(item.getlField01());
                row.createCell(9).setCellValue(item.getlField02());

                // Apply the double format style to the appropriate columns (7, 8, 9)
                row.getCell(7).setCellStyle(doubleStyle);
                row.getCell(8).setCellStyle(doubleStyle);
                row.getCell(9).setCellStyle(doubleStyle);
            }else{
                
                row.createCell(5).setCellValue(item.getlField01());
                row.createCell(6).setCellValue(item.getsField06());

                // Apply the double format style to the appropriate columns (6)
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
}
