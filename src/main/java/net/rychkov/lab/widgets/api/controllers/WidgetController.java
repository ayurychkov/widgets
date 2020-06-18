package net.rychkov.lab.widgets.api.controllers;

import net.rychkov.lab.widgets.dal.model.Widget;
import net.rychkov.lab.widgets.api.model.WidgetCreateRequest;
import net.rychkov.lab.widgets.api.model.WidgetUpdateRequest;
import net.rychkov.lab.widgets.dal.repository.ConstraintViolationException;
import net.rychkov.lab.widgets.service.WidgetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.NotSupportedException;
import javax.validation.Valid;
import java.util.Collection;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/widget")
public class WidgetController {

    private final WidgetService service;

    public WidgetController(WidgetService service) {
        this.service = service;
    }

    /**
     * Get all widgets or page if page number set
     * @param page Page number
     * @return Collection of widgets or page of widgets
     */
    @GetMapping
    public ResponseEntity getList(@RequestParam(required = false) Integer page) {
        if(page!=null) {
            return new ResponseEntity(service.getAllWidgets(page), HttpStatus.OK);
        }
        else {
            return new ResponseEntity(service.getAllWidgets(), HttpStatus.OK);
        }
    }

    /**
     * Get widgets that fall entirely into the region
     * Possible 501 NOT_IMPLEMENTED response
     * @param x1 Left border (included)
     * @param y1 Top border (included)
     * @param x2 Right border (included)
     * @param y2 Bottom border (included)
     * @return Collection of widgets
     */
    @GetMapping("/filter")
    public ResponseEntity<Collection<Widget>> getFilteredByRectangle(@RequestParam int x1, @RequestParam int y1, @RequestParam int x2, @RequestParam int y2) {
        try {
            return new ResponseEntity<>(service.getFilteredByRectangle(x1, y1, x2, y2), HttpStatus.OK);
        }
        catch(NotSupportedException e) {
            return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
        }
        catch(IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    /**
     * Get widget details
     * @param widgetId Widget ID
     * @return Complete widget description
     */
    @GetMapping("/{widgetId}")
    public ResponseEntity<Widget> getWidget(@PathVariable int widgetId) {

        Widget found = service.getWidget(widgetId);

        if(found!=null) {
            return new ResponseEntity<>(found, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }

    /**
     * Update widget
     * @param widgetId Widget ID
     * @param widget Widget delta (only fields with changed values)
     * @return Updated widget
     */
    @PatchMapping("/{widgetId}")
    public ResponseEntity<Widget> updateWidget(@PathVariable int widgetId, @Valid @RequestBody WidgetUpdateRequest widget) {

        try {
            return new ResponseEntity<>(service.updateWidget(widgetId, widget), HttpStatus.OK);
        }
        catch(NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        catch (ConstraintViolationException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Create new widget
     * @param widget Widget delta (only fields with values for set)
     * @return Created widget
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Widget createWidget(@Valid @RequestBody WidgetCreateRequest widget) {
        return service.addWidget(widget);
    }

    /**
     * Remove widget
     * @param widgetId Widget ID
     * @return Removed widget
     */
    @DeleteMapping("/{widgetId}")
    public ResponseEntity<Widget> removeWidget(@PathVariable int widgetId) {

        // no widget with such id
        if(service.removeWidget(widgetId)==null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
