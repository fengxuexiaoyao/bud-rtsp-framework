package com.buildud.tools;

import com.buildud.bean.AbstractQueueBean;
import com.buildud.queue.BudByteQueue;
import com.buildud.queue.BudScreenRecordThread;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.internal.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


public class RtspUtils {

    public static final Logger log = LoggerFactory.getLogger(RtspUtils.class);

    public static String getPalyVideoPath(FullHttpRequest r)
    {
        if (r==null){
            return null;
        }
        QueryStringDecoder uri = new QueryStringDecoder(r.uri());
        if (!uri.path().contains("/vod/"))
        {
            return "";
        }
        int index = uri.path().indexOf("/vod/");
        return uri.path().substring(index+5);
    }

    /**
     * 解析出当前数组中的所有nalu单元
     * @param decodeByte   需要解析的数组
     * @return
     */
    public static byte[] pareNalu(byte[] decodeByte, AbstractQueueBean<byte[]> dudQueueBean){
        if (decodeByte==null||decodeByte.length<0){
            return null;
        }
        int len = decodeByte.length;
        int start = 0;                             //下一个nalu的开始位置
        int offset = 0;                            //当前循环中的偏移量
        while (offset<len){
            // 循环到起始符位置
            if (offset+3<len&&
                    decodeByte[offset] == 0x00 &&
                    decodeByte[offset + 1] == 0x00 &&
                    decodeByte[offset + 2] == 0x00 &&
                    decodeByte[offset + 3] == 0x01 ) {
                if (offset==0) {
                    start = 4;
                    offset = 4;
                    continue;
                }

                byte[] nalu = new byte[offset -start];
                System.arraycopy(decodeByte, start, nalu, 0, nalu.length);

                System.out.println("nalu>>"+Arrays.toString(nalu));
                RtpUtils.naluToRtp(nalu,dudQueueBean);
//                try {
//                    dudQueueBean.getNaluQueue().put(nalu);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    log.error(e.getMessage(),e.getCause());
//                }

                start = offset+4;
                offset += 4;

                continue;
            } else if(offset+2<len&&
                    decodeByte[offset] == 0x00 &&
                    decodeByte[offset + 1] == 0x00 &&
                    decodeByte[offset + 2] == 0x01 ){
                if (offset==0) {
                    start = 3;
                    offset = 3;
                    continue;
                }

                byte[] nalu = new byte[offset -start];
                System.arraycopy(decodeByte, start, nalu, 0, nalu.length);

                System.out.println("nalu>>"+Arrays.toString(nalu));
                RtpUtils.naluToRtp(nalu,dudQueueBean);
//                try {
//                    dudQueueBean.getNaluQueue().put(nalu);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    log.error(e.getMessage(),e.getCause());
//                }

                start = offset+3;
                offset += 3;

                continue;
            }
            else {
                offset++;
            }
        }
        if (start==0){
            return decodeByte;
        }else if (start<len){
            byte[] upEndData = new byte[len-start];
            System.arraycopy(decodeByte, start, upEndData, 0, upEndData.length);


            return upEndData;
        }
        return null;
    }

}
