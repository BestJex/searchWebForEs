package com.gy.cpcsearch.entity;

public class FieldInfo {
    private Integer id;

    private String fTableName;

    private String fName;

    private String fType;

    private String fDes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getfTableName() {
        return fTableName;
    }

    public void setfTableName(String fTableName) {
        this.fTableName = fTableName == null ? null : fTableName.trim();
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName == null ? null : fName.trim();
    }

    public String getfType() {
        return fType;
    }

    public void setfType(String fType) {
        this.fType = fType == null ? null : fType.trim();
    }

    public String getfDes() {
        return fDes;
    }

    public void setfDes(String fDes) {
        this.fDes = fDes == null ? null : fDes.trim();
    }
}