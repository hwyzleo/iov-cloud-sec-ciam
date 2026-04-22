package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.OAuthClientPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接入应用表 Mapper。
 */
@Mapper
public interface CiamOAuthClientMapper extends BaseMapper<OAuthClientPo> {
}
