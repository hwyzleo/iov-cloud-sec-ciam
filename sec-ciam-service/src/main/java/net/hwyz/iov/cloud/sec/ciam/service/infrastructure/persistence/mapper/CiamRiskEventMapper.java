package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.RiskEventPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险事件表 Mapper。
 */
@Mapper
public interface CiamRiskEventMapper extends BaseMapper<RiskEventPo> {
}
