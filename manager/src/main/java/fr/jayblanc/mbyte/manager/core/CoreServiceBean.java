package fr.jayblanc.mbyte.manager.core;

import fr.jayblanc.mbyte.manager.auth.AuthenticationService;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.store.StoreManager;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;
import fr.jayblanc.mbyte.manager.store.StoreProviderNotFoundException;
import fr.jayblanc.mbyte.manager.topology.TopologyService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CoreServiceBean implements CoreService {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

    @Inject EntityManager em;
    @Inject AuthenticationService authenticationService;
    @Inject StoreManager manager;
    @Inject TopologyService topology;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store createStore(String name) {
        LOGGER.log(Level.INFO, "Creating new store with name: {0}", name);
        String owner = authenticationService.getConnectedProfile().getUsername();
        
        // Create a new store (multi-store support - no check for existing stores)
        Store store = new Store();
        store.setId(UUID.randomUUID().toString());
        store.setName(name);
        store.setCreationDate(System.currentTimeMillis());
        store.setOwner(owner);
        store.setUsage(0);
        store.setStatus(Store.Status.PENDING);
        
        try {
            // Create Docker container with pattern: owner-storeIdShort
            String storeIdentifier = owner + "-" + store.getId().substring(0, 8);
            LOGGER.log(Level.INFO, "Creating Docker container with identifier: {0}", storeIdentifier);
            String output = manager.getProvider().createApp(storeIdentifier, store.getOwner(), store.getName());
            
            // Lookup the new store location in Consul using the store identifier
            // Wait up to 15 seconds for the store to register in Consul
            // Note: TopologyService.lookup() already adds "mbyte.store." prefix
            String location = null;
            int maxRetries = 15;
            for (int i = 0; i < maxRetries; i++) {
                location = topology.lookup(storeIdentifier);
                if (location != null) {
                    LOGGER.log(Level.INFO, "Store location found in Consul: {0}", location);
                    break;
                }
                LOGGER.log(Level.INFO, "Waiting for store to register in Consul (attempt {0}/{1})", new Object[]{i + 1, maxRetries});
                Thread.sleep(1000); // Wait 1 second before retry
            }
            
            if (location == null) {
                LOGGER.log(Level.WARNING, "Store did not register in Consul after {0} seconds", maxRetries);
            }
            
            String sanitizedLocation = sanitizeLocation(location);
            LOGGER.log(Level.INFO, "Sanitized location: {0}", sanitizedLocation);
            store.setLocation(sanitizedLocation);
            LOGGER.log(Level.INFO, "Location set on store entity: {0}", store.getLocation());
            store.setStatus(Store.Status.AVAILABLE);
            store.setLog(output);
        } catch (StoreProviderException | StoreProviderNotFoundException e) {
            LOGGER.log(Level.WARNING, "Unable to create store container, see logs", e);
            store.setStatus(Store.Status.PENDING);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for Consul registration", e);
            Thread.currentThread().interrupt();
            store.setStatus(Store.Status.PENDING);
        }
        
        em.persist(store);
        LOGGER.log(Level.INFO, "Store created successfully with ID: {0}", store.getId());
        return store;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Store getConnectedUserStore() throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "Getting store for connected user");
        String owner = authenticationService.getConnectedProfile().getUsername();
        Store store = findByOwner(owner);
        // Try multiple patterns for location lookup
        // Note: TopologyService.lookup() already adds "mbyte.store." prefix
        String location = lookup(owner);
        if ( location != null ) {
            LOGGER.log(Level.INFO, "Found store instance at location: " + location);
            store.setLocation(sanitizeLocation(location));
        } else {
            LOGGER.log(Level.INFO, "Store NOT found in the topology, attempting re-provision");
            try {
                String output = manager.getProvider().createApp(store.getId(), store.getOwner(), store.getName());
                String newLocation = lookup("mbyte.store." + owner);
                if (newLocation == null) {
                    newLocation = lookup(owner);
                }
                store.setLocation(sanitizeLocation(newLocation) != null ? sanitizeLocation(newLocation) : "#");
                store.setStatus(Store.Status.AVAILABLE);
                store.setLog(output);
            } catch (StoreProviderException | StoreProviderNotFoundException e) {
                LOGGER.log(Level.INFO, "Re-provision failed", e);
                store.setLocation("#");
            }
        }
        return store;
    }

    private String sanitizeLocation(String location) {
        if (location == null) {
            return null;
        }
        // Consul tag sometimes prefixes with "fqdn."; strip it so the href is a valid URL
        if (location.startsWith("fqdn.")) {
            return location.substring("fqdn.".length());
        }
        return location;
    }

    private void updateStatus(String owner, Store.Status status) throws StoreNotFoundException {
        Store store = findByOwner(owner);
        store.setStatus(status);
    }

    private Store findByOwner(String owner) throws StoreNotFoundException {
        try {
            Store store = em.createNamedQuery("Store.findByOwner", Store.class).setParameter("owner", owner).getSingleResult();
            if ( store == null ) {
                throw new StoreNotFoundException(owner);
            }
            return store;
        } catch (NoResultException e) {
            throw new StoreNotFoundException(owner);
        }
    }

    private String lookup(String name) throws CoreServiceException {
        return topology.lookup(name);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<Store> getAllUserStores() throws CoreServiceException {
        LOGGER.log(Level.INFO, "Getting all stores for connected user");
        String owner = authenticationService.getConnectedProfile().getUsername();
        List<Store> stores = em.createNamedQuery("Store.findAllByOwner", Store.class)
                .setParameter("owner", owner)
                .getResultList();
        
        for (Store store : stores) {
            // Try multiple patterns for location lookup
            // Note: TopologyService.lookup() already adds "mbyte.store." prefix
            String location = null;
            String storeIdentifier = owner + "-" + store.getId().substring(0, 8);
            // First try: storeIdentifier (TopologyService adds mbyte.store. prefix)
            location = lookup(storeIdentifier);
            if (location == null) {
                // Fallback: try owner for legacy/first store
                location = lookup(owner);
            }
            if (location != null) {
                store.setLocation(sanitizeLocation(location));
            } else {
                LOGGER.log(Level.WARNING, "Store location not found in topology for {0}", store.getId());
            }
        }
        return stores;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Store getStoreById(String id) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "Getting store by id: {0}", id);
        String owner = authenticationService.getConnectedProfile().getUsername();
        try {
            Store store = em.createNamedQuery("Store.findById", Store.class)
                    .setParameter("id", id)
                    .getSingleResult();
            if (!store.getOwner().equals(owner)) {
                throw new StoreNotFoundException("Store does not belong to user");
            }
            // Try multiple patterns for location lookup
            // Note: TopologyService.lookup() already adds "mbyte.store." prefix
            String location = null;
            String storeIdentifier = store.getOwner() + "-" + store.getId().substring(0, 8);
            location = lookup(storeIdentifier);
            if (location == null) {
                // Fallback: try owner for legacy store
                location = lookup(owner);
            }
            if (location != null) {
                store.setLocation(sanitizeLocation(location));
            }
            return store;
        } catch (NoResultException e) {
            throw new StoreNotFoundException(id);
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store renameStore(String id, String newName) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "Renaming store {0} to {1}", new Object[]{id, newName});
        String owner = authenticationService.getConnectedProfile().getUsername();
        Store store = getStoreById(id);
        if (!store.getOwner().equals(owner)) {
            throw new StoreNotFoundException("Store does not belong to user");
        }
        store.setName(newName);
        em.merge(store);
        return store;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void deleteStore(String id) throws StoreNotFoundException, CoreServiceException, StoreProviderException {
        LOGGER.log(Level.INFO, "Deleting store: {0}", id);
        String owner = authenticationService.getConnectedProfile().getUsername();
        Store store = getStoreById(id);
        if (!store.getOwner().equals(owner)) {
            throw new StoreNotFoundException("Store does not belong to user");
        }
        try {
            manager.getProvider().destroyApp(store.getOwner() + "-" + store.getId().substring(0, 8));
        } catch (StoreProviderNotFoundException e) {
            LOGGER.log(Level.WARNING, "Provider not found, skipping container deletion", e);
        }
        em.remove(store);
    }

}
