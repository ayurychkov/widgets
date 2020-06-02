package net.rychkov.lab.widgets;

import net.rychkov.lab.widgets.api.model.WidgetCreateRequest;
import net.rychkov.lab.widgets.api.model.WidgetUpdateRequest;
import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import net.rychkov.lab.widgets.dal.repository.WidgetRepositoryTransaction;
import net.rychkov.lab.widgets.service.WidgetService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DefaultWidgetServiceImplTests {

    /**
     * Const for gaps in z-coordinate sequence
     */
    private final int Z_STEP=16;

    @MockBean
    @Qualifier("repository")
    private WidgetRepository repository;

    @Mock
    private WidgetRepositoryTransaction tx;

    @Autowired
    private WidgetService service;


    //region Helpers

    /**
     * Generate repository content
     * @param count Elements in repository
     * @return Content list
     */
    private ArrayList<Widget> generateRepoContent(int count) {

        ArrayList<Widget> content = new ArrayList<>();

        for(int i=0; i<count; i++) {
            Widget widget = new Widget(i+1, i, i, i, i, i, new Date());
            content.add(widget);
        }

        return content;
    }

    //endregion

    @Test
    public void get() {

        // samples
        Widget origin = new Widget(1, 2, 2, 2, 2, 2, new Date());

        // mock
        when(repository.get(1)).thenReturn(origin);

        // test
        Widget result = service.getWidget(origin.getId());

        // check returned widget
        assertNotNull(result);
        assertEquals(origin.getId(), result.getId());
        assertEquals(origin.getX(), result.getX());
        assertEquals(origin.getY(), result.getY());
        assertEquals(origin.getZ(), result.getZ());
        assertEquals(origin.getWidth(), result.getWidth());
        assertEquals(origin.getHeight(), result.getHeight());
    }

    @Test
    public void getAll() {

        // repo
        final int repoCount = 16;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);

        // test
        Collection<Widget> serviceResult = service.getAllWidgets();

        // check result
        assertNotNull(serviceResult);
        assertEquals(repoCount, serviceResult.size());
    }

    @Test
    public void addWithoutZEmptyList() throws ConstraintViolationException {

        // repo
        final int repoCount = 0;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,null,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                0,
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(0, result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        verify(repository).add(delta);
    }

    @Test
    public void addWithoutZNotEmptyList() throws ConstraintViolationException {

        // repo
        final int repoCount = 5;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,null,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                repoContent.get(repoCount-1).getZ()+Z_STEP,
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(repoContent.get(repoCount-1).getZ()+Z_STEP, result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        verify(repository).add(delta);
    }

    @Test
    public void addToEmptyRepo() throws ConstraintViolationException {

        // repo
        final int repoCount = 0;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,30,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                0,
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                request.getX(),
                request.getY(),
                request.getZ(),
                request.getWidth(),
                request.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(new ArrayList<>());
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(request.getZ(), result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        verify(repository).add(delta);
    }

    @Test
    public void addToFirstPosition() throws ConstraintViolationException {

        // repo
        final int repoCount = 5;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,0,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                repoContent.get(0).getZ()-Z_STEP,
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(repoContent.get(0).getZ()-Z_STEP, result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        verify(repository).add(delta);

    }

    @Test
    public void addToLastPosition() throws ConstraintViolationException {

        // repo
        final int repoCount = 5;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,5,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                repoContent.get(repoCount-1).getZ()+Z_STEP,
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(repoContent.get(repoCount-1).getZ()+Z_STEP, result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        verify(repository).add(delta);

    }

    @Test
    public void addToMiddleWithShift() throws ConstraintViolationException {

        // repo
        final int repoCount = 8;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        int zCoord = 6;
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,zCoord,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                request.getZ(),
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(request.getZ(), result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        InOrder mockOrder = inOrder(repository);
        mockOrder.verify(repository).update(eq(8), argThat(wd -> wd.getZ()==7+Z_STEP));
        mockOrder.verify(repository).update(eq(7), argThat(wd -> wd.getZ()==7));
        mockOrder.verify(repository).add(delta);
    }

    @Test
    public void addToMiddleWithForceRebuildIndex() throws ConstraintViolationException {

        // repo
        final int repoCount = 8;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        int zCoord = 3;
        WidgetCreateRequest request = new WidgetCreateRequest(10,20,zCoord,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                request.getZ(),
                request.getWidth(),
                request.getHeight()
        );
        Widget createdWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.add(any(WidgetDelta.class))).thenReturn(createdWidget);

        // test
        Widget result = service.addWidget(request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(request.getZ(), result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        InOrder mockOrder = inOrder(repository);

        mockOrder.verify(repository).update(eq(8), argThat(wd -> wd.getZ() == (zCoord + Z_STEP*5) ));
        mockOrder.verify(repository).update(eq(7), argThat(wd -> wd.getZ() == (zCoord + Z_STEP*4) ));
        mockOrder.verify(repository).update(eq(6), argThat(wd -> wd.getZ() == (zCoord + Z_STEP*3) ));
        mockOrder.verify(repository).update(eq(5), argThat(wd -> wd.getZ() == (zCoord + Z_STEP*2) ));
        mockOrder.verify(repository).update(eq(4), argThat(wd -> wd.getZ() == (zCoord + Z_STEP*1) ));

        mockOrder.verify(repository).add(delta);
    }

    @Test
    public void updateNoZConflict() throws ConstraintViolationException {

        // repo
        final int repoCount = 8;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        int widgetId = 3;
        int zCoord = 10;
        WidgetUpdateRequest request = new WidgetUpdateRequest(10,10,zCoord,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                request.getZ(),
                request.getWidth(),
                request.getHeight()
        );
        Widget updatedWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.get(widgetId)).thenReturn(repoContent.get((int)widgetId-1));
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.update(eq(widgetId),any(WidgetDelta.class))).thenReturn(updatedWidget);

        // test
        Widget result = service.updateWidget(widgetId, request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(request.getZ(), result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        InOrder mockOrder = inOrder(repository);

        mockOrder.verify(repository).update(eq(widgetId), eq(delta) );
    }

    @Test
    public void updateZConflict() throws ConstraintViolationException {

        // repo
        final int repoCount = 8;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        int widgetId = 3;
        int zCoord = 7;
        WidgetUpdateRequest request = new WidgetUpdateRequest(10,10,zCoord,10,10);
        WidgetDelta delta = new WidgetDelta(
                request.getX(),
                request.getY(),
                request.getZ(),
                request.getWidth(),
                request.getHeight()
        );
        Widget updatedWidget = new Widget(
                1,
                delta.getX(),
                delta.getY(),
                delta.getZ(),
                delta.getWidth(),
                delta.getHeight(),
                new Date()
        );

        // mock
        when(repository.getAllOrderByZ()).thenReturn(repoContent);
        when(repository.get(widgetId)).thenReturn(repoContent.get((int)widgetId-1));
        when(repository.BeginTransaction()).thenReturn(tx);
        when(repository.update(eq(widgetId),any(WidgetDelta.class))).thenReturn(updatedWidget);

        // test
        Widget result = service.updateWidget(widgetId, request);

        // check returned widget
        assertNotNull(result);
        assertEquals(request.getX(), result.getX());
        assertEquals(request.getY(), result.getY());
        assertEquals(request.getZ(), result.getZ());
        assertEquals(request.getHeight(), result.getHeight());
        assertEquals(request.getWidth(), result.getWidth());

        // check repository call
        InOrder mockOrder = inOrder(repository);

        mockOrder.verify(repository).update(eq(8), argThat(wd -> wd.getZ() == (7 + Z_STEP) ));

        mockOrder.verify(repository).update(eq(widgetId), eq(delta) );

    }

    @Test
    public void remove() throws ConstraintViolationException {

        // repo
        final int repoCount = 8;
        ArrayList<Widget> repoContent = generateRepoContent(repoCount);

        // samples
        int widgetId = 3;

        // mock
        when(repository.remove(widgetId)).thenReturn(repoContent.get((int)widgetId-1));

        // test
        Widget result = service.removeWidget(widgetId);

        // check returned widget
        assertNotNull(result);
        assertEquals(widgetId, result.getId());

        // check repository call
        InOrder mockOrder = inOrder(repository);

        mockOrder.verify(repository).remove(eq(widgetId));
    }
}
