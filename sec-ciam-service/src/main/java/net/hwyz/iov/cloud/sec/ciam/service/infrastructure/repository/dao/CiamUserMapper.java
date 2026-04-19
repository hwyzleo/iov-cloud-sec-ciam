package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户主表 Mapper。
 */
@Mapper
public interface CiamUserMapper extends BaseMapper<CiamUserDo> {
}
