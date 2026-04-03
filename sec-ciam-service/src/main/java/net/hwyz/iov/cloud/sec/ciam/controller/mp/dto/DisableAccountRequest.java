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
public class DisableAccountRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String adminId;
    private boolean invalidateSessions;
}
