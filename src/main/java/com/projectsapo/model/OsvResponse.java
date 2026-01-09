package com.projectsapo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Represents the response from the OSV API.
 *
 * @param vulns A list of vulnerabilities found for the queried package.
 */
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsvResponse(
        @JsonProperty("vulns") List<OsvVulnerability> vulns
) {
}
