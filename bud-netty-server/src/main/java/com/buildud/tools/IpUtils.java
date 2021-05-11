package com.buildud.tools;


import com.buildud.config.RtspConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpUtils {
    public static final Logger log = LoggerFactory.getLogger(IpUtils.class);

    public static String localHostIp;

    public static String getLocalHostIp() {
        if (localHostIp==null||"".equals(localHostIp.trim())){
            InetAddress addr = null;
            try {
                addr = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                log.error(e.getMessage(),e.getCause());
            }
            localHostIp = addr.getHostAddress();
        }
        return localHostIp;
    }

}
