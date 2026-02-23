package com.rdapp.deploy.model;

public class AffectVersionInfo {

    private String id;
    private String name;
    private String status;
    private String releaseDate;
    private String description;

    public AffectVersionInfo() {}

    public AffectVersionInfo(String id, String name, String status, String releaseDate, String description) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.releaseDate = releaseDate;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String v) { this.releaseDate = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
}
