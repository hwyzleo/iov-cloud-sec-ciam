package net.hwyz.iov.cloud.sec.ciam.service.domain.enums;

import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.sec.ciam.service.common.exception.CiamErrorCode;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 头像图片类型枚举（SEC-CIAM-DSN-CR-004 §5.1、RD-004-11）。
 * <p>
 * 与 EDD-OSS 允许的 MIME 及其扩展名保持一致，避免 CIAM 侧放宽 OSS 契约：
 * image/jpeg→jpg、image/png→png、image/webp→webp。
 * <p>
 * 本枚举同时承载头像上传的命名约定与前置校验，保证传给 OSS 的 originalFilename
 * 恒为 avatar-{userId}.{ext}，不使用 APP 侧原始文件名。
 */
@Getter
public enum AvatarImageType {

    JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg")),
    PNG("image/png", "png", Set.of("png")),
    WEBP("image/webp", "webp", Set.of("webp"));

    /**
     * 头像原始文件名前缀
     */
    private static final String FILENAME_PREFIX = "avatar-";
    /**
     * userId 允许字符集，避免写入 OSS 原始文件名时引入路径分隔符或控制字符
     */
    private static final Pattern USER_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    /**
     * 单张头像最大字节数（10 MiB），与 EDD-OSS 一致
     */
    public static final long MAX_SIZE_BYTES = 10_485_760L;

    private final String contentType;
    private final String extension;
    private final Set<String> supportedExtensions;

    AvatarImageType(String contentType, String extension, Set<String> supportedExtensions) {
        this.contentType = contentType;
        this.extension = extension;
        this.supportedExtensions = supportedExtensions;
    }

    /**
     * 按声明 MIME 精确匹配（忽略首尾空格与大小写）。
     *
     * @param contentType 声明 MIME
     * @return 匹配的图片类型
     */
    public static Optional<AvatarImageType> fromContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(item -> item.contentType.equals(normalized))
                .findFirst();
    }

    /**
     * 按声明 MIME 匹配，不匹配时抛出参数错误。
     *
     * @param contentType 声明 MIME
     * @return 图片类型
     */
    public static AvatarImageType requireContentType(String contentType) {
        return fromContentType(contentType)
                .orElseThrow(() -> new BusinessException(CiamErrorCode.INVALID_PARAM,
                        "头像仅支持 image/jpeg、image/png、image/webp"));
    }

    /**
     * 校验声明大小：必须大于 0 且不超过 10 MiB。
     *
     * @param size 声明字节数
     * @return 校验后的声明大小
     */
    public static long requireSize(Long size) {
        if (size == null || size <= 0 || size > MAX_SIZE_BYTES) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM,
                    "头像大小必须大于 0 且不超过 " + MAX_SIZE_BYTES + " 字节");
        }
        return size;
    }

    /**
     * 校验 APP 侧提交的原始文件名：仅用于快速失败，扩展名必须与声明 MIME 匹配。
     * <p>
     * 该文件名不会作为传给 OSS 的 originalFilename，见 {@link #buildOriginalFilename(String, String)}。
     *
     * @param originalFilename APP 侧原始文件名
     * @param contentType      声明 MIME
     */
    public static void validateClientFilename(String originalFilename, String contentType) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "原始文件名不能为空");
        }
        if (originalFilename.length() > 255) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "原始文件名长度不能超过 255");
        }
        if (originalFilename.indexOf('/') >= 0 || originalFilename.indexOf('\\') >= 0) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "原始文件名不能包含路径分隔符");
        }
        AvatarImageType imageType = requireContentType(contentType);
        String extension = extensionOf(originalFilename);
        if (extension == null || !imageType.supportedExtensions.contains(extension)) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "文件扩展名与声明 MIME 不匹配");
        }
    }

    /**
     * 生成传给 OSS 的原始文件名 avatar-{userId}.{ext}。
     * <p>
     * 扩展名由声明 MIME 派生，不使用 APP 侧文件名，便于 OSS 管理后台按用户聚类头像版本。
     *
     * @param userId    用户业务唯一标识
     * @param contentType 声明 MIME
     * @return OSS 原始文件名
     */
    public static String buildOriginalFilename(String userId, String contentType) {
        if (userId == null || !USER_ID_PATTERN.matcher(userId).matches()) {
            throw new BusinessException(CiamErrorCode.INVALID_PARAM, "非法用户标识，无法生成头像文件名");
        }
        return FILENAME_PREFIX + userId + "." + requireContentType(contentType).extension;
    }

    private static String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
