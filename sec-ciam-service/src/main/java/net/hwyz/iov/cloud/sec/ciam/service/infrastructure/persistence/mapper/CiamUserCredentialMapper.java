package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserCredentialPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭据表 Mapper。
 */
@Mapper
public interface CiamUserCredentialMapper extends BaseMapper<UserCredentialPo> {
}
