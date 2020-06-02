package net.rychkov.lab.widgets.dal.repository.h2;

import net.rychkov.lab.widgets.dal.model.Widget;
import org.springframework.data.jpa.repository.JpaRepository;


//@Repository
public interface DbWidgetRepository extends JpaRepository<Widget, Integer> {
}
