package fr.jayblanc.mbyte.manager.core;

import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;

import java.util.List;

public interface CoreService {

    Store createStore(String name);

    Store getConnectedUserStore() throws StoreNotFoundException, CoreServiceException;

    List<Store> getAllUserStores() throws CoreServiceException;

    Store getStoreById(String id) throws StoreNotFoundException, CoreServiceException;

    Store renameStore(String id, String newName) throws StoreNotFoundException, CoreServiceException;

    void deleteStore(String id) throws StoreNotFoundException, CoreServiceException, StoreProviderException;

}
