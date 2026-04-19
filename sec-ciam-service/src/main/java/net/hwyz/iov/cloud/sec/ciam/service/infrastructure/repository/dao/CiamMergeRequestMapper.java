package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamMergeRequestDo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号合并申请表 Mapper。
 */
@Mapper
public interface CiamMergeRequestMapper extends BaseMapper<CiamMergeRequestDo> {
}
