package com.rdapp.deploy.controller;

import com.rdapp.deploy.dto.AffectVersionDto;
import com.rdapp.deploy.service.AffectVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/versions")
@RequiredArgsConstructor
public class AffectVersionController {

    private final AffectVersionService service;

    @GetMapping
    public List<AffectVersionDto.Response> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AffectVersionDto.Response findById(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AffectVersionDto.Response create(@Valid @RequestBody AffectVersionDto.Create dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}")
    public AffectVersionDto.Response update(@PathVariable String id,
                                            @Valid @RequestBody AffectVersionDto.Update dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
