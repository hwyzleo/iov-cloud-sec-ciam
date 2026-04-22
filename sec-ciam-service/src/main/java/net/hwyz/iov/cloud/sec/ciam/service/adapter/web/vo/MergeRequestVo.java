package net.hwyz.iov.cloud.sec.ciam.service.adapter.web.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 账号合并申请 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequestVo {

    @JsonProperty("merge_request_id")
    private String mergeRequestId;

    @JsonProperty("source_user_id")
    private String sourceUserId;

    @JsonProperty("target_user_id")
    private String targetUserId;

    @JsonProperty("review_status")
    private Integer reviewStatus;

    @JsonProperty("request_source")
    private String requestSource;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("request_time")
    private Instant requestTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("review_time")
    private Instant reviewTime;

    @JsonProperty("reviewer")
    private String reviewer;

    @JsonProperty("remark")
    private String remark;

    @JsonProperty("description")
    private String description;
}
