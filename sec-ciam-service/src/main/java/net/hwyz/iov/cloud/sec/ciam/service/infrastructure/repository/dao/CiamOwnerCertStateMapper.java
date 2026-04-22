package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.OwnerCertStatePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车主认证状态表 Mapper。
 */
@Mapper
public interface CiamOwnerCertStateMapper extends BaseMapper<OwnerCertStatePo> {
}
