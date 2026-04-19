package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamUserIdentityDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录标识表 Mapper。
 */
@Mapper
public interface CiamUserIdentityMapper extends BaseMapper<CiamUserIdentityDo> {
}
