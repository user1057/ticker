package com.example.domain;

import java.math.BigDecimal;

public class ConfigData {
    private String paramname;
    private BigDecimal numval;
    private String stringval;

    public String getParamname() {
        return paramname;
    }

    public void setParamname(String paramname) {
        this.paramname = paramname;
    }

    public BigDecimal getNumval() {
        return numval;
    }

    public void setNumval(BigDecimal numval) {
        this.numval = numval;
    }

    public String getStringval() {
        return stringval;
    }

    public void setStringval(String stringval) {
        this.stringval = stringval;
    }

    public ConfigData(String paramname, BigDecimal numval, String stringval) {
        this.paramname = paramname;
        this.numval = numval;
        this.stringval = stringval;
    }
}
