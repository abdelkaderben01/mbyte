package fr.jayblanc.mbyte.manager.store.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import fr.jayblanc.mbyte.manager.store.StoreProvider;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class DockerStoreProvider implements StoreProvider {

    private static final Logger LOGGER = Logger.getLogger(DockerStoreProvider.class.getName());
    private static final String NAME = "docker";

    private DockerClient client;

    @Inject DockerStoreProviderConfig config;

    @PostConstruct
    private void init() {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(config.server())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        client = DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> listApps() {
        LOGGER.log(Level.INFO, "Listing store apps");
        return client.listContainersCmd().exec().stream().map(container -> Arrays.stream(container.getNames()).collect(Collectors.joining()) + " / " + container.getImage()).collect(Collectors.toList());
    }

    @Override
    public String createApp(String id, String owner, String name) {
        LOGGER.log(Level.INFO, "Creating new store app for owner: {0}", owner);
        String networksList = client.listNetworksCmd().exec().stream().map(Network::getName).collect(Collectors.joining(", "));
        LOGGER.log(Level.INFO, "Existing networks: {0}", networksList);
        
        List<Network> networks = client.listNetworksCmd().withNameFilter("mbyte_network").exec();
        if (networks.isEmpty()) {
            LOGGER.log(Level.SEVERE, "MByte network not found, cannot create store app");
            throw new RuntimeException("MByte network not found");
        }
        if (networks.size() > 1) {
            LOGGER.log(Level.WARNING, "Multiple existing MByte networks found, using the first one");
        }
        
        Network network = networks.get(0);
        String containerName = id; // id is already in format "owner-storeIdShort"
        
        // Create the container with store image
        CreateContainerResponse container = client.createContainerCmd("abdelkader2020/store:25.1")
            .withName(containerName)
            .withEnv(
                    "QUARKUS_HTTP_PORT=8080",
                    "STORE_AUTH_OWNER=" + owner,
                    "STORE_TOPOLOGY_ENABLED=true",
                    "STORE_TOPOLOGY_HTTPS=false",
                    "STORE_TOPOLOGY_HOST=consul",
                    "STORE_TOPOLOGY_PORT=8500",
                    "STORE_TOPOLOGY_SERVICE_NAME=" + containerName,
                    "STORE_TOPOLOGY_SERVICE_PROTOCOL=http",
                    "STORE_TOPOLOGY_SERVICE_HOST=" + containerName + ".s.mbyte.fr",
                    "STORE_TOPOLOGY_SERVICE_PORT=80"
            )
            .withLabels(java.util.Map.of(
                    "traefik.enable", "true",
                    "traefik.docker.network", "mbyte_network",
                    "traefik.http.routers." + containerName + ".rule", "Host(`" + containerName + ".s.mbyte.fr`)",
                    "traefik.http.routers." + containerName + ".entrypoints", "http",
                    "traefik.http.services." + containerName + ".loadbalancer.server.port", "8080"
            ))
            .withNetworkMode("mbyte_network")
            .exec();
        
        LOGGER.log(Level.INFO, "Container created: {0}", container.getId());
        
        // Start the container
        client.startContainerCmd(container.getId()).exec();
        LOGGER.log(Level.INFO, "Container started: {0}", containerName);
        
        return "Container " + containerName + " created and started successfully";
    }

    @Override
    public void destroyApp(String id) throws StoreProviderException {
        LOGGER.log(Level.INFO, "Destroying store app: {0}", id);
        try {
            client.removeContainerCmd(id).withForce(true).exec();
            LOGGER.log(Level.INFO, "Container {0} removed successfully", id);
        } catch (com.github.dockerjava.api.exception.NotFoundException e) {
            LOGGER.log(Level.WARNING, "Container {0} not found, already removed", id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to remove container: " + id, e);
            throw new StoreProviderException("Failed to remove container: " + id, e);
        }
    }

}
