
import java.util.Iterator;
import java.util.NoSuchElementException;


//This class was taken directly from the Algorithms textbook by Sedewick & Wayne.
public class IndexMinPQ<Key extends Comparable<Key>>
        implements Iterable<Integer> {

    private int maxN;        // maximum number of elements on PQ
    private int n;           // number of elements on PQ
    private int[] pq;        // binary heap using 1-based indexing
    private int[] qp;        // inverse of pq - qp[pq[i]] = pq[qp[i]] = i
    private Key[] keys;      // keys[i] = priority of i


    //Initializes an empty indexed priority queue with indices between {@code 0}
    IndexMinPQ(int maxN) {

        if (maxN < 0) {
            throw new IllegalArgumentException();
        }

        this.maxN = maxN;
        n = 0;
        keys = (Key[]) new Comparable[maxN + 1];  // make this of length maxN??
        pq = new int[maxN + 1];
        qp = new int[maxN + 1];  // make this of length maxN??

        for (int i = 0; i <= maxN; i++) {
            qp[i] = -1;
        }
    }


    //Returns true if this priority queue is empty.
    public boolean isEmpty() {
        return n == 0;
    }


    public boolean contains(int i) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        return qp[i] != -1;
    }


    //Returns the number of keys on this priority queue.
    public int size() {
        return n;
    }


    //Associates key with index {@code i}.
    public void insert(int i, Key key) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        if (contains(i)) {
            throw new IllegalArgumentException("index is already in "
                    + "the priority queue");
        }

        n++;
        qp[i] = n;
        pq[n] = i;
        keys[i] = key;
        swim(n);
    }


    //Returns an index associated with a minimum key.
    public int minIndex() {

        if (n == 0) {
            throw new NoSuchElementException("Priority queue underflow");
        }

        return pq[1];
    }


    //Returns a minimum key.
    public Key minKey() {

        if (n == 0) {
            throw new NoSuchElementException("Priority queue underflow");
        }

        return keys[pq[1]];
    }

    //Removes a minimum key and returns its associated index.
    public int delMin() {

        if (n == 0) {
            throw new NoSuchElementException("Priority queue underflow");
        }

        int min = pq[1];
        exch(1, n--);
        sink(1);
        assert min == pq[n + 1];
        qp[min] = -1;        // delete
        keys[min] = null;    // to help with garbage collection
        pq[n + 1] = -1;        // not needed
        return min;
    }


    //Returns the key associated with index {@code i}.
    public Key keyOf(int i) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        if (!contains(i)) {
            throw new NoSuchElementException("index is not "
                    + "in the priority queue");

        } else {
            return keys[i];
        }
    }


    //Change the key associated with index {@code i} to the specified value.
    public void changeKey(int i, Key key) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        if (!contains(i)) {
            throw new NoSuchElementException("index is not in the priority queue");
        }

        keys[i] = key;
        swim(qp[i]);
        sink(qp[i]);
    }


    //Change the key associated with index {@code i} to the specified value.
    public void change(int i, Key key) {
        changeKey(i, key);
    }


    //Decrease the key associated with index {@code i} to the specified value.
    public void decreaseKey(int i, Key key) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        if (!contains(i)) {
            throw new NoSuchElementException("index is not in the priority queue");
        }

        if (keys[i].compareTo(key) <= 0) {
            throw new IllegalArgumentException("Calling decreaseKey() with given "
                    + "argument would not strictly decrease the key");
        }

        keys[i] = key;
        swim(qp[i]);
    }


    //Increase the key associated with index
    public void increaseKey(int i, Key key) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        if (!contains(i)) {
            throw new NoSuchElementException("index is not in "
                    + "the priority queue");
        }

        if (keys[i].compareTo(key) >= 0) {
            throw new IllegalArgumentException("Calling increaseKey() "
                    + "with given argument would"
                    + " not strictly increase the key");
        }

        keys[i] = key;
        sink(qp[i]);
    }


    //Remove the key associated with index {@code i}.
    public void delete(int i) {

        if (i < 0 || i >= maxN) {
            throw new IndexOutOfBoundsException();
        }

        if (!contains(i)) {
            throw new NoSuchElementException("index is not in "
                    + "the priority queue");
        }

        int index = qp[i];
        exch(index, n--);
        swim(index);
        sink(index);
        keys[i] = null;
        qp[i] = -1;
    }


    //General helper functions.
    private boolean greater(int i, int j) {
        return keys[pq[i]].compareTo(keys[pq[j]]) > 0;
    }

    private void exch(int i, int j) {
        int swap = pq[i];
        pq[i] = pq[j];
        pq[j] = swap;
        qp[pq[i]] = i;
        qp[pq[j]] = j;
    }


    //Heap helper functions.
    private void swim(int k) {

        while (k > 1 && greater(k / 2, k)) {
            exch(k, k / 2);
            k = k / 2;
        }
    }


    private void sink(int k) {

        while (2 * k <= n) {
            int j = 2 * k;

            if (j < n && greater(j, j + 1)) {
                j++;
            }

            if (!greater(k, j)) {
                break;
            }

            exch(k, j);
            k = j;
        }
    }


    //Returns an iterator that iterates over the keys on the
    //priority queue in ascending order
    public Iterator<Integer> iterator() {
        return new HeapIterator();
    }


    /************************************************************************************
     * HELPER CLASS
     **********************************************************************************/

    private class HeapIterator implements Iterator<Integer> {

        private IndexMinPQ<Key> copy;  // create a new pq

        // add all elements to copy of heap
        // takes linear time since already in heap order so no keys move
        HeapIterator() {
            copy = new IndexMinPQ<Key>(pq.length - 1);

            for (int i = 1; i <= n; i++) {
                copy.insert(pq[i], keys[pq[i]]);
            }
        }


        public boolean hasNext() {
            return !copy.isEmpty();
        }


        public void remove() {
            throw new UnsupportedOperationException();
        }


        public Integer next() {

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return copy.delMin();
        }


    }

}
