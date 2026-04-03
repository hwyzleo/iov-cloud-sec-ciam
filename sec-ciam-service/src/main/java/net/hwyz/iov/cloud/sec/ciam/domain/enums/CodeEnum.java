package net.hwyz.iov.cloud.sec.ciam.domain.enums;

/**
 * 数值编码枚举统一接口。
 * <p>
 * 所有以 TINYINT 存储的状态/类型枚举均实现此接口，
 * 便于通用序列化、反序列化与 MyBatis 类型处理。
 */
public interface CodeEnum {

    /** 数据库存储的数值编码 */
    int getCode();

    /** 中文描述 */
    String getDescription();
}
