package com.rdapp.deploy.mapper;

import com.rdapp.deploy.entity.AffectVersion;
import com.rdapp.deploy.entity.SprintIssue;
import com.rdapp.deploy.entity.SprintSync;
import com.rdapp.deploy.model.AffectVersionInfo;
import com.rdapp.deploy.model.SprintInfo;
import com.rdapp.deploy.model.SprintTicket;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir les entités Sprint en modèles de réponse API.
 */
@Component
public class SprintMapper {

    /**
     * Convertit une entité SprintSync en SprintInfo.
     */
    public SprintInfo toSprintInfo(SprintSync sprintSync) {
        if (sprintSync == null) {
            return null;
        }

        SprintInfo info = new SprintInfo();
        info.setName(sprintSync.getName());
        info.setState(sprintSync.getState());
        info.setStartDate(sprintSync.getStartDate() != null ?
            sprintSync.getStartDate().toString() : null);
        info.setEndDate(sprintSync.getEndDate() != null ?
            sprintSync.getEndDate().toString() : null);

        return info;
    }

    /**
     * Convertit une entité SprintIssue en SprintTicket.
     */
    public SprintTicket toSprintTicket(SprintIssue issue) {
        if (issue == null) {
            return null;
        }

        SprintTicket ticket = new SprintTicket();

        // Champs de base
        ticket.setTicket(issue.getIssueKey());
        ticket.setTitle(issue.getSummary());
        ticket.setStoryPoints(issue.getStoryPoints());
        ticket.setAuthor(issue.getAssigneeName());

        // Squad ID depuis la relation
        if (issue.getSprintSync() != null && issue.getSprintSync().getSquad() != null) {
            ticket.setSquad(issue.getSprintSync().getSquad().getId());
        }

        // Priority mapping
        ticket.setPriority(mapPriority(issue.getPriority()));

        // Status mapping basé sur statusCategory
        ticket.setStatus(mapStatus(issue.getStatusCategory()));

        // Champs spécifiques selon le statut
        if ("done".equalsIgnoreCase(issue.getStatusCategory())) {
            ticket.setVersion(issue.getFixVersion());
            ticket.setCompletedDate(issue.getResolutionDate());
        }

        // Affect Version - disponible pour tous les tickets
        ticket.setAffectVersion(issue.getAffectVersion());

        // TODO: Progress et branch peuvent être ajoutés depuis d'autres sources
        // ticket.setProgress(...);
        // ticket.setBranch(...);

        return ticket;
    }

    /**
     * Mappe la priorité Jira vers l'enum Priority.
     */
    private SprintTicket.Priority mapPriority(String jiraPriority) {
        if (jiraPriority == null) {
            return SprintTicket.Priority.MEDIUM;
        }

        String lower = jiraPriority.toLowerCase();
        if (lower.contains("highest") || lower.contains("critical")) {
            return SprintTicket.Priority.CRITICAL;
        } else if (lower.contains("high")) {
            return SprintTicket.Priority.HIGH;
        } else if (lower.contains("low") || lower.contains("lowest")) {
            return SprintTicket.Priority.LOW;
        }
        return SprintTicket.Priority.MEDIUM;
    }

    /**
     * Mappe la status category Jira vers l'enum Status.
     */
    private SprintTicket.Status mapStatus(String statusCategory) {
        if (statusCategory == null) {
            return SprintTicket.Status.NOT_STARTED;
        }

        String lower = statusCategory.toLowerCase();
        if (lower.contains("done")) {
            return SprintTicket.Status.DONE;
        } else if (lower.contains("indeterminate") || lower.contains("progress")) {
            return SprintTicket.Status.IN_PROGRESS;
        }
        return SprintTicket.Status.NOT_STARTED;
    }

    /**
     * Convertit une entité AffectVersion en AffectVersionInfo.
     */
    public AffectVersionInfo toAffectVersionInfo(AffectVersion version) {
        if (version == null) {
            return null;
        }

        AffectVersionInfo info = new AffectVersionInfo();
        info.setId(version.getId());
        info.setName(version.getName());
        info.setStatus(version.getStatus() != null ? version.getStatus().name() : null);
        info.setReleaseDate(version.getReleaseDate() != null ?
            version.getReleaseDate().toString() : null);
        info.setDescription(version.getDescription());

        return info;
    }
}
