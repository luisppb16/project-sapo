package com.projectsapo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents the response from the OSV API.
 *
 * @param vulns A list of vulnerabilities found for the queried package.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsvResponse(
        List<OsvVulnerability> vulns
) {
}
