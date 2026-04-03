package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamMfaChallengeDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * MFA 挑战表 Mapper。
 */
@Mapper
public interface CiamMfaChallengeMapper extends BaseMapper<CiamMfaChallengeDo> {
}
