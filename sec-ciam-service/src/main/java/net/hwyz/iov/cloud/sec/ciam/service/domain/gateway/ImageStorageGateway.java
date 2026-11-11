package net.hwyz.iov.cloud.sec.ciam.service.domain.gateway;

/**
 * 图片存储网关（SEC-CIAM-DSN-CR-004 §3 RD-004-1/RD-004-3）。
 * <p>
 * CIAM 只做 EDD-OSS 图片服务的场景化薄封装：申请上传、确认上传。
 * 不持有 COS 密钥、不中转图片二进制、不做对象核验与内容审核。
 */
public interface ImageStorageGateway {

    /**
     * 申请上传：调用 EDD-OSS 创建上传任务并返回预签名参数。
     *
     * @param originalFilename 传给 OSS 的原始文件名（avatar-{userId}.{ext}）
     * @param contentType      声明 MIME
     * @param size             声明字节数
     * @return 上传凭证
     */
    ImageUploadTicket generateUpload(String originalFilename, String contentType, long size);

    /**
     * 确认上传：调用 EDD-OSS 核验 COS 对象并返回已确认的图片信息。
     *
     * @param objectId 上传任务标识
     * @return 已确认的图片对象
     */
    ImageObjectInfo completeUpload(String objectId);
}
