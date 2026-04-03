package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserCredentialDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭据表 Mapper。
 */
@Mapper
public interface CiamUserCredentialMapper extends BaseMapper<CiamUserCredentialDo> {
}
