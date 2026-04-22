package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.MfaChallengePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * MFA 挑战表 Mapper。
 */
@Mapper
public interface CiamMfaChallengeMapper extends BaseMapper<MfaChallengePo> {
}
