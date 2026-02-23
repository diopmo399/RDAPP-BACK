package com.rdapp.deploy.controller;

import com.rdapp.deploy.dto.BatchIngestDto.*;
import com.rdapp.deploy.service.BatchIngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints batch — appelés par GitHub Actions.
 *
 * POST /v1/batch/ingest         → Ingest une escouade
 * POST /v1/batch/ingest/bulk    → Ingest toutes les escouades d'un coup
 */
@RestController
@RequestMapping("/v1/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchIngestController {

    private final BatchIngestService service;

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public IngestResponse ingest(@Valid @RequestBody IngestPayload payload) {
        log.info("Batch ingest — squad={} runId={} triggeredBy={}",
                payload.getSquadId(), payload.getRunId(), payload.getTriggeredBy());
        return service.ingestSquad(payload);
    }

    @PostMapping("/ingest/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkIngestResponse ingestBulk(@Valid @RequestBody BulkIngestPayload payload) {
        log.info("Batch ingest bulk — {} squads runId={}", payload.getSquads().size(), payload.getRunId());
        return service.ingestBulk(payload);
    }
}
