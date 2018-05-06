package linkedList;

/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

import java.util.*;
import java.util.function.Consumer;

/**
 * List和Deque接口的双向链表（具有向前和向后的指针）实现。 实现所有可选的列表操作，并允许所有元素为null。
 *
 * 所有这些操作的执行情况可能与双向链表相符。索引到列表中的操作将从头或尾遍历列表，以哪个更接近指定的索引为准。
 *
 * 请注意，此实现不同步。如果多个线程同时访问链接列表，并且至少有一个线程在结构上修改列表，则它必须在外部同步。
 * （结构修改是添加或删除一个或多个元素的任何操作;仅设置元素的值不是结构修改。）
 * 这通常是通过在自然封装列表的某个对象上进行同步来完成的。
 *
 * 如果不存在这样的对象，列表应该使用Collections.synchronizedList方法“包装”。
 * 这最好在创建时完成，以防止意外的不同步访问列表：
 * List list = Collections.synchronizedList(new LinkedList(...));
 *
 * 这个类的iterator和listIterator方法返回的迭代器是快速失败的：
 * 如果列表在迭代器创建后的任何时候在结构上被修改，除了通过迭代器自己的remove或add方法外，迭代器将抛出一个ConcurrentModificationException异常。
 * 因此，面对并发修改，迭代器快速而干净地失败，而不是在将来某个未确定的时间冒着任意的，非确定性的行为风险。
 * 但是，这是尽最大努力去快速失败的，所以并不能依靠这个机制来保证不会发生并发修改。
 *
 * 请注意，迭代器的故障快速行为无法得到保证，因为一般来说，在存在非同步并发修改的情况下不可能做出任何硬性保证。
 * 失败快速迭代器尽最大努力抛出ConcurrentModificationException异常。
 * 因此，编写一个依赖于此异常的程序是错误的：迭代器的快速失败行为应仅用于检测错误。
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch
 * @see     List
 * @see     ArrayList
 * @since 1.2
 * @param <E> the type of elements held in this collection
 */

