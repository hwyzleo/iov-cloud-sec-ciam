package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamUserProfileDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料扩展表 Mapper。
 */
@Mapper
public interface CiamUserProfileMapper extends BaseMapper<CiamUserProfileDo> {
}
