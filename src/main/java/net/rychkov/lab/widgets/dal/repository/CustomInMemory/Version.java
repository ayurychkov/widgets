package net.rychkov.lab.widgets.dal.repository.CustomInMemory;

import net.rychkov.lab.widgets.dal.model.Widget;

import java.util.*;

public class Version {
    /**
     * Map id-widget
     */
    private final Map<Integer, Widget> widgets;

    /**
     * Index by 'z'
     */
    private final TreeMap<Integer, Widget> zIndex;

    /**
     * Index by 'xy'
     */
    private final TreeMap<Integer, TreeMap<Integer, ArrayList<Widget>>> rIndex;

    public Map<Integer, Widget> getWidgets() {
        return widgets;
    }

    public TreeMap<Integer, Widget> getZIndex() {
        return zIndex;
    }

    public TreeMap<Integer, TreeMap<Integer, ArrayList<Widget>>> getRIndex() {
        return rIndex;
    }

    public Version() {
        widgets = new HashMap<>();
        zIndex = new TreeMap<>();
        rIndex = new TreeMap<>();
    }

    public Version(Version origin) {
        widgets = new HashMap<>(origin.widgets);
        zIndex = new TreeMap<>(origin.zIndex);

        // copy r-index
        rIndex = new TreeMap<>();
        origin.rIndex.forEach((kx, vx) ->
                {
                    TreeMap<Integer, ArrayList<Widget>> yLayer = new TreeMap<>();
                    rIndex.put(kx,yLayer);
                    vx.forEach((ky, vy)->
                            {
                                yLayer.put(ky, new ArrayList<>(vy));
                            });
                });
    }
}
