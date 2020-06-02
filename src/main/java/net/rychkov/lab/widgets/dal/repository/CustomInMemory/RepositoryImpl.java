package net.rychkov.lab.widgets.dal.repository.CustomInMemory;

import net.rychkov.lab.widgets.dal.model.Page;
import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.dal.model.WidgetDelta;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.dal.repository.WidgetRepository;
import net.rychkov.lab.widgets.dal.repository.WidgetRepositoryTransaction;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Repository("customInMemory")
public class RepositoryImpl implements WidgetRepository {

    /**
     * Last version of repository content (widgets and indexes)
     */
    private Version lastVersion;

    /**
     * Transactions lock (only one transaction at same time)
     */
    private final ReentrantLock txLock;

    /**
     * Sequence for transaction IDs
     */
    private final AtomicLong txSequence;

    /**
     * Transaction's version of content
     */
    private Version txVersion = null;

    /**
     * Active transaction ID
     */
    private long txId = 0;

    /**
     * Active transaction's thread ID
     * All write-operations of this thread will be in transaction
     */
    private long txThreadId = 0;

    /**
     * Sequence for widget's id
     */
    private final AtomicInteger idSequence;

    public RepositoryImpl() {
        txLock = new ReentrantLock();
        txSequence = new AtomicLong(1);
        lastVersion = new Version();
        idSequence = new AtomicInteger(1);
    }

    /**
     * Get version of content for current thread
     * if thread has active transaction - transaction version of content
     * else - last common version
     * @return Version of content
     */
    private Version getVersion() {
        if(txThreadId !=Thread.currentThread().getId()) {
            return lastVersion;
        }
        else {
            return txVersion;
        }
    }

    //region Transactions

    public WidgetRepositoryTransaction BeginTransaction() {
        txLock.lock();
        txThreadId = Thread.currentThread().getId();
        txId = txSequence.getAndAdd(1);
        Transaction tx = new Transaction(txId, this);
        txVersion = new Version(lastVersion);
        return tx;
    }

    public void txCommit(WidgetRepositoryTransaction tx) {
        // if not current transaction - say goodbye
        if(tx==null || tx.getId()!= txId) {
            // TODO: create custom exception
            throw new NoSuchElementException("Wrong transaction for commit");
        }

        // apply new data version
        lastVersion = txVersion;

        // clear transaction
        txThreadId = 0;
        txId = 0;
        txVersion = null;

        txLock.unlock();
    }

    public void txRollback(WidgetRepositoryTransaction tx) {
        // if not current transaction - say goodbye
        if(tx==null || tx.getId()!= txId) {
            // TODO: create custom exception
            throw new NoSuchElementException("Wrong transaction for rollback");
        }

        // clear transaction
        txThreadId = 0;
        txId = 0;
        txVersion = null;

        txLock.unlock();
    }

    private WidgetRepositoryTransaction beginTransactionIfNotExist() {

        if(txThreadId !=Thread.currentThread().getId()) {
            return BeginTransaction();
        }

        return null;
    }

    private void commitTransactionIfExist(WidgetRepositoryTransaction tx) {
        if(tx!=null) { tx.commit(); }
    }

    private void rollbackTransactionIfExist(WidgetRepositoryTransaction tx) {
        if(tx!=null) { tx.rollback(); }
    }

    //endregion

    //region Read

    @Override
    public Widget get(int id) {
        return getVersion().getWidgets().get(id);
    }

    @Override
    public Collection<Widget> getAll() {
        return getVersion().getWidgets().values();
    }

    @Override
    public Page<Widget> getAll(int pageNum, int pageSize) {

        Version currentVersion = getVersion();

        int elementCount = currentVersion.getWidgets().size();

        return new Page<>(
                pageNum,
                pageSize,
                (elementCount/pageSize)+1,
                elementCount,
                currentVersion.getWidgets().values().stream().skip(pageNum*pageSize).limit(pageSize).collect(Collectors.toList())
        );
    }

    @Override
    public Collection<Widget> getAllOrderByZ() {
        return getVersion().getZIndex().values();
    }

    @Override
    public Page<Widget> getAllOrderByZ(int pageNum, int pageSize) {
        Version currentVersion = getVersion();

        int elementCount = currentVersion.getZIndex().size();

        return new Page<>(
                pageNum,
                pageSize,
                (elementCount/pageSize)+1,
                elementCount,
                currentVersion.getZIndex().values().stream().skip(pageNum*pageSize).limit(pageSize).collect(Collectors.toList())
        );
    }

    @Override
    public Collection<Widget> getFilteredByRectangle(int x1, int y1, int x2, int y2) {

        // include high borders
        int includedBottomBorder = y2+1;
        int includedRightBorder = x2+1;

        Version txVersion = getVersion();

        HashMap<Integer, Widget> hash = new HashMap<Integer, Widget>();
        TreeSet<Widget> result = new TreeSet<>(Comparator.comparing(Widget::getZ));

        txVersion.getRIndex().tailMap(x1).headMap(includedRightBorder).values().forEach(x->
                x.tailMap(y1).headMap(includedBottomBorder).values().forEach(y->
                        y.forEach(w->
                        {
                            if (hash.containsKey(w.getId())) {
                                result.add(w);
                            }
                            else {
                                hash.put(w.getId(), w);
                            }
                        })
                )
        );

        return result;
    }

    //endregion

    // region rIndex helpers

