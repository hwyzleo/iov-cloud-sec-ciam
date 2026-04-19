package net.hwyz.iov.cloud.sec.ciam.service.controller.mp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectMergeRequest {
    @NotBlank
    private String mergeRequestId;
    @NotBlank
    private String reviewer;
}
