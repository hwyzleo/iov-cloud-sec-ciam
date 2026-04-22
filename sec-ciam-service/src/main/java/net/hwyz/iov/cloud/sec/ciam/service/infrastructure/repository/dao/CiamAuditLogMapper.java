package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.AuditLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志表 Mapper。
 */
@Mapper
public interface CiamAuditLogMapper extends BaseMapper<AuditLogPo> {
}
