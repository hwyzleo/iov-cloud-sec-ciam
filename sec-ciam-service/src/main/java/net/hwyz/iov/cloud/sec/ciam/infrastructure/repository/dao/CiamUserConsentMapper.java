package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserConsentDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协议与营销同意表 Mapper。
 */
@Mapper
public interface CiamUserConsentMapper extends BaseMapper<CiamUserConsentDo> {
}
