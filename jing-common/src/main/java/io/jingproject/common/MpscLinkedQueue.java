package io.jingproject.common;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

// zero-dependency port of a lock-free mpsc (multi-producer/single-consumer)
// linked queue, using single-inheritance byte padding in a fixed 64-byte scheme:
// one cache line between producerNode and consumerNode, plus a 64-byte tail pad
// to avoid false sharing with an adjacent allocation.
//
// field mapping (unsafe -> varhandle):
//   producerNode -> getAndSet (xchg) / getVolatile, direct set + releaseFence in ctor
//   consumerNode -> get / set (consumer thread only)
//   node.next    -> setRelease (release write) / getAcquire (acquire read)
//   node.value   -> set / get (plain field)
//
// algorithm:
//   the queue always keeps a stub node; producerNode points to the tail,
//   consumerNode points to the head (already-drained node).
//   offer: allocate a node, xchg it in as the new tail, then release-link the
//     old tail; the consumer may spin in the gap between the two steps, the
//     only non-wait-free point of this algorithm.
//   poll: read consumerNode.next; take the value, null-out and drain the old
//     node, advance the stub; spin while a producer is mid-offer.
//   null is not allowed to be enqueued.
public final class MpscLinkedQueue<E> extends QueuePad2Impl<E> {

    private static final VarHandle P_NODE;
    private static final VarHandle C_NODE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            P_NODE = lookup.findVarHandle(MpscLinkedQueue.class, "producerNode", Node.class);
            C_NODE = lookup.findVarHandle(MpscLinkedQueue.class, "consumerNode", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public MpscLinkedQueue() {
        Node<E> stub = new Node<>(null);
        consumerNode = stub;
        producerNode = stub;
        VarHandle.releaseFence();
    }

    // enqueue, safe for concurrent producers; allocates a node, grabs the tail
    // by xchg, then links the old tail.
    @SuppressWarnings("unchecked")
    public boolean offer(E e) {
        if (e == null) {
            throw new IllegalArgumentException("element must not be null");
        }
        Node<E> nextNode = new Node<>(e);
        Node<E> prevProducerNode = (Node<E>) P_NODE.getAndSet(this, nextNode);
        prevProducerNode.soNext(nextNode);
        return true;
    }

    // dequeue, single-consumer only; returns null when the queue is empty,
    // spins while a producer is between xchg and link.
    @SuppressWarnings("unchecked")
    public E poll() {
        Node<E> currConsumerNode = (Node<E>) C_NODE.get(this);
        Node<E> nextNode = currConsumerNode.lvNext();
        if (nextNode == null) {
            if (currConsumerNode == (Node<E>) P_NODE.getVolatile(this)) {
                return null;                                   // queue empty
            }
            // producer in the xchg/link gap, spin until the link appears
            while ((nextNode = currConsumerNode.lvNext()) == null) {
                Thread.onSpinWait();
            }
        }
        final E value = nextNode.getAndNullValue();            // take the value and clear the node
        currConsumerNode.soNext(currConsumerNode);             // self-link the drained node, keep gc containment
        C_NODE.set(this, nextNode);                            // advance the stub
        return value;
    }
}

// linked-list node; value is a plain field (write by producer, read by
// consumer, made visible by release/acquire), next is the only ordering point.
final class Node<E> {

    static final VarHandle VALUE;
    static final VarHandle NEXT;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            VALUE = lookup.findVarHandle(Node.class, "value", Object.class);
            NEXT = lookup.findVarHandle(Node.class, "next", Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    private E value;

    @SuppressWarnings("unused")
    private Node<E> next;

    Node(E value) {
        VALUE.set(this, value);
    }

    // take the value and null it out, consumer only.
    @SuppressWarnings("unchecked")
    E getAndNullValue() {
        E v = (E) VALUE.get(this);
        VALUE.set(this, null);
        return v;
    }

    // release write of next (maps to putOrderedObject).
    void soNext(Node<E> n) {
        NEXT.setRelease(this, n);
    }

    // acquire read of next (maps to the volatile next read).
    @SuppressWarnings("unchecked")
    Node<E> lvNext() {
        return (Node<E>) NEXT.getAcquire(this);
    }
}

// first 64-byte pad, safe under any object header size.
@SuppressWarnings("unused")
abstract class QueuePad0Impl<E>  {
    byte b000,b001,b002,b003,b004,b005,b006,b007;
    byte b008,b009,b010,b011,b012,b013,b014,b015;
    byte b016,b017,b018,b019,b020,b021,b022,b023;
    byte b024,b025,b026,b027,b028,b029,b030,b031;
    byte b032,b033,b034,b035,b036,b037,b038,b039;
    byte b040,b041,b042,b043,b044,b045,b046,b047;
    byte b048,b049,b050,b051,b052,b053,b054,b055;
    byte b056,b057,b058,b059,b060,b061,b062,b063;
}

// holds the producer tail reference.
abstract class ProducerNodeRef<E> extends QueuePad0Impl<E> {
    // queue tail, advanced only by producers via xchg (volatile semantics via varhandle).
    Node<E> producerNode;
}

// second 64-byte pad between producerNode and consumerNode (one cache line).
@SuppressWarnings("unused")
abstract class QueuePad1Impl<E> extends ProducerNodeRef<E>  {
    byte g000,g001,g002,g003,g004,g005,g006,g007;
    byte g008,g009,g010,g011,g012,g013,g014,g015;
    byte g016,g017,g018,g019,g020,g021,g022,g023;
    byte g024,g025,g026,g027,g028,g029,g030,g031;
    byte g032,g033,g034,g035,g036,g037,g038,g039;
    byte g040,g041,g042,g043,g044,g045,g046,g047;
    byte g048,g049,g050,g051,g052,g053,g054,g055;
    byte g056,g057,g058,g059,g060,g061,g062,g063;
}

// holds the consumer head (stub) reference.
abstract class ConsumerNodeRef<E> extends QueuePad1Impl<E> {
    // queue head (stub), owned exclusively by the single consumer thread.
    Node<E> consumerNode;
}

// 64-byte tail pad isolating the consumerNode from an adjacent allocation.
@SuppressWarnings("unused")
abstract class QueuePad2Impl<E> extends ConsumerNodeRef<E>  {
    byte p000,p001,p002,p003,p004,p005,p006,p007;
    byte p008,p009,p010,p011,p012,p013,p014,p015;
    byte p016,p017,p018,p019,p020,p021,p022,p023;
    byte p024,p025,p026,p027,p028,p029,p030,p031;
    byte p032,p033,p034,p035,p036,p037,p038,p039;
    byte p040,p041,p042,p043,p044,p045,p046,p047;
    byte p048,p049,p050,p051,p052,p053,p054,p055;
    byte p056,p057,p058,p059,p060,p061,p062,p063;
}