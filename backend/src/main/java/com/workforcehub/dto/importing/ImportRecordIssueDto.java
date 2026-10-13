package com.workforcehub.dto.importing;

public class ImportRecordIssueDto {
    private String sourceId;
    private String identifier;
    private String issueType; // 'EXISTING', 'CONFLICT', 'INVALID', 'UNRESOLVED_REF', 'INFO'
    private String message;

    public ImportRecordIssueDto() {}

    public ImportRecordIssueDto(String sourceId, String identifier, String issueType, String message) {
        this.sourceId = sourceId;
        this.identifier = identifier;
        this.issueType = issueType;
        this.message = message;
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
