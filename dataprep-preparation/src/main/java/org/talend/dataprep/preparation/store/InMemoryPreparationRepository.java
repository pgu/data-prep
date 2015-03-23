package org.talend.dataprep.preparation.store;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.talend.dataprep.preparation.Preparation;

public class InMemoryPreparationRepository implements PreparationRepository {

    private final Map<String, Preparation> store = new HashMap<>();

    @Override
    public Iterable<Preparation> list() {
        return store.values();
    }

    @Override
    public synchronized void add(Preparation preparation) {
        store.put(preparation.id(), preparation);
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public Preparation get(String id) {
        return store.get(id);
    }

    @Override
    public void remove(String id) {
        store.remove(id);
    }

    @Override
    public InputStream getCache(String preparation, String step) {
        return null;
    }

    @Override
    public boolean hasCache(String preparation, String step) {
        return false;
    }
}
