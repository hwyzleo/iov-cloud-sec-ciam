package net.hwyz.iov.cloud.sec.ciam.service.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 账号注销申请 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeactivationRequestVO {

    @JsonProperty("deactivation_request_id")
    private String deactivationRequestId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("review_status")
    private Integer reviewStatus;

    @JsonProperty("request_reason")
    private String requestReason;

    @JsonProperty("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("request_time")
    private Instant requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("review_time")
    private Instant reviewTime;

    @JsonProperty("reviewer")
    private String reviewer;

    @JsonProperty("description")
    private String description;
}