    private TreeMap<Integer, TreeMap<Integer, ArrayList<Widget>>> rIndex = new TreeMap<>();

    public void addPointToRIndex(Version version, int x, int y, Widget widget) {
        version.getRIndex().computeIfAbsent(x, k -> new TreeMap<>())
                .computeIfAbsent(y, k -> new ArrayList<>())
                .add(widget);
    }

    public void addToRIndex(Version version, Widget widget) {
        // left top corner
        addPointToRIndex(version, widget.getX()-widget.getWidth()/2, widget.getY()-widget.getHeight()/2, widget);
        // right bottom corner
        addPointToRIndex(version, widget.getX()+widget.getWidth()/2, widget.getY()+widget.getHeight()/2, widget);
    }

    public void deletePointFromRIndex(Version version, int x, int y, Widget widget) {
        TreeMap<Integer, ArrayList<Widget>> yLayer = version.getRIndex().get(x);
        ArrayList<Widget> widgets = yLayer.get(y);
        widgets.remove(widget);

        // clear if needed
        if (widgets.isEmpty()) {
            yLayer.remove(y);
        }

        if(yLayer.isEmpty()) {
            rIndex.remove(x);
        }
    }

    public void deleteFromRIndex(Version version, Widget widget) {
        // left top corner
        deletePointFromRIndex(version, widget.getX()-widget.getWidth()/2, widget.getY()-widget.getHeight()/2, widget);
        // right bottom corner
        deletePointFromRIndex(version, widget.getX()+widget.getWidth()/2, widget.getY()+widget.getHeight()/2, widget);
    }

    // endregion

    //region Write

    @Override
    public Widget add(@NotNull WidgetDelta widgetDelta) throws ConstraintViolationException {

        // check validation
        if(widgetDelta==null) {
            throw new IllegalArgumentException("widgetDelta must be not null");
        }

        if(widgetDelta.getZ()==null) {
            throw new NullPointerException("Widget must have none null z-coordinate");
        }


        // start new transaction (if not exist global transaction)
        WidgetRepositoryTransaction localTx = beginTransactionIfNotExist();

        try {
            Version txVersion = getVersion();

            // check z-uniq constrain
            if(txVersion.getZIndex().containsKey(widgetDelta.getZ())) {
                throw new ConstraintViolationException("Not unique z-coordinate");
            }

            // create widget
            int newId = idSequence.getAndAdd(1);
            Widget createdWidget = widgetDelta.createNewWidget(newId);

            // add to z-index
            txVersion.getZIndex().put(createdWidget.getZ(), createdWidget);

            // add to r-index
            addToRIndex(txVersion, createdWidget);

            // add to repository
            txVersion.getWidgets().put(newId, createdWidget);

            // commit local transaction (if created), don't commit global transaction
            commitTransactionIfExist(localTx);

            return createdWidget;
        }
        catch( Exception e) {
            rollbackTransactionIfExist(localTx);
            throw e;
        }
    }

    @Override
    public Widget remove(int id) {

        WidgetRepositoryTransaction localTx = beginTransactionIfNotExist();

        Version txVersion = getVersion();

        // remove widget
        Widget result = txVersion.getWidgets().remove(id);

        // remove from z-index
        txVersion.getZIndex().remove(result.getZ(), result);

        // remove from r-index
        deleteFromRIndex(txVersion, result);

        commitTransactionIfExist(localTx);

        return result;
    }

    /**
     * Update widget by delta
     * @param widgetId Widget id
     * @param widgetDelta Widget changed properties (other - null)
     * @return Updated widget full description
     * @throws IllegalArgumentException WidgetDelta must be not null
     * @throws NoSuchElementException No widget with such id in repository
     */
    @Override
    public Widget update(int widgetId, WidgetDelta widgetDelta)
            throws IllegalArgumentException, NoSuchElementException, ConstraintViolationException {

        if(widgetDelta==null) {
            throw new IllegalArgumentException("widgetDelta must be not null");
        }

        WidgetRepositoryTransaction localTx = beginTransactionIfNotExist();

        Version txVersion = getVersion();

        Widget originWidget = txVersion.getWidgets().get(widgetId);

        if(originWidget==null) {
            throw new NoSuchElementException("No widget with id \""+widgetId+"\" found in repository");
        }

        Widget changedWidget = widgetDelta.createUpdatedWidget(originWidget);

        // if no changes - rollback and return
        if(changedWidget==null) {
            rollbackTransactionIfExist(localTx);
            return originWidget;
        }

        // check constrains (unique z-coordinate)
        if(
                widgetDelta.getZ()!=null &&
                widgetDelta.getZ()!=originWidget.getZ() &&
                txVersion.getZIndex().containsKey(widgetDelta.getZ())
        ) {
            // already has widget with same z-coordinate
            rollbackTransactionIfExist(localTx);
            throw new ConstraintViolationException("Not unique z-coordinate");
        }

        // update widget
        txVersion.getWidgets().put(changedWidget.getId(), changedWidget);

        // update z-index
        txVersion.getZIndex().remove(originWidget.getZ());
        txVersion.getZIndex().put(changedWidget.getZ(), changedWidget);

        // update r-index
        deleteFromRIndex(txVersion, originWidget);
        addToRIndex(txVersion, changedWidget);

        commitTransactionIfExist(localTx);

        return changedWidget;
    }

    //endregion
}
