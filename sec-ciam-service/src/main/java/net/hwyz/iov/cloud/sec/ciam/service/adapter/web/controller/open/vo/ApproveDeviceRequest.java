package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.controller.open.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveDeviceRequest {
    @NotBlank
    private String userCode;
    @NotBlank
    private String userId;
}
