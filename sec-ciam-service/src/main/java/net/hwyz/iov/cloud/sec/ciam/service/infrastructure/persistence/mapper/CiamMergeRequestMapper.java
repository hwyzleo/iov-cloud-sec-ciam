package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.MergeRequestPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 账号合并申请表 Mapper。
 */
@Mapper
public interface CiamMergeRequestMapper extends BaseMapper<MergeRequestPo> {
}
