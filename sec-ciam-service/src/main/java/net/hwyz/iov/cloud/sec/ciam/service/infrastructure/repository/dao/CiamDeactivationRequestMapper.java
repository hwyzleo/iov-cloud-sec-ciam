package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamDeactivationRequestDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 注销申请表 Mapper。
 */
@Mapper
public interface CiamDeactivationRequestMapper extends BaseMapper<CiamDeactivationRequestDo> {
}
