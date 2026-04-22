package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.SessionPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话表 Mapper。
 */
@Mapper
public interface CiamSessionMapper extends BaseMapper<SessionPo> {
}
