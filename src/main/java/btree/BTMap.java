package btree;

import java.util.ArrayList;
import java.util.List;

public class BTMap<K extends Comparable<? super K>, V> {

    private static final int M = 3;           // B木のオーダー
    private static final int D = M / 2; // M = 2D + 1

    private Node<K, V> root = new Node<K, V>();

    public void insert(final K key, final V val) {
        final var e = new Elem<>(key, val);

        var needSplit = root.insert(e);
        if (needSplit) {
            var split = root.split();
            var newRoot = new Node<K, V>();
            newRoot.es.add(split.x);
            newRoot.ns.add(split.y);
            newRoot.ns.add(split.z);
            root = newRoot;
        }
    }

    public void remove(final K key) {
        var needRebalance = root.remove(key);
        if (needRebalance) {
            root = root.ns.get(0);
        }
    }

    @Override
    public String toString() {
        if (root == null) {
            return "empty";
        }

        final var nl = System.lineSeparator();
        final var sb = new StringBuilder();
        sb.append(root.toString());
        sb.append(nl);

        var children = root.ns;
        while (children.size() > 0) {
            children.stream().forEach((n) -> {
                sb.append(n.toString());
                sb.append(" ");
            });
            sb.append(nl);
            var next = new ArrayList<Node<K, V>>();
            children.stream().forEach(n -> {
                next.addAll(n.ns);
            });
            children = next;
        }
        return sb.toString();
    }


    private static class Pair<X, Y> {
        X x;
        Y y;

