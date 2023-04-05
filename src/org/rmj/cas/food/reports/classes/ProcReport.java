/**
 * Food Reports Parent Class
 * @author Michael T. Cuison
 * @started 2018.11.26
 */

package org.rmj.cas.food.reports.classes;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agentfx.ShowMessageFX;
import org.rmj.appdriver.iface.GReport;

public class ProcReport {
    private GRider oApp;
    private String psReportID;
    
    public void setGRider(GRider foApp){oApp = foApp;}
    public void setReportID(String fsValue){psReportID = fsValue;}
    
    public boolean showReport(){
        String lsSQL = "SELECT" +
                            "  sReportID" +
                            ", sReportNm" +
                            ", sRepLibxx" +
                            ", sRepClass" +
                            ", cLogRepxx" +
                            ", cSaveRepx" +
                         " FROM xxxReportMaster" +
                         " WHERE sReportID = " + SQLUtil.toSQL(psReportID);
            
            
        try {
            ResultSet loRS = oApp.executeQuery(lsSQL);
            if (MiscUtil.RecordCount(loRS) == 1){
                loRS.absolute(1);

                System.setProperty("store.report.id", loRS.getString("sReportID"));
                System.setProperty("store.report.name", loRS.getString("sReportNm"));
                System.setProperty("store.report.jar", loRS.getString("sRepLibxx"));
                System.setProperty("store.report.class", loRS.getString("sRepClass"));
                System.setProperty("store.report.is_save", loRS.getString("cLogRepxx"));
                System.setProperty("store.report.is_log", loRS.getString("cSaveRepx"));
                loRS = null;

                System.out.println(System.getProperty("store.report.class"));
                GReport instance = (GReport) MiscUtil.createInstance(System.getProperty("store.report.class"));
                instance.setGRider(oApp);    
                if (instance.getParam()){
                    instance.hasPreview(true);
                    if (!instance.processReport())
                        ShowMessageFX.Warning(null, "Warning", "Unable to load report.");
                }
            }
        } catch (SQLException ex) {
            ShowMessageFX.Error(ex.getMessage(), "Error", "Plese inform MIS Department.");
        }
            
        return true;
    }
}
