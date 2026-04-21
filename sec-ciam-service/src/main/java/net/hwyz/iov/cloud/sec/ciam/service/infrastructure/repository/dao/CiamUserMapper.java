package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 用户主表 Mapper。
 */
@Mapper
public interface CiamUserMapper extends BaseMapper<CiamUserDo> {

    /**
     * 联表搜索用户（带分页支持）
     */
    @Select("<script>" +
            "SELECT DISTINCT u.* FROM ciam_user u " +
            "LEFT JOIN ciam_user_identity ui ON u.user_id = ui.user_id AND ui.row_valid = 1 " +
            "LEFT JOIN ciam_user_profile up ON u.user_id = up.user_id AND up.row_valid = 1 " +
            "WHERE u.row_valid = 1 " +
            "<if test='userId != null and userId != \"\"'> AND u.user_id = #{userId} </if> " +
            "<if test='identityType != null and identityType != \"\"'> AND ui.identity_type = #{identityType} </if> " +
            "<if test='identityValue != null and identityValue != \"\"'> AND ui.identity_value LIKE CONCAT('%', #{identityValue}, '%') </if> " +
            "<if test='nickname != null and nickname != \"\"'> AND up.nickname LIKE CONCAT('%', #{nickname}, '%') </if> " +
            "<if test='registerSource != null and registerSource != \"\"'> AND u.register_source = #{registerSource} </if> " +
            "<if test='userStatus != null'> AND u.user_status = #{userStatus} </if> " +
            "<if test='startTime != null'> AND u.create_time &gt;= #{startTime} </if> " +
            "<if test='endTime != null'> AND u.create_time &lt;= #{endTime} </if> " +
            "</script>")
    List<CiamUserDo> searchUsers(@Param("userId") String userId,
                                 @Param("identityType") String identityType,
                                 @Param("identityValue") String identityValue,
                                 @Param("nickname") String nickname,
                                 @Param("registerSource") String registerSource,
                                 @Param("userStatus") Integer userStatus,
                                 @Param("startTime") OffsetDateTime startTime,
                                 @Param("endTime") OffsetDateTime endTime);
}
