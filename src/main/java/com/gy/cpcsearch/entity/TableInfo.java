package com.gy.cpcsearch.entity;

public class TableInfo {
    private Integer id;

    private String tName;

    private Integer tStatus;

    private String tAlias;

    private String tType;

    private String tTimeField;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String gettName() {
        return tName;
    }

    public void settName(String tName) {
        this.tName = tName == null ? null : tName.trim();
    }

    public Integer gettStatus() {
        return tStatus;
    }

    public void settStatus(Integer tStatus) {
        this.tStatus = tStatus;
    }

    public String gettAlias() {
        return tAlias;
    }

    public void settAlias(String tAlias) {
        this.tAlias = tAlias == null ? null : tAlias.trim();
    }

    public String gettType() {
        return tType;
    }

    public void settType(String tType) {
        this.tType = tType == null ? null : tType.trim();
    }

    public String gettTimeField() {
        return tTimeField;
    }

    public void settTimeField(String tTimeField) {
        this.tTimeField = tTimeField == null ? null : tTimeField.trim();
    }
}