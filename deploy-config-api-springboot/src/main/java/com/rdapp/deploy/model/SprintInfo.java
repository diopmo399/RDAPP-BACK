package com.rdapp.deploy.model;

public class SprintInfo {

    private String name;
    private String startDate;
    private String endDate;
    private String state;

    public SprintInfo() {}

    public SprintInfo(String name, String startDate, String endDate, String state) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.state = state;
    }

    public String getName()      { return name; }
    public void setName(String v) { this.name = v; }

    public String getStartDate()      { return startDate; }
    public void setStartDate(String v) { this.startDate = v; }

    public String getEndDate()        { return endDate; }
    public void setEndDate(String v)  { this.endDate = v; }

    public String getState()          { return state; }
    public void setState(String v)    { this.state = v; }
}
