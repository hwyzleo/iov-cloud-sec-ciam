package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamSessionDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话表 Mapper。
 */
@Mapper
public interface CiamSessionMapper extends BaseMapper<CiamSessionDo> {
}
