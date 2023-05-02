/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.rmj.cas.food.reports.classes;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author User
 */
public class DailyVsRequest {
    private SimpleStringProperty index01;
    private SimpleStringProperty index02;
    private SimpleStringProperty index03;
    public DailyVsRequest(){

    }

    public void setIndex01(String strDlyTotal) {
        this.index01 = new SimpleStringProperty(strDlyTotal);
    }

    public String getIndex01() {
        return index01.get();
    }

    public void setIndex02(String strReqTotal) {
        this.index02 = new SimpleStringProperty(strReqTotal);
    }

    public String getIndex02() {
        return index02.get();
    }
    public String getIndex03() {
        return index03.get();
    }

    public void setIndex03(String index03) {
        this.index03 = new SimpleStringProperty(index03);
    }

}
