package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.InvitationRelationPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 邀请关系表 Mapper。
 */
@Mapper
public interface CiamInvitationRelationMapper extends BaseMapper<InvitationRelationPo> {
}
