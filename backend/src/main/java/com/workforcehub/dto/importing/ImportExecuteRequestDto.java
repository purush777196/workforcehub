package com.workforcehub.dto.importing;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ImportExecuteRequestDto {
    private UUID previewId;
    private String conflictStrategy = "SKIP_EXISTING"; // 'SKIP_EXISTING', 'FAIL_ON_CONFLICT'
    
    @NotNull(message = "Explicit confirmation of database backup is required before committing import")
    private Boolean confirmBackupTaken;

    private ImportPayloadDto payload;

    public ImportExecuteRequestDto() {}

    public UUID getPreviewId() { return previewId; }
    public void setPreviewId(UUID previewId) { this.previewId = previewId; }

    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }

    public Boolean getConfirmBackupTaken() { return confirmBackupTaken; }
    public void setConfirmBackupTaken(Boolean confirmBackupTaken) { this.confirmBackupTaken = confirmBackupTaken; }

    public ImportPayloadDto getPayload() { return payload; }
    public void setPayload(ImportPayloadDto payload) { this.payload = payload; }
}
