package net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamOAuthClientDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 接入应用表 Mapper。
 */
@Mapper
public interface CiamOAuthClientMapper extends BaseMapper<CiamOAuthClientDo> {
}
