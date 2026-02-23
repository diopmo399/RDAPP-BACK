package com.rdapp.deploy.service;

import com.rdapp.deploy.dto.AffectVersionDto;
import com.rdapp.deploy.entity.AffectVersion;
import com.rdapp.deploy.repository.AffectVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AffectVersionService {

    private final AffectVersionRepository repo;

    // ── Read ──

    @Transactional(readOnly = true)
    public List<AffectVersionDto.Response> findAll() {
        return repo.findAllByOrderByReleaseDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AffectVersionDto.Response findById(String id) {
        return toResponse(getOrThrow(id));
    }

    // ── Create ──

    @Transactional
    public AffectVersionDto.Response create(AffectVersionDto.Create dto) {
        var entity = AffectVersion.builder()
                .id("v-" + UUID.randomUUID().toString().substring(0, 8))
                .name(dto.getName())
                .status(dto.getStatus())
                .releaseDate(dto.getReleaseDate())
                .description(dto.getDescription())
                .build();
        return toResponse(repo.save(entity));
    }

    // ── Update ──

    @Transactional
    public AffectVersionDto.Response update(String id, AffectVersionDto.Update dto) {
        var entity = getOrThrow(id);
        if (dto.getName() != null)        entity.setName(dto.getName());
        if (dto.getStatus() != null)      entity.setStatus(dto.getStatus());
        if (dto.getReleaseDate() != null)  entity.setReleaseDate(dto.getReleaseDate());
        if (dto.getDescription() != null)  entity.setDescription(dto.getDescription());
        return toResponse(repo.save(entity));
    }

    // ── Delete ──

    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Version non trouvée: " + id);
        }
        repo.deleteById(id);
    }

    // ── Mapping ──

    private AffectVersionDto.Response toResponse(AffectVersion e) {
        return AffectVersionDto.Response.builder()
                .id(e.getId())
                .name(e.getName())
                .status(e.getStatus())
                .releaseDate(e.getReleaseDate())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private AffectVersion getOrThrow(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version non trouvée: " + id));
    }
}
