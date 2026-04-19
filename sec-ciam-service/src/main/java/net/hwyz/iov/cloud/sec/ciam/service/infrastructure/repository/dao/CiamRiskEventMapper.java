package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamRiskEventDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险事件表 Mapper。
 */
@Mapper
public interface CiamRiskEventMapper extends BaseMapper<CiamRiskEventDo> {
}
