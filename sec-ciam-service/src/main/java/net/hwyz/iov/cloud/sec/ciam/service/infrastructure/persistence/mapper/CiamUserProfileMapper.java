package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserProfilePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料扩展表 Mapper。
 */
@Mapper
public interface CiamUserProfileMapper extends BaseMapper<UserProfilePo> {
}
