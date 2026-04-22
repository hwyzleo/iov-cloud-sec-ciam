package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * JWK 密钥表 Mapper。
 */
@Mapper
public interface CiamJwkMapper extends BaseMapper<JwkPo> {
}
