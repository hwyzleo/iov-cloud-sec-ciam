package net.hwyz.iov.cloud.sec.ciam.service.controller.mpt.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectDeactivationRequest {
    @NotBlank
    private String deactivationRequestId;
    @NotBlank
    private String reviewer;
}
