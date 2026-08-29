package io.jingproject.marshalljsontest.entity;

import io.jingproject.marshall.Marshallable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Marshallable
public final class RecursiveEntity {
    private int value;
    private RecursiveEntity left;
    private RecursiveEntity right;

    public static RecursiveEntity createRecursiveEntity(int level) {
        if(level < 1) {
            throw new IllegalArgumentException("level must be greater than or equal to 1");
        }
        int counter = 1;
        Deque<RecursiveEntity> q = new ArrayDeque<>();
        RecursiveEntity root = new RecursiveEntity();
        root.setValue(counter++);
        q.addLast(root);
        for (int i = 1; i < level; i++) {
            int size = q.size();
            for (int j = 0; j < size; j++) {
                RecursiveEntity r = q.removeFirst();
                RecursiveEntity left = new RecursiveEntity();
                left.setValue(counter++);
                RecursiveEntity right = new RecursiveEntity();
                right.setValue(counter++);
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RecursiveEntity that)) return false;
        return value == that.value && Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, left, right);
    }
}
