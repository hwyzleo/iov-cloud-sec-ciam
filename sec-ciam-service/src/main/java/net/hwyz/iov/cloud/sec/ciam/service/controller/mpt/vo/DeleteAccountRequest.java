package net.hwyz.iov.cloud.sec.ciam.service.controller.mpt.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 删除账号请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteAccountRequest {

    @NotEmpty
    private List<String> userId;
}
