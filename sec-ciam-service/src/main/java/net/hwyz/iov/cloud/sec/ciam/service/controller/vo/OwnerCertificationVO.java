package net.hwyz.iov.cloud.sec.ciam.service.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 车主认证 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerCertificationVo {

    @JsonProperty("owner_cert_id")
    private String ownerCertId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("vehicle_id")
    private String vehicleId;

    @JsonProperty("vin")
    private String vin;

    @JsonProperty("cert_status")
    private Integer certStatus;

    @JsonProperty("cert_source")
    private String certSource;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("callback_time")
    private Instant callbackTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("last_query_time")
    private Instant lastQueryTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("effective_time")
    private Instant effectiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonProperty("expire_time")
    private Instant expireTime;

    @JsonProperty("result_message")
    private String resultMessage;

    @JsonProperty("description")
    private String description;
}
