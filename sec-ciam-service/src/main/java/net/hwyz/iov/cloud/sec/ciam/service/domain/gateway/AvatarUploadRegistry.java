package net.hwyz.iov.cloud.sec.ciam.service.domain.gateway;

import java.util.Optional;

/**
 * 头像上传任务归属登记（SEC-CIAM-DSN-CR-004 §4.2、RD-004-2）。
 * <p>
 * 生命周期仅为一个预签名有效期，使用 Redis 一次性登记，不新增数据库表。
 * 记录同时承载上传任务的两种状态：
 * <ul>
 *   <li>PENDING：仅记录 userId，表示允许该用户完成确认；</li>
 *   <li>COMPLETED：记录 userId、已确认图片信息，用于重复确认的幂等返回。</li>
 * </ul>
 */
public interface AvatarUploadRegistry {

    /**
     * 登记上传任务归属（PENDING）。
     *
     * @param uploadId  上传任务标识
     * @param userId    用户业务唯一标识
     * @param ttlSeconds 过期时间（秒）
     */
    void bind(String uploadId, String userId, long ttlSeconds);

    /**
     * 查询上传任务归属用户。
     *
     * @param uploadId 上传任务标识
     * @return 归属用户，不存在或已过期时为空
     */
    Optional<String> findOwner(String uploadId);

    /**
     * 查询已完成确认的图片信息。
     *
     * @param uploadId 上传任务标识
     * @return 已确认图片信息，未完成时为空
     */
    Optional<ImageObjectInfo> findCompletion(String uploadId);

    /**
     * 将上传任务标记为 COMPLETED，保留至 TTL 到期以支持幂等返回。
     *
     * @param uploadId  上传任务标识
     * @param userId    用户业务唯一标识
     * @param object    已确认图片信息
     * @param ttlSeconds 过期时间（秒）
     */
    void markCompleted(String uploadId, String userId, ImageObjectInfo object, long ttlSeconds);

    /**
     * 释放上传任务登记。
     *
     * @param uploadId 上传任务标识
     */
    void release(String uploadId);
}
