package net.hwyz.iov.cloud.sec.ciam.controller.mp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveMergeRequest {
    @NotBlank
    private String mergeRequestId;
    @NotBlank
    private String reviewer;
}
