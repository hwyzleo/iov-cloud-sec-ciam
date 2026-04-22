package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.AuthCodePo;

import java.util.Optional;

/**
 * 授权码记录表仓储接口。
 */
public interface CiamAuthCodeRepository {

    /** 根据授权码哈希查询 */
    Optional<AuthCodePo> findByCodeHash(String codeHash);

    /** 根据业务 ID 查询 */
    Optional<AuthCodePo> findByAuthCodeId(String authCodeId);

    /** 插入授权码记录 */
    int insert(AuthCodePo entity);

    /** 根据业务 ID 更新 */
    int updateByAuthCodeId(AuthCodePo entity);
}
