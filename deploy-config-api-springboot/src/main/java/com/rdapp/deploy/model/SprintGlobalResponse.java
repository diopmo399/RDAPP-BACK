package com.rdapp.deploy.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SprintGlobalResponse {

    private SprintInfo sprint;
    private List<SprintTicket> notStarted;
    private List<SprintTicket> inProgress;
    private List<SprintTicket> done;
    private Map<String, SquadInfo> squads;
    private List<AffectVersionInfo> versions;
    private Double totalPoints;
    private Double donePoints;
    private Instant lastSync;

    public static class SquadInfo {
        private String id;
        private String name;
        private String color;

        public SquadInfo() {}
        public SquadInfo(String id, String name, String color) {
            this.id = id; this.name = name; this.color = color;
        }

        public String getId()    { return id; }
        public String getName()  { return name; }
        public String getColor() { return color; }
    }

    public SprintInfo getSprint()                  { return sprint; }
    public void setSprint(SprintInfo v)            { this.sprint = v; }

    public List<SprintTicket> getNotStarted()      { return notStarted; }
    public void setNotStarted(List<SprintTicket> v) { this.notStarted = v; }

    public List<SprintTicket> getInProgress()      { return inProgress; }
    public void setInProgress(List<SprintTicket> v) { this.inProgress = v; }

    public List<SprintTicket> getDone()            { return done; }
    public void setDone(List<SprintTicket> v)      { this.done = v; }

    public Map<String, SquadInfo> getSquads()      { return squads; }
    public void setSquads(Map<String, SquadInfo> v) { this.squads = v; }

    public List<AffectVersionInfo> getVersions()       { return versions; }
    public void setVersions(List<AffectVersionInfo> v) { this.versions = v; }

    public Double getTotalPoints()                    { return totalPoints; }
    public void setTotalPoints(Double v)              { this.totalPoints = v; }

    public Double getDonePoints()                     { return donePoints; }
    public void setDonePoints(Double v)               { this.donePoints = v; }

    public Instant getLastSync()                   { return lastSync; }
    public void setLastSync(Instant v)             { this.lastSync = v; }
}
