package org.rmj.cas.food.reports.classes;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class DailyProductionModel {

    private SimpleObjectProperty sField01;
    private SimpleObjectProperty sField02;
    private SimpleObjectProperty sField03;
    private SimpleObjectProperty sField04;
    private SimpleObjectProperty sField05;
    private SimpleObjectProperty sField06;
    private SimpleObjectProperty sField07;
    private SimpleObjectProperty sField08;
    private SimpleObjectProperty sField09;
    private SimpleObjectProperty sField10;
    private SimpleObjectProperty nField01;
    private SimpleObjectProperty nField02;
    private SimpleObjectProperty nField03;
    private SimpleObjectProperty nField04;
    private SimpleObjectProperty nField05;

    //summarized production
    DailyProductionModel(String sField01, String sField02, String sField03, String sField04, String sField05,
            Double nField01, Double nField02, Double nField03, Double nField04) {
        this.sField01 = new SimpleObjectProperty(sField01);
        this.sField02 = new SimpleObjectProperty(sField02);
        this.sField03 = new SimpleObjectProperty(sField03);
        this.sField04 = new SimpleObjectProperty(sField04);
        this.sField05 = new SimpleObjectProperty(sField05);
        this.nField01 = new SimpleObjectProperty(nField01);
        this.nField02 = new SimpleObjectProperty(nField02);
        this.nField03 = new SimpleObjectProperty(nField03);
        this.nField04 = new SimpleObjectProperty(nField04);
    }

    //detailed production
    DailyProductionModel(String sField01, String sField02, String sField03, String sField04, String sField05,
            String sField06,String sField07,String sField08,
            Double nField01, Double nField02, Double nField03, Double nField04) {
        this.sField01 = new SimpleObjectProperty(sField01);
        this.sField02 = new SimpleObjectProperty(sField02);
        this.sField03 = new SimpleObjectProperty(sField03);
        this.sField04 = new SimpleObjectProperty(sField04);
        this.sField05 = new SimpleObjectProperty(sField05);
        this.sField06 = new SimpleObjectProperty(sField06);
        this.sField07 = new SimpleObjectProperty(sField07);
        this.sField08 = new SimpleObjectProperty(sField08);
        this.nField01 = new SimpleObjectProperty(nField01);
        this.nField02 = new SimpleObjectProperty(nField02);
        this.nField03 = new SimpleObjectProperty(nField03);
        this.nField04 = new SimpleObjectProperty(nField04);
    }

    public Object getsField01() {
        return sField01.get();
    }

    public void setsField01(String sField01) {
        this.sField01.set(sField01);
    }

    public Object getsField02() {
        return sField02.get();
    }

    public void setsField02(Object sField02) {
        this.sField02.set(sField02);
    }

    public Object getsField03() {
        return sField03.get();
    }

    public void setsField03(Object sField03) {
        this.sField03.set(sField03);
    }

    public Object getsField04() {
        return sField04.get();
    }

    public void setsField04(Object sField04) {
        this.sField04.set(sField04);
    }

    public Object getsField05() {
        return sField05.get();
    }

    public void setsField05(Object sField05) {
        this.sField05.set(sField05);
    }

    public Object getsField06() {
        return sField06.get();
    }

    public void setsField06(Object sField06) {
        this.sField06.set(sField06);;
    }

    public Object getsField07() {
        return sField07.get();
    }

    public void setsField07(Object sField07) {
        this.sField07.set(sField07);;
    }

    public Object getsField08() {
        return sField08.get();
    }

    public void setsField08(Object sField08) {
        this.sField08.set(sField08);;
    }

    public Object getsField09() {
        return sField09.get();
    }

    public void setsField09(Object sField09) {
        this.sField09.set(sField09);;
    }

    public Object getsField10() {
        return sField10.get();
    }

    public void setsField10(Object sField10) {
        this.sField10.set(sField10);;
    }

    public Object getnField01() {
        return nField01.get();
    }

    public void setnField01(Object nField01) {
        this.nField01.set(nField01);
    }

    public Object getnField02() {
        return nField02.get();
    }

    public void setnField02(Object nField02) {
        this.nField02.set(nField02);
    }

    public Object getnField03() {
        return nField03.get();
    }

    public void setnField03(Object nField03) {
        this.nField03.set(nField03);
    }

    public Object getnField04() {
        return nField04.get();
    }

    public void setnField04(Object nField04) {
        this.nField04.set(nField04);
    }

    public Object getnField05() {
        return nField05.get();
    }

    public void setnField05(Object nField05) {
        this.nField05.set(nField05);
    }

}
