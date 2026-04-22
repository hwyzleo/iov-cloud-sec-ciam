package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.AuthCode;

import java.util.Optional;

/**
 * 授权码记录表仓储接口。
 */
public interface CiamAuthCodeRepository {

    /** 根据授权码哈希查询 */
    Optional<AuthCode> findByCodeHash(String codeHash);

    /** 根据业务 ID 查询 */
    Optional<AuthCode> findByAuthCodeId(String authCodeId);

    /** 插入授权码记录 */
    int insert(AuthCode entity);

    /** 根据业务 ID 更新 */
    int updateByAuthCodeId(AuthCode entity);
}
