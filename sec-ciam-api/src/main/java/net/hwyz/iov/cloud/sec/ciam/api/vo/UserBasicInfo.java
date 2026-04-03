package net.hwyz.iov.cloud.sec.ciam.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户基础信息（供外部服务查询）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfo {

    /** 用户业务唯一标识 */
    private String userId;

    /** 用户状态 */
    private Integer userStatus;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatarUrl;

    /** 认证标签列表 */
    private List<String> tags;
}
