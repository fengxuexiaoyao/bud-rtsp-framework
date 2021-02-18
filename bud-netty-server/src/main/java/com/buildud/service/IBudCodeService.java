package com.buildud.service;

import com.buildud.bean.AbstractQueueBean;
import com.buildud.exception.BudNettyServerException;
import io.netty.channel.Channel;

import java.io.BufferedInputStream;
import java.io.IOException;


public interface IBudCodeService {

    public boolean readFileToQueue(String filePath, AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel) throws BudNettyServerException, IOException, InterruptedException;

    public boolean readFileToQueue(BufferedInputStream in, AbstractQueueBean<byte[]> dudQueueBean,Channel sendChannel) throws BudNettyServerException, IOException, InterruptedException;

    public void sendRtp(AbstractQueueBean<byte[]> dudQueueBean, Channel sendChannel) throws Exception;

    public void screenShotStream(AbstractQueueBean<byte[]> dudQueueBean) throws BudNettyServerException;
}
