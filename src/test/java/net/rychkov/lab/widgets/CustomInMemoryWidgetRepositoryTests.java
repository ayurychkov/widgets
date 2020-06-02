package net.rychkov.lab.widgets;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.CustomInMemory.RepositoryImpl;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import javax.transaction.NotSupportedException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CustomInMemoryWidgetRepositoryTests {

    @Autowired
    @Qualifier("customInMemory")
    public WidgetRepository repository;



    @Test
    @DirtiesContext
    public void get() throws ConstraintViolationException {

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
    @DirtiesContext
    public void getWrongId() throws ConstraintViolationException {

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        Widget addedWidget = repository.add(testWidget);

        Widget result = repository.get(addedWidget.getId()+1);

        assertNull(result);
    }

    @Test
    @DirtiesContext
    public void getAll() throws ConstraintViolationException {

        assertNotNull(repository.getAll());
        assertEquals(0, repository.getAll().size());

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        repository.add(testWidget);
        assertEquals(1, repository.getAll().size());

        testWidget.setZ(1);
        repository.add(testWidget);
        assertEquals(2, repository.getAll().size());

        testWidget.setZ(2);
        repository.add(testWidget);
        assertEquals(3, repository.getAll().size());
    }

    @Test
    @DirtiesContext
    public void getAllByZ() throws ConstraintViolationException {
        assertNotNull(repository.getAll());
        assertEquals(0, repository.getAll().size());

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
    @DirtiesContext
    public void add() throws ConstraintViolationException {

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        Widget result = repository.add(testWidget);

        assertNotNull(result);
        assertEquals(testWidget.getX(), result.getX());
        assertEquals(testWidget.getY(), result.getY());
        assertEquals(testWidget.getWidth(), result.getWidth());
        assertEquals(testWidget.getHeight(), result.getHeight());
    }

    @Test
    @DirtiesContext
    public void addZConflict() throws ConstraintViolationException {

        WidgetDelta testWidget = new WidgetDelta(0,0,0,0,0);

        repository.add(testWidget);

        assertThrows(ConstraintViolationException.class, () -> {repository.add(testWidget);});
    }

    @Test
    @DirtiesContext
    public void update() throws ConstraintViolationException {
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
    @DirtiesContext
    public void updateZConflict() throws ConstraintViolationException {

        WidgetDelta createDescriptor1 = new WidgetDelta(0,0,0,0,0);
        WidgetDelta createDescriptor2 = new WidgetDelta(0,0,10,0,0);
        WidgetDelta updateDescriptor = new WidgetDelta(10,10,10,10,10);

        Widget createdWidget = repository.add(createDescriptor1);
        repository.add(createDescriptor2);

        assertThrows(ConstraintViolationException.class, () -> repository.update(createdWidget.getId(), updateDescriptor));
    }

    @Test
    @DirtiesContext
    public void remove() throws ConstraintViolationException {

        WidgetDelta createDescriptor1 = new WidgetDelta(0,0,0,0,0);
        WidgetDelta createDescriptor2 = new WidgetDelta(0,0,10,0,0);
        WidgetDelta createDescriptor3 = new WidgetDelta(10,10,20,10,10);

        Widget createdWidget = repository.add(createDescriptor1);
        repository.add(createDescriptor2);
        repository.add(createDescriptor3);

        repository.remove(createdWidget.getId());

        assertEquals(2, repository.getAll().size());
        assertEquals(2, repository.getAllOrderByZ().size());
        assertNull(repository.get(createdWidget.getId()));

    }

    @Test
    @DirtiesContext
    public void filteredByRectangle() throws ConstraintViolationException, NotSupportedException {

        int x1=0,x2=50,y1=0,y2=100;

        repository.add(new WidgetDelta(10,10,3,11,12));
        repository.add(new WidgetDelta(200,20,4,21,20));
        repository.add(new WidgetDelta(25,25,1,50,50));

        Collection<Widget> result = repository.getFilteredByRectangle(x1,y1,x2,y2);

        assertEquals(2, result.size());
        Iterator<Widget> iterator = result.iterator();
        assertEquals(25, iterator.next().getX());
        assertEquals(10, iterator.next().getX());


        //assertEquals(1,result.size());

    }
}
