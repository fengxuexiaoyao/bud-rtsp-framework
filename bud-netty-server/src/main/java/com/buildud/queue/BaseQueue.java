package com.buildud.queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class BaseQueue<E> extends LinkedBlockingQueue<E> {
    @Override
    public void clear() {
        super.clear();
    }

}
