package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Jwk;

import java.util.List;
import java.util.Optional;

/**
 * JWK 密钥仓储接口。
 */
public interface JwkRepository {

    /** 根据业务 ID 查询 */
    Optional<Jwk> findByKeyId(String keyId);

    /** 获取当前生效的主密钥 */
    Optional<Jwk> findPrimary();

    /** 获取所有活跃密钥 */
    List<Jwk> findAllActive();

    /** 插入密钥记录 */
    int insert(Jwk entity);

    /** 更新密钥信息 */
    int update(Jwk entity);

    /** 撤销主密钥状态（将指定密钥设为非主密钥） */
    int revokePrimary();
}
