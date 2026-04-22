package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.JwkPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * JWK 密钥表 Mapper。
 */
@Mapper
public interface CiamJwkMapper extends BaseMapper<JwkPo> {
}
