public class Test {

   /*// @org.junit.Test
    public void testSort() {

        TraceEvent e1 = new LocalEvent("0", 0);
        var e2 = new LocalEvent("0", 0); // send 1
        var e3 = new LocalEvent("0", 0);
        var e4 = new LocalEvent("0", 0); // receive 2
        var e5 = new LocalEvent("0", 0); // receive 3

        var v = VectorClock.create(List.of("0", "1"));

        e1.setVectorClock(v);
        e2.setVectorClock(e1.getVectorClock().increment("0"));
        e3.setVectorClock(e2.getVectorClock().increment("0"));

        TraceEvent e1_ = new LocalEvent("1", 0); // receive 1
        var e2_ = new LocalEvent("1", 0); // send 2
        var e3_ = new LocalEvent("1", 0);
        var e4_ = new LocalEvent("1", 0);
        var e5_ = new LocalEvent("1", 0); // send 3

        e1_.setVectorClock(v);
        e1_.setVectorClock(e1_.getVectorClock().merge(e2.getVectorClock()));

        e2_.setVectorClock(e1_.getVectorClock().increment("0"));
        e3_.setVectorClock(e2_.getVectorClock().increment("0"));
        e4_.setVectorClock(e3_.getVectorClock().increment("0"));
        e5_.setVectorClock(e4_.getVectorClock().increment("0"));

        e4.setVectorClock(e3.getVectorClock().increment("0"));
        e4.setVectorClock(e4.getVectorClock().merge(e2_.getVectorClock()));
        e5.setVectorClock(e4.getVectorClock().increment("0"));
        e5.setVectorClock(e5.getVectorClock().merge(e5_.getVectorClock()));

        for (int i = 0; i < 1000; i++) {

            var l = new ArrayList<>(List.of(e1, e2, e3, e4, e5, e1_, e2_, e3_, e4_, e5_));
            Collections.shuffle(l);

            //TopologicalSort.sort(l);

            assertTrue(l.indexOf(e1) < l.indexOf(e2));
            assertTrue(l.indexOf(e2) < l.indexOf(e3));
            assertTrue(l.indexOf(e3) < l.indexOf(e4));
            assertTrue(l.indexOf(e4) < l.indexOf(e5));
            assertTrue(l.indexOf(e1_) < l.indexOf(e2_));
            assertTrue(l.indexOf(e2_) < l.indexOf(e3_));
            assertTrue(l.indexOf(e3_) < l.indexOf(e4_));
            assertTrue(l.indexOf(e4_) < l.indexOf(e5_));

            // send/rcv
            assertTrue(l.indexOf(e2) < l.indexOf(e1_));
            assertTrue(l.indexOf(e2_) < l.indexOf(e4));
            assertTrue(l.indexOf(e5_) < l.indexOf(e5));
        }
    }*/
}
