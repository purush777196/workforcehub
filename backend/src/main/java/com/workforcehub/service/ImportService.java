package com.workforcehub.service;

import com.workforcehub.dto.importing.*;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface ImportService {
    ImportPreviewResponseDto previewImport(ImportPayloadDto payload, Authentication auth);
    ImportResultDto executeImport(ImportExecuteRequestDto request, Authentication auth);
    ImportJobDto getJobStatus(UUID jobId);
}
