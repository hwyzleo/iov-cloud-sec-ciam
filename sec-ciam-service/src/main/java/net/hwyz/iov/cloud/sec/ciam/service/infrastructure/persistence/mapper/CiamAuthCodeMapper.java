package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuthCodePo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权码记录表 Mapper。
 */
@Mapper
public interface CiamAuthCodeMapper extends BaseMapper<AuthCodePo> {
}
