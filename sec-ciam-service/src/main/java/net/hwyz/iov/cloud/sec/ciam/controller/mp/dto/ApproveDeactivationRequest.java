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
public class ApproveDeactivationRequest {
    @NotBlank
    private String deactivationRequestId;
    @NotBlank
    private String reviewer;
}
