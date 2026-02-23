package com.rdapp.deploy.service;

import com.rdapp.deploy.dto.SquadDto;
import com.rdapp.deploy.entity.Squad;
import com.rdapp.deploy.entity.SquadMember;
import com.rdapp.deploy.repository.SquadMemberRepository;
import com.rdapp.deploy.repository.SquadRepository;
import com.rdapp.deploy.jira.service.JiraSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SquadService {

    private final SquadRepository squadRepo;
    private final SquadMemberRepository memberRepo;
    private final JiraSyncService jiraSyncService;
    private final GitHubDispatchService ghDispatch;

    // ══════════════════════════════════════════
    // Squads
    // ══════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SquadDto.Response> findAll() {
        return squadRepo.findAllWithMembers().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SquadDto.Response findById(String id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public SquadDto.Response create(SquadDto.Create dto) {
        var squad = Squad.builder()
                .id("sq-" + UUID.randomUUID().toString().substring(0, 8))
                .name(dto.getName())
                .color(dto.getColor())
                .boardId(dto.getBoardId())
                .members(new ArrayList<>())
                .build();

        if (dto.getMembers() != null) {
            dto.getMembers().forEach(m -> squad.addMember(toMemberEntity(m)));
        }

        return toResponse(squadRepo.save(squad));
    }

    @Transactional
    public SquadDto.Response update(String id, SquadDto.Update dto) {
        var squad = getOrThrow(id);
        if (dto.getName() != null)    squad.setName(dto.getName());
        if (dto.getColor() != null)   squad.setColor(dto.getColor());
        if (dto.getBoardId() != null)  squad.setBoardId(dto.getBoardId());
        return toResponse(squadRepo.save(squad));
    }

    @Transactional
    public void delete(String id) {
        if (!squadRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Escouade non trouvée: " + id);
        }
        squadRepo.deleteById(id);
    }

    // ══════════════════════════════════════════
    // Members
    // ══════════════════════════════════════════

    @Transactional
    public SquadDto.MemberResponse addMember(String squadId, SquadDto.MemberCreate dto) {
        var squad = getOrThrow(squadId);
        var member = toMemberEntity(dto);
        squad.addMember(member);
        squadRepo.save(squad);
        return toMemberResponse(member);
    }

    @Transactional
    public SquadDto.MemberResponse updateMember(String squadId, String memberId, SquadDto.MemberUpdate dto) {
        var squad = getOrThrow(squadId);
        var member = squad.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé: " + memberId));

        if (dto.getName() != null)         member.setName(dto.getName());
        if (dto.getRole() != null)         member.setRole(dto.getRole());
        if (dto.getEmployeeCode() != null) member.setEmployeeCode(dto.getEmployeeCode());
        if (dto.getUsername() != null)      member.setUsername(dto.getUsername());
        if (dto.getGithub() != null)       member.setGithub(dto.getGithub());

        squadRepo.save(squad);
        return toMemberResponse(member);
    }

    @Transactional
    public void removeMember(String squadId, String memberId) {
        var squad = getOrThrow(squadId);
        var member = squad.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre non trouvé: " + memberId));
        squad.removeMember(member);
        squadRepo.save(squad);
    }

    // ══════════════════════════════════════════
    // Board Sync
    // ══════════════════════════════════════════

    @Transactional
    public SquadDto.BoardSyncResponse syncBoard(String squadId, SquadDto.BoardSyncRequest request) {
        var squad = getOrThrow(squadId);
        squad.setBoardId(request.getBoardId());
        squadRepo.save(squad);

        log.info("Board sync — squad={} boardId={}", squad.getName(), request.getBoardId());

        // Stratégie : GHA dispatch (async) si configuré, sinon appel direct Jira
        if (ghDispatch.isConfigured()) {
            try {
                ghDispatch.triggerSquadSync(squadId, request.getBoardId());
                return SquadDto.BoardSyncResponse.builder()
                        .squadId(squadId)
                        .boardId(request.getBoardId())
                        .success(true)
                        .message("Sync déclenché via GitHub Actions (async)")
                        .syncedAt(LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                log.warn("GHA dispatch failed, fallback Jira direct: {}", e.getMessage());
            }
        }

        // Fallback : appel direct Jira DC
        try {
            var result = jiraSyncService.syncSquadSprint(squadId);
            var activeName = result.getActiveSprint() != null ? result.getActiveSprint().getName() : "aucun";
            return SquadDto.BoardSyncResponse.builder()
                    .squadId(squadId)
                    .boardId(request.getBoardId())
                    .success(true)
                    .message("Synchronisé — sprint actif: " + activeName)
                    .syncedAt(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.warn("Jira sync failed for squad {}: {}", squad.getName(), e.getMessage());
            return SquadDto.BoardSyncResponse.builder()
                    .squadId(squadId)
                    .boardId(request.getBoardId())
                    .success(false)
                    .message("Board ID sauvegardé, mais sync échoué: " + e.getMessage())
                    .syncedAt(LocalDateTime.now())
                    .build();
        }
    }

    // ══════════════════════════════════════════
    // Mapping
    // ══════════════════════════════════════════

    private SquadDto.Response toResponse(Squad s) {
        return SquadDto.Response.builder()
                .id(s.getId())
                .name(s.getName())
                .color(s.getColor())
                .boardId(s.getBoardId())
                .members(s.getMembers().stream().map(this::toMemberResponse).toList())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private SquadDto.MemberResponse toMemberResponse(SquadMember m) {
        return SquadDto.MemberResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .role(m.getRole())
                .employeeCode(m.getEmployeeCode())
                .username(m.getUsername())
                .github(m.getGithub())
                .build();
    }

    private SquadMember toMemberEntity(SquadDto.MemberCreate dto) {
        return SquadMember.builder()
                .id("m-" + UUID.randomUUID().toString().substring(0, 8))
                .name(dto.getName())
                .role(dto.getRole())
                .employeeCode(dto.getEmployeeCode())
                .username(dto.getUsername())
                .github(dto.getGithub())
                .build();
    }

    private Squad getOrThrow(String id) {
        return squadRepo.findByIdWithMembers(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escouade non trouvée: " + id));
    }
}
