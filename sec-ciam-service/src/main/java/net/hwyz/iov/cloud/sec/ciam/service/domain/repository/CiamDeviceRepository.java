package net.hwyz.iov.cloud.sec.ciam.service.domain.repository;

import net.hwyz.iov.cloud.sec.ciam.service.domain.model.Device;

import java.util.List;
import java.util.Optional;

/**
 * 设备表仓储接口。
 */
public interface CiamDeviceRepository {

    /** 根据业务 ID 查询 */
    Optional<Device> findByDeviceId(String deviceId);

    /** 根据用户 ID 和设备状态查询 */
    List<Device> findByUserIdAndStatus(String userId, int deviceStatus);

    /** 根据用户 ID 查询 */
    List<Device> findByUserId(String userId);

    /** 根据设备指纹查询 */
    Optional<Device> findByDeviceFingerprint(String deviceFingerprint);

    /** 查询所有设备 */
    List<Device> findAll();

    /** 插入设备记录 */
    int insert(Device device);

    /** 根据业务 ID 更新 */
    int updateByDeviceId(Device device);
}
