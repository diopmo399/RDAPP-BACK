package com.rdapp.deploy.controller;

import com.rdapp.deploy.dto.SquadDto;
import com.rdapp.deploy.service.SquadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService service;

    // ══════════════════════════════════════════
    // Squads CRUD
    // ══════════════════════════════════════════

    @GetMapping
    public List<SquadDto.Response> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SquadDto.Response findById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SquadDto.Response create(@Valid @RequestBody SquadDto.Create dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}")
    public SquadDto.Response update(@PathVariable String id,
                                    @Valid @RequestBody SquadDto.Update dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    // ══════════════════════════════════════════
    // Members
    // ══════════════════════════════════════════

    @PostMapping("/{squadId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public SquadDto.MemberResponse addMember(@PathVariable String squadId,
                                             @Valid @RequestBody SquadDto.MemberCreate dto) {
        return service.addMember(squadId, dto);
    }

    @PatchMapping("/{squadId}/members/{memberId}")
    public SquadDto.MemberResponse updateMember(@PathVariable String squadId,
                                                @PathVariable String memberId,
                                                @Valid @RequestBody SquadDto.MemberUpdate dto) {
        return service.updateMember(squadId, memberId, dto);
    }

    @DeleteMapping("/{squadId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable String squadId,
                             @PathVariable String memberId) {
        service.removeMember(squadId, memberId);
    }

    // ══════════════════════════════════════════
    // Board Sync
    // ══════════════════════════════════════════

    @PostMapping("/{squadId}/sync")
    public SquadDto.BoardSyncResponse syncBoard(@PathVariable String squadId,
                                                @Valid @RequestBody SquadDto.BoardSyncRequest request) {
        return service.syncBoard(squadId, request);
    }
}
