package com.buildud.queue;


import io.netty.channel.socket.DatagramPacket;

import java.util.concurrent.TimeUnit;

public class RtpBudQueue {

    private BaseQueue<DatagramPacket> rtpQueue;
    private int write_seq=0;
    private int read_seq=0;


    public RtpBudQueue() {
        init();
    }

    public void init(){
        rtpQueue = new BaseQueue<DatagramPacket>();
    }

    public void clear() {
        rtpQueue.clear();
    }

    public int size() {
        return rtpQueue.size();
    }

    public void put(DatagramPacket e) throws InterruptedException {
        rtpQueue.put(e);
        write_seq++;
    }

    public DatagramPacket take() throws InterruptedException {
        DatagramPacket b =rtpQueue.take();
        read_seq++;
        return b;
    }

    public DatagramPacket poll() {
        DatagramPacket b =rtpQueue.poll();
        read_seq++;
        return b;
    }

    public DatagramPacket poll(long timeout, TimeUnit unit) throws InterruptedException {
        DatagramPacket b =rtpQueue.poll(timeout, unit);
        read_seq++;
        return b;
    }

    public DatagramPacket peek() {
        DatagramPacket b =rtpQueue.peek();
        read_seq++;
        return b;
    }

    public boolean isEmpty() {
        if (write_seq>read_seq){
            return false;
        }
        return true;
    }
}
