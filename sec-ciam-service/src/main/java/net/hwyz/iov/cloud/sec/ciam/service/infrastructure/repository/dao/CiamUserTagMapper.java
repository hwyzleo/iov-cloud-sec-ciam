package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserTagDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 认证标签表 Mapper。
 */
@Mapper
public interface CiamUserTagMapper extends BaseMapper<CiamUserTagDo> {
}
