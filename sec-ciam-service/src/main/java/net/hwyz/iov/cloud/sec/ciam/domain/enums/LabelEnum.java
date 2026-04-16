package net.hwyz.iov.cloud.sec.ciam.domain.enums;

/**
 * 字符串标签枚举统一接口。
 * <p>
 * 所有以 VARCHAR 存储的类型枚举均实现此接口，
 * 便于通用序列化、反序列化与 MyBatis 类型处理。
 */
public interface LabelEnum {

    /** 数据库存储的字符串值 */
    String getCode();

    /** 中文描述 */
    String getDescription();
}
