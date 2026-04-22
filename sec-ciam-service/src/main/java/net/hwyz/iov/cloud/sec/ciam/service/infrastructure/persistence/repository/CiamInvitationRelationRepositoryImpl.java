package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamInvitationRelationRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.mapper.CiamInvitationRelationMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.InvitationRelationPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamInvitationRelationRepositoryImpl implements CiamInvitationRelationRepository {

    private final CiamInvitationRelationMapper mapper;

    @Override
    public Optional<InvitationRelationPo> findByRelationId(String relationId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getRelationId, relationId)));
    }

    @Override
    public Optional<InvitationRelationPo> findByInviteeUserId(String inviteeUserId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getInviteeUserId, inviteeUserId)
                        .eq(InvitationRelationPo::getRowValid, 1)));
    }

    @Override
    public List<InvitationRelationPo> findByInviterUserId(String inviterUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getInviterUserId, inviterUserId)
                        .eq(InvitationRelationPo::getRowValid, 1));
    }

    @Override
    public List<InvitationRelationPo> findByInviteChannelCode(String inviteChannelCode) {
        return mapper.selectList(
                new LambdaQueryWrapper<InvitationRelationPo>()
                        .eq(InvitationRelationPo::getInviteChannelCode, inviteChannelCode)
                        .eq(InvitationRelationPo::getRowValid, 1));
    }

    @Override
    public int insert(InvitationRelationPo entity) {
        return mapper.insert(entity);
    }
}
