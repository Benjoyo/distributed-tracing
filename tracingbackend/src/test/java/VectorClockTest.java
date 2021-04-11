

public class VectorClockTest {

   /* @Test
    public void test() {

        VectorClock<String> v1 = VectorClock.fromString("{0=3, 2=1, 3=3}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=3, 1=1, 2=1, 3=3}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(-1, c1);
    }

    @Test
    public void test1() {

        VectorClock<String> v1 = VectorClock.fromString("{0=2, 1=1, 2=1, 3=2}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=3, 1=1, 2=1, 3=3}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(-1, c1);
    }

    @Test
    public void test2() {

        VectorClock<String> v1 = VectorClock.fromString("{0=3, 1=1, 2=1, 3=3}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=2, 1=1, 3=3}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(1, c1);
    }

    @Test
    public void test3() {

        VectorClock<String> v1 = VectorClock.fromString("{0=2, 2=1, 3=3}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=2, 1=1, 3=3}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(0, c1);
    }

    @Test
    public void test4() {

        VectorClock<String> v1 = VectorClock.fromString("{0=2, 1=1, 3=3}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=2, 2=1, 3=3}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(0, c1);
    }

    @Test
    public void test5() {

        VectorClock<String> v1 = VectorClock.fromString("{0=2, 1=1, 2=1, 3=3}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=3, 1=1, 2=1, 3=2}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(0, c1);
    }

    @Test
    public void test6() {

        VectorClock<String> v1 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=9, 5=7, 6=10, 7=8}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=24, 5=15, 6=10, 7=8}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(-1, c1);
    }

    @Test
    public void test7() {

        VectorClock<String> v1 = VectorClock.fromString("{0=12, 1=16, 2=28, 3=17, 4=9, 5=7, 6=10, 7=8}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=24, 5=15, 6=10, 7=8}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(0, c1);
    }

    @Test
    public void test8() {

        VectorClock<String> v1 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=9, 5=7, 6=10, 7=8}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=9, 5=7, 6=10, 7=7}", String::toString);

        var c1 = v1.compareTo(v2);

        System.out.println(v1);
        System.out.println(v2);

        assertEquals(1, c1);
    }

    @Test
    public void testSortTopo() {

        VectorClock<String> v1 = VectorClock.fromString("{0=18, 1=19, 2=15, 3=20, 4=15, 5=17, 6=14, 7=21}", String::toString);
        VectorClock<String> v2 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=9, 5=7, 6=10, 7=8}", String::toString);
        VectorClock<String> v3 = VectorClock.fromString("{0=23, 1=28, 2=15, 3=23, 4=15, 5=22, 6=14, 7=19}", String::toString); // rcv
        VectorClock<String> v4 = VectorClock.fromString("{0=11, 1=16, 2=28, 3=17, 4=24, 5=15, 6=10, 7=8}", String::toString);
        VectorClock<String> v5 = VectorClock.fromString("{0=15, 1=19, 2=15, 3=20, 4=15, 5=22, 6=14, 7=19}", String::toString); // send
        VectorClock<String> v6 = VectorClock.fromString("{0=15, 1=25, 2=15, 3=27, 4=9, 5=5, 6=9, 7=14}", String::toString);
        VectorClock<String> v7 = VectorClock.fromString("{0=18, 1=25, 2=26, 3=25, 4=15, 5=17, 6=19, 7=24}", String::toString);
        VectorClock<String> v8 = VectorClock.fromString("{0=15, 1=30, 2=15, 3=23, 4=9, 5=5, 6=9, 7=14}", String::toString);

        var s = new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
        var r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v2, v1, v3, v4, v8, v7, v6, v5));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v2, v3, v1, v4, v6, v5, v8, v7));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v7, v8, v2, v3, v5, v4, v1, v6));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v4, v2, v6, v1, v8, v3, v7, v5));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v4, v2, v6, v1, v8, v5, v7, v3));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v4, v2, v5, v1, v8, v3, v6, v5));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        s = new ArrayList<>(List.of(v5, v2, v6, v1, v8, v7, v3));
        r = TopologicalSort.sort(s);
        assertTrue(r.indexOf(v5) < r.indexOf(v3));

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(s);
            r = TopologicalSort.sort(s);
            assertTrue(r.indexOf(v5) < r.indexOf(v3));
        }
    }

    @Test
    public void testSortTopo2() {

        VectorClock<String> v1 = VectorClock.fromString("{0=18, 1=19, 2=15, 3=20, 4=15, 5=17, 6=14, 7=21}", String::toString);
        VectorClock<String> v2 = v1.increment("2");
        VectorClock<String> v3 = v2.increment("3");
        VectorClock<String> v4 = v3.increment("1");
        VectorClock<String> v5 = v4.increment("5");
        VectorClock<String> v6 = v5.increment("7");
        VectorClock<String> v7 = v6.increment("6");
        VectorClock<String> v8 = v7.increment("2");

        for (int i = 0; i < 1000; i++) {
            var l = new ArrayList<>(List.of(v1, v2, v3, v4, v5, v6, v7, v8));
            Collections.shuffle(l);
            var r = TopologicalSort.sort(l);

            assertEquals(List.of(v1, v2, v3, v4, v5, v6, v7, v8), r);
        }
    }

    @Test
    public void testSortTopo3() {

        VectorClock<String> v1 = VectorClock.fromString("{0=18, 1=19, 2=15, 3=20, 4=15, 5=17, 6=14, 7=21}", String::toString);
        VectorClock<String> v2 = v1.increment("2");

        var l = new ArrayList<>(List.of(v2, v1));
        var r = TopologicalSort.sort(l);

        assertEquals(List.of(v1, v2), r);

        l = new ArrayList<>(List.of(v1));
        r = TopologicalSort.sort(l);

        assertEquals(List.of(v1), r);
    }*/
}
