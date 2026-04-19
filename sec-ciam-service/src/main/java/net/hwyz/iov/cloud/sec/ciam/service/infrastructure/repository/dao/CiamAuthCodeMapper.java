package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamAuthCodeDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 授权码记录表 Mapper。
 */
@Mapper
public interface CiamAuthCodeMapper extends BaseMapper<CiamAuthCodeDo> {
}
