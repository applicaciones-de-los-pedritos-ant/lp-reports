package org.rmj.cas.food.reports.classes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class InventoryModel {  
    private SimpleStringProperty sField00;
    private SimpleStringProperty sField01;        
    private SimpleStringProperty sField02;       
    private SimpleStringProperty sField03;     
    private SimpleStringProperty sField04;     
    private SimpleStringProperty sField05;  
    private SimpleStringProperty sField06;
    private SimpleDoubleProperty lField01;    
    private SimpleDoubleProperty lField02;     
    private SimpleDoubleProperty lField03;
    private SimpleDoubleProperty lField04;
    private SimpleDoubleProperty lField05;
    
    InventoryModel(String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08, String index09, String index10, String index11, String index12){
        this.sField00 = new SimpleStringProperty(index01);
        this.sField01 = new SimpleStringProperty(index02);
        this.sField02 = new SimpleStringProperty(index03);
        this.sField03 = new SimpleStringProperty(index04);
        this.sField04 = new SimpleStringProperty(index05);
        this.sField05 = new SimpleStringProperty(index06);
        this.lField01 = new SimpleDoubleProperty(Double.parseDouble(index07));
        this.lField02 = new SimpleDoubleProperty(Double.parseDouble(index08));
        this.lField03 = new SimpleDoubleProperty(Double.parseDouble(index09));
        this.lField04 = new SimpleDoubleProperty(Double.parseDouble(index10));
        this.lField05 = new SimpleDoubleProperty(Double.parseDouble(index11));
        this.sField06 = new SimpleStringProperty(index12);
    }
    /*
    *Model for Inventory presentation
    */
    InventoryModel(String index00,String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08){
        this.sField00 = new SimpleStringProperty(index00);
        this.sField01 = new SimpleStringProperty(index01);
        this.sField02 = new SimpleStringProperty(index02);
        this.sField03 = new SimpleStringProperty(index03);
        this.sField04 = new SimpleStringProperty(index04);
        this.sField05 = new SimpleStringProperty(index05);
        this.sField06 = new SimpleStringProperty(index06);
        this.lField01 = new SimpleDoubleProperty(Double.parseDouble(index07));
        this.lField02 = new SimpleDoubleProperty(Double.parseDouble(index08));
    }
    /*
    *Model for Inventory movement
    */
    InventoryModel(String index00,String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08, String index09, String index10){
        this.sField00 = new SimpleStringProperty(index00);
        this.sField01 = new SimpleStringProperty(index01);
        this.sField02 = new SimpleStringProperty(index02);
        this.sField03 = new SimpleStringProperty(index03);
        this.sField04 = new SimpleStringProperty(index04);
        this.sField05 = new SimpleStringProperty(index05);
        this.sField06 = new SimpleStringProperty(index06);
        this.lField01 = new SimpleDoubleProperty(Double.parseDouble(index07));
        this.lField02 = new SimpleDoubleProperty(Double.parseDouble(index08));
        this.lField03 = new SimpleDoubleProperty(Double.parseDouble(index09));
        this.lField04 = new SimpleDoubleProperty(Double.parseDouble(index10));
    }
    public String getsField00() {
        return sField00.get();
    }

    public void setsField00(String sField00) {
        this.sField00.set(sField00);
    }

    public String getsField01() {
        return sField01.get();
    }

    public void setsField01(String sField01) {
        this.sField01.set(sField01);
    }

    public String getsField02() {
        return sField02.get();
    }

    public void setsField02(String sField02) {
        this.sField02.set(sField02);
    }

    public String getsField03() {
        return sField03.get();
    }

    public void setsField03(String sField03) {
        this.sField03.set(sField03);
    }

    public String getsField04() {
        return sField04.get();
    }

    public void setsField04(String sField04) {
        this.sField04.set(sField04);
    }

    public String getsField05() {
        return sField05.get();
    }

    public void setsField05(String sField05) {
        this.sField05.set(sField05);
    }

    public Double getlField01() {
        return lField01.get();
    }

    public void setlField01(String lField01) {
        this.lField01.set(Double.parseDouble(lField01));
    }

    public Double getlField02() {
        return lField02.get();
    }

    public void setlField02(String lField02) {
        this.lField02.set(Double.parseDouble(lField02));
    }

    public Double getlField03() {
        return lField03.get();
    }

    public void setlField03(String lField03) {
        this.lField03.set(Double.parseDouble(lField03));
    }

    public Double getlField04() {
        return lField04.get();
    }

    public void setlField04(String lField04) {
        this.lField04.set(Double.parseDouble(lField04));
    }

    public Double getlField05() {
        return lField05.get();
    }

    public void setlField05(String lField05) {
        this.lField05.set(Double.parseDouble(lField05));
    }
      public String getsField06() {
        return sField06.get();
    }

    public void setsField06(String sField06) {
        this.sField06.set(sField06); ;
    }
}