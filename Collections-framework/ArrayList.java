import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import sun.misc.SharedSecrets;

public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    // 序列化id，用来保证反序列化之后还是同一个对象。
    private static final long serialVersionUID = 8683452581122892189L;

    // 默认的初始容量
    private static final int DEFAULT_CAPACITY = 10;

    // 在public ArrayList(int initialCapacity)中，若initialCapacity为0，则用这个数组初始化elementData。
    // 用于空实例的共享空数组实例。
    private static final Object[] EMPTY_ELEMENTDATA = {};

    // 用于在使用默认（不带参数）构造器时初始化实际存放数据的 elementData 数组。
    // 共享空数组实例用于默认大小的空实例。 我们将这与EMPTY_ELEMENTDATA区分开来，以知道在添加第一个元素时要膨胀多少。
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /** 实际存放数据的数组，transient 关键字用来说明指定属性不进行序列化。
     * 由此可以看到 ArrayList 的底层就是用数组来实现的
     * 当第一个元素被添加进来时，如果这个数组等于DEFAULTCAPACITY_EMPTY_ELEMENTDATA，则它的容量会扩展为DEFAULT_CAPACITY
     */
    transient Object[] elementData; // non-private to simplify nested class access

    // 表示数组当前存储的元素个数。
    private int size;

    // 带有一个初始容量的构造函数。
    public ArrayList(int initialCapacity) {
        // 如果initialCapacity大于0，则将elementData初始化为一个容量为initialCapacity的数组。
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            // 如果initialCapacity小于0，则直接把空数组对象引用EMPTY_ELEMENTDATA赋值给elementData。
            this.elementData = EMPTY_ELEMENTDATA;
        } else {    // initialCapacity小于0则抛出异常。
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        }
    }

    // 默认构造器，此时还没有进行数组的初始化，只是将创建的一个空数组对象赋值给elementData
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    // 用另一个集合去构造这个ArrayList
    public ArrayList(Collection<? extends E> c) {
        /** 先将这个集合c转换成数组，然后再赋值给elementData。
         * elementData并不一定引用的是Object类型的数组，如Arrays.asList("foo", "bar").toArray()返回的是一个String数组。
         * 因为在Arrays类里面，toArray方法返回的是一个泛型数组。
         * 例如：List<Object> l = new ArrayList<Object>(Arrays.asList("foo", "bar"));
         * l.set(0, new Object()); // 这样会引起ArrayStoreException异常，因为不能把一个Object对象存储在一个底层数组已经为String类型的ArrayList。
         * 也因为这样，下面的一个地方就要去判断elementData引用的是不是Object类型的数组。
         */
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {    // 再初始化这个ArrayList的size。
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                // 如果elementData引用的不是Object类型的数组，则将原先的数组拷贝过来，变为一个Object类型的数组。
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // 如果传进来的集合为空，则用空数组对象引用EMPTY_ELEMENTDATA赋值给elementData。
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

    public void trimToSize() {
        /** 这个方法把ArrayList的容量变为当前存储的元素个数。
         * modCount 表示集合被结构化修改（如delete、add等等）的次数(modCount是HashMap、TreeMap、AbstractList等集合的实力域，与Java中fail fast机制有关)
         * 详细的关于fail fast的以后会讲到吧
         */
        modCount++;
        if (size < elementData.length) {    // 如果元素的个数小于ArrayList的容量。
            elementData = (size == 0)    // 再判断原先是否有元素存储。
                    ? EMPTY_ELEMENTDATA    // 没有元素存储则赋值一个空数组对象的引用。
                    : Arrays.copyOf(elementData, size);    // 有元素的话就把原来的数组按size复制一份并用elementData引用。
        }
    }

    // 这个方法用于扩展ArrayList的容量，是一个public的方法，可供用户调用。如有必要，确保它至少可以保存最小容量minCapacity参数指定的元素数量。
    public void ensureCapacity(int minCapacity) {
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)    // 最小扩展（扩容）。先判断elementData是否与DEFAULTCAPACITY_EMPTY_ELEMENTDATA相等
                // any size if not default element table
                ? 0    // 不相等的话设置为0
                // larger than default for default empty table. It's already
                // supposed to be at default size.
                : DEFAULT_CAPACITY;    // 相等的话设置为默认的初始容量：10

        if (minCapacity > minExpand) {    // 如果指定的最小容量minCapacity大于最小扩展，则以minCapacity参数调用ensureExplicitCapacity方法。
            ensureExplicitCapacity(minCapacity);
        }
    }

    // 计算容量，为扩容作准备。
    private static int calculateCapacity(Object[] elementData, int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {    // 如果elementData与DEFAULTCAPACITY_EMPTY_ELEMENTDATA相等，即elementData指向空数组对象引用。
            return Math.max(DEFAULT_CAPACITY, minCapacity);    // 则在DEFAULT_CAPACITY（16）和传进去的参数两者间取最大的。
        }
        return minCapacity;    // 否则直接返回传进去的参数minCapacity。
    }

    // 用来检查是否需要扩容，比如在add、addAll方法,需要事先检查一下数组列表是否满了。
    private void ensureCapacityInternal(int minCapacity) {
        ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));    // 直接调用ensureExplicitCapacity方法。
    }

    private void ensureExplicitCapacity(int minCapacity) {
        // modCount 表示集合被结构化修改（如delete、add等等）的次数，前面 trimToSize 里有讲过。
        modCount++;

        // overflow-conscious code
        // 提防负增长（即容量不能越增越少）。
        if (minCapacity - elementData.length > 0)    // 如果 minCapacity 大于 elementData.length，即进行容量增长。
            grow(minCapacity);    // 调用 grow 方法进行真正的扩容。
    }

    /**
     * 分配给数组的最大容量。
     * 有些虚拟机会保留一些header words在数组里，所以设定数组大小的时候不能"赶尽杀绝"。
     * 尝试分配较大的数组可能会导致OutOfMemoryError：请求的数组大小超出VM限制。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;    // 用来控制溢出

    // 终于来到真正扩容的方法了
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;    // 用 oldCapacity 来保存当前数组的大小
        int newCapacity = oldCapacity + (oldCapacity >> 1);    // 新的容量设置为原来容量的1.5倍，oldCapacity >> 1 等价于 oldCapacity / 2，而位运算的速度快过直接的除法。
        if (newCapacity - minCapacity < 0)    // 如果 newCapacity 小于 minCapacity，
            newCapacity = minCapacity;    // 则将新的容量设置为 minCapacity
        if (newCapacity - MAX_ARRAY_SIZE > 0)    // 如果 newCapacity 大于 MAX_ARRAY_SIZE（Integer.MAX_VALUE - 8），
            newCapacity = hugeCapacity(minCapacity);    // 则将新的容量设置为 hugeCapacity 方法返回的值,这里主要是数组容量防止溢出
        // minCapacity is usually close to size, so this is a win:
        elementData = Arrays.copyOf(elementData, newCapacity);    // 用新的容量复制 elementData 为一个新的数组，并赋值给 elementData。
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?    // 如果 minCapacity 大于 MAX_ARRAY_SIZE，则直接返回 Integer.MAX_VALUE，否则返回 MAX_ARRAY_SIZE
                Integer.MAX_VALUE :    // 然而这里为什么会能取到Integer.MAX_VALUE呢？之前不是定义了MAX_ARRAY_SIZE吗？所以这个疑问待解决。
                MAX_ARRAY_SIZE;
    }

    // 返回当前数组元素的个数
    public int size() {
        return size;
    }

    // 判断数组列表是否为空
    public boolean isEmpty() {
        return size == 0;
    }

    // 判断数组列表是否包含某个对象
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    // 返回特定元素第一次出现的位置
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    // 如果一个对象出现多次，则返回它出现的最后一次的index，方法是从后向前遍历，遇到的第一个就是。
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size-1; i >= 0; i--)
                if (elementData[i]==null)
                    return i;
        } else {
            for (int i = size-1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;
        }
        return -1;
    }

    // 克隆一个 ArrayList
    public Object clone() {
        try {
            ArrayList<?> v = (ArrayList<?>) super.clone();
            v.elementData = Arrays.copyOf(elementData, size);
            v.modCount = 0;
            return v;
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    // 把数组列表转换为一个数组，注意返回的是 Object[]
    public Object[] toArray() {
        return Arrays.copyOf(elementData, size);
    }

    // // 把数组列表转换为一个数组，注意返回的是 T[]
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    // Positional Access Operations
    // 按位置访问操作
    @SuppressWarnings("unchecked")
    E elementData(int index) {
        return (E) elementData[index];
    }

    // 按 index 取一个元素
    public E get(int index) {
        rangeCheck(index);    // 检查是否数组越界

        return elementData(index);
    }

    // 在某个位置设置一个值
    public E set(int index, E element) {
        rangeCheck(index);    // 检查是否数组越界

        E oldValue = elementData(index);
        elementData[index] = element;
        return oldValue;
    }

    // 添加元素
    public boolean add(E e) {
        // 添加操作会改变数组的结构，故modCount会加1。
        // 在这里我们也可以看到，ArrayList 是在第一次add操作时才会确定容量并分配内存空间给elementData数组。
        ensureCapacityInternal(size + 1);  // Increments modCount!!
        elementData[size++] = e;
        return true;
    }

    public void add(int index, E element) {
        rangeCheckForAdd(index);    // 检查是否数组越界

        ensureCapacityInternal(size + 1);  // Increments modCount!!
        // 对数组进行复制，目的就是空出index的位置插入element，并将index后的元素向后移动一个位置
        System.arraycopy(elementData, index, elementData, index + 1,
                size - index);
        elementData[index] = element;
        size++;
    }

    // 按指定位置删除元素
    public E remove(int index) {
        rangeCheck(index);    // 检查是否数组越界

        modCount++;    // 删除也是一个对数组的结构化修改，所以modCount要加1
        E oldValue = elementData(index);

        int numMoved = size - index - 1;    // 要移动的元素的个数
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        // 让 GC 回收无用的空引用
        elementData[--size] = null; // clear to let GC do its work

        return oldValue;
    }

    // 按指定元素删除第一个出现的元素
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    /**
     * 这是一个私有方法，它会跳过数组越界检查并且不会返回任何东西。
     * 这也会导致结构化修改，所以modCount要加1。
     */
    private void fastRemove(int index) {
        modCount++;
        int numMoved = size - index - 1;
        if (numMoved > 0)
            System.arraycopy(elementData, index+1, elementData, index,
                    numMoved);
        elementData[--size] = null; // clear to let GC do its work
    }

    /**
     * 删除所有元素
     */
    public void clear() {
        modCount++;

        // clear to let GC do its work
        for (int i = 0; i < size; i++)
            elementData[i] = null;

        size = 0;
    }

    // 将集合c的元素全部添加进来
    public boolean addAll(Collection<? extends E> c) {
        Object[] a = c.toArray();
        int numNew = a.length;
        // 扩容前进行检查
        ensureCapacityInternal(size + numNew);  // Increments modCount
        System.arraycopy(a, 0, elementData, size, numNew);
        size += numNew;
        return numNew != 0;
    }

    // 从某个位置开始添加，并把index（包含index）之后的元素向后移动
    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);

        Object[] a = c.toArray();
        int numNew = a.length;
        ensureCapacityInternal(size + numNew);  // Increments modCount

        int numMoved = size - index;
        if (numMoved > 0)
            System.arraycopy(elementData, index, elementData, index + numNew,
                    numMoved);

        System.arraycopy(a, 0, elementData, index, numNew);
        size += numNew;
        return numNew != 0;
    }

    // 删除给定范围内的元素，其中不包括toIndex的元素
    protected void removeRange(int fromIndex, int toIndex) {
        modCount++;
        int numMoved = size - toIndex;
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                numMoved);

        // clear to let GC do its work
        int newSize = size - (toIndex-fromIndex);
        for (int i = newSize; i < size; i++) {
            elementData[i] = null;
        }
        size = newSize;
    }

    // 数组越界检查
    private void rangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    // add和addAll方法的rangeCheck版本
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    // 越界信息
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    // 从这个ArrayList中删除集合c中存在的所有元素。如果这个调用确实改变了ArrayList，则返回true。
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);    // 检查一下集合c是否为null
        return batchRemove(c, false);
    }

    // 从这个ArrayList中删除所有与集合c中的元素不同的元素。如果这个调用确实改变了ArrayList，则返回true。
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }

    private boolean batchRemove(Collection<?> c, boolean complement) {
        final Object[] elementData = this.elementData;
        int r = 0, w = 0;
        boolean modified = false;
        try {
            for (; r < size; r++)
                if (c.contains(elementData[r]) == complement)
                    elementData[w++] = elementData[r];
        } finally {
            // Preserve behavioral compatibility with AbstractCollection,
            // even if c.contains() throws.
            if (r != size) {
                System.arraycopy(elementData, r,
                        elementData, w,
                        size - r);
                w += size - r;
            }
            if (w != size) {
                // clear to let GC do its work
                for (int i = w; i < size; i++)
                    elementData[i] = null;
                modCount += size - w;
                size = w;
                modified = true;
            }
        }
        return modified;
    }

    // 将ArrayList实例的状态保存到一个流（即序列化它）。
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException{
        // Write out element count, and any hidden stuff
        int expectedModCount = modCount;
        s.defaultWriteObject();

        // Write out size as capacity for behavioural compatibility with clone()
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (int i=0; i<size; i++) {
            s.writeObject(elementData[i]);
        }

        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    // 从流中重构ArrayList实例（即反序列化它）。
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        elementData = EMPTY_ELEMENTDATA;

        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in capacity
        s.readInt(); // ignored

        if (size > 0) {
            // be like clone(), allocate array based upon size not capacity
            int capacity = calculateCapacity(elementData, size);
            SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, capacity);
            ensureCapacityInternal(size);

            Object[] a = elementData;
            // Read in all elements in the proper order.
            for (int i=0; i<size; i++) {
                a[i] = s.readObject();
            }
        }
    }

    /**
     * 从列表中的指定位置开始，返回此列表中元素的列表迭代器（以正确的顺序）。
     * 指定的索引表示首次调用next时将返回的第一个元素。
     * 对previous的初始调用将返回指定索引减1的元素。
     *
     * 返回的列表迭代器是fail-fast（快速失败）的。所谓的快速失败，是一种错误处理机制（在系统设计中很常用）。
     * 在软件工程领域，如果序列元素在迭代期间发生改变，一种叫fail-fast迭代器会试图去引发一个错误。
     * 在Java中，这个错误叫ConcurrentModificationException。
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)    // 先判断index是否合法
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);    // 返回一个内部类ListItr对象，index是指要返回的下一个元素的index
    }

    // 返回此列表中元素的列表迭代器（按适当顺序）。
    // 返回的列表迭代器是fail-fast（快速失败）的。
    public ListIterator<E> listIterator() {
        return new ListItr(0);    // index从0开始
    }

    // 以适当的顺序返回此列表中元素的迭代器。
    // 返回的列表迭代器是fail-fast（快速失败）的。
    public Iterator<E> iterator() {
        return new Itr();    // 返回一个内部类Itr的对象
    }

    // AbstractList.Itr的优化版本
    private class Itr implements Iterator<E> {
        int cursor;       // index of next element to return
        int lastRet = -1; // index of last element returned; -1 if no such
        int expectedModCount = modCount;

        Itr() {}

        public boolean hasNext() {
            return cursor != size;    // 判断是否还有下一个元素
        }

        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();    // 进行并发修改检查，一旦发现在迭代期间发生了结构化修改，就会抛出ConcurrentModificationException
            int i = cursor;
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i >= size) {
                return;
            }
            final Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            while (i != size && modCount == expectedModCount) {
                consumer.accept((E) elementData[i++]);
            }
            // update once at end of iteration to reduce heap write traffic
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }

        final void checkForComodification() {    // 检测是否有并发修改，其原理是通过modCount和expectedModCount的值的对比
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;
            return (E) elementData[lastRet = i];
        }

        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                ArrayList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // 表示子范围视图
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, 0, fromIndex, toIndex);
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                    ") > toIndex(" + toIndex + ")");
    }

    private class SubList extends AbstractList<E> implements RandomAccess {
        private final AbstractList<E> parent;
        private final int parentOffset;
        private final int offset;
        int size;

        SubList(AbstractList<E> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;
            this.parentOffset = fromIndex;
            this.offset = offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = ArrayList.this.modCount;
        }

        public E set(int index, E e) {
            rangeCheck(index);
            checkForComodification();
            E oldValue = ArrayList.this.elementData(offset + index);
            ArrayList.this.elementData[offset + index] = e;
            return oldValue;
        }

        public E get(int index) {
            rangeCheck(index);
            checkForComodification();
            return ArrayList.this.elementData(offset + index);
        }

        public int size() {
            checkForComodification();
            return this.size;
        }

        public void add(int index, E e) {
            rangeCheckForAdd(index);
            checkForComodification();
            parent.add(parentOffset + index, e);
            this.modCount = parent.modCount;
            this.size++;
        }

        public E remove(int index) {
            rangeCheck(index);
            checkForComodification();
            E result = parent.remove(parentOffset + index);
            this.modCount = parent.modCount;
            this.size--;
            return result;
        }

        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            parent.removeRange(parentOffset + fromIndex,
                    parentOffset + toIndex);
            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }

        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();
            parent.addAll(parentOffset + index, c);
            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }

        public Iterator<E> iterator() {
            return listIterator();
        }

        public ListIterator<E> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = ArrayList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }

                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if (i >= size) {
                        return;
                    }
                    final Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i != size && modCount == expectedModCount) {
                        consumer.accept((E) elementData[offset + (i++)]);
                    }
                    // update once at end of iteration to reduce heap write traffic
                    lastRet = cursor = i;
                    checkForComodification();
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(E e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        ArrayList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(E e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
                    if (expectedModCount != ArrayList.this.modCount)
                        throw new ConcurrentModificationException();
                }
            };
        }

        /**
         * 返回指定的fromIndex（包含）和toIndex（不包括）之间的此列表部分的视图。
         * 如果fromIndex和toIndex相等，则返回的列表为空。
         * 返回的列表由此列表支持，因此返回列表中的非结构化更改将反映在此列表中，反之亦然。
         * 返回的列表支持所有可选的列表操作。
         *
         * 此方法消除了对显式范围操作（数组通常存在的那种操作）的需要。
         * 任何需要列表的操作都可以通过传递子列表视图而不是整个列表来用作范围操作。
         * 例如，下面的语句表示从列表中删除一系列元素：list.subList(from,to).clear();
         * 可以为indexOf(Object)和lastIndexOf(Object)构造类似的语句，并且Collections类中的所有算法都可以应用于subList。
         *
         * 如果支持列表（即，该列表）在结构上以除了通过返回列表之外的方式进行修改，则由该方法返回的列表的语义会变得不确定。
         * 结构修改是那些改变这个列表的大小，或者以这样一种方式干扰它，以致正在进行的迭代可能产生不正确的结果。
         */
        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        private void checkForComodification() {
            if (ArrayList.this.modCount != this.modCount)
                throw new ConcurrentModificationException();
        }

        public Spliterator<E> spliterator() {
            checkForComodification();
            return new ArrayListSpliterator<E>(ArrayList.this, offset,
                    offset + this.size, this.modCount);
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator<>(this, 0, -1, 0);
    }

    static final class ArrayListSpliterator<E> implements Spliterator<E> {

        private final ArrayList<E> list;
        private int index; // current index, modified on advance/split
        private int fence; // -1 until used; then one past last index
        private int expectedModCount; // initialized when fence set

        /** Create new spliterator covering the given  range */
        ArrayListSpliterator(ArrayList<E> list, int origin, int fence,
                             int expectedModCount) {
            this.list = list; // OK if null unless traversed
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // initialize fence to size on first use
            int hi; // (a specialized variant appears in method forEach)
            ArrayList<E> lst;
            if ((hi = fence) < 0) {
                if ((lst = list) == null)
                    hi = fence = 0;
                else {
                    expectedModCount = lst.modCount;
                    hi = fence = lst.size;
                }
            }
            return hi;
        }

        public ArrayListSpliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : // divide range in half unless too small
                    new ArrayListSpliterator<E>(list, lo, index = mid,
                            expectedModCount);
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)
                throw new NullPointerException();
            int hi = getFence(), i = index;
            if (i < hi) {
                index = i + 1;
                @SuppressWarnings("unchecked") E e = (E)list.elementData[i];
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            ArrayList<E> lst; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((lst = list) != null && (a = lst.elementData) != null) {
                if ((hi = fence) < 0) {
                    mc = lst.modCount;
                    hi = lst.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) a[i];
                        action.accept(e);
                    }
                    if (lst.modCount == mc)
                        return;
                }
            }
            throw new ConcurrentModificationException();
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        // figure out which elements are to be removed
        // any exception thrown from the filter predicate at this stage
        // will leave the collection unmodified
        int removeCount = 0;
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            @SuppressWarnings("unchecked")
            final E element = (E) elementData[i];
            if (filter.test(element)) {
                removeSet.set(i);
                removeCount++;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }

        final boolean anyToRemove = removeCount > 0;
        if (anyToRemove) {
            final int newSize = size - removeCount;
            for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
                i = removeSet.nextClearBit(i);
                elementData[j] = elementData[i];
            }
            for (int k=newSize; k < size; k++) {
                elementData[k] = null;  // Let gc do its work
            }
            this.size = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            elementData[i] = operator.apply((E) elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }
}