public class LinkedList<E>
        extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
    // 此字段不进行序列化。
    // 链表中元素个数。
    transient int size = 0;

    // 此字段不进行序列化。
    // 指向第一个节点的指针（节点变量引用）即头指针，Node 存储一个节点的所有信息。
    // 不变性：这个链表要么为空，要么不为空。
    transient Node<E> first;

    // 此字段不进行序列化。
    // 指向最后一个节点的指针（节点变量引用）即尾指针。
    // 不变性：这个链表要么为空，要么不为空。
    transient Node<E> last;

    // 构造一个空链表。
    public LinkedList() {
    }

    // 构造一个包含集合c所有元素的链表,如果集合c为null，则抛出NullPointerException异常。
    public LinkedList(Collection<? extends E> c) {
        this();    // 先构造一个空的链表，this() 用来调用无参数的另一个构造器。
        addAll(c);    // 然后将集合c的全部元素添加进去。
    }

    // 将元素e（即节点内的那个数据字段）链接为第一个元素。
    private void linkFirst(E e) {
        final Node<E> f = first;    // 保存 first 变量引用。并且注意这是一个 final 变量引用，这个引用不能变，但是它引用的那个对象（如果不是final的话）是可变的。
        final Node<E> newNode = new Node<>(null, e, f);    // 构造一个新的Node节点。
        first = newNode;    // 将头指针指向新的Node节点。
        if (f == null)    // 如果链表本来为空，就把尾指针也指向这个节点。
            last = newNode;
        else
            f.prev = newNode;    // 否则的话将向前的指针指向刚才的新建节点。
        size++;    // 将元素个数加1。
        modCount++;    // 结构化修改次数也加1。
    }

    // 将元素e（即节点内的那个数据字段）链接为最后一个元素。
    // 这个与前面的 linkFirst 方法类似，就不细说了。
    void linkLast(E e) {
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        size++;
        modCount++;
    }

    // 在非null节点succ前插进元素e即包含元素e的节点。
    // 这里也和前面类似，但要注意分情况，例如节点succ是否为第一个节点。
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;    // 断言succ不为null。
        final Node<E> pred = succ.prev;
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        size++;
        modCount++;
    }

    // 不链接非null的第一个节点（即删除不为null的第一个节点），并返回删除的那个节点的元素值。
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;    // 断言f == fist 并且 f != null
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null; // help GC    // 帮助 GC 回收无用的引用
        first = next;
        if (next == null)    // 考虑next是否为null，即原本链表是否只有一个节点。
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        return element;
    }

    // 不链接非null的最后一个节点（即删除不为null的最后一个节点），并返回删除的那个节点的元素值。
    // 与 unlinkFirst 类似，就不细说了。
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;
        final Node<E> prev = l.prev;
        l.item = null;
        l.prev = null; // help GC
        last = prev;
        if (prev == null)
            first = null;
        else
            prev.next = null;
        size--;
        modCount++;
        return element;
    }

    // 不链接某个非null节点（即删除某个节点），并返回删除的那个节点里的元素值。
    E unlink(Node<E> x) {
        // assert x != null;
        final E element = x.item;
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        if (prev == null) {    // 当这个节点是第一个节点的时候。
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }

        if (next == null) {    // 当这个节点是最后一个节点的时候。
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }

        x.item = null;
        size++;    // 将元素个数加1。
        modCount++;    // 结构化修改次数也加1。
        return element;
    }

    // 获取链表中第一个元素的值，如果链表为空则抛出NoSuchElementException异常。
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }

    // 获取链表中最后一个元素的值，如果链表为空则抛出NoSuchElementException异常。
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    // 删除并且返回这个链表中的第一个元素的值，如果链表为空则抛出NoSuchElementException异常。
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);    // 调用unlinkFirst方法。
    }

    // 删除并且返回这个链表中的最后一个元素的值，如果链表为空则抛出NoSuchElementException异常。
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);    // 调用unlinkLast方法。
    }

    // 在链表的开头插入一个特定的元素。
    public void addFirst(E e) {
        linkFirst(e);    // 调用linkFirst方法。
    }

    //  往链表的尾部追加一个特定的元素。
    public void addLast(E e) {
        linkLast(e);    // 调用linkLast方法。
    }

    // 如果这个链表中存在参数指定的元素，则返回true。
    // 更正式地说，当且仅当该列表包含至少一个元素e，满足(o == null ？ e == null ： o.equals(e))时才返回true。
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    // 返回这个链表里元素的个数。
    public int size() {
        return size;
    }

    // 往链表的末尾添加给定的元素。
    public boolean add(E e) {
        linkLast(e);    // 具体就是调用linkLast方法。
        return true;
    }

    // 删除给定元素在链表中第一次出现时的那一个元素（即链表中中存在多个相同的元素，这个方法删除第一个出现的那个）。
    // 更正式地说，删除的那个元素的索引i满足(o == null ? get(i) == null : o.equals(get(i)))（如果存在这个元素的话）。
    // 如果这个链表确实包含这个元素的话就返回true，等价地说，如果因为这个调用确实改变了列表就返回true。
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    // 按照指定集合的迭代器返回的顺序，将指定集合中的所有元素追加到此列表的末尾。
    // 如果在操作过程中修改了指定的集合，则此操作的行为未定义。
    // 请注意，如果指定的集合是这个链表本身，并且它是非空的，则会发生这种情况。
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);    // 调用addAll(int, Collection<? extends E>)方法。
    }

    // 从指定的位置开始将指定集合中的所有元素插入此列表。
    // 将当前在该位置的元素（如果有的话）和随后的元素移到右侧（增加它们的索引）。
    // 新元素将按照它们由指定集合的迭代器返回的顺序出现在列表中。
    // 抛出IndexOutOfBoundsException如果传入的参数index不合法的话，抛出NullPointerException如果传入的参数集合c为null的话。
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);    // 检查输入的参数是否是有效的位置索引。

        Object[] a = c.toArray();    // 把集合c转换成一个数组。
        int numNew = a.length;    // 存储转换成的数组的元素个数。
        if (numNew == 0)
            return false;

        Node<E> pred, succ;    // 两个节点变量引用，用来添加元素时作辅助的作用，可以想象成指针。
        if (index == size) {
            succ = null;
            pred = last;
        } else {
            succ = node(index);
            pred = succ.prev;
        }

        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;
            pred = newNode;
        }

        if (succ == null) {
            last = pred;
        } else {
            pred.next = succ;
            succ.prev = pred;
        }

        size += numNew;
        modCount++;
        return true;
    }

    // 删除链表中的所有元素。
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit
        //   more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        // 清除节点之间的所有链接是“不必要的”，但是：
        // - 如果被丢弃的节点栖息超过一代，则有助于分代GC。
        // - 即使有可用的迭代器，也一定会释放内存。
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }


    // Positional Access Operations
    // 位置访问操作

    // 返回链表中指定的位置的那个元素。
    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }

    // 替换（设置）链表中指定的位置的那个元素。
    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        x.item = element;
        return oldVal;
    }

    // 在指定的位置的插入指定的元素。
    // 将当前位置的元素（如果有的话）和任何后续元素移到右侧（将其中的一个添加到它们的索引）。
    public void add(int index, E element) {
        checkPositionIndex(index);

        if (index == size)
            linkLast(element);    // 调用linkLast方法
        else
            linkBefore(element, node(index));    // 调用linkBefore方法
    }

    // 删除一个特定位置的元素并返回其值。
    public E remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }

    // 判断这个索引是否是一个在链表中存在的元素的索引。index从0开始，而不是1，注意一下。
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    // 判断参数是否为迭代器或添加操作的有效位置的索引。暂时还不知道与isElementIndex有什么区别。
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    // 构造一个详细的IndexOutOfBoundsException信息。
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    // 检查元素的有效索引。
    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    // 检查输入的参数是否为有效的位置索引。
    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    // 按给出的index返回节点。
    // 这里会先判断index的位置，如果是小于size的一半，就会从头开始找，如果大于等于size的一半，就会从末尾开始找。
    // 这稍微提高了一下查找效率，但不高。
    Node<E> node(int index) {
        // assert isElementIndex(index);

        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // Search Operations
    // 查找操作

    // 返回特定元素第一次出现时的索引，或者当这个链表不存在这个元素时，返回-1。
    // 更正式地说，返回最小索引i满足(o == null ? get(i) == null : o.equals(get(i)))，或者返回-1（如果没有这样的索引的话）
    public int indexOf(Object o) {
        int index = 0;

        // 如果传进的参数为null时，则遍历整个链表寻找是否有元素的值为null。
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {    // 如果传进的参数不为null时，则遍历整个链表寻找是否有元素的值等于传进去的参数的值。
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;    // 如果不存在这个元素，则返回-1。
    }

    // 返回此链表中指定元素的最后一次出现的索引，如果此列表不包含元素，则返回-1。
    // 更正式地说，返回最高索引i，满足(o == null ? get(i) == null : o.equals(get(i)))，或者没有这个元素的话就返回-1。
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }

    // Queue operations.
    // 队列操作。

    // 检索但不删除此链表的头（第一个元素）。即返回第一个元素但不删除它。如果这个链表为空，则返回null。
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    // 检索但不删除此链表的头（第一个元素）。即返回第一个元素但不删除它。如果这个链表为空，相比peek方法，它则抛出NoSuchElementExceptionu异常。
    public E element() {
        return getFirst();
    }

    // 检索并删除此链表的头（即第一个元素）。即返回第一个元素并且删除它。如果这个链表为空，则返回null。
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    // 检索并删除此链表的头（第一个元素）。即返回第一个元素并删除它。如果这个链表为空，相比poll方法，它则抛出NoSuchElementExceptionu异常。
    public E remove() {
        return removeFirst();
    }

    // 将指定的元素添加为此链表的尾部（即最后一个元素）并返回true。这个方法实现了Queue接口里的offer方法。
    public boolean offer(E e) {
        return add(e);
    }


    // Deque operations
    // 双端队列操作

    // 在此链表的前面插入指定的元素并返回true。这个方法实现了Deque接口里的offerFirst方法。
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    // 在链表的末尾插入指定的元素。
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    // 返回此链表的第一个元素但不删除，或者如果此链表为空，则返回null。
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }

    // 返回此链表的最后一个元素但不删除，或者如果此链表为空，则返回null。
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    // 返回并删除此链表的第一个元素，或者如果此链表为空，则返回null。
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    // 返回并删除此链表的最后一个元素，或者如果此链表为空，则返回null。
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    // 将元素push到由此链表表示的堆栈。 换句话说，将该元素插入此链表的前面。
    // 此方法与addFirst方法等价。
    public void push(E e) {
        addFirst(e);
    }

    // 从该链表表示的堆栈中弹出一个元素。 换句话说，删除并返回此链表的第一个元素。
    // 此方法与removeFirst方法等价。
    // 如果此链表为空，则抛出NoSuchElementException异常。
    public E pop() {
        return removeFirst();
    }

    // 删除此链表中首次出现的指定元素（当从头到尾遍历链表时）。 如果该链表不包含该元素，则不变，否则就返回true。
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    // 删除此链表中最后一次出现的指定元素（当从尾到头遍历链表时）。 如果该链表不包含该元素，则不变，否则就返回true。
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从列表中的指定位置开始，返回此列表中元素的列表迭代器（按适当顺序）。 遵守List.listIterator(int)的一般合约。
     * 列表迭代器是快速失败的：如果列表在迭代器创建后的任何时候在结构上被修改，除了通过列表迭代器自己的删除或添加
     * 方法，列表迭代器将抛出ConcurrentModificationException异常。
     * 因此，面对并发修改，迭代器快速而干净地失败，而不是在将来某个未确定的时间冒着任意的，非确定性的行为风险。
     * 但是，这是尽最大努力去快速失败的，所以并不能依靠这个机制来保证不会发生并发修改。
     */
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();

            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    // 节点的具体类，包括3个字段，分别是元素值，指向上一个节点的指针和指向下一个节点的指针。
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    // 降序迭代器
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    // 通过ListItr.previous提供降序迭代器
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    // 返回此LinkedList的浅拷贝副本。（即元素本身没有被克隆。）
    public Object clone() {
        LinkedList<E> clone = superClone();

        // 把副本变成初始化状态。
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // 用我们的元素去初始化副本。
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
    }

    // 以适当的顺序返回一个包含此列表中所有元素的数组（从第一个元素到最后一个元素）。
    // 返回的数组将是“安全的”，因为此列表不会保留对它的引用。（换句话说，这个方法必须分配一个新的数组）。调用者可以自由修改返回的数组。
    // 此方法充当基于数组和基于集合的API之间的桥梁。
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    /**
     * 以正确的顺序返回一个包含此列表中所有元素的数组（从第一个元素到最后一个元素）; 返回数组的运行时类型是指定数组的运行时类型。
     * 如果列表符合指定的数组，则直接返回。 否则，将使用指定数组的运行时类型和此列表的大小分配一个新数组。
     *
     * 如果列表符合指定数组并且有空余空间（即数组的元素多于列表），紧接列表结尾的数组中的元素将设置为null。
     * （仅当调用者知道列表不包含任何空元素时，这对确定列表的长度很有用。）
     *
     * 像toArray()方法一样，此方法充当基于数组和基于集合的API之间的桥梁。
     * 此外，该方法允许精确控制输出数组的运行时类型，并且在某些情况下可以用于节省分配成本。
     *
     * 假设x是已知只包含字符串的列表。下面的代码可用于将列表转储到新分配的String数组中：
     * String [] y = x.toArray(new String [0]);
     *
     * 请注意，toArray(new Object[0])在功能上与toArray()相同。
     *
     * 如果指定数组的运行时类型不是此列表中每个元素的运行时类型的超类型，则抛出ArrayStoreException异常。
     * 如果指定的数组为null则抛出NullPointerException异常。
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;

        if (a.length > size)
            a[size] = null;

        return a;
    }

    // 序列化版本号id
    private static final long serialVersionUID = 876323262645176354L;

    /**
     * Saves the state of this {@code LinkedList} instance to a stream
     * (that is, serializes it).
     *
     * @serialData The size of the list (the number of elements it
     *             contains) is emitted (int), followed by all of its
     *             elements (each an Object) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * Reconstitutes this {@code LinkedList} instance from a stream
     * (that is, deserializes it).
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E)s.readObject());
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @implNote
     * The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}
     * and implements {@code trySplit} to permit limited parallelism..
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() { return (long) getEst(); }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do { a[j++] = p.item; } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p; int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

}

