package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuditLogPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志表 Mapper。
 */
@Mapper
public interface CiamAuditLogMapper extends BaseMapper<AuditLogPo> {
}
