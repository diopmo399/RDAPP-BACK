package com.rdapp.deploy.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SprintTicket {

    public enum Status { NOT_STARTED, IN_PROGRESS, DONE }
    public enum Priority { CRITICAL, HIGH, MEDIUM, LOW }

    private String ticket;
    private String title;
    private String squad;
    private Double storyPoints;
    private Priority priority;
    private String author;
    private String app;
    private Status status;

    // In Progress fields
    private Integer progress;
    private String branch;

    // Done fields
    private String completedDate;
    private String version;

    // Version where the bug/issue was found (affect version)
    private String affectVersion;

    public SprintTicket() {}

    public String getTicket()       { return ticket; }
    public void setTicket(String v) { this.ticket = v; }

    public String getTitle()        { return title; }
    public void setTitle(String v)  { this.title = v; }

    public String getSquad()        { return squad; }
    public void setSquad(String v)  { this.squad = v; }

    public Double getStoryPoints()     { return storyPoints; }
    public void setStoryPoints(Double v) { this.storyPoints = v; }

    public Priority getPriority()   { return priority; }
    public void setPriority(Priority v) { this.priority = v; }

    public String getAuthor()       { return author; }
    public void setAuthor(String v) { this.author = v; }

    public String getApp()          { return app; }
    public void setApp(String v)    { this.app = v; }

    public Status getStatus()       { return status; }
    public void setStatus(Status v) { this.status = v; }

    public Integer getProgress()    { return progress; }
    public void setProgress(Integer v) { this.progress = v; }

    public String getBranch()       { return branch; }
    public void setBranch(String v) { this.branch = v; }

    public String getCompletedDate()       { return completedDate; }
    public void setCompletedDate(String v) { this.completedDate = v; }

    public String getVersion()      { return version; }
    public void setVersion(String v) { this.version = v; }

    public String getAffectVersion()      { return affectVersion; }
    public void setAffectVersion(String v) { this.affectVersion = v; }
}
