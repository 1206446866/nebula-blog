package com.nebula.auth.service;

/** IP归属地查询服务。 */
public interface LocationService {

    /**
     * 查询IP归属地，本地地址返回Local，无法识别时返回Unknown。
     *
     * @param ip IPv4或IPv6地址
     * @return 归属地显示文本
     */
    String getLocation(String ip);

}