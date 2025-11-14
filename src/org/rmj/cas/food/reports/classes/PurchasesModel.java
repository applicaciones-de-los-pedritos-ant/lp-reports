package org.rmj.cas.food.reports.classes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class PurchasesModel {

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
    private SimpleStringProperty sField11;
    private SimpleStringProperty sField12;
    private SimpleDoubleProperty nField01;
    private SimpleDoubleProperty nField02;
    private SimpleDoubleProperty nField03;
    private SimpleDoubleProperty lField01;
    private SimpleDoubleProperty lField02;
    private SimpleDoubleProperty lField03;

    /*
    *Model for Purchases Summary
     */
    PurchasesModel(String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08) {
        this.sField01 = new SimpleStringProperty(index01);
        this.sField02 = new SimpleStringProperty(index02);
        this.sField03 = new SimpleStringProperty(index03);
        this.sField04 = new SimpleStringProperty(index04);
        this.sField05 = new SimpleStringProperty(index05);
        this.sField06 = new SimpleStringProperty(index06);
        this.sField07 = new SimpleStringProperty(index07);
        this.lField01 = new SimpleDoubleProperty(Double.parseDouble(index08));
    }

    /*
    *Model for Purchases Detail
     */
    PurchasesModel(String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08, String index09, String index10, String index11) {
        this.sField01 = new SimpleStringProperty(index01);
        this.sField02 = new SimpleStringProperty(index02);
        this.sField03 = new SimpleStringProperty(index03);
        this.sField04 = new SimpleStringProperty(index04);
        this.sField05 = new SimpleStringProperty(index05);
        this.sField06 = new SimpleStringProperty(index06);
        this.sField07 = new SimpleStringProperty(index07);
        this.sField08 = new SimpleStringProperty(index08);
        this.nField01 = new SimpleDoubleProperty(Double.parseDouble(index09));
        this.lField01 = new SimpleDoubleProperty(Double.parseDouble(index10));
        this.lField02 = new SimpleDoubleProperty(Double.parseDouble(index11));
    }

    /*
    *Model for Purchases Receiving Detail
     */
    PurchasesModel(String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08, String index09, String index10, String index11,
            String index12, String index13, String index14, String index15) {
        this.sField01 = new SimpleStringProperty(index01);
        this.sField02 = new SimpleStringProperty(index02);
        this.sField03 = new SimpleStringProperty(index03);
        this.sField04 = new SimpleStringProperty(index04);
        this.sField05 = new SimpleStringProperty(index05);
        this.sField06 = new SimpleStringProperty(index06);
        this.sField07 = new SimpleStringProperty(index07);
        this.sField08 = new SimpleStringProperty(index08);
        this.sField09 = new SimpleStringProperty(index09);
        this.sField10 = new SimpleStringProperty(index10);
        this.sField11 = new SimpleStringProperty(index11);
        this.sField12 = new SimpleStringProperty(index12);
        this.nField01 = new SimpleDoubleProperty(Double.parseDouble(index13));
        this.nField02 = new SimpleDoubleProperty(Double.parseDouble(index14));
        this.nField03 = new SimpleDoubleProperty(Double.parseDouble(index15));
    }

    /*
    *Model for Purchases Receiving Summary
     */
    PurchasesModel(String index01, String index02, String index03, String index04, String index05,
            String index06, String index07, String index08, String index09) {
        this.sField01 = new SimpleStringProperty(index01);
        this.sField02 = new SimpleStringProperty(index02);
        this.sField03 = new SimpleStringProperty(index03);
        this.sField04 = new SimpleStringProperty(index04);
        this.sField05 = new SimpleStringProperty(index05);
        this.lField01 = new SimpleDoubleProperty(Double.parseDouble(index06));
        this.lField02 = new SimpleDoubleProperty(Double.parseDouble(index07));
        this.lField03 = new SimpleDoubleProperty(Double.parseDouble(index08));
        this.sField06 = new SimpleStringProperty(index09);
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

    public Double getnField01() {
        return nField01.get();
    }

    public void setnField01(String nField01) {
        this.nField01.set(Double.parseDouble(nField01));
    }

    public Double getnField02() {
        return nField02.get();
    }

    public void setnField02(String nField02) {
        this.nField02.set(Double.parseDouble(nField02));
    }

    public Double getnField03() {
        return nField03.get();
    }

    public void setnField03(String nField03) {
        this.nField03.set(Double.parseDouble(nField03));
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

    public String getsField06() {
        return sField06.get();
    }

    public void setsField06(String sField06) {
        this.sField06.set(sField06);
    }

    public String getsField07() {
        return sField07.get();
    }

    public void setsField07(String sField07) {
        this.sField07.set(sField07);
    }

    public String getsField08() {
        return sField08.get();
    }

    public void setsField08(String sField08) {
        this.sField08.set(sField08);
    }

    public String getsField09() {
        return sField09.get();
    }

    public void setsField09(String sField09) {
        this.sField09.set(sField09);
    }

    public String getsField10() {
        return sField10.get();
    }

    public void setsField10(String sField10) {
        this.sField10.set(sField10);
    }

    public String getsField11() {
        return sField11.get();
    }

    public void setsField11(String sField11) {
        this.sField11.set(sField11);
    }

    public String getsField12() {
        return sField12.get();
    }

    public void setsField12(String sField12) {
        this.sField12.set(sField12);
    }

}
