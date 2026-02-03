package fr.jayblanc.mbyte.manager.core.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

@Entity
@NamedQueries({
    @NamedQuery(name = "Store.findByOwner", query = "SELECT s FROM Store s WHERE s.owner = :owner"),
    @NamedQuery(name = "Store.findAllByOwner", query = "SELECT s FROM Store s WHERE s.owner = :owner ORDER BY s.creationDate DESC"),
    @NamedQuery(name = "Store.findById", query = "SELECT s FROM Store s WHERE s.id = :id")
})
@Table(indexes = {
        @Index(name = "stores_idx", columnList = "owner")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Store {

    @Id
    private String id;
    private String type;
    private String owner;
    private String name;
    private long creationDate;
    private float usage;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Lob
    @jakarta.persistence.Basic(fetch = jakarta.persistence.FetchType.LAZY)
    private String log;
    private String location;

    public Store() {
    }

    public Store(String id, String type, String owner, String name) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.name = name;
        this.creationDate = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public float getUsage() {
        return usage;
    }

    public void setUsage(float usage) {
        this.usage = usage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public enum Status {
        PENDING,
        AVAILABLE,
        LOST
    }
}
