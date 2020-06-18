package net.rychkov.lab.widgets;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.CustomInMemory.RepositoryImpl;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.NotSupportedException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CustomInMemoryWidgetRepositoryTests {

    @Test
    public void get() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        Widget addedWidget = repository.add(testWidget);

        Widget result = repository.get(addedWidget.getId());

        assertNotNull(result);
        assertEquals(addedWidget.getId(), result.getId());
        assertEquals(addedWidget.getX(), result.getX());
        assertEquals(addedWidget.getY(), result.getY());
        assertEquals(addedWidget.getWidth(), result.getWidth());
        assertEquals(addedWidget.getHeight(), result.getHeight());
    }

    @Test
    public void getWrongId() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        Widget addedWidget = repository.add(testWidget);

        Widget result = repository.get(addedWidget.getId()+1);

        assertNull(result);
    }

    @Test
    public void getAllByZ() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        assertNotNull(repository.getAllOrderByZ());
        assertEquals(0, repository.getAllOrderByZ().size());

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);


        // adding first widget
        repository.add(testWidget);

        assertEquals(1, repository.getAllOrderByZ().size());


        // adding second widget
        testWidget.setZ(-1);
        repository.add(testWidget);
        List<Widget> all = new ArrayList<>(repository.getAllOrderByZ());

        assertEquals(2, all.size());
        // check order
        for(int i=1; i<all.size(); i++) {
            assertTrue(all.get(i).getZ()>all.get(i-1).getZ());
        }


        // adding third widget
        testWidget.setZ(1);
        repository.add(testWidget);
        all = new ArrayList<>(repository.getAllOrderByZ());

        assertEquals(3, all.size());
        // check order
        for(int i=1; i<all.size(); i++) {
            assertTrue(all.get(i).getZ()>all.get(i-1).getZ());
        }

    }


    @Test
    public void add() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        Widget result = repository.add(testWidget);

        assertNotNull(result);
        assertEquals(testWidget.getX(), result.getX());
        assertEquals(testWidget.getY(), result.getY());
        assertEquals(testWidget.getWidth(), result.getWidth());
        assertEquals(testWidget.getHeight(), result.getHeight());
    }

    @Test
    public void addZConflict() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        repository.add(testWidget);

        assertThrows(ConstraintViolationException.class, () -> {repository.add(testWidget);});
    }

    @Test
    public void update() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta createDescriptor = new WidgetDelta(0,0,0,0,0);
        WidgetDelta updateDescriptor = new WidgetDelta(10,10,10,10,10);

        Widget createdWidget = repository.add(createDescriptor);

        Widget updatedWidget = repository.update(createdWidget.getId(), updateDescriptor);

        assertNotNull(updatedWidget);
        assertEquals(createdWidget.getId(), updatedWidget.getId());
        assertEquals(updateDescriptor.getX(), updatedWidget.getX());
        assertEquals(updateDescriptor.getY(), updatedWidget.getY());
        assertEquals(updateDescriptor.getWidth(), updatedWidget.getWidth());
        assertEquals(updateDescriptor.getHeight(), updatedWidget.getHeight());
    }

    @Test
    public void updateZConflict() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta createDescriptor1 = new WidgetDelta(0,0,0,0,0);
        WidgetDelta createDescriptor2 = new WidgetDelta(0,0,10,0,0);
        WidgetDelta updateDescriptor = new WidgetDelta(10,10,10,10,10);

        Widget createdWidget = repository.add(createDescriptor1);
        repository.add(createDescriptor2);

        assertThrows(ConstraintViolationException.class, () -> repository.update(createdWidget.getId(), updateDescriptor));
    }

    @Test
    public void remove() throws ConstraintViolationException {

        WidgetRepository repository = new RepositoryImpl();

        WidgetDelta createDescriptor1 = new WidgetDelta(0,0,0,0,0);
        WidgetDelta createDescriptor2 = new WidgetDelta(0,0,10,0,0);
        WidgetDelta createDescriptor3 = new WidgetDelta(10,10,20,10,10);

        Widget createdWidget = repository.add(createDescriptor1);
        repository.add(createDescriptor2);
        repository.add(createDescriptor3);

        repository.remove(createdWidget.getId());

        assertEquals(2, repository.getAllOrderByZ().size());
        assertNull(repository.get(createdWidget.getId()));

    }

    @Test
    public void filteredByRectangle() throws ConstraintViolationException, NotSupportedException {

        WidgetRepository repository = new RepositoryImpl();

        int x1=0,x2=50,y1=0,y2=100;

        Widget w1 = repository.add(new WidgetDelta(10,10,3,11,12));
        Widget w2 = repository.add(new WidgetDelta(200,20,4,21,20));
        Widget w3 = repository.add(new WidgetDelta(25,25,1,50,50));

        Collection<Widget> result = repository.getFilteredByRectangle(x1,y1,x2,y2);

        assertEquals(2, result.size());

        boolean containWidget = false;
        assertTrue(result.stream().map(w -> w.equals(w1)).reduce((l, r) -> l || r).orElse(false));
        assertFalse(result.stream().map(w -> w.equals(w2)).reduce((l, r) -> l || r).orElse(false));
        assertTrue(result.stream().map(w -> w.equals(w3)).reduce((l, r) -> l || r).orElse(false));

        //assertEquals(1,result.size());

    }

    static class WriteMonitor {
        volatile public boolean complete = false;
    }

    @Test
    public void ThreadedRead() throws ConstraintViolationException, InterruptedException {

        RepositoryImpl repository = new RepositoryImpl();
        repository.add(new WidgetDelta(1,1,0,1,1));

        AtomicInteger writeIteration = new AtomicInteger(1);
        AtomicInteger readIteration = new AtomicInteger(1);

        int numberOfThreads = 4;
        ExecutorService service = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger wrongReadCount = new AtomicInteger();

        // reader
        service.submit(() -> {
            try {
                for(int i=2; i<1000;i++) {
                    while (writeIteration.get() <= i) {
                        Thread.sleep(1);
                    }
                    wrongReadCount.addAndGet(repository.get(1).getZ()==i ? 0 : 1);
                    readIteration.set(i);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        });

        // updater
        service.submit(() -> {
            try {
                for(int i=1; i<1000; i++) {
                    repository.update(1, new WidgetDelta(null, null, i, null, null));
                    writeIteration.getAndAdd(1);
                    while(readIteration.get()<i) {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        });

        // creator
        service.submit(() -> {
            try {
                for(int i=1001;i<2000; i++) {
                    repository.add(new WidgetDelta(i, i, i, i, i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        });

        // index reader
        service.submit(() -> {
            try {
                for(int i=1001;i<2000; i++) {
                    repository.getAllOrderByZ();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            latch.countDown();
        });


        latch.await();

        assertEquals(0,wrongReadCount.get());
    }
}
