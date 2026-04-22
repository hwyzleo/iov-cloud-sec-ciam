package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.infrastructure.persistence.po.JwkPo;

import java.util.List;
import java.util.Optional;

/**
 * JWK 密钥表仓储接口。
 */
public interface CiamJwkRepository {

    /** 根据密钥 ID 查询 */
    Optional<JwkPo> findByKeyId(String keyId);

    /** 查询主密钥（用于签名新 token） */
    Optional<JwkPo> findPrimary();

    /** 查询所有激活状态的密钥（用于验证 token 和 JWKS 端点） */
    List<JwkPo> findAllActive();

    /** 插入密钥记录 */
    int insert(JwkPo entity);

    /** 更新密钥 */
    int update(JwkPo entity);

    /** 撤销主密钥状态（将指定密钥设为非主密钥） */
    int revokePrimary();
}
