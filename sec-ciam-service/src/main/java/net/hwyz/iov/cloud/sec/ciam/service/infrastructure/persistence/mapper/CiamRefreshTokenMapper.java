package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RefreshTokenPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.Instant;
import java.util.List;

/**
 * 刷新令牌表 Mapper。
 */
@Mapper
public interface CiamRefreshTokenMapper extends BaseMapper<RefreshTokenPo> {

    /**
     * 极简原生 SQL 搜索（物理分页支持）
     * 故意去掉所有 MP 风格的符号，确保 PageHelper 能精准识别这是一个标准 Select 语句
     */
    @Select("<script>" +
            "SELECT id, refresh_token_id, user_id, session_id, client_id, token_fingerprint, parent_token_id, token_status, issue_time, used_time, revoke_time, expire_time, description " +
            "FROM ciam_refresh_token " +
            "WHERE row_valid = 1 " +
            "<if test='refreshTokenId != null and refreshTokenId != \"\"'> AND refresh_token_id = #{refreshTokenId} </if> " +
            "<if test='userId != null and userId != \"\"'> AND user_id = #{userId} </if> " +
            "<if test='sessionId != null and sessionId != \"\"'> AND session_id = #{sessionId} </if> " +
            "<if test='clientId != null and clientId != \"\"'> AND client_id = #{clientId} </if> " +
            "<if test='tokenStatus != null'> AND token_status = #{tokenStatus} </if> " +
            "<if test='startTime != null'> AND issue_time &gt;= #{startTime} </if> " +
            "<if test='endTime != null'> AND issue_time &lt;= #{endTime} </if> " +
            "ORDER BY issue_time DESC" +
            "</script>")
    List<RefreshTokenPo> searchTokens(@Param("refreshTokenId") String refreshTokenId,
                                          @Param("userId") String userId,
                                          @Param("sessionId") String sessionId,
                                          @Param("clientId") String clientId,
                                          @Param("tokenStatus") Integer tokenStatus,
                                          @Param("startTime") Instant startTime,
                                          @Param("endTime") Instant endTime);
}
