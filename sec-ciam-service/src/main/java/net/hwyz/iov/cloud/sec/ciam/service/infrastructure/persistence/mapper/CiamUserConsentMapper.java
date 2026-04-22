package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserConsentPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协议与营销同意表 Mapper。
 */
@Mapper
public interface CiamUserConsentMapper extends BaseMapper<UserConsentPo> {
}
