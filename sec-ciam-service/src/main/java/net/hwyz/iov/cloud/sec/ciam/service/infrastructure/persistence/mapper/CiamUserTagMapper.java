package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.UserTagPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 认证标签表 Mapper。
 */
@Mapper
public interface CiamUserTagMapper extends BaseMapper<UserTagPo> {
}