        Pair(final X x, final Y y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    private static class Triple<X, Y, Z> {
        X x;
        Y y;
        Z z;

        Triple(final X x, final Y y, final Z z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class Elem<K extends Comparable<? super K>, V> { // 要素の型
        K key;
        V value;

        Elem(final K key, final V x) {
            this.key = key;
            this.value = x;
        }

        @Override
        public String toString() {
            return key.toString();
        }
    }

    private class Node<K extends Comparable<? super K>, V> { // ノードの型(抽象型)
        List<Elem<K, V>> es = new ArrayList<Elem<K, V>>(); // 要素のリスト
        List<Node<K, V>> ns = new ArrayList<Node<K, V>>(); // 枝のリスト

        Node() {
        }

        // Raise needSplit event
        public boolean insert(final Elem<K, V> e) {
            final var pos = findPos(e);
            if (ns.size() == 0) {
                if (pos.y) {
                    // overwrite
                    es.set(pos.x, e);
                    return false;
                }
                // insert
                es.add(pos.x, e);
                return es.size() == M;
            }

            var delegated = ns.get(pos.x);
            var needSplit = delegated.insert(e);
            if (needSplit) {
                splitChild(pos.x, delegated);
                return es.size() == M;
            }
            return false;
        }

        public boolean remove(final K k) {
            System.out.println("RRR 000");
            final var pos = findPos2(k);
            System.out.println("pos = " + pos);
            if (ns.size() == 0) {
                // 1. Remove from Leaf
                System.out.println("Case 1 " + pos);
                if (pos.y) {
                    es.remove((int) pos.x);
                    return es.size() < D;
                }
                // Not found
                return false;
            }

            // 2. Remove from Internal Node
            if (pos.y) {
                System.out.println("Case 2");
                var removed = ns.get(pos.x).removeRightMost();
                es.set(pos.x, removed.x);
                if (removed.y) {
                    borrowOrMergeFromR(pos.x);
                }
                return es.size() < D;
            }

            // 3. Remove from child node
            System.out.println("Case 3");
            var needX = ns.get(pos.x).remove(k);
            if (needX) {
                if (pos.x == es.size()) {
                    borrowOrMergeFromL(pos.x);
                } else {
                    borrowOrMergeFromR(pos.x);
                }
                return es.size() < D;
            }
            return false;
        }


        private Pair<Elem<K, V>, Boolean> removeRightMost() {
            System.out.println("RRM 000");
            if (ns.size() == 0) {
                // 1. Remove from Leaf
                System.out.println("RRM LEAF");
                var last = es.size() - 1;
                var victim = es.get(last);
                es.remove(last);
                return new Pair<>(victim, es.size() == 0);
            }

            // 2. Remove from child node
            System.out.println("RRM Not LEAF");
            var last = ns.size() - 1;
            var delegatee = ns.get(last);
            var result = delegatee.removeRightMost();
            if (result.y) {
                borrowOrMergeFromL(last);
            }
            return new Pair<>(result.x, es.size() < D);
        }


        @Override
        public String toString() {
            final var sb = new StringBuilder();
            sb.append("[");
            if (es.size() == 0) {
                sb.append("]");
            } else {
                es.forEach(e -> {
                    sb.append(e.toString());
                    sb.append(",");
                });
                sb.setCharAt(sb.length() - 1, ']');
            }
            return sb.toString();
        }

        public Triple<Elem<K, V>, Node<K, V>, Node<K, V>> split() {
            var left = new Node<K, V>();
            var right = new Node<K, V>();

            final var mid = M / 2;

            for (int i = 0; i < mid; i++) {
                left.es.add(es.get(i));
            }
            for (int i = mid + 1; i < es.size(); i++) {
                right.es.add(es.get(i));
            }

            if (ns.size() > 0) {
                for (int i = 0; i < mid; i++) {
                    left.ns.add(ns.get(i));
                }
                left.ns.add(ns.get(mid));

                for (int i = mid + 1; i < es.size(); i++) {
                    right.ns.add(ns.get(i));
                }
                right.ns.add(ns.get(es.size()));
            }

            var midE = es.get(mid);
            return new Triple<>(midE, left, right);
        }

        private void splitChild(int pos, Node<K, V> child) {
            var split = child.split();
            es.add(pos, split.x);
            ns.set(pos, split.y);
            ns.add(pos + 1, split.z);
        }


        public void rebalance(int pos) {

        }

        private void borrowOrMergeFromR(int pos) {
            System.out.println("XXX borrowOrMergeFromR: " + pos);
            var binbo = ns.get(pos);
            var tonari = ns.get(pos + 1);
            var pivot = es.get(pos);
            if (tonari.es.size() > D) {
                System.out.println("Borrow!");
                // borrow
                binbo.es.add(pivot);
                if (tonari.ns.size() > 0) {
                    var borrowedN = tonari.ns.remove(0);
                    binbo.ns.add(borrowedN);
                }
                var borrowedE = tonari.es.remove(0);
                es.set(pos, borrowedE);
            } else {
                System.out.println("Merge!");
                // merge
                binbo.es.add(pivot);
                binbo.es.addAll(tonari.es);
                binbo.ns.addAll(tonari.ns);
                es.remove(pos);
                ns.remove(pos + 1);
            }
        }

        private void borrowOrMergeFromL(int pos) {
            System.out.println("XXX borrowOrMergeFromL: " + pos);
            var binbo = ns.get(pos);
            var tonari = ns.get(pos - 1);
            var pivot = es.get(pos - 1);
            if (tonari.es.size() > D) {
                // borrow
                binbo.es.add(pivot);
                if (tonari.ns.size() > 0) {
                    var borrowedN = tonari.ns.remove(tonari.es.size());
                    binbo.ns.add(borrowedN);
                }
                var borrowedE = tonari.es.remove(tonari.es.size() - 1);
                es.set(pos - 1, borrowedE);
            } else {
                // merge
                binbo.es.add(pivot);
                binbo.es.addAll(tonari.es);
                binbo.ns.addAll(tonari.ns);
                es.remove(pos);
                ns.remove(pos + 1);
            }
        }

        private Pair<Integer, Boolean> findPos(final Elem<K, V> e) {
            int i = 0;
            for (i = 0; i < es.size(); i++) {
                final int cmp = e.key.compareTo(es.get(i).key);
                if (cmp < 0) {
                    return new Pair<>(i, false);
                } else if (cmp == 0) {
                    return new Pair<>(i, true);
                }
            }
            return new Pair<>(i, false);
        }

        private Pair<Integer, Boolean> findPos2(final K k) {
            int i = 0;
            for (i = 0; i < es.size(); i++) {
                final int cmp = k.compareTo(es.get(i).key);
                if (cmp < 0) {
                    return new Pair<>(i, false);
                } else if (cmp == 0) {
                    return new Pair<>(i, true);
                }
            }
            return new Pair<>(i, false);
        }


    }


}