package io.jingproject.marshalljsontest.entity;

import io.jingproject.marshall.Marshallable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

@Marshallable
public final class RecursiveEntity {
    private int value;
    private RecursiveEntity left;
    private RecursiveEntity right;

    public static RecursiveEntity createRecursiveEntity(int level) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Deque<RecursiveEntity> q = new ArrayDeque<>();
        RecursiveEntity root = new RecursiveEntity();
        q.addLast(root);
        int range = 1;
        for (int i = 0; i < level; i++) {
            for (int j = 0; j < range; j++) {
                RecursiveEntity r = q.removeFirst();
                r.setValue(random.nextInt(10));
                RecursiveEntity left = new RecursiveEntity();
                RecursiveEntity right = new RecursiveEntity();
                r.setLeft(left);
                r.setRight(right);
                q.addLast(left);
                q.addLast(right);
            }
        }
        return root;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public RecursiveEntity getLeft() {
        return left;
    }

    public void setLeft(RecursiveEntity left) {
        this.left = left;
    }

    public RecursiveEntity getRight() {
        return right;
    }

    public void setRight(RecursiveEntity right) {
        this.right = right;
    }
}
