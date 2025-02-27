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
    private SimpleStringProperty sField07;
    private SimpleStringProperty sField08;
    private SimpleStringProperty sField09;
    private SimpleStringProperty sField10;
    private SimpleDoubleProperty lField01;
    private SimpleDoubleProperty lField02;
    private SimpleDoubleProperty lField03;
    private SimpleDoubleProperty lField04;
    private SimpleDoubleProperty lField05;

    InventoryModel(String sindex00, String sindex01, String sindex02, String sindex03, String sindex04, String sindex05,
            String sindex06, Double dindex07, Double dindex08, Double dindex09, Double dindex10) {
        this.sField00 = new SimpleStringProperty(sindex00);
        this.sField01 = new SimpleStringProperty(sindex01);
        this.sField02 = new SimpleStringProperty(sindex02);
        this.sField03 = new SimpleStringProperty(sindex03);
        this.sField04 = new SimpleStringProperty(sindex04);
        this.sField05 = new SimpleStringProperty(sindex05);
        this.sField06 = new SimpleStringProperty(sindex06);
        this.lField01 = new SimpleDoubleProperty(dindex07);
        this.lField02 = new SimpleDoubleProperty(dindex08);
        this.lField03 = new SimpleDoubleProperty(dindex09);
        this.lField04 = new SimpleDoubleProperty(dindex10);
    }

    InventoryModel(String sindex00, String sindex01, String sindex02, String sindex03, String sindex04, String sindex05,
            String sindex06, String sindex07, Double dindex07, Double dindex08, Double dindex09, Double dindex10, Double dindex11
    ) {
        this.sField00 = new SimpleStringProperty(sindex00);
        this.sField01 = new SimpleStringProperty(sindex01);
        this.sField02 = new SimpleStringProperty(sindex02);
        this.sField03 = new SimpleStringProperty(sindex03);
        this.sField04 = new SimpleStringProperty(sindex04);
        this.sField05 = new SimpleStringProperty(sindex05);
        this.sField06 = new SimpleStringProperty(sindex06);
        this.sField07 = new SimpleStringProperty(sindex07);
        this.lField01 = new SimpleDoubleProperty(dindex07);
        this.lField02 = new SimpleDoubleProperty(dindex08);
        this.lField03 = new SimpleDoubleProperty(dindex09);
        this.lField04 = new SimpleDoubleProperty(dindex10);
        this.lField05 = new SimpleDoubleProperty(dindex11);
    }

    /*
    *Model for Inventory presentation
     */
    InventoryModel(String sindex00, String sindex01, String sindex02, String sindex03, String sindex04, String sindex05,
            String sindex06, Double dindex07, Double dindex08) {
        this.sField00 = new SimpleStringProperty(sindex00);
        this.sField01 = new SimpleStringProperty(sindex01);
        this.sField02 = new SimpleStringProperty(sindex02);
        this.sField03 = new SimpleStringProperty(sindex03);
        this.sField04 = new SimpleStringProperty(sindex04);
        this.sField05 = new SimpleStringProperty(sindex05);
        this.sField06 = new SimpleStringProperty(sindex06);
        this.lField01 = new SimpleDoubleProperty(dindex07);
        this.lField02 = new SimpleDoubleProperty(dindex08);
    }

    /*
    *Model for Inventory movement
     */
    InventoryModel(String sindex00, String sindex01, String sindex02, String sindex03, String sindex04, String sindex05,
            String sindex06, String sindex07, Double dindex08, Double dindex09, Double dindex10, Double dindex11) {
        this.sField00 = new SimpleStringProperty(sindex00);
        this.sField01 = new SimpleStringProperty(sindex01);
        this.sField02 = new SimpleStringProperty(sindex02);
        this.sField03 = new SimpleStringProperty(sindex03);
        this.sField04 = new SimpleStringProperty(sindex04);
        this.sField05 = new SimpleStringProperty(sindex05);
        this.sField06 = new SimpleStringProperty(sindex06);
        this.sField07 = new SimpleStringProperty(sindex07);
        this.lField01 = new SimpleDoubleProperty(dindex08);
        this.lField02 = new SimpleDoubleProperty(dindex09);
        this.lField03 = new SimpleDoubleProperty(dindex10);
        this.lField04 = new SimpleDoubleProperty(dindex11);
    }

    /*
    *Model for Inventory Ledger
     */
    InventoryModel(String sindex01, String sindex02, String sindex03, String sindex04, String sindex05,
            String sindex06, String sindex07, String sindex08, String sindex09, String sindex10,
            Double dIndex01, Double dIndex02, Double dIndex03) {
        this.sField01 = new SimpleStringProperty(sindex01);
        this.sField02 = new SimpleStringProperty(sindex02);
        this.sField03 = new SimpleStringProperty(sindex03);
        this.sField04 = new SimpleStringProperty(sindex04);
        this.sField05 = new SimpleStringProperty(sindex05);
        this.sField06 = new SimpleStringProperty(sindex06);
        this.sField07 = new SimpleStringProperty(sindex07);
        this.sField08 = new SimpleStringProperty(sindex08);
        this.sField09 = new SimpleStringProperty(sindex09);
        this.sField10 = new SimpleStringProperty(sindex10);
        this.lField01 = new SimpleDoubleProperty(dIndex01);
        this.lField02 = new SimpleDoubleProperty(dIndex02);
        this.lField03 = new SimpleDoubleProperty(dIndex03);
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

    public String getsField06() {
        return sField06.get();
    }

    public void setsField06(String sField06) {
        this.sField06.set(sField06);;
    }

    public String getsField07() {
        return sField07.get();
    }

    public void setsField07(String sField07) {
        this.sField07.set(sField07);;
    }

    public String getsField08() {
        return sField08.get();
    }

    public void setsField08(String sField08) {
        this.sField08.set(sField08);;
    }

    public String getsField09() {
        return sField09.get();
    }

    public void setsField09(String sField09) {
        this.sField09.set(sField09);;
    }

    public String getsField10() {
        return sField10.get();
    }

    public void setsField10(String sField10) {
        this.sField10.set(sField10);;
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

}
