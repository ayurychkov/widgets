package net.rychkov.lab.widgets.dal.repository.h2;

import net.rychkov.lab.widgets.dal.model.Widget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;


public interface DbWidgetRepository extends JpaRepository<Widget, Integer> {

    /**
     * Get max z value
     * @return max z value
     */
    @Query("select max(z) from Widget")
    Integer getMaxZ();
}
