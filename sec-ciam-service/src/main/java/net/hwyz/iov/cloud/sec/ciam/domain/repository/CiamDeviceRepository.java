package net.hwyz.iov.cloud.sec.ciam.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.infrastructure.repository.dao.dataobject.CiamDeviceDo;

import java.util.List;
import java.util.Optional;

/**
 * 设备表仓储接口。
 */
public interface CiamDeviceRepository {

    /** 根据业务 ID 查询 */
    Optional<CiamDeviceDo> findByDeviceId(String deviceId);

    /** 根据用户 ID 和设备状态查询 */
    List<CiamDeviceDo> findByUserIdAndStatus(String userId, int deviceStatus);

    /** 根据用户 ID 查询 */
    List<CiamDeviceDo> findByUserId(String userId);

    /** 根据设备指纹查询 */
    Optional<CiamDeviceDo> findByDeviceFingerprint(String deviceFingerprint);

    /** 查询所有设备 */
    List<CiamDeviceDo> findAll();

    /** 插入设备记录 */
    int insert(CiamDeviceDo entity);

    /** 根据业务 ID 更新 */
    int updateByDeviceId(CiamDeviceDo entity);
}
