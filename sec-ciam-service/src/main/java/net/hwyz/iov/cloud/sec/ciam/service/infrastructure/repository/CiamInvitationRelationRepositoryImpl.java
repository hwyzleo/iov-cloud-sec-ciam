package net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.sec.ciam.service.domain.repository.CiamInvitationRelationRepository;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.CiamInvitationRelationMapper;
import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.repository.dao.dataobject.CiamInvitationRelationDo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CiamInvitationRelationRepositoryImpl implements CiamInvitationRelationRepository {

    private final CiamInvitationRelationMapper mapper;

    @Override
    public Optional<CiamInvitationRelationDo> findByRelationId(String relationId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamInvitationRelationDo>()
                        .eq(CiamInvitationRelationDo::getRelationId, relationId)));
    }

    @Override
    public Optional<CiamInvitationRelationDo> findByInviteeUserId(String inviteeUserId) {
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<CiamInvitationRelationDo>()
                        .eq(CiamInvitationRelationDo::getInviteeUserId, inviteeUserId)
                        .eq(CiamInvitationRelationDo::getRowValid, 1)));
    }

    @Override
    public List<CiamInvitationRelationDo> findByInviterUserId(String inviterUserId) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamInvitationRelationDo>()
                        .eq(CiamInvitationRelationDo::getInviterUserId, inviterUserId)
                        .eq(CiamInvitationRelationDo::getRowValid, 1));
    }

    @Override
    public List<CiamInvitationRelationDo> findByInviteChannelCode(String inviteChannelCode) {
        return mapper.selectList(
                new LambdaQueryWrapper<CiamInvitationRelationDo>()
                        .eq(CiamInvitationRelationDo::getInviteChannelCode, inviteChannelCode)
                        .eq(CiamInvitationRelationDo::getRowValid, 1));
    }

    @Override
    public int insert(CiamInvitationRelationDo entity) {
        return mapper.insert(entity);
    }
}
