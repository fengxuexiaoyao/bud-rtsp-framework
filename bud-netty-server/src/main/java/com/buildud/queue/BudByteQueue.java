package com.buildud.queue;


import java.util.concurrent.TimeUnit;

public class BudByteQueue {

    private BaseQueue<byte[]> rtpQueue;
    private int write_seq=0;
    private int read_seq=0;


    public BudByteQueue() {
        init();
    }

    public void init(){
        rtpQueue = new BaseQueue<byte[]>();
    }

    public void clear() {
        rtpQueue.clear();
    }

    public int size() {
        return rtpQueue.size();
    }

    public void put(byte[] b) throws InterruptedException {
        rtpQueue.put(b);
        write_seq++;
    }

    public byte[] take() throws InterruptedException {
        byte[] b =rtpQueue.take();
        read_seq++;
        return b;
    }

    public byte[] poll() {
        byte[] b =rtpQueue.poll();
        read_seq++;
        return b;
    }

    public byte[] poll(long timeout, TimeUnit unit) throws InterruptedException {
        byte[] b =rtpQueue.poll(timeout, unit);
        read_seq++;
        return b;
    }

    public byte[] peek() {
        byte[] b =rtpQueue.peek();
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
