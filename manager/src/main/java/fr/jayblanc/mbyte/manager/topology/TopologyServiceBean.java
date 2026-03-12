package fr.jayblanc.mbyte.manager.topology;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.NotRegisteredException;
import com.orbitz.consul.model.agent.ImmutableRegCheck;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.catalog.CatalogService;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class TopologyServiceBean implements TopologyService {

    private static final Logger LOGGER = Logger.getLogger(TopologyService.class.getName());

    @Inject TopologyConfig config;

    private volatile Consul consulClient;
    private volatile String serviceName;
    private volatile String instanceId;
    private volatile boolean registered = false;

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing topology bean");
        consulClient = Consul.builder().withHttps(config.https()).withHostAndPort(HostAndPort.fromParts(config.host(), config.port())).build();
        serviceName = "mbyte.manager";
        instanceId = serviceName.concat(".1");
        Registration service = this.buildRegistration();
        consulClient.agentClient().register(service);
        try {
            consulClient.agentClient().pass(instanceId);
            registered = true;
            LOGGER.log(Level.INFO, "Instance registered with id=" + instanceId);
        } catch (NotRegisteredException e) {
            LOGGER.log(Level.WARNING, "Unable to checkin topology registration", e);
        }
    }

    @PreDestroy
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping topology bean");
        if (registered) {
            consulClient.agentClient().deregister(instanceId);
            this.registered = false;
            LOGGER.log(Level.INFO, "Service instance unregistered for id=" + instanceId);
        }
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    @Scheduled(every = "10s")
    public void checkin() {
        if (registered) {
            LOGGER.log(Level.FINEST, "Checkin manager in topology");
            try {
                consulClient.agentClient().pass(instanceId);
            } catch (NotRegisteredException e) {
                LOGGER.log(Level.WARNING, "Error while trying to checkin service in topology", e);
                Registration service = this.buildRegistration();
                consulClient.agentClient().deregister(instanceId);
                this.registered = false;
                consulClient.agentClient().register(service);
                this.registered = true;
            }
        }
    }

    @Override
    public String lookup(String name) {
        CatalogClient catalog = consulClient.catalogClient();
        // Try mbyte.store. prefix (new naming convention)
        List<CatalogService> services = catalog.getService("mbyte.store.".concat(name)).getResponse();
        LOGGER.log(Level.INFO, "Services list: " + services);
        // Fallback: try miage24.store. prefix (legacy containers)
        if (services.isEmpty()) {
            services = catalog.getService("miage24.store.".concat(name)).getResponse();
            LOGGER.log(Level.INFO, "Fallback services list: " + services);
        }
        Optional<String> fqdn = services.stream().flatMap(s -> s.getServiceTags().stream().filter(tag -> tag.startsWith("fqdn"))).findFirst();
        if (fqdn.isPresent()) {
            return fqdn.get();
        }
        return null;
    }

    private Registration buildRegistration() {
        return ImmutableRegistration.builder()
                .id(instanceId)
                .name(serviceName)
                .check(ImmutableRegCheck.builder().ttl(String.format("%ss", 30l)).deregisterCriticalServiceAfter(String.format("%sh",1)).build())
                .build();
    }
}
